package edu.mayo.mprc.database;

import edu.mayo.mprc.MprcException;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.*;

public final class DatabaseUtilities {
	private static final Logger LOGGER = Logger.getLogger(DatabaseUtilities.class);

	private DatabaseUtilities() {
	}

	public enum SchemaInitialization {
		/**
		 * Empties database when creating
		 */
		Create("create"),

		/**
		 * Drops when database gets closed, empties when creating
		 */
		CreateDrop("create-drop"),

		Update("update"),

		None("");

		private String value;

		SchemaInitialization(final String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public static SchemaInitialization getForValue(final String value) {
			SchemaInitialization initialization = Update;
			for (final SchemaInitialization schema : SchemaInitialization.values()) {
				if (schema.getValue().equals(value)) {
					initialization = schema;
				}
			}
			return initialization;
		}
	}

	public static SessionFactory getSessionFactory(final String url, final String userName, final String password,
	                                               final String dialect,
	                                               final String driverClassName,
	                                               final String defaultSchema, final String schema,
	                                               final Map<String, String> hibernateProperties,
	                                               final List<String> mappingResources,
	                                               final SchemaInitialization initialization,
	                                               final FileTokenToDatabaseTranslator translator) {
		try {
			final Configuration cfg = getHibernateConfiguration(url, userName, password, dialect, driverClassName, defaultSchema,
					schema, hibernateProperties, mappingResources, initialization, translator);
			return cfg.buildSessionFactory();
		} catch (Exception t) {
			throw new MprcException("Could not establish database access", t);
		}
	}

	public static Configuration getHibernateConfiguration(final String url, final String userName, final String password, final String dialect,
	                                                      final String driverClassName, final String defaultSchema, final String schema,
	                                                      final Map<String, String> hibernateProperties, final List<String> mappingResources,
	                                                      final SchemaInitialization initialization, final FileTokenToDatabaseTranslator translator) {
		final Configuration cfg = new Configuration();
		cfg.registerTypeOverride(new FileType(translator), new String[]{"file"});

		for (final String resource : mappingResources) {
			cfg.addResource(resource);
		}

		schemaInitialization(cfg, initialization);

		cfg.setProperty("hibernate.connection.driver_class", driverClassName);
		if (userName != null) {
			cfg.setProperty("hibernate.connection.username", userName);
		}
		if (password != null) {
			cfg.setProperty("hibernate.connection.password", password);
		}
		if (dialect != null) {
			cfg.setProperty("hibernate.dialect", dialect);
		}
		if (url != null) {
			cfg.setProperty("hibernate.connection.url", url);
		}
		if (schema != null) {
			cfg.setProperty("hibernate.connection.schema", schema);
		}
		if (defaultSchema != null) {
			cfg.setProperty("hibernate.default_schema", defaultSchema);
		}

		cfg.setNamingStrategy(new SwiftDatabaseNamingStrategy());

		for (final Map.Entry<String, String> entry : hibernateProperties.entrySet()) {
			cfg.setProperty(entry.getKey(), entry.getValue());
		}
		return cfg;
	}

	public static void schemaInitialization(final Configuration cfg, final SchemaInitialization initialization) {
		switch (initialization) {
			case Create:
				cfg.setProperty("hibernate.hbm2ddl.auto", "create");
				break;
			case CreateDrop:
				cfg.setProperty("hibernate.hbm2ddl.auto", "create-drop");
				break;
			case Update:
				cfg.setProperty("hibernate.hbm2ddl.auto", "update");
				break;
			case None:
				cfg.setProperty("hibernate.hbm2ddl.auto", "");
				break;
			default:
				throw new MprcException("Unsupported database initialization operation: " + initialization);
		}
	}

	/**
	 * @param mappingResources List of .hbm.xml files to use for mapping objects.
	 * @return A session factory for a test database.
	 */
	public static Configuration getTestHibernateConfiguration(final List<String> mappingResources) {
		LOGGER.debug("Creating test database configuration");
		final Map<String, String> hibernateProperties = new HashMap<String, String>();
		hibernateProperties.put("hibernate.show_sql", "false");
		hibernateProperties.put("hibernate.statement_cache.size", "100");
		hibernateProperties.put("hibernate.jdbc.batch_size", "100");
		hibernateProperties.put("hibernate.current_session_context_class", "thread");
		hibernateProperties.put("hibernate.transaction.factory_class", "org.hibernate.transaction.JDBCTransactionFactory");
		hibernateProperties.put("hibernate.cache.provider_class", "org.hibernate.cache.HashtableCacheProvider");
		hibernateProperties.put("hibernate.order_inserts", "true");
		hibernateProperties.put("hibernate.order_updates", "true");

		hibernateProperties.put("hibernate.c3p0.min_size", "5");
		hibernateProperties.put("hibernate.c3p0.max_size", "20");
		hibernateProperties.put("hibernate.c3p0.timeout", "6000");
		hibernateProperties.put("hibernate.c3p0.max_statements", "100");
		hibernateProperties.put("hibernate.c3p0.max_statements_per_connection", "20");
		hibernateProperties.put("hibernate.c3p0.idle_connection_test_period", "300");
		hibernateProperties.put("hibernate.connection.provider_class", "org.hibernate.connection.C3P0ConnectionProvider");


		return getHibernateConfiguration(
				//"jdbc:h2:tcp://localhost/~/test",
				"jdbc:h2:mem:test",
				"sa", "", "org.hibernate.dialect.H2Dialect", "org.h2.Driver", "PUBLIC", "PUBLIC", hibernateProperties, mappingResources,
				SchemaInitialization.CreateDrop, new DummyFileTokenTranslator());
	}

	public static SessionFactory getTestSessionFactory(final List<String> mappingResources) {
		return getTestHibernateConfiguration(mappingResources).buildSessionFactory();
	}

	/**
	 * Turns a set of persistable objects into a list of their ids.
	 */
	public static <T extends PersistableBase> Integer[] getIdList(final Collection<T> items) {
		final Integer[] ids = new Integer[items.size()];
		final Iterator<T> iterator = items.iterator();
		for (int i = 0; i < ids.length; i++) {
			final T item = iterator.next();
			ids[i] = item.getId();
		}
		return ids;
	}


}
