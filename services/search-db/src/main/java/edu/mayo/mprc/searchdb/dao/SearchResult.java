package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.database.PersistableBase;

/**
 * A list of search results for a particular tandem mass spectrometry sample.
 * <p/>
 * This Scaffold spectrum report field is being parsed when creating this object:
 * <ul>
 * <li>MS/MS sample name - used to link to {@link TandemMassSpectrometrySample} with more information</li>
 * </ul>
 *
 * @author Roman Zenka
 */
public class SearchResult extends PersistableBase {
	/**
	 * The mass spec sample.
	 */
	private TandemMassSpectrometrySample massSpecSample;

	/**
	 * List of all protein groups identified in this sample.
	 */
	private ProteinGroupList proteinGroups;

	/**
	 * Empty constructor for Hibernate.
	 */
	public SearchResult() {
	}

	public SearchResult(final TandemMassSpectrometrySample massSpecSample, final ProteinGroupList proteinGroups) {
		setMassSpecSample(massSpecSample);
		setProteinGroups(proteinGroups);
	}

	public TandemMassSpectrometrySample getMassSpecSample() {
		return massSpecSample;
	}

	public void setMassSpecSample(final TandemMassSpectrometrySample massSpecSample) {
		this.massSpecSample = massSpecSample;
	}

	public ProteinGroupList getProteinGroups() {
		return proteinGroups;
	}

	public void setProteinGroups(final ProteinGroupList proteinGroups) {
		this.proteinGroups = proteinGroups;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !(o instanceof SearchResult)) {
			return false;
		}

		final SearchResult that = (SearchResult) o;

		if (getMassSpecSample() != null ? !getMassSpecSample().equals(that.getMassSpecSample()) : that.getMassSpecSample() != null) {
			return false;
		}
		if (getProteinGroups() != null ? !getProteinGroups().equals(that.getProteinGroups()) : that.getProteinGroups() != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = getMassSpecSample() != null ? getMassSpecSample().hashCode() : 0;
		result = 31 * result + (getProteinGroups() != null ? getProteinGroups().hashCode() : 0);
		return result;
	}
}
