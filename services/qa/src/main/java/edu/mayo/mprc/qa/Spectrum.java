package edu.mayo.mprc.qa;

import java.util.LinkedList;

/**
 * MS/MS specific data. This corresponds to an entry in a Mgf or mzML file.
 */
public final class Spectrum {
	private String spectrumName;
	private double mz;
	private int charge;
	private long scanId;
	private String inputFileName;
	private long spectrumNumber;
	private LinkedList<String> scaffoldInfos;

	public Spectrum(final String spectrumName) {
		this.spectrumName = spectrumName;

		scaffoldInfos = new LinkedList<String>();
	}

	public Spectrum(final String spectrumName, final double mz, final int charge, final long scanId, final String inputFileName, final long spectrumNumber) {
		this(spectrumName);
		this.mz = mz;
		this.charge = charge;
		this.scanId = scanId;
		this.inputFileName = inputFileName;
		this.spectrumNumber = spectrumNumber;
	}

	public String getInputFileName() {
		return inputFileName;
	}

	public void setInputFileName(final String inputFileName) {
		this.inputFileName = inputFileName;
	}

	/**
	 * @return Spectra within .mgf file are numbered, starting with spectrum #0. This returns the number of the spectrum.
	 */
	public long getSpectrumNumber() {
		return spectrumNumber;
	}

	public void setSpectrumNumber(final long spectrumNumber) {
		this.spectrumNumber = spectrumNumber;
	}

	public long getScanId() {
		return scanId;
	}

	public void setScanId(final long scanId) {
		this.scanId = scanId;
	}

	public void addScaffoldInfo(final String scaffoldInfo) {
		scaffoldInfos.add(scaffoldInfo);
	}

	public LinkedList<String> getScaffoldInfos() {
		return scaffoldInfos;
	}

	public String getSpectrumName() {
		return spectrumName;
	}

	public double getMz() {
		return mz;
	}

	public void setMz(final double mz) {
		this.mz = mz;
	}

	public int getCharge() {
		return charge;
	}

	public void setCharge(final int charge) {
		this.charge = charge;
	}
}
