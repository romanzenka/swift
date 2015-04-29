package edu.mayo.mprc.searchdb.builder;

import edu.mayo.mprc.searchdb.dao.BiologicalSample;

import java.util.Set;

/**
 * Builds biological samples.
 *
 * @author Roman Zenka
 */
public class BiologicalSampleBuilder implements Builder<BiologicalSample> {
	private AnalysisBuilder analysis;

	private BiologicalSampleId biologicalSampleId;

	/**
	 * Results of protein searches for this particular biological sample. Would usually contain only one mass
	 * spec sample.
	 */
	private SearchResultListBuilder searchResults;

	public BiologicalSampleBuilder(final AnalysisBuilder analysis, final BiologicalSampleId biologicalSampleId) {
		this.analysis = analysis;
		this.biologicalSampleId = biologicalSampleId;
		searchResults = new SearchResultListBuilder(this);
	}

	@Override
	public BiologicalSample build() {
		return new BiologicalSample(biologicalSampleId, searchResults.build());
	}

	public AnalysisBuilder getAnalysis() {
		return analysis;
	}

	public String getSampleName() {
		return biologicalSampleId.getSampleName();
	}

	public String getCategory() {
		return biologicalSampleId.getCategory();
	}

	public SearchResultListBuilder getSearchResults() {
		return searchResults;
	}

	public void setSearchResults(final SearchResultListBuilder searchResults) {
		this.searchResults = searchResults;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final BiologicalSampleBuilder that = (BiologicalSampleBuilder) o;

		if (biologicalSampleId != null ? !biologicalSampleId.equals(that.biologicalSampleId) : that.biologicalSampleId != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = biologicalSampleId != null ? biologicalSampleId.hashCode() : 0;
		return result;
	}

	public void collectAccnums(final Set<String> allAccnums) {
		getSearchResults().collectAccnums(allAccnums);
	}
}
