package edu.mayo.mprc.database;

import org.hibernate.SessionFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * This is a base class providing utilities to test a Dao.
 * It will setup a fresh in-memory database before every method call.
 */
public abstract class DaoTest {
	private SessionFactory factory;
	private final Database database = new Database();

	/**
	 * Shortcut for {@link #initializeDatabase(java.util.Collection, String...)}.
	 *
	 * @param daoToInitialize Single dao to initialize.
	 */
	public void initializeDatabase(final DaoBase daoToInitialize) {
		initializeDatabase(Arrays.asList(daoToInitialize));
	}

	/**
	 * Initializes the database and given DAOs with it.
	 *
	 * @param daosToInitialize List of DAOs to initialize. The DAOs also list hibernate mapping files needed.
	 * @param mappingFiles     Additional mapping files to use.
	 */
	public void initializeDatabase(final Collection<? extends DaoBase> daosToInitialize, final String... mappingFiles) {
		final List<String> mappingResources = Database.Factory.collectMappingResouces(daosToInitialize, mappingFiles);

		factory = DatabaseUtilities.getTestSessionFactory(mappingResources);
		database.setSessionFactory(factory);

		for (final DaoBase daoBase : daosToInitialize) {
			daoBase.setDatabase(database);
		}
	}

	/**
	 * Closes the current database.
	 */
	public void teardownDatabase() {
		factory.close();
	}

	/**
	 * @return Current database placeholder if you need to create e.g. an additional DAO.
	 */
	public Database getDatabase() {
		return database;
	}

	/**
	 * Utility method that will commit the current transaction and immediatelly restart a new one.
	 * Useful for tests that need to do some action in multiple transactions.
	 */
	public void nextTransaction() {
		database.flushSession();
		database.getSession().clear();
		database.commit();
		database.begin();
	}
}
