package edu.mayo.mprc.qa;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.io.mgf.MGFPeakListReader;
import edu.mayo.mprc.msmseval.MSMSEvalOutputReader;
import edu.mayo.mprc.myrimatch.MyriMatchPepXmlReader;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldQaSpectraReader;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;
import org.proteomecommons.io.mgf.MascotGenericFormatPeakList;

import java.io.File;
import java.io.FileWriter;
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
public final class SpectrumInfoJoiner {

	private static final Pattern SPECTRUM_FROM_TITLE = Pattern.compile(".*\\(([^)]*\\d\\.dta)\\)\\s*$");
	private static final Logger LOGGER = Logger.getLogger(SpectrumInfoJoiner.class);

	private SpectrumInfoJoiner() {
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
	 * @param mgfFile        Input .mgf file
	 * @param scaffold       Access to information about Scaffold results
	 * @param rawDumpReader  Access to information about .RAW spectra
	 * @param msmsEvalReader Access to information from msmsEval
	 * @param outputFile     A file that will contain information about every spectrum in the .mgf files, enriched by Scaffold identifications
	 * @param rawFileName
	 * @return Number of rows in output file, not including the column headers.
	 */
	public static int joinSpectrumData(final File mgfFile, final ScaffoldQaSpectraReader scaffold, final RawDumpReader rawDumpReader, final MSMSEvalOutputReader msmsEvalReader, final MyriMatchPepXmlReader myrimatchReader, final File outputFile, final String rawFileName) {
		FileWriter fileWriter = null;

		int rowCount = 0;
		final Map<String, MgfSpectrum> mgfSpectrumMap = new HashMap<String, MgfSpectrum>();

		try {
			fileWriter = new FileWriter(outputFile);

			getMgfInformation(mgfFile, mgfSpectrumMap, true);
			addScaffoldInformation(scaffold, mgfSpectrumMap, true);

			fileWriter.write("Scan Id\tMz\tZ\tMgf File Name");
			if (scaffold != null) {
				fileWriter.write('\t');
				fileWriter.write(scaffold.getHeaderLine());
				fileWriter.write('\t');
				fileWriter.write("Scaffold version");
			}
			fileWriter.write('\t');
			fileWriter.write(msmsEvalReader.getHeaderLine());
			fileWriter.write('\t');
			if (rawFileName != null) {
				fileWriter.write("Raw File\t");
			}
			fileWriter.write(rawDumpReader.getHeaderLine());
			if (myrimatchReader != null) {
				fileWriter.write('\t');
				fileWriter.write(myrimatchReader.getHeaderLine());
			}
			fileWriter.write("\n");

			if (!rawDumpReader.emptyFile()) {
				// We have a raw output file, use it to drive the output
				final Map<Long, List<MgfSpectrum>> mgfMapByScanId = indexMgfSpectraByScanId(mgfSpectrumMap);
				final String scaffoldVersion = scaffold == null ? null : scaffold.getScaffoldVersion();
				for (final String scanIdStr : rawDumpReader) {
					final long scanId = Long.parseLong(scanIdStr);
					final List<MgfSpectrum> matchingSpectra = mgfMapByScanId.get(scanId);
					if (matchingSpectra == null) {
						writeSpectrumLine(
								fileWriter,
								msmsEvalReader,
								rawDumpReader,
								myrimatchReader,
								scanIdStr,
								null,
								scaffold != null ? scaffold.getEmptyLine() : "", rawFileName,
								scaffoldVersion);
						rowCount++;
					} else {
						for (final MgfSpectrum mgfSpectrum : matchingSpectra) {
							rowCount = writeMgfWithScaffoldInfos(
									scaffold,
									fileWriter,
									rowCount,
									msmsEvalReader,
									rawDumpReader,
									myrimatchReader,
									scanIdStr,
									mgfSpectrum,
									rawFileName);
						}
					}
				}
			} else {
				// No raw data, drive the output by mgf spectra
				//Output gather information for output file.
				for (final MgfSpectrum mgfSpectrum : mgfSpectrumMap.values()) {
					rowCount = writeMgfWithScaffoldInfos(
							scaffold,
							fileWriter,
							rowCount,
							msmsEvalReader,
							rawDumpReader,
							myrimatchReader,
							String.valueOf(mgfSpectrum.getScanId()),
							mgfSpectrum,
							rawFileName);
				}
			}
		} catch (IOException e) {
			throw new MprcException("Failed to generated QA output file [" + outputFile.getAbsolutePath() + "]", e);
		} finally {
			FileUtilities.closeQuietly(fileWriter);
		}
		return rowCount;
	}

	private static int writeMgfWithScaffoldInfos(final ScaffoldQaSpectraReader scaffold, final FileWriter fileWriter, int rowCount, final MSMSEvalOutputReader msmsEvalReader, final RawDumpReader rawDumpReader, final MyriMatchPepXmlReader myrimatchReader, final String scanId, final MgfSpectrum mgfSpectrum, final String rawFileName) throws IOException {
		final String scaffoldVersion = scaffold == null ? null : scaffold.getScaffoldVersion();
		if (mgfSpectrum.getScaffoldInfos() == null || mgfSpectrum.getScaffoldInfos().size() == 0) {
			writeSpectrumLine(
					fileWriter,
					msmsEvalReader,
					rawDumpReader,
					myrimatchReader,
					scanId,
					mgfSpectrum,
					scaffold != null ? scaffold.getEmptyLine() : null,
					rawFileName,
					scaffoldVersion);
			rowCount++;
		} else {
			for (final String scaffoldInfo : mgfSpectrum.getScaffoldInfos()) {
				writeSpectrumLine(
						fileWriter,
						msmsEvalReader,
						rawDumpReader,
						myrimatchReader,
						scanId,
						mgfSpectrum,
						scaffoldInfo,
						rawFileName,
						scaffoldVersion);
				rowCount++;
			}
		}
		return rowCount;
	}

	private static void writeSpectrumLine(
			final FileWriter fileWriter,
			final MSMSEvalOutputReader msmsEvalReader,
			final RawDumpReader rawDumpReader,
			final MyriMatchPepXmlReader myrimatchReader,
			final String scanIdStr,
			final MgfSpectrum mgfSpectrum,
			final String scaffoldInfo,
			final String rawFileName,
			final String scaffoldVersion) throws IOException {
		fileWriter.write(scanIdStr
				+ "\t" + (mgfSpectrum != null ? mgfSpectrum.getMgfMz() : "")
				+ "\t" + (mgfSpectrum != null ? mgfSpectrum.getMgfCharge() : "")
				+ "\t" + (mgfSpectrum != null ? mgfSpectrum.getMgfFileName() : "")
				+ (scaffoldInfo != null ? ("\t" + scaffoldInfo + "\t" + scaffoldVersion) : "")
				+ "\t");

		// msmsEval part (even if no msmsEval data is present, we produce a consistent format)
		fileWriter.write(msmsEvalReader.getLineForKey(scanIdStr));
		fileWriter.write('\t');
		if (rawFileName != null) {
			fileWriter.write(rawFileName);
			fileWriter.write('\t');
		}
		fileWriter.write(rawDumpReader.getLineForKey(scanIdStr));
		if (myrimatchReader != null && mgfSpectrum != null) {
			fileWriter.write('\t');
			fileWriter.write(myrimatchReader.getLineForKey(String.valueOf(mgfSpectrum.getSpectrumNumber())));
		}
		fileWriter.write("\n");
	}

	private static Map<Long, List<MgfSpectrum>> indexMgfSpectraByScanId(final Map<String, MgfSpectrum> mgfSpectrumMap) {
		final Map<Long, List<MgfSpectrum>> mgfSpectraByScan = new HashMap<Long, List<MgfSpectrum>>();
		for (final MgfSpectrum mgfSpectrum : mgfSpectrumMap.values()) {

			if (mgfSpectraByScan.containsKey(mgfSpectrum.getScanId())) {
				mgfSpectraByScan.get(mgfSpectrum.getScanId()).add(mgfSpectrum);
			} else {
				final List<MgfSpectrum> list = new ArrayList<MgfSpectrum>(2);
				list.add(mgfSpectrum);
				mgfSpectraByScan.put(mgfSpectrum.getScanId(), list);
			}
		}
		return mgfSpectraByScan;
	}

	/**
	 * Extract information from a Scaffold file into String -> {@link edu.mayo.mprc.qa.MgfSpectrum} map.
	 *
	 * @param scaffoldSpectraInfo Parsed Scaffold spectra output
	 * @param mgfSpectrumMap      Map from spectrum name (when usingSpectrumNameAsKey is set) or from spectrum ID to Ms2Data
	 * @param spectrumNameAsKey   If true, the map is indexed by full spectrum name, not just spectrum ID.
	 */
	public static void addScaffoldInformation(final ScaffoldQaSpectraReader scaffoldSpectraInfo, final Map<String, MgfSpectrum> mgfSpectrumMap, final boolean spectrumNameAsKey) {

		LOGGER.debug("Matching with scaffold spectra file.");
		for (final String spectrumName : scaffoldSpectraInfo) {
			final String scaffoldInfo = scaffoldSpectraInfo.getLineForKey(spectrumName);
			final MgfSpectrum mgfSpectrum = mgfSpectrumMap.get(spectrumNameAsKey ? getSpectrum(spectrumName) : Long.toString(getScanIdFromScaffoldSpectrum(spectrumName)));
			if (mgfSpectrum != null) {
				mgfSpectrum.addScaffoldInfo(scaffoldInfo);
			}
		}
		LOGGER.debug("Done matching with scaffold spectra file.");
	}

	/**
	 * Extract information about MS/MS spectra from a list of .mgf files
	 *
	 * @param mgfFile           MGF file to extract information from
	 * @param mgfSpectrumMap    Map from either spectrum name or scan id to information about MS2 spectrum. The map is being created from scratch, existing values will be overwritten.
	 * @param spectrumNameAsKey If true, the map is indexed by full spectrum name, otherwise it is indexed by scan id
	 */
	public static void getMgfInformation(final File mgfFile, final Map<String, MgfSpectrum> mgfSpectrumMap, final boolean spectrumNameAsKey) {
		MgfSpectrum mgfSpectrum = null;
		MGFPeakListReader peakListReader = null;
		MascotGenericFormatPeakList peakList = null;
		long spectrumNumber = 0;

		try {
			//Get basic mgf spectrum information from mgf file.

			final String mgfPath = mgfFile.getAbsolutePath();

			LOGGER.debug("Reading mgf file [" + mgfPath + "].");

			peakListReader = new MGFPeakListReader(mgfFile);
			peakListReader.setReadPeaks(false);

			while ((peakList = peakListReader.nextPeakList()) != null) {
				mgfSpectrum = new MgfSpectrum(
						getSpectrum(peakList.getTitle()),
						getMz(peakList.getPepmass()),
						getCharge(peakList.getCharge()),
						getScanId(peakList.getTitle()),
						mgfPath,
						spectrumNumber);
				spectrumNumber++;

				mgfSpectrumMap.put(spectrumNameAsKey ? mgfSpectrum.getSpectrumName() : Long.toString(mgfSpectrum.getScanId()), mgfSpectrum);
			}

		} finally {
			FileUtilities.closeQuietly(peakListReader);
		}

		LOGGER.debug("Done reading mgf files.");
	}

	private static long getScanId(final String spectrum) {
		String str = spectrum.substring(0, spectrum.lastIndexOf(".dta)"));
		str = str.substring(0, str.lastIndexOf('.'));
		return Long.parseLong(str.substring(str.lastIndexOf('.') + 1).trim());
	}

	private static long getScanIdFromScaffoldSpectrum(final String spectrum) {
		String str = spectrum.substring(0, spectrum.lastIndexOf('.'));
		str = str.substring(0, str.lastIndexOf('.'));
		return Long.parseLong(str.substring(str.lastIndexOf('.') + 1).trim());
	}

	/**
	 * Return either the .dta portion of spectrum title, if this is missing, return full title.
	 *
	 * @param title Title to extract spectrum info from
	 * @return Extracted spectrum name in form <code>file.scan1.scan2.charge.dta</code>
	 */
	public static String getSpectrum(final String title) {
		final Matcher matcher = SPECTRUM_FROM_TITLE.matcher(title);
		if (matcher.matches()) {
			return matcher.group(1);
		} else {
			return title;
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
}