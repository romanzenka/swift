package edu.mayo.mprc.swift.configuration.client.view;

import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.swift.configuration.client.model.ResourceModel;

import java.util.HashMap;
import java.util.Map;

public final class DatabaseView extends SimplePanel implements ModuleView {
	public static final String DEFAULT_H2_FILE = "var/db/swift";
	private PropertyList customPropertyList;
	private PropertyList oraclePropertyList;
	private PropertyList mysqlPropertyList;
	private PropertyList h2PropertyList;

	public static final String DRIVER_CLASS_NAME = "driverClassName";
	public static final String DIALECT = "dialect";
	public static final String URL = "url";
	public static final String MYSQL_URL = "mysqlUrl";
	public static final String USER_NAME = "username";
	public static final String PASSWORD = "password";
	public static final String DEFAULT_SCHEMA = "defaultSchema";
	public static final String SCHEMA = "schema";

	private static final String ORACLE_SERVER = "server";
	private static final String ORACLE_PORT = "port";
	private static final String ORACLE_SID = "sid";
	private static final String ORACLE_URL_PREFIX = "jdbc:oracle:thin:@";

	private static final String H2_FILE = "file";
	private static final String H2_URL_PREFIX = "jdbc:h2:";

	private static final String MYSQL_URL_PREFIX = "jdbc:mysql://";

	private final TabPanel databaseConfiguration;

	public DatabaseView(final GwtUiBuilder builder, final ResourceModel model) {
		databaseConfiguration = new TabPanel();
		final PropertyChangeListener listener = new PropertyChangeListener();

		{
			builder.start();
			builder.property(H2_FILE, "Database file", "Where should the database be stored").required()
					.defaultValue(DEFAULT_H2_FILE).addEventListener(listener);
			h2PropertyList = builder.end();
			databaseConfiguration.add(h2PropertyList, "Local");
		}

		{
			builder.start();
			builder.property(ORACLE_SERVER, "Server", "Server where the database is hosted").required().addEventListener(listener);
			builder.property(ORACLE_PORT, "Port", "Database server port").required().addEventListener(listener);
			builder.property(ORACLE_SID, "SID", "System id").required().addEventListener(listener);
			builder.property(USER_NAME, "User name", "Database account user name").required().addEventListener(listener);
			builder.property(PASSWORD, "Password", "Database account password").required().addEventListener(listener);
			builder.property(DRIVER_CLASS_NAME, "Driver", "Database JDBC driver class<p>Bundled driver: <tt>oracle.jdbc.driver.OracleDriver</tt>").defaultValue("oracle.jdbc.driver.OracleDriver").required().addEventListener(listener);
			builder.property(DIALECT, "Dialect", "Database dialect<p>Example: <tt>org.hibernate.dialect.Oracle10gDialect</tt>").defaultValue("org.hibernate.dialect.Oracle10gDialect").required().addEventListener(listener);
			builder.property(DEFAULT_SCHEMA, "Default Schema", "Database default schema name").required().addEventListener(listener);
			builder.property(SCHEMA, "Schema", "Database schema name").required().addEventListener(listener);
			oraclePropertyList = builder.end();
			databaseConfiguration.add(oraclePropertyList, "Oracle");
		}

		{
			builder.start();
			builder.property(MYSQL_URL, "Server", "Database server - <pre>host[:port]</pre>").required().addEventListener(listener);
			builder.property(USER_NAME, "User name", "Database account user name").required().addEventListener(listener);
			builder.property(PASSWORD, "Password", "Database account password").required().addEventListener(listener);
			builder.property(SCHEMA, "Schema", "Database schema name").required().addEventListener(listener);
			builder.property(DRIVER_CLASS_NAME, "Driver", "Database JDBC driver class<p>Bundled driver: <tt>com.mysql.jdbc.Driver</tt>").defaultValue("com.mysql.jdbc.Driver").required().addEventListener(listener);
			builder.property(DIALECT, "Dialect", "Database dialect<p>Choose from<ul><li><tt>org.hibernate.dialect.MySQLInnoDBDialect</tt> - InnoDB (preferred)</li><li><tt>org.hibernate.dialect.MySQLMyISAMDialect</tt> - MyISAM</li></ul>").defaultValue("org.hibernate.dialect.MySQLInnoDBDialect").required().addEventListener(listener);
			mysqlPropertyList = builder.end();
			databaseConfiguration.add(mysqlPropertyList, "MySql");
		}

		{
			// The custom property list is the one that is getting actually loaded/saved
			builder.start();
			builder.property(URL, "URL", "Database JDBC URL").required().addEventListener(listener);
			builder.property(USER_NAME, "User name", "Database account user name").required().addEventListener(listener);
			builder.property(PASSWORD, "Password", "Database account password").required().addEventListener(listener);
			builder.property(DRIVER_CLASS_NAME, "Driver", "Database JDBC driver class.<p>The driver has to be available on the classpath, meaning you would have to modify the Swift startup scripts to include the extra drivers.").required().addEventListener(listener);
			builder.property(DIALECT, "Dialect", "Database dialect").required().addEventListener(listener);
			builder.property(DEFAULT_SCHEMA, "Default Schema", "Database default schema name").required().addEventListener(listener);
			builder.property(SCHEMA, "Schema", "Database schema name").required().addEventListener(listener);
			customPropertyList = builder.end();
			databaseConfiguration.add(customPropertyList, "Custom");
		}

		databaseConfiguration.selectTab(0);
		databaseConfiguration.addTabListener(new TabListener() {
			@Override
			public boolean onBeforeTabSelected(final SourcesTabEvents sourcesTabEvents, final int i) {
				return true;
			}

			@Override
			public void onTabSelected(final SourcesTabEvents sourcesTabEvents, final int i) {
				saveUI();
				// Fire validations for every single resulting property
				customPropertyList.fireValidations();
			}
		});

		setWidget(databaseConfiguration);
	}

	@Override
	public Widget getModuleWidget() {
		return this;
	}

	@Override
	public void loadUI(final Map<String, String> values) {
		if (values == null) {
			return;
		}
		final String urlValue = values.get(URL);
		customPropertyList.loadUI(values);

		if (urlValue == null) {
			values.put(H2_FILE, DEFAULT_H2_FILE);
			h2PropertyList.loadUI(values);
			databaseConfiguration.selectTab(0);
		} else if (urlValue.startsWith(H2_URL_PREFIX)) {
			values.put(H2_FILE, urlValue.substring(H2_URL_PREFIX.length()));
			h2PropertyList.loadUI(values);
			databaseConfiguration.selectTab(0);
		} else if (urlValue.startsWith(ORACLE_URL_PREFIX)) {
			final String str = urlValue.substring(ORACLE_URL_PREFIX.length());
			values.put(ORACLE_SERVER, str.substring(0, str.indexOf(':')));
			values.put(ORACLE_PORT, str.substring(str.indexOf(':') + 1, str.lastIndexOf(':')));
			values.put(ORACLE_SID, str.substring(str.lastIndexOf(':') + 1));
			oraclePropertyList.loadUI(values);
			databaseConfiguration.selectTab(1);
		} else if (urlValue.startsWith(MYSQL_URL_PREFIX)) {
			final String str = urlValue.substring(MYSQL_URL_PREFIX.length());
			final int lastSlash = str.lastIndexOf('/');
			if (lastSlash >= 0) {
				values.put(MYSQL_URL, str.substring(0, lastSlash));
				values.put(SCHEMA, str.substring(lastSlash + 1));
			} else {
				values.put(MYSQL_URL, str);
			}
			mysqlPropertyList.loadUI(values);
			databaseConfiguration.selectTab(2);
		} else {
			databaseConfiguration.selectTab(3);
		}
	}

	/**
	 * Populate the resulting set of parameters based on which tab is selected.
	 *
	 * @return Saved version of the resulting parameter set.
	 */
	@Override
	public HashMap<String, String> saveUI() {
		if (databaseConfiguration.getTabBar().getSelectedTab() == 0) {
			final HashMap<String, String> properties = h2PropertyList.saveUI();

			customPropertyList.setPropertyValue(URL, H2_URL_PREFIX + properties.get(H2_FILE));
			customPropertyList.setPropertyValue(USER_NAME, "sa");
			customPropertyList.setPropertyValue(PASSWORD, "");
			customPropertyList.setPropertyValue(DIALECT, "org.hibernate.dialect.H2Dialect");
			customPropertyList.setPropertyValue(DRIVER_CLASS_NAME, "org.h2.Driver");
			customPropertyList.setPropertyValue(DEFAULT_SCHEMA, "");
			customPropertyList.setPropertyValue(SCHEMA, "");

		} else if (databaseConfiguration.getTabBar().getSelectedTab() == 1) {
			final HashMap<String, String> properties = oraclePropertyList.saveUI();

			customPropertyList.setPropertyValue(URL, ORACLE_URL_PREFIX + properties.get(ORACLE_SERVER) + ":" + properties.get(ORACLE_PORT) + ":" + properties.get(ORACLE_SID));
			customPropertyList.setPropertyValue(USER_NAME, properties.get(USER_NAME));
			customPropertyList.setPropertyValue(PASSWORD, properties.get(PASSWORD));
			customPropertyList.setPropertyValue(DIALECT, properties.get(DIALECT));
			customPropertyList.setPropertyValue(DRIVER_CLASS_NAME, properties.get(DRIVER_CLASS_NAME));
			customPropertyList.setPropertyValue(DEFAULT_SCHEMA, properties.get(DEFAULT_SCHEMA));
			customPropertyList.setPropertyValue(SCHEMA, properties.get(SCHEMA));

		} else if (databaseConfiguration.getTabBar().getSelectedTab() == 2) {
			final HashMap<String, String> properties = mysqlPropertyList.saveUI();

			customPropertyList.setPropertyValue(URL, MYSQL_URL_PREFIX + properties.get(MYSQL_URL) + "/" + properties.get(SCHEMA));
			customPropertyList.setPropertyValue(USER_NAME, properties.get(USER_NAME));
			customPropertyList.setPropertyValue(PASSWORD, properties.get(PASSWORD));
			customPropertyList.setPropertyValue(DIALECT, properties.get(DIALECT));
			customPropertyList.setPropertyValue(DRIVER_CLASS_NAME, properties.get(DRIVER_CLASS_NAME));
			customPropertyList.setPropertyValue(DEFAULT_SCHEMA, properties.get(SCHEMA));
			customPropertyList.setPropertyValue(SCHEMA, properties.get(SCHEMA));
		}

		return customPropertyList.saveUI();
	}

	private class PropertyChangeListener implements ChangeListener {
		@Override
		public void onChange(final Widget sender) {
			saveUI();
		}
	}
}
