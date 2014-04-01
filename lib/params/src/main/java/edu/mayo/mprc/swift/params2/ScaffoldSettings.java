package edu.mayo.mprc.swift.params2;

import com.google.common.base.Objects;
import edu.mayo.mprc.database.PersistableBase;

/**
 * Scaffold settings - thresholds, starred proteins, exports.
 */
public class ScaffoldSettings extends PersistableBase {
	private double proteinProbability;
	private double peptideProbability;
	private int minimumPeptideCount;
	private int minimumNonTrypticTerminii;

	private StarredProteins starredProteins;

	private boolean saveOnlyIdentifiedSpectra;
	private boolean saveNoSpectra;
	private boolean connectToNCBI;
	private boolean annotateWithGOA;
	private boolean useIndependentSampleGrouping;
	private boolean useFamilyProteinGrouping;
	private boolean mzIdentMlReport;

	public static final double PROBABILITY_PRECISION = 1E-5;

	public ScaffoldSettings() {
	}

	private static final double DEFAULT_PEPTIDE_PROBABILITY = 0.95;
	private static final double DEFAULT_PROTEIN_PROBABILITY = 0.95;

	public static final ScaffoldSettings DEFAULT = new ScaffoldSettings(
			DEFAULT_PROTEIN_PROBABILITY,
			DEFAULT_PEPTIDE_PROBABILITY,
			2,
			1,
			null,
			false,
			false,
			false,
			false,
			false,
			true,
			false
	);

	public ScaffoldSettings(final double proteinProbability, final double peptideProbability, final int minimumPeptideCount, final int minimumNonTrypticTerminii, final StarredProteins starredProteins, final boolean saveOnlyIdentifiedSpectra, final boolean saveNoSpectra, final boolean connectToNCBI, final boolean annotateWithGOA,
	                        final boolean useIndependentSampleGrouping, final boolean useFamilyProteinGrouping, final boolean mzIdentMlReport) {
		this.proteinProbability = proteinProbability;
		this.peptideProbability = peptideProbability;
		this.minimumPeptideCount = minimumPeptideCount;
		this.minimumNonTrypticTerminii = minimumNonTrypticTerminii;
		this.starredProteins = starredProteins;
		this.saveOnlyIdentifiedSpectra = saveOnlyIdentifiedSpectra;
		this.saveNoSpectra = saveNoSpectra;
		this.connectToNCBI = connectToNCBI;
		this.annotateWithGOA = annotateWithGOA;
		this.useIndependentSampleGrouping = useIndependentSampleGrouping;
		this.useFamilyProteinGrouping = useFamilyProteinGrouping;
		this.mzIdentMlReport = mzIdentMlReport;
	}

	public double getProteinProbability() {
		return proteinProbability;
	}

	public void setProteinProbability(final double proteinProbability) {
		this.proteinProbability = proteinProbability;
	}

	public double getPeptideProbability() {
		return peptideProbability;
	}

	public void setPeptideProbability(final double peptideProbability) {
		this.peptideProbability = peptideProbability;
	}

	public int getMinimumPeptideCount() {
		return minimumPeptideCount;
	}

	public void setMinimumPeptideCount(final int minimumPeptideCount) {
		this.minimumPeptideCount = minimumPeptideCount;
	}

	public int getMinimumNonTrypticTerminii() {
		return minimumNonTrypticTerminii;
	}

	public void setMinimumNonTrypticTerminii(final int minimumNonTrypticTerminii) {
		this.minimumNonTrypticTerminii = minimumNonTrypticTerminii;
	}

	public StarredProteins getStarredProteins() {
		return starredProteins;
	}

	public void setStarredProteins(final StarredProteins starredProteins) {
		this.starredProteins = starredProteins;
	}

	public boolean isSaveOnlyIdentifiedSpectra() {
		return saveOnlyIdentifiedSpectra;
	}

	public void setSaveOnlyIdentifiedSpectra(final boolean saveOnlyIdentifiedSpectra) {
		this.saveOnlyIdentifiedSpectra = saveOnlyIdentifiedSpectra;
	}

	public boolean isSaveNoSpectra() {
		return saveNoSpectra;
	}

	public void setSaveNoSpectra(final boolean saveNoSpectra) {
		this.saveNoSpectra = saveNoSpectra;
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

	public boolean isMzIdentMlReport() {
		return mzIdentMlReport;
	}

	public void setMzIdentMlReport(boolean mzIdentMlReport) {
		this.mzIdentMlReport = mzIdentMlReport;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ScaffoldSettings)) {
			return false;
		}
		final ScaffoldSettings that = (ScaffoldSettings) obj;

		return Objects.equal(isAnnotateWithGOA(), that.isAnnotateWithGOA()) &&
				Objects.equal(isConnectToNCBI(), that.isConnectToNCBI()) &&
				Objects.equal(getMinimumNonTrypticTerminii(), that.getMinimumNonTrypticTerminii()) &&
				Objects.equal(getMinimumPeptideCount(), that.getMinimumPeptideCount()) &&
				(Math.abs(getPeptideProbability() - that.getPeptideProbability()) <= PROBABILITY_PRECISION) &&
				(Math.abs(getProteinProbability() - that.getProteinProbability()) <= PROBABILITY_PRECISION) &&
				Objects.equal(isSaveNoSpectra(), that.isSaveNoSpectra()) &&
				Objects.equal(isSaveOnlyIdentifiedSpectra(), that.isSaveOnlyIdentifiedSpectra()) &&
				Objects.equal(getStarredProteins(), that.getStarredProteins()) &&
				Objects.equal(isUseIndependentSampleGrouping(), that.isUseIndependentSampleGrouping()) &&
				Objects.equal(isUseFamilyProteinGrouping(), that.isUseFamilyProteinGrouping()) &&
				Objects.equal(isMzIdentMlReport(), that.isMzIdentMlReport());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getProteinProbability(),
				getPeptideProbability(),
				getMinimumPeptideCount(),
				getMinimumNonTrypticTerminii(),
				getStarredProteins(),
				isSaveOnlyIdentifiedSpectra(),
				isSaveNoSpectra(),
				isConnectToNCBI(),
				isAnnotateWithGOA(),
				isUseIndependentSampleGrouping(),
				isUseFamilyProteinGrouping(),
				isMzIdentMlReport());
	}

	public ScaffoldSettings copy() {
		final ScaffoldSettings scaffoldSettings = new ScaffoldSettings(getProteinProbability(),
				getPeptideProbability(),
				getMinimumPeptideCount(),
				getMinimumNonTrypticTerminii(),
				getStarredProteins(),
				isSaveOnlyIdentifiedSpectra(),
				isSaveNoSpectra(),
				isConnectToNCBI(),
				isAnnotateWithGOA(),
				isUseIndependentSampleGrouping(),
				isUseFamilyProteinGrouping(),
				isMzIdentMlReport());
		scaffoldSettings.setId(getId());
		return scaffoldSettings;
	}
}
