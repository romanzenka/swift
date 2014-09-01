package edu.mayo.mprc.searchdb.bulk;

import edu.mayo.mprc.searchdb.builder.AnalysisBuilder;
import edu.mayo.mprc.searchdb.dao.Analysis;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.swift.dbmapping.ReportData;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;

/**
 * @author Roman Zenka
 */
public interface BulkSearchDbDao extends SearchDbDao {
	/**
	 * Just like {@link SearchDbDao#addAnalysis},
	 * only uses bulk loading for getting the analysis data into the database. To do so, the
	 * {@link edu.mayo.mprc.searchdb.builder.AnalysisBuilder} is required, as we want to add the data before the whole analysis is fully built.
	 */
	Analysis addAnalysis(AnalysisBuilder analysisBuilder, ReportData reportData, UserProgressReporter reporter);
}
