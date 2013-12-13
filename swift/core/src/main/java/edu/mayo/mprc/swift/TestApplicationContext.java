package edu.mayo.mprc.swift;

import edu.mayo.mprc.config.*;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.daemon.MessageBroker;
import edu.mayo.mprc.daemon.SimpleRunner;
import edu.mayo.mprc.database.Database;
import edu.mayo.mprc.swift.commands.SwiftCommandLine;
import edu.mayo.mprc.swift.commands.SwiftEnvironment;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.params2.ParamsDao;
import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.swift.search.SwiftSearcher;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;

/**
 * Support for setting up Spring test context.
 * <p/>
 * It basically runs a test Swift application with its own simple config.
 * Messaging broker and database get set up and executed.
 */
public final class TestApplicationContext implements Lifecycle {
	private static final Logger LOGGER = Logger.getLogger(TestApplicationContext.class);
	private ApplicationContext testContext;
	private SwiftEnvironment swiftEnvironment;
	private DaemonConfig daemonConfig;
	private Daemon daemon;

	/**
	 * This creates a context that overrides swiftContext.xml with properties in testContext.xml.  All tests should use
	 * this context.  If your test dirties the context for some reason then you will need to reset the context using the
	 * AfterClass or AfterTest methods.  Tests should be able to act on the database as it exists in this context and
	 * IF a test dirties the database so it is not usable by other tests (IT SHOULDN'T) then that test needs to drop
	 * and rebuild the database.  An alternative would be to manage the transaction for the session by starting a new Transaction
	 * before the test and rolling it back after the test.
	 */
	public TestApplicationContext() {
		initialize(getInMemoryDatabaseConfig());
	}

	@Override
	public boolean isRunning() {
		return swiftEnvironment != null;
	}

	@Override
	public void start() {
		final SwiftCommandLine cmdLine = new SwiftCommandLine("create-test", null, null, daemonConfig.getName(), null, null);
		swiftEnvironment.runSwiftCommand(cmdLine);
		final CreateTestCommand command = (CreateTestCommand) testContext.getBean("create-test-command");
		daemon = command.getDaemon();
	}

	@Override
	public void stop() {
		if (daemon != null) {
			daemon.stop();
		}
		swiftEnvironment.stop();
	}

	private void initialize(final Database.Config databaseConfig) {
		System.setProperty("SWIFT_INSTALL",
				new File(System.getenv("SWIFT_HOME"), "install.properties").getAbsolutePath());

		testContext = new ClassPathXmlApplicationContext(new String[]{"/testContext.xml"});

		LOGGER.info("Setting up Test Database.");

		// Create a test application config with one daemon, message broker, database and searcher
		final ApplicationConfig testConfig = new ApplicationConfig(null);

		daemonConfig = defaultDaemonConfig(databaseConfig, testConfig);

		swiftEnvironment = (SwiftEnvironment) testContext.getBean("swiftEnvironment");
		testConfig.setDependencyResolver(new DependencyResolver(getResourceTable()));
		swiftEnvironment.setApplicationConfig(testConfig);
		swiftEnvironment.setDaemonConfig(daemonConfig);
	}

	private static DaemonConfig defaultDaemonConfig(Database.Config database, ApplicationConfig application) {
		final String fastaFolder = FileUtilities.createTempFolder().getAbsolutePath();
		final String fastaArchiveFolder = FileUtilities.createTempFolder().getAbsolutePath();
		final String fastaUploadFolder = FileUtilities.createTempFolder().getAbsolutePath();
		final String tempFolder = FileUtilities.createTempFolder().getAbsolutePath();

		final DaemonConfig daemonConfig = DaemonConfig.getDefaultDaemonConfig("test", true);
		daemonConfig.setTempFolderPath(tempFolder);
		application.addDaemon(daemonConfig);

		final MessageBroker.Config messageBrokerConfig = MessageBroker.Config.getEmbeddedBroker();
		daemonConfig.addResource(messageBrokerConfig);

		daemonConfig.addResource(database);

		final SwiftSearcher.Config searcherConfig = new SwiftSearcher.Config(
				fastaFolder, fastaArchiveFolder, fastaUploadFolder,
				null, null, null, null, null, null, null, null, null, null, database);

		ServiceConfig searcherService = new ServiceConfig("searcher1", new SimpleRunner.Config(searcherConfig));

		final WebUi.Config webUiConfig = new WebUi.Config(searcherService, "18080", "Swift Test", tempFolder, tempFolder, null, null, null);

		daemonConfig.addResource(webUiConfig);


		daemonConfig.addResource(searcherService);
		return daemonConfig;
	}

	private static Database.Config getInMemoryDatabaseConfig() {
		return new Database.Config(
				"jdbc:h2:mem:test",
				"sa",
				"",
				"org.h2.Driver",
				"org.hibernate.dialect.H2Dialect",
				"PUBLIC",
				"PUBLIC");
	}

	/**
	 * Returns a bean of a given id.
	 *
	 * @param beanId Bean id we want.
	 * @return The bean for {@code beanId}.
	 */
	private Object getBean(final String beanId) {
		return testContext.getBean(beanId);
	}

	/* ============================================================================================================== */

	public SwiftDao getSwiftDao() {
		return (SwiftDao) getBean("swiftDao");
	}

	public ParamsDao getParamsDao() {
		return (ParamsDao) getBean("paramsDao");
	}

	public String getTitle() {
		return (String) getBean("title");
	}

	public MultiFactory getResourceTable() {
		return (MultiFactory) getBean("resourceTable");
	}
}
