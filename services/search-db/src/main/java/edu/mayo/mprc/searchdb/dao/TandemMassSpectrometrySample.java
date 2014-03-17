package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.database.PersistableBase;
import org.joda.time.DateTime;

import java.io.File;

/**
 * Information about a particular mass spectrometry sample. The sample corresponds to a single .RAW or .mgf file.
 * <p/>
 * The useful information here is the total amount of MS2 spectra, and amount of identified MS2 spectra. This can be
 * utilized for normalization. Other metadata might include date of collection, instrument serial number, instrument
 * method, etc. The goal is to provide enough data for statistical normalization.
 *
 * @author Roman Zenka
 */
public class TandemMassSpectrometrySample extends PersistableBase {
	/**
	 * Link to the .RAW or .mgf file that was analyzed. It can contain a null or just a filename without the path in case the file could not be found.
	 */
	private File file;

	/**
	 * When the file got last modified. Can store an old value in case only the modification time changed, but the
	 * file contents remained identical.
	 */
	private DateTime lastModified;

	/**
	 * Number of survey or MS1 spectra.
	 */
	private int ms1Spectra;

	/**
	 * Number of MS2 spectra. This can be important for normalization.
	 */
	private int ms2Spectra;

	/**
	 * MS3, etc.. spectra. This can be useful to determine if MS3 or other complex techniques were used.
	 */
	private int ms3PlusSpectra;

	/**
	 * Name of the instrument.
	 */
	private String instrumentName;

	/**
	 * Serial number of the instrument.
	 */
	private String instrumentSerialNumber;

	/**
	 * Time when the instrument started to collect the data.
	 */
	private DateTime startTime;

	/**
	 * For how long have the data been collected - in seconds.
	 */
	private double runTimeInSeconds;

	/**
	 * Comment (what shows in Qual Browser when you select 'Comment' in the heading editor)
	 */
	private String comment;

	/**
	 * Information about the sample being processed. Who entered it, what vial it was in, what was the volume, etc.
	 */
	private String sampleInformation;

	/**
	 * Empty constructor for Hibernate.
	 */
	public TandemMassSpectrometrySample() {
	}

	public TandemMassSpectrometrySample(final File file, final DateTime lastModified, final int ms1Spectra, final int ms2Spectra, final int ms3PlusSpectra, final String instrumentName, final String instrumentSerialNumber, final DateTime startTime, final double runTimeInSeconds, final String comment, final String sampleInformation) {
		this.file = file;
		this.lastModified = lastModified;
		this.ms1Spectra = ms1Spectra;
		this.ms2Spectra = ms2Spectra;
		this.ms3PlusSpectra = ms3PlusSpectra;
		this.instrumentName = instrumentName;
		this.instrumentSerialNumber = instrumentSerialNumber;
		this.startTime = startTime;
		this.runTimeInSeconds = runTimeInSeconds;
		this.comment = comment;
		this.sampleInformation = sampleInformation;
	}

	public File getFile() {
		return file;
	}

	public void setFile(final File file) {
		this.file = file;
	}

	public DateTime getLastModified() {
		return lastModified;
	}

	public void setLastModified(final DateTime lastModified) {
		this.lastModified = lastModified;
	}

	public int getMs1Spectra() {
		return ms1Spectra;
	}

	public void setMs1Spectra(final int ms1Spectra) {
		this.ms1Spectra = ms1Spectra;
	}

	public int getMs2Spectra() {
		return ms2Spectra;
	}

	public void setMs2Spectra(final int ms2Spectra) {
		this.ms2Spectra = ms2Spectra;
	}

	public int getMs3PlusSpectra() {
		return ms3PlusSpectra;
	}

	public void setMs3PlusSpectra(final int ms3PlusSpectra) {
		this.ms3PlusSpectra = ms3PlusSpectra;
	}

	public String getInstrumentName() {
		return instrumentName;
	}

	public void setInstrumentName(final String instrumentName) {
		this.instrumentName = instrumentName;
	}

	public String getInstrumentSerialNumber() {
		return instrumentSerialNumber;
	}

	public void setInstrumentSerialNumber(final String instrumentSerialNumber) {
		this.instrumentSerialNumber = instrumentSerialNumber;
	}

	public DateTime getStartTime() {
		return startTime;
	}

	public void setStartTime(final DateTime startTime) {
		this.startTime = startTime;
	}

	public double getRunTimeInSeconds() {
		return runTimeInSeconds;
	}

	public void setRunTimeInSeconds(final double runTimeInSeconds) {
		this.runTimeInSeconds = runTimeInSeconds;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(final String comment) {
		this.comment = comment;
	}

	public String getSampleInformation() {
		return sampleInformation;
	}

	public void setSampleInformation(final String sampleInformation) {
		this.sampleInformation = sampleInformation;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !(o instanceof TandemMassSpectrometrySample)) {
			return false;
		}

		final TandemMassSpectrometrySample that = (TandemMassSpectrometrySample) o;

		if (getFile() != null ? !getFile().getAbsoluteFile().equals(that.getFile().getAbsoluteFile()) : that.getFile() != null) {
			return false;
		}
		if (getLastModified() != null ? !getLastModified().equals(that.getLastModified()) : that.getLastModified() != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result;
		result = getFile() != null ? getFile().hashCode() : 0;
		result = 31 * result + (getLastModified() != null ? getLastModified().hashCode() : 0);
		return result;
	}
}


