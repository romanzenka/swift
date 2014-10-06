package edu.mayo.mprc.qa;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.msmseval.MSMSEvalOutputReader;
import edu.mayo.mprc.myrimatch.MyriMatchPepXmlReader;
import edu.mayo.mprc.peaklist.PeakListReader;
import edu.mayo.mprc.peaklist.PeakListReaders;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldQaSpectraReader;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;
import org.proteomecommons.io.mgf.MascotGenericFormatPeakList;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Joins information about spectra coming from .mgf, Scaffold, msmsEval and raw Dumper.
 */
@Component("spectrumInfoJoiner")
public final class SpectrumInfoJoiner {

	private static final Pattern SPECTRUM_FROM_TITLE = Pattern.compile(".*\\(([^)]*\\d\\.dta)\\)\\s*$");
	private static final Logger LOGGER = Logger.getLogger(SpectrumInfoJoiner.class);

	private PeakListReaders readers;

	private SpectrumInfoJoiner() {
	}

	public SpectrumInfoJoiner(final PeakListReaders readers) {
		this.readers = readers;
	}

	/**
	 * Generates tab separated value file with following column headers:
	 * <p/>
	 * <pre>MGF File Name -- MGF Spectrum Name</pre> (optional)
	 * <pre>Scan Id</pre>
	 * <pre>Mz -- Z</pre> (from the .mgf file)
	 * <pre>Scaffold headers</pre>
	 * <pre>Raw File</pre> - only if specified
	 * <pre>rawDump headers</pre>
	 * <pre>msmsEval headers</pre>
	 *
	 * @param inputFile      Input .mgf or mzML file
	 * @param scaffold       Access to information about Scaffold results
	 * @param rawDumpReader  Access to information about .RAW spectra
	 * @param msmsEvalReader Access to information from msmsEval
	 * @param sink           The joined spectrum data will be written to this sink.
	 * @param rawFileName    Name of the RAW file that all this output originated from
	 * @return Number of rows in output file, not including the column headers.
	 */
	public int joinSpectrumData(final File inputFile, final ScaffoldQaSpectraReader scaffold,
	                            final RawDumpReader rawDumpReader, final MSMSEvalOutputReader msmsEvalReader,
	                            final MyriMatchPepXmlReader myrimatchReader, final SpectrumInfoSink sink,
	                            final UvDataReader uvDataReader,
	                            final String rawFileName) {
		int rowCount = 0;
		final Map<String, Spectrum> mgfSpectrumMap = new HashMap<String, Spectrum>();

		try {
			getSourceInformation(inputFile, mgfSpectrumMap);
			addScaffoldInformation(scaffold, mgfSpectrumMap);

			sink.initialize(scaffold, rawDumpReader, msmsEvalReader, myrimatchReader, uvDataReader, rawFileName);

			if (!rawDumpReader.emptyFile()) {
				// We have a raw output file, use it to drive the output
				final Map<Long, List<Spectrum>> mgfMapByScanId = indexMgfSpectraByScanId(mgfSpectrumMap);
				final String scaffoldVersion = scaffold == null ? null : scaffold.getScaffoldVersion();
				for (final String scanIdStr : rawDumpReader) {
					final long scanId = Long.parseLong(scanIdStr);
					final List<Spectrum> matchingSpectra = mgfMapByScanId.get(scanId);
					if (matchingSpectra == null) {
						writeSpectrumLine(
								sink,
								msmsEvalReader,
								rawDumpReader,
								myrimatchReader,
								uvDataReader,
								scanIdStr,
								null,
								scaffold != null ? scaffold.getEmptyLine() : "",
								scaffoldVersion);
						rowCount++;
					} else {
						for (final Spectrum spectrum : matchingSpectra) {
							rowCount = writeMgfWithScaffoldInfos(
									scaffold,
									sink,
									rowCount,
									msmsEvalReader,
									rawDumpReader,
									myrimatchReader,
									uvDataReader,
									scanIdStr,
									spectrum);
						}
					}
				}
			} else {
				// No raw data, drive the output by mgf spectra
				//Output gather information for output file.
				for (final Spectrum spectrum : mgfSpectrumMap.values()) {
					rowCount = writeMgfWithScaffoldInfos(
							scaffold,
							sink,
							rowCount,
							msmsEvalReader,
							rawDumpReader,
							myrimatchReader,
							uvDataReader,
							String.valueOf(spectrum.getScanId()),
							spectrum);
				}
			}
		} catch (IOException e) {
			throw new MprcException("Failed to join QA spectra and write into [" + sink.getDescription() + "]", e);
		} finally {
			FileUtilities.closeQuietly(sink);
		}
		return rowCount;
	}

	private static int writeMgfWithScaffoldInfos(final ScaffoldQaSpectraReader scaffold, final SpectrumInfoSink sink, int rowCount,
	                                             final MSMSEvalOutputReader msmsEvalReader, final RawDumpReader rawDumpReader, final MyriMatchPepXmlReader myrimatchReader,
	                                             final UvDataReader uvDataReader,
	                                             final String scanId, final Spectrum spectrum) throws IOException {
		final String scaffoldVersion = scaffold == null ? null : scaffold.getScaffoldVersion();
		if (spectrum.getScaffoldInfos() == null || spectrum.getScaffoldInfos().isEmpty()) {
			writeSpectrumLine(
					sink,
					msmsEvalReader,
					rawDumpReader,
					myrimatchReader,
					uvDataReader,
					scanId,
					spectrum,
					scaffold != null ? scaffold.getEmptyLine() : null,
					scaffoldVersion);
			rowCount++;
		} else {
			for (final String scaffoldInfo : spectrum.getScaffoldInfos()) {
				writeSpectrumLine(
						sink,
						msmsEvalReader,
						rawDumpReader,
						myrimatchReader,
						uvDataReader,
						scanId,
						spectrum,
						scaffoldInfo,
						scaffoldVersion);
				rowCount++;
			}
		}
		return rowCount;
	}

	private static void writeSpectrumLine(
			final SpectrumInfoSink sink,
			final MSMSEvalOutputReader msmsEvalReader,
			final RawDumpReader rawDumpReader,
			final MyriMatchPepXmlReader myrimatchReader,
			final UvDataReader uvDataReader,
			final String scanIdStr,
			final Spectrum spectrum,
			final String scaffoldInfo,
			final String scaffoldVersion) throws IOException {
		// msmsEval part (even if no msmsEval data is present, we produce a consistent format)
		final String msmsEvalData = msmsEvalReader.getLineForKey(scanIdStr);
		final String rawDumpReaderData = rawDumpReader.getLineForKey(scanIdStr);
		final String myrimatchReaderData;
		if (myrimatchReader != null && spectrum != null) {
			myrimatchReaderData = myrimatchReader.getLineForKey(String.valueOf(spectrum.getSpectrumNumber()));
		} else {
			myrimatchReaderData = null;
		}
		final String uvDataReaderData;
		if (uvDataReader != null && rawDumpReader!=null && rawDumpReaderData != null) {
			final String retentionTime = rawDumpReader.getRtFromLine(rawDumpReaderData);
			uvDataReaderData = uvDataReader.getLineForKey(String.valueOf(retentionTime));
		} else {
			uvDataReaderData = null;
		}

		sink.writeSpectrumInfo(scanIdStr, spectrum, scaffoldInfo, scaffoldVersion, msmsEvalData, rawDumpReaderData, myrimatchReaderData, uvDataReaderData);
	}

	private static Map<Long, List<Spectrum>> indexMgfSpectraByScanId(final Map<String, Spectrum> mgfSpectrumMap) {
		final Map<Long, List<Spectrum>> mgfSpectraByScan = new HashMap<Long, List<Spectrum>>();
		for (final Spectrum spectrum : mgfSpectrumMap.values()) {

			if (mgfSpectraByScan.containsKey(spectrum.getScanId())) {
				mgfSpectraByScan.get(spectrum.getScanId()).add(spectrum);
			} else {
				final List<Spectrum> list = new ArrayList<Spectrum>(2);
				list.add(spectrum);
				mgfSpectraByScan.put(spectrum.getScanId(), list);
			}
		}
		return mgfSpectraByScan;
	}

	/**
	 * Extract information from a Scaffold file into String -> {@link Spectrum} map.
	 *
	 * @param scaffoldSpectraInfo Parsed Scaffold spectra output
	 * @param mgfSpectrumMap      Map from spectrum name (when usingSpectrumNameAsKey is set) or from spectrum ID to Ms2Data
	 */
	public static void addScaffoldInformation(final ScaffoldQaSpectraReader scaffoldSpectraInfo, final Map<String, Spectrum> mgfSpectrumMap) {

		LOGGER.debug("Matching with scaffold spectra file.");
		for (final String spectrumName : scaffoldSpectraInfo) {
			final String scaffoldInfo = scaffoldSpectraInfo.getLineForKey(spectrumName);
			final Spectrum spectrum = mgfSpectrumMap.get(getSpectrum(spectrumName));
			if (spectrum != null) {
				spectrum.addScaffoldInfo(scaffoldInfo);
			}
		}
		LOGGER.debug("Done matching with scaffold spectra file.");
	}

	/**
	 * Extract information about MS/MS spectra from a list of mgf/mzML files
	 *
	 * @param inputFile   input file to extract information from
	 * @param spectrumMap Map from either spectrum name or scan id to information about MS2 spectrum. The map is being created from scratch, existing values will be overwritten.
	 */
	public void getSourceInformation(final File inputFile, final Map<String, Spectrum> spectrumMap) {
		Spectrum spectrum = null;
		PeakListReader peakListReader = null;
		MascotGenericFormatPeakList peakList = null;
		long spectrumNumber = 0;

		try {
			//Get basic mgf spectrum information from mgf file.

			final String mgfPath = inputFile.getAbsolutePath();

			LOGGER.debug("Reading source file [" + mgfPath + "].");

			peakListReader = readers.createReader(inputFile, false);

			while ((peakList = peakListReader.nextPeakList()) != null) {
				spectrum = new Spectrum(
						getSpectrum(peakList.getTitle()),
						getMz(peakList.getPepmass()),
						getCharge(peakList.getCharge()),
						getScanId(peakList.getTitle()),
						mgfPath,
						spectrumNumber);
				spectrumNumber++;

				spectrumMap.put(spectrum.getSpectrumName(), spectrum);
			}

		} finally {
			FileUtilities.closeQuietly(peakListReader);
		}

		LOGGER.debug("Done reading input files.");
	}

	public static long getScanId(final String spectrum) {
		String str;
		if (spectrum.contains(".dta)")) {
			str = spectrum.substring(0, spectrum.lastIndexOf(".dta)"));
		} else {
			str = spectrum;
		}
		str = str.substring(0, str.lastIndexOf('.'));
		return Long.parseLong(str.substring(str.lastIndexOf('.') + 1).trim());
	}

	/**
	 * Return either the .dta portion of spectrum title, if this is missing, return full title.
	 *
	 * @param title Title to extract spectrum info from
	 * @return Extracted spectrum name in form {@code file.scan1.scan2.charge.dta}
	 */
	public static String getSpectrum(final String title) {
		final Matcher matcher = SPECTRUM_FROM_TITLE.matcher(title);
		if (matcher.matches()) {
			return matcher.group(1);
		} else {
			if (title.contains(".dta")) {
				return title;
			} else {
				return title + ".dta";
			}
		}
	}

	private static int getCharge(final String charge) {
		if (charge == null) {
			return 0;
		}
		final String str = charge.substring(charge.indexOf('=') + 1).trim();
		return Integer.parseInt(str.substring(0, str.length() - (str.endsWith("+") ? 1 : 0)).trim());
	}

	private static double getMz(final String mz) {
		if (mz == null) {
			return 0.0;
		}
		return Double.parseDouble(mz.substring(mz.indexOf('=') + 1).trim());
	}

	public PeakListReaders getReaders() {
		return readers;
	}

	@Resource(name = "peakListReaders")
	public void setReaders(final PeakListReaders readers) {
		this.readers = readers;
	}
}