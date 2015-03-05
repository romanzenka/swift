package edu.mayo.mprc.searchdb.builder;

import edu.mayo.mprc.searchdb.dao.SearchResult;
import edu.mayo.mprc.searchdb.dao.SearchResultList;

import java.util.*;

/**
 * Builds the list of search results.
 *
 * @author Roman Zenka
 */
public class SearchResultListBuilder implements Builder<SearchResultList> {
	private Map<String, SearchResultBuilder> list = new LinkedHashMap<String, SearchResultBuilder>();

	private BiologicalSampleBuilder biologicalSample;

	public SearchResultListBuilder(final BiologicalSampleBuilder biologicalSample) {
		this.biologicalSample = biologicalSample;
	}

	@Override
	public SearchResultList build() {
		final List<SearchResult> items = new ArrayList<SearchResult>(list.size());
		for (final SearchResultBuilder builder : list.values()) {
			items.add(builder.build());
		}
		return new SearchResultList(items);
	}

	/**
	 * Get the current mass spec sample result test for given biological sample. If a new set is discovered,
	 * it is initialized and added to the biological sample.
	 *
	 * @param msmsSampleName The name of the tandem mass spectrometry sample.
	 *                       Warning, Scaffold tends to prefix this with Mudpit_ in not-well understood circumstances.
	 * @return Current tandem mass spec search result object.
	 */
	public SearchResultBuilder getTandemMassSpecResult(final String msmsSampleName) {
		final SearchResultBuilder searchResult = list.get(msmsSampleName);
		if (searchResult == null) {
			final SearchResultBuilder newSearchResult = new SearchResultBuilder(biologicalSample);
			newSearchResult.setMassSpecSample(
					biologicalSample.getAnalysis().getMassSpecDataExtractor().getTandemMassSpectrometrySample(biologicalSample.getSampleName(), msmsSampleName)
			);
			list.put(msmsSampleName, newSearchResult);
			return newSearchResult;
		}
		return searchResult;
	}

	public void collectAccnums(Set<String> allAccnums) {
		for (SearchResultBuilder sr : list.values()) {
			sr.collectAccnums(allAccnums);
		}
	}
}
