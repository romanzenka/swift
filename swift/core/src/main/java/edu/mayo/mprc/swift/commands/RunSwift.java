package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.messaging.ActiveMQConnectionPool;
import edu.mayo.mprc.messaging.ServiceFactory;
import edu.mayo.mprc.swift.ExitCode;
import edu.mayo.mprc.swift.WebUi;
import edu.mayo.mprc.utilities.FileListener;
import edu.mayo.mprc.utilities.FileMonitor;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

/**
 * Run the Swift as a daemon.
 *
 * @author Roman Zenka
 */
@Component("swiftRunCommand")
public class RunSwift implements FileListener, SwiftCommand {
	private static final Logger LOGGER = Logger.getLogger(RunSwift.class);
	public static final String RUN_SWIFT = "run-swift";

	private final CountDownLatch configFileChanged = new CountDownLatch(1);
	private ActiveMQConnectionPool connectionPool;
	private ServiceFactory serviceFactory;

	private SwiftCommand runSwiftWeb;
	private SwiftCommand runSwiftConfig;

	@Override
	public String getName() {
		return RUN_SWIFT;
	}

	@Override
	public String getDescription() {
		return "Runs all workers defined for this daemon. This is the default command.";
	}

	/**
	 * Run all workers configured for this daemon.
	 */
	@Override
	public ExitCode run(final SwiftEnvironment environment) {
		final DaemonConfig config = environment.getDaemonConfig();
		final File configFile = environment.getConfigFile();

		if (config == null || configFile == null) {
			return getRunSwiftConfig().run(environment);
		}

		if (containsWebModule(config)) {
			return getRunSwiftWeb().run(environment);
		}

		serviceFactory.initialize(environment.getMessageBroker().getBrokerUrl(), config.getName());
		final Daemon daemon = environment.createDaemon(config);
		LOGGER.debug(daemon.toString());

		startListeningToConfigFileChanges(configFile);

		boolean terminateDaemon = true;

		if (daemon.getNumRunners() > 0) {
			try {
				daemon.check();
			} catch (MprcException e) {
				// SWALLOWED: We just display the exception
				LOGGER.error("Daemon check failed, terminating", e);
				return ExitCode.Error;
			}
			daemon.start();

			try {
				configFileChanged.await();
				// Since the config file changed, we want to restart, not terminate
				terminateDaemon = false;
			} catch (InterruptedException ignore) {
				LOGGER.info("Execution interrupted");
			}
			if (terminateDaemon) {
				LOGGER.info("Stopping the daemon");
				daemon.stop();
			} else {
				LOGGER.info("Clean shutdown of daemon initiated");
				daemon.stop();
				LOGGER.info("Waiting for tasks to be completed");
				daemon.awaitTermination();
			}
			LOGGER.info("Daemon stopped");
			FileUtilities.closeQuietly(connectionPool);
		} else {
			throw new MprcException("No daemons are configured in " + configFile.getAbsolutePath() + ". Exiting.");
		}

		return terminateDaemon ? ExitCode.Ok : ExitCode.Restart;
	}

	/**
	 * Setup listener to config file, so when the config file changes, we restart.
	 *
	 * @param configFile File to check.
	 */
	private void startListeningToConfigFileChanges(final File configFile) {
		final FileMonitor monitor = new FileMonitor(10 * 1000);
		monitor.fileToBeChanged(configFile, this);
	}

	/**
	 * Makes sure we are not running a web module as a classical daemon. The web module has to be run using a wrapper.
	 *
	 * @param config Config of the deamon we are trying to run.
	 */
	private static boolean containsWebModule(final DaemonConfig config) {
		for (final ResourceConfig resourceConfig : config.getResources()) {
			if (resourceConfig instanceof WebUi.Config) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void fileChanged(final Collection<File> file, final boolean timeout) {
		configFileChanged.countDown();
	}

	public ActiveMQConnectionPool getConnectionPool() {
		return connectionPool;
	}

	@Resource(name = "connectionPool")
	public void setConnectionPool(ActiveMQConnectionPool connectionPool) {
		this.connectionPool = connectionPool;
	}

	public ServiceFactory getServiceFactory() {
		return serviceFactory;
	}

	@Resource(name = "serviceFactory")
	public void setServiceFactory(ServiceFactory serviceFactory) {
		this.serviceFactory = serviceFactory;
	}

	public SwiftCommand getRunSwiftWeb() {
		return runSwiftWeb;
	}

	@Resource(name = "swiftRunWebCommand")
	public void setRunSwiftWeb(SwiftCommand runSwiftWeb) {
		this.runSwiftWeb = runSwiftWeb;
	}

	public SwiftCommand getRunSwiftConfig() {
		return runSwiftConfig;
	}

	@Resource(name="swiftRunConfigCommand")
	public void setRunSwiftConfig(SwiftCommand runSwiftConfig) {
		this.runSwiftConfig = runSwiftConfig;
	}
}
