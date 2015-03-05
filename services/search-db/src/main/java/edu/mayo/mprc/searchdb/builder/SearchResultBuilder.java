package edu.mayo.mprc.searchdb.builder;

import edu.mayo.mprc.searchdb.dao.SearchResult;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;

import java.util.Set;

/**
 * @author Roman Zenka
 */
public class SearchResultBuilder implements Builder<SearchResult> {
	private TandemMassSpectrometrySample massSpecSample;

	public SearchResultBuilder(final BiologicalSampleBuilder biologicalSample) {
		this.biologicalSample = biologicalSample;
		proteinGroups = new ProteinGroupListBuilder(this);
	}

	/**
	 * List of all protein groups identified in this sample.
	 */
	private ProteinGroupListBuilder proteinGroups;

	private BiologicalSampleBuilder biologicalSample;

	public BiologicalSampleBuilder getBiologicalSample() {
		return biologicalSample;
	}

	@Override
	public SearchResult build() {
		return new SearchResult(massSpecSample, proteinGroups.build());
	}

	public TandemMassSpectrometrySample getMassSpecSample() {
		return massSpecSample;
	}

	public void setMassSpecSample(final TandemMassSpectrometrySample massSpecSample) {
		this.massSpecSample = massSpecSample;
	}

	public ProteinGroupListBuilder getProteinGroups() {
		return proteinGroups;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final SearchResultBuilder that = (SearchResultBuilder) o;

		if (massSpecSample != null ? !massSpecSample.equals(that.massSpecSample) : that.massSpecSample != null) {
			return false;
		}
		if (proteinGroups != null ? !proteinGroups.equals(that.proteinGroups) : that.proteinGroups != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = massSpecSample != null ? massSpecSample.hashCode() : 0;
		result = 31 * result + (proteinGroups != null ? proteinGroups.hashCode() : 0);
		return result;
	}

	public void collectAccnums(final Set<String> allAccnums) {
		proteinGroups.collectAccnums(allAccnums);
	}
}
