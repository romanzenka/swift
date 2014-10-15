package edu.mayo.mprc.database;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.FactoryDescriptor;
import edu.mayo.mprc.config.ui.FixTag;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.usertype.UserType;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Used for spring injection - holds a session factory that gets created manually after the database is validated.
 * Your beans can depend on the database directly.
 * <p/>
 * This is THE way how to get to the database that all the DAOs use. The DAOs do not hold their own
 * session, they defer to this object, which uses {@link ThreadLocal} storage for the session.
 */
@Component("database")
public final class Database implements RuntimeInitializer, Lifecycle {
	private static final Logger LOGGER = Logger.getLogger(Database.class);
	private SessionFactory sessionFactory;
	private Config config;
	private Map<String, String> hibernateProperties;
	/**
	 * A special set of properties to be used when a new database is being created.
	 * We had problems with C3P0 messing up H2 database creation.
	 */
	private Map<String, String> hibernateCreationProperties;
	private List<String> mappingResources;
	private Map<String, UserType> userTypes;
	private List<RuntimeInitializer> runtimeInitializers;
	private FileTokenToDatabaseTranslator translator;

	public Database() {
		int i = 1;
	}

	@Override
	public void install(final Map<String, String> params) {
		LOGGER.info("Installing database");
		final DatabaseUtilities.SchemaInitialization action = DatabaseUtilities.SchemaInitialization.getForValue(params.get("action"));
		initializeSessionFactory(action, true);

		LOGGER.info("Installing DAOs");
		try {
			beginTransaction();

			for (final RuntimeInitializer initializer : runtimeInitializers) {
				initializer.install(params);
				getSession().flush();
				// We completely wipe out the caches between the initialization steps to prevent
				// huge memory consumption.
				getSession().clear();
			}

			commitTransaction();
		} catch (final Exception e) {
			rollbackTransaction();
			throw new MprcException(e);
		}
	}

	@Override
	public String check() {
		LOGGER.info("Checking database");
		String errors = "";
		try {
			// Before checking, update the schema
			final HashMap<String, String> params = new HashMap<String, String>();
			params.put("action", DatabaseUtilities.SchemaInitialization.Update.getValue());
			final DatabaseUtilities.SchemaInitialization action = DatabaseUtilities.SchemaInitialization.getForValue(params.get("action"));
			initializeSessionFactory(action, true);

			beginTransaction();

			String initializationToDo = null;

			// Go through a list of RuntimeInitializer, stop when one of them reports it is not ready
			for (final RuntimeInitializer initializer : runtimeInitializers) {
				final String result = initializer.check();
				getSession().flush();
				if (result != null) {
					initializationToDo = result;
					break;
				}
			}

			if (initializationToDo != null) {
				errors += "Database is not initialized: " + initializationToDo + " - " + FixTag.getTag(
						DatabaseUtilities.SchemaInitialization.Update.getValue(), "Initialize Database");
			}
			commitTransaction();
		} catch (final Exception e) {
			errors += "Database connection could not be established.<br/>Error: " + e.getMessage()
					+ "<br/>Database may not exist. " + FixTag.getTag(
					DatabaseUtilities.SchemaInitialization.Create.getValue(), "Create Database");
			rollbackTransaction();
		}
		return !errors.isEmpty() ? errors : null;
	}

	public String getDdl() {
		try {
			final Configuration cfg = DatabaseUtilities.getHibernateConfiguration(
					config.getUrl(),
					config.getUserName(),
					config.getPassword(),
					config.getDialect(),
					config.getDriverClassName(),
					config.getDefaultSchema(),
					config.getSchema(),
					getHibernateCreationProperties(),
					getMappingResources(),
					getUserTypes(),
					DatabaseUtilities.SchemaInitialization.Create,
					getTranslator()
			);
//			cfg.setProperty("hibernate.hbm2ddl.auto", "create");
//			cfg.setProperty("hibernate.dialect", config.getDialect());
//			cfg.setProperty("hibernate.connection.schema", config.getSchema());
//			cfg.setNamingStrategy(new SwiftDatabaseNamingStrategy());

			final SchemaExport export = new SchemaExport(cfg);
			export.setDelimiter(";");
			final File tempFile = File.createTempFile("dump", ".sql");

			export.setOutputFile(tempFile.getAbsolutePath());
			export.setFormat(true);
			export.execute(false, false, false, true);
			return Files.toString(tempFile, Charsets.UTF_8);
		} catch (final IOException e) {
			throw new MprcException("Could not get DDL script", e);
		}
	}

	private void initializeSessionFactory(final DatabaseUtilities.SchemaInitialization action, final boolean install) {
		if (sessionFactory == null) {
			final SessionFactory sessionFactory1 = DatabaseUtilities.getSessionFactory(config.getUrl()
					, config.getUserName()
					, config.getPassword()
					, config.getDialect()
					, config.getDriverClassName()
					, config.getDefaultSchema()
					, config.getSchema()
					, install ? hibernateCreationProperties : hibernateProperties
					, getMappingResources()
					, getUserTypes()
					, action
					, translator);

			setSessionFactory(sessionFactory1);
		}
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
		} catch (final Exception e) {
			LOGGER.warn("Cannot rollback transaction", e);
		}
	}

	/**
	 * Session-per-request pattern - commit to database.
	 */
	public void commit() {
		try {
			commitTransaction();
		} catch (final Exception t) {
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
		} catch (final Exception e) {
			// SWALLOWED - failing rollback is not so tragic
			LOGGER.warn("Rollback failed", e);
		}
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(final Config config) {
		this.config = config;
	}

	public Map<String, String> getHibernateProperties() {
		return hibernateProperties;
	}

	public void setHibernateProperties(final Map<String, String> hibernateProperties) {
		this.hibernateProperties = hibernateProperties;
	}

	public Map<String, String> getHibernateCreationProperties() {
		return hibernateCreationProperties;
	}

	public void setHibernateCreationProperties(final Map<String, String> hibernateCreationProperties) {
		this.hibernateCreationProperties = hibernateCreationProperties;
	}

	public List<String> getMappingResources() {
		return mappingResources;
	}

	public void setMappingResources(final List<String> mappingResources) {
		this.mappingResources = mappingResources;
	}

	public Map<String, UserType> getUserTypes() {
		return userTypes;
	}

	public void setUserTypes(Map<String, UserType> userTypes) {
		this.userTypes = userTypes;
	}

	public List<RuntimeInitializer> getRuntimeInitializers() {
		return runtimeInitializers;
	}

	public void setRuntimeInitializers(final List<RuntimeInitializer> runtimeInitializers) {
		this.runtimeInitializers = runtimeInitializers;
	}

	public FileTokenToDatabaseTranslator getTranslator() {
		return translator;
	}

	public void setTranslator(final FileTokenToDatabaseTranslator translator) {
		this.translator = translator;
		if (getUserTypes() != null) {
			// Update all the user types
			for (UserType userType : getUserTypes().values()) {
				if (userType instanceof NeedsTranslator) {
					((NeedsTranslator) userType).setTranslator(translator);
				}
			}
		}
	}

	@Override
	public boolean isRunning() {
		return sessionFactory != null;
	}

	@Override
	public void start() {
		if (!isRunning()) {
			if (getTranslator() instanceof Lifecycle) {
				((Lifecycle) getTranslator()).start();
			}
			initializeSessionFactory(DatabaseUtilities.SchemaInitialization.None, false);
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			if (getTranslator() instanceof Lifecycle) {
				((Lifecycle) getTranslator()).stop();
			}
			sessionFactory.close();
		}
	}

	/**
	 * This factory always returns the same singleton object {@link edu.mayo.mprc.database.Database}.
	 * <p/>
	 * That is because the database is meant to be a singleton.
	 * <p/>
	 * The object gets configured on creation, but needs to be initialized after it gets created to actually function.
	 * Initialization is split from creation so we can run checks on the created object before it goes live.
	 */
	@Component("databaseFactory")
	public static final class Factory extends FactoryBase<ResourceConfig, Database> implements FactoryDescriptor {

		public static final String TYPE = "database";
		public static final String NAME = "Swift SQL Database";
		public static final String DESC = "Database for storing information about Swift searches and Swift configuration.<p>The database gets created and initialized through the module that uses it (in this case, the Swift Searcher module).<p><b>Important:</b> Swift Searcher and Swift Website have to run within the same daemon as the database.";
		private Map<String, String> hibernateProperties;
		private Map<String, String> hibernateCreationProperties;
		private List<DaoBase> daoList;
		private Database database;
		private List<RuntimeInitializer> runtimeInitializers;
		private FileTokenToDatabaseTranslator translator;

		public Factory() {
		}

		/**
		 * Collect all mapping resources from a selection of DAOs and additionally specified mapping files.
		 *
		 * @param daos         List of DAOs.
		 * @param mappingFiles Array of additional mapping files.
		 * @return All resources needed for the DAOs in a list. Each resource listed once.
		 */
		public static List<String> collectMappingResouces(final Collection<? extends DaoBase> daos, final String... mappingFiles) {
			final TreeSet<String> strings = new TreeSet<String>();
			Collections.addAll(strings, mappingFiles);

			for (final DaoBase daoBase : daos) {
				strings.addAll(daoBase.getHibernateMappings());
			}

			return Lists.newArrayList(strings);
		}

		/**
		 * Collect all {@link org.hibernate.usertype.UserType} instances that DAOs need to function.
		 *
		 * @param daos List of DAOs.
		 * @return All user types needed for the DAOs in a "type name" -> UserType map.
		 */
		public static Map<String, UserType> collectUserTypes(final Collection<? extends DaoBase> daos) {
			final Map<String, UserType> userTypes = new HashMap<String, UserType>();

			for (final DaoBase daoBase : daos) {
				userTypes.putAll(daoBase.getUserTypes());
			}

			return userTypes;
		}


		public Map<String, String> getHibernateProperties() {
			return hibernateProperties;
		}

		@Resource(name = "hibernateProperties")
		public void setHibernateProperties(final Map<String, String> hibernateProperties) {
			this.hibernateProperties = hibernateProperties;
		}

		public Map<String, String> getHibernateCreationProperties() {
			return hibernateCreationProperties;
		}

		@Resource(name = "hibernateCreationProperties")
		public void setHibernateCreationProperties(final Map<String, String> hibernateCreationProperties) {
			this.hibernateCreationProperties = hibernateCreationProperties;
		}

		public List<DaoBase> getDaoList() {
			return daoList;
		}

		@Resource
		public void setDaoList(final List<DaoBase> daoList) {
			this.daoList = daoList;
		}

		public Database getDatabase() {
			return database;
		}

		@Resource(name = "database")
		public void setDatabase(final Database database) {
			this.database = database;
		}

		public List<RuntimeInitializer> getRuntimeInitializers() {
			return runtimeInitializers;
		}

		@Resource(name = "searcherRuntimeInitializers")
		public void setRuntimeInitializers(final List<RuntimeInitializer> runtimeInitializers) {
			this.runtimeInitializers = runtimeInitializers;
		}

		public FileTokenToDatabaseTranslator getTranslator() {
			return translator;
		}

		@Resource(name = "fileTokenFactory")
		public void setTranslator(final FileTokenToDatabaseTranslator translator) {
			this.translator = translator;
		}

		@Override
		public Database create(final ResourceConfig config, final DependencyResolver dependencies) {
			if (!(config instanceof Config)) {
				ExceptionUtilities.throwCastException(config, Config.class);
				return null;
			}
			final Config localConfig = (Config) config;

			final Database placeholder = getDatabase();
			placeholder.setConfig(localConfig);
			final Map<String, String> properties = getHibernateProperties();
			fixMsSqlKeywords(localConfig, properties);
			placeholder.setHibernateProperties(properties);
			placeholder.setHibernateCreationProperties(getHibernateCreationProperties());
			placeholder.setMappingResources(collectMappingResouces(getDaoList()));
			placeholder.setUserTypes(collectUserTypes(getDaoList()));
			placeholder.setRuntimeInitializers(getRuntimeInitializers());
			placeholder.setTranslator(getTranslator());
			return placeholder;
		}

		/**
		 * MS SQL has keywords that clash with our schema.
		 * Switch hibernate to quote all table names.
		 * This makes H2 fail, sadly.
		 *
		 * @param localConfig Configuration of the database.
		 * @param properties  Hibernate properties to be patched.
		 */
		private void fixMsSqlKeywords(final Config localConfig, final Map<String, String> properties) {
			if (localConfig.getDialect().contains("SQLServer2008Dialect")) {
				properties.put("hibernate.globally_quoted_identifiers", "true");
			}
		}

		@Override
		public String getType() {
			return TYPE;
		}

		@Override
		public String getUserName() {
			return NAME;
		}

		@Override
		public String getDescription() {
			return DESC;
		}

		@Override
		public Class<? extends ResourceConfig> getConfigClass() {
			return Config.class;
		}

		@Override
		public ServiceUiFactory getServiceUiFactory() {
			return new Ui();
		}
	}

	public Dialect getDialect() {
		if (sessionFactory instanceof SessionFactoryImplementor) {
			final SessionFactoryImplementor implementor = (SessionFactoryImplementor) sessionFactory;
			return implementor.getDialect();
		}
		throw new MprcException("Cannot determine dialect of the database");
	}

	public String getQualifiedTableName(final String name) {
		if (config == null || !(getDialect() instanceof SQLServerDialect)) {
			return name;
		}
		return config.getSchema() + "." + name;
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {

		private String url;
		private String userName;
		private String password;
		private String driverClassName;
		private String dialect;
		private String defaultSchema;
		private String schema;

		public Config() {
		}

		public Config(final String url, final String userName, final String password, final String driverClassName, final String dialect, final String defaultSchema, final String schema) {
			this.url = url;
			this.userName = userName;
			this.password = password;
			this.driverClassName = driverClassName;
			this.dialect = dialect;
			this.defaultSchema = defaultSchema;
			this.schema = schema;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(final String url) {
			this.url = url;
		}

		public String getUserName() {
			return userName;
		}

		public void setUserName(final String userName) {
			this.userName = userName;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(final String password) {
			this.password = password;
		}

		public String getDriverClassName() {
			return driverClassName;
		}

		public void setDriverClassName(final String driverClassName) {
			this.driverClassName = driverClassName;
		}

		public String getDialect() {
			return dialect;
		}

		public void setDialect(final String dialect) {
			this.dialect = dialect;
		}

		public String getDefaultSchema() {
			return defaultSchema;
		}

		public void setDefaultSchema(final String defaultSchema) {
			this.defaultSchema = defaultSchema;
		}

		public String getSchema() {
			return schema;
		}

		public void setSchema(final String schema) {
			this.schema = schema;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put("url", getUrl(), "JDBC-style URL of the server");
			writer.put("username", getUserName(), "Database user");
			writer.put("password", getPassword(), "Database password");
			writer.put("driverClassName", getDriverClassName(), "Name of the JDBC driver class");
			writer.put("dialect", getDialect(), "Database dialect");
			writer.put("defaultSchema", getDefaultSchema(), "Default database schema name");
			writer.put("schema", getSchema(), "Database schema name");
		}

		@Override
		public void load(final ConfigReader reader) {
			url = reader.get("url");
			userName = reader.get("username");
			password = reader.get("password");
			driverClassName = reader.get("driverClassName");
			dialect = reader.get("dialect");
			defaultSchema = reader.get("defaultSchema");
			schema = reader.get("schema");
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	public static final class Ui implements ServiceUiFactory {
		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.nativeInterface("database");
		}
	}
}
