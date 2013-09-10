package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.MultiFactory;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.daemon.MessageBroker;
import edu.mayo.mprc.dbcurator.server.CurationWebContext;
import edu.mayo.mprc.messaging.ActiveMQConnectionPool;
import edu.mayo.mprc.messaging.ServiceFactory;
import edu.mayo.mprc.swift.ExitCode;
import edu.mayo.mprc.swift.SwiftMonitor;
import edu.mayo.mprc.swift.WebUi;
import edu.mayo.mprc.swift.WebUiHolder;
import edu.mayo.mprc.swift.search.DefaultSwiftSearcherCaller;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author Roman Zenka
 */
@Component("initializeWebCommand")
public final class InitializeWebCommand implements SwiftCommand {
	private static final Logger LOGGER = Logger.getLogger(InitializeWebCommand.class);

	public static final String INITIALIZE_WEB = "initialize-web";

	private WebUiHolder webUiHolder;
	private MultiFactory factoryTable;
	private ServiceFactory serviceFactory;
	private CurationWebContext curationWebContext;
	private SwiftMonitor swiftMonitor;
	private ActiveMQConnectionPool connectionPool;
	private DefaultSwiftSearcherCaller swiftSearcherCaller;

	@Override
	public String getName() {
		return INITIALIZE_WEB;
	}

	@Override
	public String getDescription() {
		return "Initializes Swift's web interface. For internal Swift use.";
	}

	@Override
	public ExitCode run(final SwiftEnvironment environment) {
		try {
			System.setProperty("SWIFT_INSTALL", environment.getConfigFile().getAbsolutePath());

			final DaemonConfig daemonConfig = environment.getDaemonConfig();

			// The service factory needs to be initialized by message broker config
			final MessageBroker.Config messageBroker = SwiftEnvironmentImpl.getMessageBroker(daemonConfig);
			serviceFactory.initialize(messageBroker.getBrokerUrl(), daemonConfig.getName());

			// WebUi needs reference to the actual daemon
			final Daemon daemon = environment.createDaemon(daemonConfig);

			final WebUi webUi = webUiHolder.getWebUi();

			if (webUi == null) {
				throw new MprcException("The daemon " + daemonConfig.getName() + " does not define any web interface module.");
			}
			webUi.setMainDaemon(daemon);
			getSwiftSearcherCaller().setSwiftSearcherConnection(webUi.getSwiftSearcherDaemonConnection());
			getSwiftSearcherCaller().setBrowseRoot(webUi.getBrowseRoot());

			// Initialize DB curator
			curationWebContext.initialize(
					webUi.getFastaFolder(),
					webUi.getFastaUploadFolder(),
					webUi.getFastaArchiveFolder(),
					// TODO: Fix this - the curator will keep creating temp folders and never deleting them
					// TODO: Also, the user should be able to specify where the temp files should go
					FileUtilities.createTempFolder());

			// Start all the services
			daemon.start();

			// Start the ping monitor
			getSwiftMonitor().initialize(environment.getApplicationConfig());
			getSwiftMonitor().start();

		} catch (Exception t) {
			LOGGER.fatal("Swift web application should be terminated", t);
			if (webUiHolder != null) {
				webUiHolder.stopSwiftMonitor();
			}
			getConnectionPool().close();
			return ExitCode.Error;
		}
		return ExitCode.Ok;
	}

	public WebUiHolder getWebUiHolder() {
		return webUiHolder;
	}

	@Resource(name = "webUiHolder")
	public void setWebUiHolder(final WebUiHolder webUiHolder) {
		this.webUiHolder = webUiHolder;
	}

	public MultiFactory getFactoryTable() {
		return factoryTable;
	}

	@Resource(name = "resourceTable")
	public void setFactoryTable(final MultiFactory factoryTable) {
		this.factoryTable = factoryTable;
	}

	public ServiceFactory getServiceFactory() {
		return serviceFactory;
	}

	@Resource(name = "serviceFactory")
	public void setServiceFactory(final ServiceFactory serviceFactory) {
		this.serviceFactory = serviceFactory;
	}

	public CurationWebContext getCurationWebContext() {
		return curationWebContext;
	}

	@Resource(name = "curationWebContext")
	public void setCurationWebContext(final CurationWebContext curationWebContext) {
		this.curationWebContext = curationWebContext;
	}

	public SwiftMonitor getSwiftMonitor() {
		return swiftMonitor;
	}

	@Resource(name = "swiftMonitor")
	public void setSwiftMonitor(final SwiftMonitor swiftMonitor) {
		this.swiftMonitor = swiftMonitor;
	}

	public ActiveMQConnectionPool getConnectionPool() {
		return connectionPool;
	}

	@Resource(name = "connectionPool")
	public void setConnectionPool(final ActiveMQConnectionPool connectionPool) {
		this.connectionPool = connectionPool;
	}

	public DefaultSwiftSearcherCaller getSwiftSearcherCaller() {
		return swiftSearcherCaller;
	}

	@Resource(name = "swiftSearcherCaller")
	public void setSwiftSearcherCaller(final DefaultSwiftSearcherCaller swiftSearcherCaller) {
		this.swiftSearcherCaller = swiftSearcherCaller;
	}
}
