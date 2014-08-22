package edu.mayo.mprc.database;

import java.util.Map;

/**
 * Implementors of this interface are able to populate themselves with initial test data to aid integration tests
 * down the line.
 * <p/>
 * Although this is used solely for testing, it is implemented directly by the shared interfaces, so users
 * of the dao in other modules can actually trigger these methods.
 * <p/>
 * This is done to enable better testing, as for performance reasons, some modules run complex database queries
 * that touch objects defined in another module. It is not enough to mock the other dao away, the data needs
 * to be present in the database for the test to work.
 *
 * @author Roman Zenka
 */
public interface TestDataProvider {
	/**
	 * Initializes the database with a data set of given name.
	 *
	 * @param params Additional parameters that define the data initialization. When in doubt, pass {@code null} for
	 *               default behavior.
	 */
	void installTestData(Map<String, String> params);
}
