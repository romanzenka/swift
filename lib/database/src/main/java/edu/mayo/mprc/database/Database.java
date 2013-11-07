package edu.mayo.mprc.database;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.Installable;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Used for spring injection - holds a session factory that gets created manually after the database is validated.
 * Your beans can depend on the placeholder directly.
 * <p/>
 * This is THE way how to get to the database that all the DAOs use. The DAOs do not hold their own
 * session, they defer to this object, which uses {@link ThreadLocal} storage for the session.
 */
@Component("database")
public final class Database implements Installable {
	private static final Logger LOGGER = Logger.getLogger(Database.class);
	private SessionFactory sessionFactory;
	private DatabaseFactory.Config config;
	private Map<String, String> hibernateProperties;
	private List<String> mappingResources;

	public Database() {
	}

	@Override
	public void install() {
		initialize(DatabaseUtilities.SchemaInitialization.Update);
	}

	private void initialize(DatabaseUtilities.SchemaInitialization initialization) {
		final SessionFactory sessionFactory = DatabaseUtilities.getSessionFactory(config.getUrl()
				, config.getUserName()
				, config.getPassword()
				, config.getDialect()
				, config.getDriverClassName()
				, config.getDefaultSchema()
				, config.getSchema()
				, hibernateProperties
				, mappingResources,
				initialization);

		setSessionFactory(sessionFactory);
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(final SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Returns current session that is ready to be used.
	 *
	 * @return Current session.
	 */
	public Session getSession() {
		if (sessionFactory.getCurrentSession() == null) {
			throw new MprcException("A database session was not yet initialized for this call");
		}
		return sessionFactory.getCurrentSession();
	}

	/**
	 * Session-per-request pattern - start database interaction.
	 */
	public void begin() {
		beginTransaction();
	}

	/**
	 * Session-per-request pattern - exception occured, rollback.
	 */
	public void rollback() {
		try {
			rollbackTransaction();
		} catch (Exception e) {
			LOGGER.warn("Cannot rollback transaction", e);
		}
	}

	/**
	 * Session-per-request pattern - commit to database.
	 */
	public void commit() {
		try {
			commitTransaction();
		} catch (Exception t) {
			rollbackTransaction();
			throw new MprcException("Could not commit data to database", t);
		}
	}

	/**
	 * Flush the session before transaction ends.
	 * <p/>
	 * Never use this function together with
	 */
	public void flushSession() {
		getSession().flush();
	}

	/**
	 * Begins a new transaction using the current session. Use this method only in high-level
	 * code - e.g. one web server request should be done in one transaction.
	 */
	public void beginTransaction() {
		getSession().beginTransaction();
	}

	/**
	 * Commits the transaction.
	 * <p/>
	 * After commit, the current session is automatically closed and
	 * can no longer be used by this thread, so make sure the commit is the very last action.
	 */
	public void commitTransaction() {
		final Session session = getSession();
		if (session == null || !session.isConnected() || session.getTransaction() == null) {
			throw new MprcException("No transaction is running");
		}
		session.getTransaction().commit();
	}

	/**
	 * Rolls back the transaction. After rollback, the current session is automatically closed
	 * and can no longer be used by this thread, so make sure the rollback is the very last action.
	 */
	public void rollbackTransaction() {
		final Session session = getSession();
		try {
			if (session == null || !session.isConnected() || session.getTransaction() == null) {
				throw new MprcException("No transaction is running");
			}
			session.getTransaction().rollback();
		} catch (Exception e) {
			// SWALLOWED - failing rollback is not so tragic
			LOGGER.warn("Rollback failed", e);
		}
	}

	public DatabaseFactory.Config getConfig() {
		return config;
	}

	public void setConfig(DatabaseFactory.Config config) {
		this.config = config;
	}

	public Map<String, String> getHibernateProperties() {
		return hibernateProperties;
	}

	public void setHibernateProperties(Map<String, String> hibernateProperties) {
		this.hibernateProperties = hibernateProperties;
	}

	public List<String> getMappingResources() {
		return mappingResources;
	}

	public void setMappingResources(List<String> mappingResources) {
		this.mappingResources = mappingResources;
	}
}
