package edu.mayo.mprc.heme.dao;

import edu.mayo.mprc.config.RuntimeInitializer;
import edu.mayo.mprc.database.Dao;

import java.util.List;

/**
 * @author Roman Zenka
 */
public interface HemeDao extends Dao, RuntimeInitializer {
	List<HemeTest> getAllTests();

	/**
	 * Add a new test. If equivalent test already exists, an exception is thrown.
	 *
	 * @param test Test to add.
	 * @return Saved instance of the test.
	 */
	HemeTest addTest(HemeTest test);

	/**
	 * Delete the given test from the database.
	 *
	 * @param test Test to delete.
	 */
	void removeTest(HemeTest test);

	/**
	 * @return How many tests are there.
	 */
	long countTests();
}
