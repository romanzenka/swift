package edu.mayo.mprc.searchdb.builder;

import org.joda.time.DateTime;

/**
 * Information contained in .RAW.info.tsv files.
 *
 * @author Roman Zenka
 */
public class InfoFileData {
	private int ms1Spectra = 0;
	private int ms2Spectra = 0;
	private int ms3PlusSpectra = 0;
	private String instrumentName = "<unknown>";
	private String instrumentSerialNumber = "<unknown>";
	private DateTime startTime = null;
	private double runTimeInSeconds = 0.0;
	private String comment = "";
	private String sampleId = "";

	/**
	 * How many MS1 (survey) spectra are in the file.
	 */
	public int getMs1Spectra() {
		return ms1Spectra;
	}

	public void setMs1Spectra(final int ms1Spectra) {
		this.ms1Spectra = ms1Spectra;
	}

	/**
	 * How many MS2 (MS/MS) spectra are in the file.
	 */
	public int getMs2Spectra() {
		return ms2Spectra;
	}

	public void setMs2Spectra(final int ms2Spectra) {
		this.ms2Spectra = ms2Spectra;
	}

	/**
	 * How many ms3 and higher spectra are in the file.
	 */
	public int getMs3PlusSpectra() {
		return ms3PlusSpectra;
	}

	public void setMs3PlusSpectra(final int ms3PlusSpectra) {
		this.ms3PlusSpectra = ms3PlusSpectra;
	}

	/**
	 * Name of the instrument, e.g. "LTQ Orbitrap"
	 */
	public String getInstrumentName() {
		return instrumentName;
	}

	public void setInstrumentName(final String instrumentName) {
		this.instrumentName = instrumentName;
	}

	/**
	 * Serial number of the instrument, e.g. "52"
	 */
	public String getInstrumentSerialNumber() {
		return instrumentSerialNumber;
	}

	public void setInstrumentSerialNumber(final String instrumentSerialNumber) {
		this.instrumentSerialNumber = instrumentSerialNumber;
	}

	/**
	 * Time when the acquisition of the .RAW file started.
	 */
	public DateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(final DateTime startTime) {
		this.startTime = startTime;
	}

	/**
	 * Duration of the .RAW file acquisition.
	 */
	public double getRunTimeInSeconds() {
		return runTimeInSeconds;
	}

	public void setRunTimeInSeconds(final double runTimeInSeconds) {
		this.runTimeInSeconds = runTimeInSeconds;
	}

	/**
	 * Comment from the user.
	 */
	public String getComment() {
		return comment;
	}

	public void setComment(final String comment) {
		this.comment = comment;
	}

	/**
	 * Sample Id. This is something set by user at the acquisition time.
	 */
	public String getSampleId() {
		return sampleId;
	}

	public void setSampleId(final String sampleId) {
		this.sampleId = sampleId;
	}
}
