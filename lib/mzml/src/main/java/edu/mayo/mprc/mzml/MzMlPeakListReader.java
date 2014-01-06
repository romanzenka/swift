package edu.mayo.mprc.mzml;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.peaklist.PeakList;
import edu.mayo.mprc.peaklist.PeakListReader;
import org.proteomecommons.io.GenericPeak;
import org.proteomecommons.io.Peak;
import uk.ac.ebi.jmzml.model.mzml.*;
import uk.ac.ebi.jmzml.xml.io.MzMLObjectIterator;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Reads MS2 spectra from given mzML file (ignoring MS1 and MS3+).
 * <p/>
 * Creates {@link PeakList} objects that should be as similar as possible to those returned by the .mgf reader.
 *
 * @author Roman Zenka
 */
public final class MzMlPeakListReader implements PeakListReader {
	public static final int MS2_SPECTRUM = 2;

	// Stages of ms achieved in a multi stage mass spectrometry experiment.
	private static final String MS_LEVEL = "MS:1000511";

	// Mass-to-charge ratio of an selected ion.
	private static final String SELECTED_ION_MZ = "MS:1000744";

	// Charge state
	private static final String CHARGE_STATE = "MS:1000041";
	private static final String POSSIBLE_CHARGE_STATE = "MS:1000633";

	public static final String MZ_ARRAY = "MS:1000514";

	public static final String INTENSITY_ARRAY = "MS:1000515";

	private boolean readPeaks;
	private final File file;
	private final MzMLUnmarshaller reader;
	private final MzMLObjectIterator<Spectrum> iterator;
	private Spectrum spectrum;
	// Peak lists we would like to report in the future
	private Queue<PeakList> parsedPeakLists = new ArrayDeque<PeakList>(10);

	public MzMlPeakListReader(final File file, final boolean readPeaks) {
		this.file = file;
		this.readPeaks = readPeaks;
		reader = new MzMLUnmarshaller(file);
		iterator = reader.unmarshalCollectionFromXpath("/indexedmzML/mzML/run/spectrumList/spectrum", Spectrum.class);
	}

	public String getCvParamValue(final List<CVParam> cvParams, final String accession) {
		for (final CVParam param : cvParams) {
			if (param.getAccession().equals(accession)) {
				return param.getValue();
			}
		}
		return null;
	}

	private void parsePeakLists() {
		while (iterator.hasNext()) {
			spectrum = iterator.next();
			final String msLevelString = getCvParamValue(spectrum.getCvParam(), MS_LEVEL);
			if (msLevelString == null) {
				throw new MprcException(spectrumException(" does not specify ms-level (" + MS_LEVEL + ")"));
			}
			final int msLevel = Integer.parseInt(msLevelString);

			final String spectrumTitle = spectrum.getId();

			if (msLevel == MS2_SPECTRUM) {
				if (spectrum.getPrecursorList().getCount() <= 0) {
					throw new MprcException(spectrumException(" has zero listed precursors"));
				}
				final boolean multiplePrecursors = 1 < spectrum.getPrecursorList().getCount();
				int precursorId = 1;
				for (final Precursor precursor : spectrum.getPrecursorList().getPrecursor()) {
					if (precursor.getSelectedIonList().getCount() <= 0) {
						throw new MprcException(spectrumException(" does not specify any precursor ions"));
					}
					final ParamGroup precursorIon = precursor.getSelectedIonList().getSelectedIon().get(0);
					final String precursorMz = getCvParamValue(precursorIon.getCvParam(), SELECTED_ION_MZ);
					final String chargeState = getCvParamValue(precursorIon.getCvParam(), CHARGE_STATE);

					Peak[] peaks = null;
					if (isReadPeaks()) {
						peaks = parsePeaks(spectrum);
					}

					if (chargeState == null) {
						boolean noChargeStates = true;
						for (final CVParam param : precursorIon.getCvParam()) {
							if (POSSIBLE_CHARGE_STATE.equals(param.getAccession())) {
								final String charge = param.getValue();
								queuePeakList(getSpectrumTitle(spectrumTitle, multiplePrecursors, precursorId, true, charge), precursorMz, charge, peaks, msLevel);
								noChargeStates = false;
							}
						}
						if (noChargeStates) {
							throw new MprcException(spectrumException(" precursor charge state not specified"));
						}
					} else {
						queuePeakList(getSpectrumTitle(spectrumTitle, multiplePrecursors, precursorId, false, chargeState), precursorMz, chargeState, peaks, msLevel);
					}
					precursorId++;
				}
			}
		}

	}

	private String getSpectrumTitle(final String spectrumTitle, final boolean multiplePrecursors, final int precursorId, final boolean multipleCharges, final String charge) {
		return spectrumTitle + (multiplePrecursors ? " precursor=" + precursorId : "") + (multipleCharges ? " charge=" + charge : "");
	}

	private void queuePeakList(final String title, final String precursorMz, final String chargeState, final Peak[] peaks, final int msLevel) {
		final PeakList peakList = new PeakList();
		peakList.setTitle(title);
		peakList.setTandemCount(msLevel);
		peakList.setPepmass("PEPMASS=" + precursorMz);
		peakList.setCharge("CHARGE=" + chargeState + "+");
		if (peaks != null) {
			peakList.setPeaks(peaks);
		}
		parsedPeakLists.add(peakList);
	}

	private Peak[] parsePeaks(final Spectrum spectrum) {
		double[] mz = null;
		double[] intensity = null;
		for (final BinaryDataArray dataArray : spectrum.getBinaryDataArrayList().getBinaryDataArray()) {
			for (final CVParam param : dataArray.getCvParam()) {
				if (MZ_ARRAY.equals(param.getAccession())) {
					mz = toDoubleArray(dataArray);
					if (intensity != null) {
						break;
					}
				} else if (INTENSITY_ARRAY.equals(param.getAccession())) {
					intensity = toDoubleArray(dataArray);
					if (mz != null) {
						break;
					}
				}
			}
		}

		if (mz == null) {
			throw new MprcException(spectrumException(" m/z data array missing"));
		}

		if (intensity == null) {
			throw new MprcException(spectrumException(" intensity data array missing"));
		}

		if (mz.length != intensity.length) {
			throw new MprcException(spectrumException(" m/z and intensity arrays have different length"));
		}

		final Peak[] peaks = new Peak[mz.length];
		for (int i = 0; i < mz.length; i++) {
			final GenericPeak peak = new GenericPeak();
			peak.setMassOverCharge(mz[i]);
			peak.setIntensity(intensity[i]);
			peaks[i] = peak;
		}
		return peaks;
	}

	@Override
	public PeakList nextPeakList() {
		if (parsedPeakLists.size() == 0) {
			parsePeakLists();
		}
		if (parsedPeakLists.size() > 0) {
			return parsedPeakLists.remove();
		}
		return null;
	}

	private String spectrumException(final String message) {
		return "MzML file [" + file.getAbsolutePath() + "] spectrum [" + spectrum.getId() + "]" + message;
	}

	private double[] toDoubleArray(final BinaryDataArray dataArray) {
		final double[] array;
		if (dataArray.getArrayLength() != null) {
			array = new double[dataArray.getArrayLength()];
			int i = 0;
			for (final Number number : dataArray.getBinaryDataAsNumberArray()) {
				array[i] = number.doubleValue();
				i++;
			}
		} else {
			final ArrayList<Double> arrayList = new ArrayList<Double>(1000);
			int i = 0;
			for (final Number number : dataArray.getBinaryDataAsNumberArray()) {
				arrayList.add(number.doubleValue());
				i++;
			}
			array = new double[arrayList.size()];
			for (int j = 0; j < array.length; j++) {
				array[j] = arrayList.get(j);
			}
		}
		return array;
	}

	@Override
	public boolean isReadPeaks() {
		return readPeaks;
	}

	@Override
	public void setReadPeaks(final boolean readPeaks) {
		this.readPeaks = readPeaks;
	}

	@Override
	public void close() throws IOException {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
