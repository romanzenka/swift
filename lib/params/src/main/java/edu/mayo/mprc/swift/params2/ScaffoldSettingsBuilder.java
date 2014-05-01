package edu.mayo.mprc.swift.params2;

public class ScaffoldSettingsBuilder {
	private static final double DEFAULT_PEPTIDE_PROBABILITY = 0.95;
	private static final double DEFAULT_PROTEIN_PROBABILITY = 0.95;

	private double proteinProbability = DEFAULT_PROTEIN_PROBABILITY;
	private double peptideProbability = DEFAULT_PEPTIDE_PROBABILITY;
	private int minimumPeptideCount = 2;
	private int minimumNonTrypticTerminii = 1;
	private StarredProteins starredProteins;
	private boolean saveOnlyIdentifiedSpectra = true;
	private boolean saveNoSpectra;
	private boolean connectToNCBI;
	private boolean annotateWithGOA;
	private boolean useIndependentSampleGrouping;
	private boolean useFamilyProteinGrouping = true;
	private boolean mzIdentMlReport;
	private boolean highMassAccuracyScoring;
	private boolean use3xScoring;

	public ScaffoldSettingsBuilder setProteinProbability(final double proteinProbability) {
		this.proteinProbability = proteinProbability;
		return this;
	}

	public ScaffoldSettingsBuilder setPeptideProbability(final double peptideProbability) {
		this.peptideProbability = peptideProbability;
		return this;
	}

	public ScaffoldSettingsBuilder setMinimumPeptideCount(final int minimumPeptideCount) {
		this.minimumPeptideCount = minimumPeptideCount;
		return this;
	}

	public ScaffoldSettingsBuilder setMinimumNonTrypticTerminii(final int minimumNonTrypticTerminii) {
		this.minimumNonTrypticTerminii = minimumNonTrypticTerminii;
		return this;
	}

	public ScaffoldSettingsBuilder setStarredProteins(final StarredProteins starredProteins) {
		this.starredProteins = starredProteins;
		return this;
	}

	public ScaffoldSettingsBuilder setSaveOnlyIdentifiedSpectra(final boolean saveOnlyIdentifiedSpectra) {
		this.saveOnlyIdentifiedSpectra = saveOnlyIdentifiedSpectra;
		return this;
	}

	public ScaffoldSettingsBuilder setSaveNoSpectra(final boolean saveNoSpectra) {
		this.saveNoSpectra = saveNoSpectra;
		return this;
	}

	public ScaffoldSettingsBuilder setConnectToNCBI(final boolean connectToNCBI) {
		this.connectToNCBI = connectToNCBI;
		return this;
	}

	public ScaffoldSettingsBuilder setAnnotateWithGOA(final boolean annotateWithGOA) {
		this.annotateWithGOA = annotateWithGOA;
		return this;
	}

	public ScaffoldSettingsBuilder setUseIndependentSampleGrouping(final boolean useIndependentSampleGrouping) {
		this.useIndependentSampleGrouping = useIndependentSampleGrouping;
		return this;
	}

	public ScaffoldSettingsBuilder setUseFamilyProteinGrouping(final boolean useFamilyProteinGrouping) {
		this.useFamilyProteinGrouping = useFamilyProteinGrouping;
		return this;
	}

	public ScaffoldSettingsBuilder setMzIdentMlReport(final boolean mzIdentMlReport) {
		this.mzIdentMlReport = mzIdentMlReport;
		return this;
	}

	public ScaffoldSettingsBuilder setHighMassAccuracyScoring(final boolean highMassAccuracyScoring) {
		this.highMassAccuracyScoring = highMassAccuracyScoring;
		return this;
	}

	public ScaffoldSettingsBuilder setUse3xScoring(final boolean use3xScoring) {
		this.use3xScoring = use3xScoring;
		return this;
	}

	public ScaffoldSettings createScaffoldSettings() {
		return new ScaffoldSettings(proteinProbability, peptideProbability, minimumPeptideCount, minimumNonTrypticTerminii, starredProteins, saveOnlyIdentifiedSpectra, saveNoSpectra, connectToNCBI, annotateWithGOA, useIndependentSampleGrouping, useFamilyProteinGrouping, mzIdentMlReport, highMassAccuracyScoring, use3xScoring);
	}
}