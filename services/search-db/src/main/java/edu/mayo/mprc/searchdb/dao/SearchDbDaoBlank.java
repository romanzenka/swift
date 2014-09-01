package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.database.QueryCallback;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.utilities.progress.PercentProgressReporter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A blank implementation of {@link SearchDbDao} that makes all methods throw an exception.
 *
 * @author Roman Zenka
 */
public abstract class SearchDbDaoBlank implements SearchDbDao {
	@Override
	public Analysis addAnalysis(final Analysis analysis, final ReportData reportData, final PercentProgressReporter reporter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Analysis getAnalysis(final int analysisId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SwiftSearchDefinition getSearchDefinition(final long reportId) {
		return null;
	}

	@Override
	public List<String> getProteinAccessionNumbers(final ProteinSequenceList proteinSequenceList) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<ReportData> getSearchesForAccessionNumber(final String accessionNumber) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Long> getReportIdsWithoutAnalysis() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void getTandemMassSpectrometrySamples(final QueryCallback callback) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TandemMassSpectrometrySample updateTandemMassSpectrometrySample(final TandemMassSpectrometrySample sample) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TreeMap<Integer, ProteinSequenceList> getAllProteinSequences(final Analysis analysis) {
		return null;
	}

	@Override
	public TreeMap<Integer, ProteinGroup> getProteinGroups(final SearchResult searchResult) {
		return null;
	}

	@Override
	public Map<Integer, List<String>> getAccessionNumbersMapForProteinGroups(final Set<Integer> proteinGroupIds, final Integer databaseId) {
		return null;
	}

	@Override
	public Map<Integer, List<String>> getAccessionNumbersMapForProteinSequences(final Set<Integer> proteinSequenceLists, final Integer databaseId) {
		return null;
	}

	@Override
	public int getScaffoldProteinGroupCount(final String inputFile, final Iterable<ReportData> reports) {
		return 0;
	}

	@Override
	public SearchResult getSearchResult(final int searchResultId) {
		return null;
	}

	@Override
	public void begin() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void commit() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void rollback() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String qualifyTableName(final String table) {
		return table;
	}

	@Override
	public String check() {
		return null;
	}

	@Override
	public void install(final Map<String, String> params) {
	}
}
