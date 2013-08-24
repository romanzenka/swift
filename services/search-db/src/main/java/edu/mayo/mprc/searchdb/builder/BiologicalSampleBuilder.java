package edu.mayo.mprc.searchdb.builder;

import edu.mayo.mprc.searchdb.dao.BiologicalSample;

/**
 * Builds biological samples.
 *
 * @author Roman Zenka
 */
public class BiologicalSampleBuilder implements Builder<BiologicalSample> {
	private AnalysisBuilder analysis;

	private String sampleName;

	/**
	 * Category of the sample. This is usually set to "none", but sometimes it can contain useful information.
	 */
	private String category;

	/**
	 * Results of protein searches for this particular biological sample. Would usually contain only one mass
	 * spec sample.
	 */
	private SearchResultListBuilder searchResults;

	public BiologicalSampleBuilder(final AnalysisBuilder analysis, final String sampleName, final String category) {
		this.analysis = analysis;
		this.sampleName = sampleName;
		this.category = category;
		searchResults = new SearchResultListBuilder(this);
	}

	@Override
	public BiologicalSample build() {
		return new BiologicalSample(sampleName, category, searchResults.build());
	}

	public AnalysisBuilder getAnalysis() {
		return analysis;
	}

	public String getSampleName() {
		return sampleName;
	}

	public void setSampleName(final String sampleName) {
		this.sampleName = sampleName;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(final String category) {
		this.category = category;
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

		if (category != null ? !category.equals(that.category) : that.category != null) {
			return false;
		}
		if (sampleName != null ? !sampleName.equals(that.sampleName) : that.sampleName != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = sampleName != null ? sampleName.hashCode() : 0;
		result = 31 * result + (category != null ? category.hashCode() : 0);
		return result;
	}
}
