package edu.mayo.mprc.database;

import com.google.common.collect.Lists;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

@Component("databaseFactory")
public final class DatabaseFactory extends FactoryBase<ResourceConfig, SessionFactory> implements FactoryDescriptor {

	public static final String TYPE = "database";
	public static final String NAME = "Swift SQL Database";
	public static final String DESC = "Database for storing information about Swift searches and Swift configuration.<p>The database gets created and initialized through the module that uses it (in this case, the Swift Searcher module).<p><b>Important:</b> Swift Searcher and Swift Website have to run within the same daemon as the database.";
	private Map<String, String> hibernateProperties;
	private List<DaoBase> daoList;
	private DatabasePlaceholder placeholder;

	public DatabaseFactory() {
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

	public Map<String, String> getHibernateProperties() {
		return hibernateProperties;
	}

	@Resource(name = "hibernateProperties")
	public void setHibernateProperties(final Map<String, String> hibernateProperties) {
		this.hibernateProperties = hibernateProperties;
	}

	public List<DaoBase> getDaoList() {
		return daoList;
	}

	@Resource
	public void setDaoList(final List<DaoBase> daoList) {
		this.daoList = daoList;
	}

	public DatabasePlaceholder getPlaceholder() {
		return placeholder;
	}

	@Resource(name = "databasePlaceholder")
	public void setPlaceholder(final DatabasePlaceholder placeholder) {
		this.placeholder = placeholder;
	}

	@Override
	public SessionFactory create(final ResourceConfig config, final DependencyResolver dependencies) {
		if (!(config instanceof Config)) {
			ExceptionUtilities.throwCastException(config, Config.class);
			return null;
		}
		final Config localConfig = (Config) config;

		final SessionFactory sessionFactory = DatabaseUtilities.getSessionFactory(localConfig.getUrl()
				, localConfig.getUserName()
				, localConfig.getPassword()
				, localConfig.getDialect()
				, localConfig.getDriverClassName()
				, localConfig.getDefaultSchema()
				, localConfig.getSchema()
				, getHibernateProperties()
				, collectMappingResouces(daoList),
				DatabaseUtilities.SchemaInitialization.None);

		placeholder.setSessionFactory(sessionFactory);

		return sessionFactory;
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