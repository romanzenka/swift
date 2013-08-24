package edu.mayo.mprc.scafml;


import edu.mayo.mprc.daemon.files.FileHolder;
import edu.mayo.mprc.utilities.xml.XMLUtilities;

/**
 * Represents the parsed contents of the scaffold definition XML file
 * This represents search configuration information for each of the search/analysis tools, ie
 * Mascot, XTandem, Sequest, Scaffold, etc.
 * <p/>
 * The document and all its parts are designated as FileHolder to enable sending the data as a part of work packet across the network.
 */
public final class ScafmlScaffold extends FileHolder {
	private static final long serialVersionUID = -8591840319884307949L;
	/**
	 * Definition of the experiment
	 */
	private ScafmlExperiment experiment;
	private int versionMajor = 2;
	private int versionMinor = 6;

	/**
	 * Represents the parsed contents of the scafml template XML file
	 */
	public ScafmlScaffold() {
	}

	public void setExperiment(final ScafmlExperiment experiment) {
		this.experiment = experiment;
	}

	public ScafmlExperiment getExperiment() {
		return experiment;
	}

	public int getVersionMajor() {
		return versionMajor;
	}

	public void setVersionMajor(final int versionMajor) {
		this.versionMajor = versionMajor;
	}

	public int getVersionMinor() {
		return versionMinor;
	}

	public void setVersionMinor(final int versionMinor) {
		this.versionMinor = versionMinor;
	}

	/**
	 * @return The time when the newest input file was modified.
	 */
	public long getNewestInputTime() {
		long newestInput = Long.MIN_VALUE;
		for (ScafmlFastaDatabase database : getExperiment().getDatabases()) {
			final long databaseModified = database.getDatabase().lastModified();
			if (databaseModified > newestInput) {
				newestInput = databaseModified;
			}
		}
		for (ScafmlBiologicalSample sample : getExperiment().getBiologicalSamples()) {
			for (ScafmlInputFile inputFile : sample.getInputFiles()) {
				final long inputFileModified = inputFile.getFile().lastModified();
				if (inputFileModified > newestInput) {
					newestInput = inputFileModified;
				}
			}
		}
		return newestInput;
	}

	/**
	 * @return .scafml file as a string.
	 */
	public String getDocument() {
		final StringBuilder result = new StringBuilder(4096);
		result.append(XMLUtilities.XML_START)
				.append("\n")
				.append("<" + "Scaffold" + XMLUtilities.wrapatt("version", "" + versionMajor + "." + versionMinor) + ">\n");

		final ScafmlExperiment e = getExperiment();
		e.appendToDocument(result, "\t", this);

		result.append("</" + "Scaffold" + ">\n");

		return result.toString();
	}

}


