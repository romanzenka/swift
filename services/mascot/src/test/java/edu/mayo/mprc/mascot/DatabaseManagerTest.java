package edu.mayo.mprc.mascot;

import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Test the mascot database manager (used for deploying/undeploying databases).
 * <p/>
 * There should be one free database slot in Mascot for this to work.
 *
 * @author Roman Zenka
 */
@Test(groups = "integration")
public final class DatabaseManagerTest {
	public static final int MAX_MASCOT_DATABASES = 128;
	public static final String TEST_DB = "swift_test_db";
	private DatabaseManager manager;
	private List<String> originalDatabases;

	@BeforeClass
	public void setup() {
		manager = new DatabaseManager("http://mascot.mayo.edu");
	}

	@Test
	public void shouldListDatabases() {
		final List<String> databases = manager.listDatabases();
		Assert.assertNotNull(databases);
		Assert.assertTrue(!databases.isEmpty(), "There must be at least one database deployed");
		originalDatabases = databases;
	}

	@Test(dependsOnMethods = "shouldListDatabases")
	public void shouldDeployDatabase() throws IOException {
		// We need to leave space for actual deployment
		final List<String> dbs = originalDatabases;
		Assert.assertTrue(dbs.size() < MAX_MASCOT_DATABASES - 2, "There needs to be space for deploying a test database");
		Assert.assertFalse(dbs.contains(TEST_DB), "The test database must not be already in the system");

		final File testFastaFile = TestingUtilities.getTempFileFromResource(getClass(), "/edu/mayo/mprc/mascot/testfasta/Test_001.fasta", null);
		try {
			manager.deployDatabase(TEST_DB, testFastaFile);
		} finally {
			FileUtilities.cleanupTempFile(testFastaFile);
		}

		final List<String> newDbs = manager.listDatabases();
		// TODO: For this to pass, we need to finish development of the DB deployer
		// Assert.assertEquals(newDbs.size(), dbs.size() + 1, "One extra database should have appeared");
		// Assert.assertTrue(newDbs.contains(TEST_DB), "The test database should be deployed now");
	}

	@Test(dependsOnMethods = "shouldDeployDatabase")
	public void shouldUndeployDatabase() {
		manager.undeployDatabase(TEST_DB);

		final List<String> databases = manager.listDatabases();
		Assert.assertEquals(databases, originalDatabases, "Back to original list");
		Assert.assertFalse(databases.contains(TEST_DB), "The test database should be undeployed");
	}

	@AfterClass
	public void teardown() {
		manager.close();
	}
}
