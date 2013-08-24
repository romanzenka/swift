package edu.mayo.mprc.scafml;


import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.files.FileHolder;
import edu.mayo.mprc.utilities.xml.XMLUtilities;

import java.util.Collection;
import java.util.LinkedHashMap;

public final class ScafmlBiologicalSample extends FileHolder {
	private static final long serialVersionUID = -3662962867954398923L;
	/**
	 * identifier for biological sample, must be unique within experiment
	 * contains InputFile's
	 */
	private String id;

	private String analyzeAsMudpit;
	private String database;
	private String name;
	private String category;

	private LinkedHashMap<String, ScafmlInputFile> inputFiles;

	public ScafmlBiologicalSample(final String id) {
		this.id = id;
		inputFiles = new LinkedHashMap<String, ScafmlInputFile>(5);
	}

	public ScafmlBiologicalSample() {
		this(null);
	}

	public void setId(final String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public ScafmlInputFile getInputFile(final String id) {
		return inputFiles.get(id);
	}

	public void addInputFile(final ScafmlInputFile pInputFile) {
		if (pInputFile == null) {
			throw new MprcException("null object for Input File");
		}
		final String id = pInputFile.getID();
		if (id == null) {
			throw new MprcException("no id for Input File\" object");
		}
		if (inputFiles.get(id) == null) {
			inputFiles.put(id, pInputFile);
		}
	}

	public Collection<ScafmlInputFile> getInputFiles() {
		return inputFiles.values();
	}

	public void setAnalyzeAsMudpit(final String sAnalyzeMudpit) {
		analyzeAsMudpit = sAnalyzeMudpit;
	}

	public String getAnalyzeAsMudpit() {
		return analyzeAsMudpit;
	}

	public void setDatabase(final String sDatabase) {
		database = sDatabase;
	}

	public String getDatabase() {
		return database;
	}

	public void setName(final String sName) {
		name = sName;
	}

	public String getName() {
		return name;
	}

	public void setCategory(final String sCategory) {
		category = sCategory;
	}

	public String getCategory() {
		return category;
	}


	public void appendToDocument(final StringBuilder result, final String indent) {
		if (inputFiles.values().isEmpty()) {
			result.append(indent).append("<!-- Biological sample with no input files: ").append(getName()).append(" -->\n");
		} else {
			final String header = indent + "<" + "BiologicalSample" +
					XMLUtilities.wrapatt("analyzeAsMudpit", getAnalyzeAsMudpit()) +
					XMLUtilities.wrapatt("database", getDatabase()) +
					XMLUtilities.wrapatt("name", getName()) +
					XMLUtilities.wrapatt("category", getCategory()) +
					">\n";
			result.append(header);
			// now the input files
			for (final ScafmlInputFile inputfile : inputFiles.values()) {
				inputfile.appendToDocument(result, indent + '\t');
			}

			result.append(indent).append("</" + "BiologicalSample" + ">\n");
		}
	}
}

