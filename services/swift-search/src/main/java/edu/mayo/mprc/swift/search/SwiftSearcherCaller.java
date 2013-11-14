package edu.mayo.mprc.swift.search;

import edu.mayo.mprc.config.Lifecycle;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.utilities.progress.ProgressListener;

import java.io.File;

/**
 * Can start a swift search by calling {@link SwiftSearcher}
 *
 * @author Roman Zenka
 */
public interface SwiftSearcherCaller extends Lifecycle {
	/**
	 * used to resubmit a transaction that has been run before
	 *
	 * @param td       - Transaction to resubmit
	 * @param listener Progress listener for this submission.
	 */
	void resubmitSearchRun(SearchRun td, ProgressListener listener);

	/**
	 * Start a new Swift search, return its id. Done to support the REST-ful API.
	 * <p/>
	 * It creates a swift search definition, validates it, makes sure that the search title is unique,
	 * saves it to the database (careful, starts its own transactions!)
	 * <p/>
	 * The ID of the saved search definition is sent to swift searcher, which then in turn loads the
	 * definition out of the database and starts performing it. The database is used because
	 * the searcher logs its progress into the database + it loads data and produces reports,
	 * all using the database entry.
	 */
	long startSearchRestful(SearchInput searchInput);

	/**
	 * Makes sure that the definition is valid.
	 * <p/>
	 * If so, it gets saved, while hiding all older searches of the same definition.
	 *
	 * @param swiftSearch Search definition to save.
	 * @return Saved search definition.
	 */
	SwiftSearchDefinition validateAndSaveSearchDefinition(SwiftSearchDefinition swiftSearch);

	/**
	 * Just like {@link #startSearch} but blocks for certain amount of time before it either throws
	 * or returns new search id.
	 */
	long submitSearch(int searchId, String batchName, int previousSearchRunId, boolean fromScratch, int priority) throws InterruptedException;

	/**
	 * @return The root folder where the web ui starts browsing from.
	 */
	File getBrowseRoot();
}
