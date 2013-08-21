package edu.mayo.mprc.scafml;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.files.FileHolder;
import edu.mayo.mprc.utilities.xml.XMLUtilities;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * definition of an experiment
 * experiment contains biological samples
 */
public final class ScafmlExperiment extends FileHolder {
	private static final long serialVersionUID = 4851459805058267855L;
	/**
	 * can have more than one biological sample per experiment.
	 * Must be linked to retain order.
	 */
	private final Map<String, ScafmlBiologicalSample> biologicalSamples;
	private String name;
	private final Map<String, ScafmlFastaDatabase> scafmlFastaDatabases;
	private ScafmlExport export;
	private boolean connectToNCBI = true;
	private boolean annotateWithGOA = true;
	private boolean reportDecoyHits;

	/**
	 * If set to true, each sample column should appear exactly as if they were ran in separate Scaffolds.
	 */
	private boolean useIndependentSampleGrouping;

	/**
	 * If set to true, the protein families are produced
	 */
	private boolean useFamilyProteinGrouping = true;

	public ScafmlExperiment(final String name) {
		biologicalSamples = new LinkedHashMap<String, ScafmlBiologicalSample>(1);
		scafmlFastaDatabases = new HashMap<String, ScafmlFastaDatabase>(1);
		this.name = name;
	}

	public ScafmlFastaDatabase getFastaDatabase(final String id) {
		return this.scafmlFastaDatabases.get(id);
	}

	public void addFastaDatabase(final ScafmlFastaDatabase pFastaDatabase) {
		if (pFastaDatabase == null) {
			throw new MprcException("null object for Biological Sample");
		}
		if (pFastaDatabase.getId() == null) {
			throw new MprcException("no id for Biological Sample object");
		}
		this.scafmlFastaDatabases.put(pFastaDatabase.getId(), pFastaDatabase);
	}

	public Collection<ScafmlFastaDatabase> getDatabases() {
		return scafmlFastaDatabases.values();
	}

	public ScafmlBiologicalSample getBiologicalSample(final String id) {
		return biologicalSamples.get(id);
	}

	public Collection<ScafmlBiologicalSample> getBiologicalSamples() {
		return biologicalSamples.values();
	}

	public void addBiologicalSample(final ScafmlBiologicalSample pBiologicalSample) {
		if (pBiologicalSample == null) {
			throw new MprcException("null object for Biological Sample");
		}
		if (pBiologicalSample.getId() == null) {
			throw new MprcException("no id for Biological Sample object");
		}
		this.biologicalSamples.put(pBiologicalSample.getId(), pBiologicalSample);
	}

	public void setName(final String sName) {
		this.name = sName;
	}

	public String getName() {
		return name;
	}


	public void setExport(final ScafmlExport pExport) {
		this.export = pExport;
	}

	public ScafmlExport getExport() {
		return export;
	}

	public boolean isConnectToNCBI() {
		return connectToNCBI;
	}

	public void setConnectToNCBI(final boolean connectToNCBI) {
		this.connectToNCBI = connectToNCBI;
	}

	public boolean isAnnotateWithGOA() {
		return annotateWithGOA;
	}

	public void setAnnotateWithGOA(final boolean annotateWithGOA) {
		this.annotateWithGOA = annotateWithGOA;
	}

	public boolean isReportDecoyHits() {
		return reportDecoyHits;
	}

	public void setReportDecoyHits(final boolean reportDecoyHits) {
		this.reportDecoyHits = reportDecoyHits;
	}

	public boolean isUseIndependentSampleGrouping() {
		return useIndependentSampleGrouping;
	}

	public void setUseIndependentSampleGrouping(boolean useIndependentSampleGrouping) {
		this.useIndependentSampleGrouping = useIndependentSampleGrouping;
	}

	public boolean isUseFamilyProteinGrouping() {
		return useFamilyProteinGrouping;
	}

	public void setUseFamilyProteinGrouping(boolean useFamilyProteinGrouping) {
		this.useFamilyProteinGrouping = useFamilyProteinGrouping;
	}

	public void appendToDocument(final StringBuilder result, final String indent, final ScafmlScaffold scaffold) {
		result
				.append(indent)
				.append("<" + "Experiment")
				.append(XMLUtilities.wrapatt("name", this.getName()))
				.append(XMLUtilities.wrapatt("connectToNCBI", isConnectToNCBI() ? "true" : "false"))
				.append(XMLUtilities.wrapatt("annotateWithGOA", isAnnotateWithGOA() ? "true" : "false"))
				.append(XMLUtilities.wrapatt("useIndependentSampleGrouping", isUseIndependentSampleGrouping() ? "true" : "false"))
				.append(XMLUtilities.wrapatt("useFamilyProteinGrouping", isUseFamilyProteinGrouping() ? "true" : "false"))
				.append(XMLUtilities.wrapatt("spectrumNameRegEx", "([^ /(]+\\.dta)"));

		if (scaffold.getVersionMajor() >= 3) {
			result
					.append(XMLUtilities.wrapatt("peakListGeneratorName", "extract_msn"))
					.append(XMLUtilities.wrapatt("peakListDeisotoped", "false"));
		}

		result.append(">\n");

		// now the fasta databases
		for (final ScafmlFastaDatabase fd : getDatabases()) {
			fd.appendToDocument(result, indent + "\t", isReportDecoyHits());
		}

		// now the biological samples
		for (final ScafmlBiologicalSample bios : getBiologicalSamples()) {
			bios.appendToDocument(result, indent + "\t");
		}

		// now the exports
		this.getExport().appendToDocument(result, indent + "\t");

		result.append(indent + "</" + "Experiment" + ">\n");
	}
}

