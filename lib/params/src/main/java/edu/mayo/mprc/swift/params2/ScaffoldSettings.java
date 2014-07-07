package edu.mayo.mprc.swift.params2;

import com.google.common.base.Objects;
import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.PersistableBase;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

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
	private boolean highMassAccuracyScoring;
	private boolean use3xScoring;

	public ScaffoldSettings() {
	}

	public ScaffoldSettings(final double proteinProbability, final double peptideProbability, final int minimumPeptideCount, final int minimumNonTrypticTerminii, final StarredProteins starredProteins, final boolean saveOnlyIdentifiedSpectra, final boolean saveNoSpectra, final boolean connectToNCBI, final boolean annotateWithGOA,
	                        final boolean useIndependentSampleGrouping, final boolean useFamilyProteinGrouping,
	                        final boolean mzIdentMlReport, final boolean highMassAccuracyScoring,
	                        final boolean use3xScoring) {
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
		this.highMassAccuracyScoring = highMassAccuracyScoring;
		this.use3xScoring = use3xScoring;
	}

	public static ScaffoldSettings defaultScaffoldSettings() {
		return new ScaffoldSettingsBuilder()
				.createScaffoldSettings();
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

	public void setUseIndependentSampleGrouping(final boolean useIndependentSampleGrouping) {
		this.useIndependentSampleGrouping = useIndependentSampleGrouping;
	}

	public boolean isUseFamilyProteinGrouping() {
		return useFamilyProteinGrouping;
	}

	public void setUseFamilyProteinGrouping(final boolean useFamilyProteinGrouping) {
		this.useFamilyProteinGrouping = useFamilyProteinGrouping;
	}

	public boolean isMzIdentMlReport() {
		return mzIdentMlReport;
	}

	public void setMzIdentMlReport(final boolean mzIdentMlReport) {
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
				Objects.equal(isMzIdentMlReport(), that.isMzIdentMlReport()) &&
				Objects.equal(isHighMassAccuracyScoring(), that.isHighMassAccuracyScoring()) &&
				Objects.equal(isUse3xScoring(), that.isUse3xScoring());
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
				isMzIdentMlReport(),
				isHighMassAccuracyScoring(),
				isUse3xScoring());
	}

	@Override
	public Criterion getEqualityCriteria() {
		return Restrictions.conjunction()
				.add(Restrictions.between("proteinProbability", getProteinProbability() - PROBABILITY_PRECISION, getProteinProbability() + PROBABILITY_PRECISION))
				.add(Restrictions.between("peptideProbability", getPeptideProbability() - PROBABILITY_PRECISION, getPeptideProbability() + PROBABILITY_PRECISION))
				.add(Restrictions.eq("minimumPeptideCount", getMinimumPeptideCount()))
				.add(Restrictions.eq("minimumNonTrypticTerminii", getMinimumNonTrypticTerminii()))
				.add(Restrictions.eq("saveOnlyIdentifiedSpectra", isSaveOnlyIdentifiedSpectra()))
				.add(Restrictions.eq("saveNoSpectra", isSaveNoSpectra()))
				.add(Restrictions.eq("connectToNCBI", isConnectToNCBI()))
				.add(Restrictions.eq("annotateWithGOA", isAnnotateWithGOA()))
				.add(Restrictions.eq("useFamilyProteinGrouping", isUseFamilyProteinGrouping()))
				.add(Restrictions.eq("useIndependentSampleGrouping", isUseIndependentSampleGrouping()))
				.add(Restrictions.eq("mzIdentMlReport", isMzIdentMlReport()))
				.add(Restrictions.eq("highMassAccuracyScoring", isHighMassAccuracyScoring()))
				.add(Restrictions.eq("use_3x_scoring", isUse3xScoring()))
				.add(DaoBase.associationEq("starredProteins", getStarredProteins()));
	}

	public ScaffoldSettings copy() {
		final ScaffoldSettings scaffoldSettings = new ScaffoldSettingsBuilder().setProteinProbability(getProteinProbability()).setPeptideProbability(getPeptideProbability()).setMinimumPeptideCount(getMinimumPeptideCount()).setMinimumNonTrypticTerminii(getMinimumNonTrypticTerminii()).setStarredProteins(getStarredProteins()).setSaveOnlyIdentifiedSpectra(isSaveOnlyIdentifiedSpectra()).setSaveNoSpectra(isSaveNoSpectra()).setConnectToNCBI(isConnectToNCBI()).setAnnotateWithGOA(isAnnotateWithGOA()).setUseIndependentSampleGrouping(isUseIndependentSampleGrouping()).setUseFamilyProteinGrouping(isUseFamilyProteinGrouping()).setMzIdentMlReport(isMzIdentMlReport()).setHighMassAccuracyScoring(isHighMassAccuracyScoring()).setUse3xScoring(isUse3xScoring()).createScaffoldSettings();
		scaffoldSettings.setId(getId());
		return scaffoldSettings;
	}

	public boolean isHighMassAccuracyScoring() {
		return highMassAccuracyScoring;
	}

	public void setHighMassAccuracyScoring(final boolean highMassAccuracyScoring) {
		this.highMassAccuracyScoring = highMassAccuracyScoring;
	}

	public boolean isUse3xScoring() {
		return use3xScoring;
	}

	public void setUse3xScoring(final boolean use3xScoring) {
		this.use3xScoring = use3xScoring;
	}
}
