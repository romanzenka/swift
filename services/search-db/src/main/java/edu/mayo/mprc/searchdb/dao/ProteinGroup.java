package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.PersistableBase;
import edu.mayo.mprc.utilities.MprcDoubles;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * Maps the following columns from the Scaffold spectrum report:
 * <ul>
 * <li>Protein accession numbers</li>
 * <li>Protein identification probability</li>
 * <li>Number of unique peptides</li>
 * <li>Number of unique spectra</li>
 * <li>Number of total spectra</li>
 * <li>Percentage of total spectra</li>
 * <li>Percentage sequence coverage</li>
 * </ul>
 *
 * @author Roman Zenka
 */
public class ProteinGroup extends PersistableBase {
	public static final double PERCENT_TOLERANCE = 0.00001;
	/**
	 * List of protein sequences belonging to the group.
	 */
	private ProteinSequenceList proteinSequences;

	/**
	 * Scaffold's calculated probability that the protein identification is correct. 100% probability is stored as 1.0
	 */
	private double proteinIdentificationProbability;

	/**
	 * The number of unique peptides that matched to the identified protein.
	 */
	private int numberOfUniquePeptides;

	/**
	 * Two spectra are unique if they match different peptides (even if the peptides overlap),
	 * or match two different charge states of the same peptide, or match both a peptide and a modified form of the peptide.
	 * The Number of unique spectra is a count of these unique spectra.
	 */
	private int numberOfUniqueSpectra;

	/**
	 * How many spectra total mapped to a peptide that mapped to this protein.
	 */
	private int numberOfTotalSpectra;

	/**
	 * How many percent of total spectra mapped to this protein group? 100% is stored as 1.0
	 */
	private double percentageOfTotalSpectra;

	/**
	 * How many percent of the protein sequence were covered by identified peptides? 100% is stored as 1.0
	 */
	private double percentageSequenceCoverage;

	public ProteinGroup() {
	}

	public ProteinGroup(final ProteinSequenceList proteinSequences, final double proteinIdentificationProbability, final int numberOfUniquePeptides, final int numberOfUniqueSpectra, final int numberOfTotalSpectra, final double percentageOfTotalSpectra, final double percentageSequenceCoverage) {
		this.proteinSequences = proteinSequences;
		this.proteinIdentificationProbability = proteinIdentificationProbability;
		this.numberOfUniquePeptides = numberOfUniquePeptides;
		this.numberOfUniqueSpectra = numberOfUniqueSpectra;
		this.numberOfTotalSpectra = numberOfTotalSpectra;
		this.percentageOfTotalSpectra = percentageOfTotalSpectra;
		this.percentageSequenceCoverage = percentageSequenceCoverage;
	}

	public ProteinSequenceList getProteinSequences() {
		return proteinSequences;
	}

	public void setProteinSequences(final ProteinSequenceList proteinSequences) {
		this.proteinSequences = proteinSequences;
	}

	public double getProteinIdentificationProbability() {
		return proteinIdentificationProbability;
	}

	public void setProteinIdentificationProbability(final double proteinIdentificationProbability) {
		this.proteinIdentificationProbability = proteinIdentificationProbability;
	}

	public int getNumberOfUniquePeptides() {
		return numberOfUniquePeptides;
	}

	public void setNumberOfUniquePeptides(final int numberOfUniquePeptides) {
		this.numberOfUniquePeptides = numberOfUniquePeptides;
	}

	public int getNumberOfUniqueSpectra() {
		return numberOfUniqueSpectra;
	}

	public void setNumberOfUniqueSpectra(final int numberOfUniqueSpectra) {
		this.numberOfUniqueSpectra = numberOfUniqueSpectra;
	}

	public int getNumberOfTotalSpectra() {
		return numberOfTotalSpectra;
	}

	public void setNumberOfTotalSpectra(final int numberOfTotalSpectra) {
		this.numberOfTotalSpectra = numberOfTotalSpectra;
	}

	public double getPercentageOfTotalSpectra() {
		return percentageOfTotalSpectra;
	}

	public void setPercentageOfTotalSpectra(final double percentageOfTotalSpectra) {
		this.percentageOfTotalSpectra = percentageOfTotalSpectra;
	}

	public double getPercentageSequenceCoverage() {
		return percentageSequenceCoverage;
	}

	public void setPercentageSequenceCoverage(final double percentageSequenceCoverage) {
		this.percentageSequenceCoverage = percentageSequenceCoverage;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !(o instanceof ProteinGroup)) {
			return false;
		}

		final ProteinGroup that = (ProteinGroup) o;

		if (getNumberOfTotalSpectra() != that.getNumberOfTotalSpectra()) {
			return false;
		}
		if (getNumberOfUniquePeptides() != that.getNumberOfUniquePeptides()) {
			return false;
		}
		if (getNumberOfUniqueSpectra() != that.getNumberOfUniqueSpectra()) {
			return false;
		}
		if (!MprcDoubles.within(that.getPercentageOfTotalSpectra(), getPercentageOfTotalSpectra(), PERCENT_TOLERANCE)) {
			return false;
		}
		if (!MprcDoubles.within(that.getPercentageSequenceCoverage(), getPercentageSequenceCoverage(), PERCENT_TOLERANCE)) {
			return false;
		}
		if (!MprcDoubles.within(that.getProteinIdentificationProbability(), getProteinIdentificationProbability(), PERCENT_TOLERANCE)) {
			return false;
		}
		if (getProteinSequences() != null ? !getProteinSequences().equals(that.getProteinSequences()) : that.getProteinSequences() != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = getProteinSequences() != null ? getProteinSequences().hashCode() : 0;
		temp = getProteinIdentificationProbability() != +0.0d ? Double.doubleToLongBits(getProteinIdentificationProbability()) : 0L;
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + getNumberOfUniquePeptides();
		result = 31 * result + getNumberOfUniqueSpectra();
		result = 31 * result + getNumberOfTotalSpectra();
		temp = getPercentageOfTotalSpectra() != +0.0d ? Double.doubleToLongBits(getPercentageOfTotalSpectra()) : 0L;
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = getPercentageSequenceCoverage() != +0.0d ? Double.doubleToLongBits(getPercentageSequenceCoverage()) : 0L;
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public Criterion getEqualityCriteria() {
		return Restrictions.conjunction()
				.add(DaoBase.associationEq("proteinSequences", getProteinSequences()))
				.add(DaoBase.nullSafeEq("proteinIdentificationProbability", getProteinIdentificationProbability()))
				.add(DaoBase.nullSafeEq("numberOfUniquePeptides", getNumberOfUniquePeptides()))
				.add(DaoBase.nullSafeEq("numberOfUniqueSpectra", getNumberOfUniqueSpectra()))
				.add(DaoBase.nullSafeEq("numberOfTotalSpectra", getNumberOfTotalSpectra()))
				.add(DaoBase.doubleEq("percentageOfTotalSpectra", getPercentageOfTotalSpectra(), PERCENT_TOLERANCE))
				.add(DaoBase.doubleEq("percentageSequenceCoverage", getPercentageSequenceCoverage(), PERCENT_TOLERANCE));
	}

}
