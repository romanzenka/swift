package edu.mayo.mprc.searchdb.dao;

import edu.mayo.mprc.database.QueryCallback;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;

import java.util.List;

/**
 * A blank implementation of {@link SearchDbDao} that makes all methods throw an exception.
 * @author Roman Zenka
 */
public class SearchDbDaoBlank implements SearchDbDao {
	@Override
	public Analysis addAnalysis(Analysis analysis, ReportData reportData, UserProgressReporter reporter) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Analysis getAnalysis(long reportId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasAnalysis(long reportId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<String> getProteinAccessionNumbers(ProteinSequenceList proteinSequenceList) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<ReportData> getSearchesForAccessionNumber(String accessionNumber) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Long> getReportIdsWithoutAnalysis() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void getTandemMassSpectrometrySamples(QueryCallback callback) {
		throw new UnsupportedOperationException();
	}

	@Override
	public TandemMassSpectrometrySample updateTandemMassSpectrometrySample(TandemMassSpectrometrySample sample) {
		throw new UnsupportedOperationException();
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
}
