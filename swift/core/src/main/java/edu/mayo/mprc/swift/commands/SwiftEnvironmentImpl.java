package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.DaemonConnectionFactory;
import edu.mayo.mprc.daemon.MessageBroker;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.swift.ExitCode;
import edu.mayo.mprc.swift.SwiftConfig;
import edu.mayo.mprc.swift.SwiftMonitor;
import edu.mayo.mprc.swift.search.SwiftSearcher;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import joptsimple.OptionParser;

import java.io.File;
import java.util.List;

/**
 * Swift environment - knows of all the resources Swift has. Given a command line,
 * it can initialize itself using the various switches and then run Swift using
 * {@link #runSwiftCommand}.
 */
public final class SwiftEnvironmentImpl implements SwiftEnvironment {
	private FileTokenFactory fileTokenFactory;
	private Daemon.Factory daemonFactory;
	private DaemonConnectionFactory daemonConnectionFactory;
	private MultiFactory swiftFactory;
	private List<SwiftCommand> commands;

	private ApplicationConfig applicationConfig;
	private DaemonConfig daemonConfig;
	private DependencyResolver dependencyResolver;
	private File configFile;
	private SwiftCommandLine commandLine;
	private SwiftMonitor monitor;

	public SwiftEnvironmentImpl() {
	}

	/**
	 * Load Swift configuration from a given file.
	 *
	 * @param configFile Swift config file to load config from.
	 * @param swiftFactory  A factory of all objects supported by Swift.
	 * @return Loaded Swift configuration.
	 */
	private static ApplicationConfig loadSwiftConfig(final File configFile, final MultiFactory swiftFactory) {
		final ApplicationConfig swiftConfig = ApplicationConfig.load(configFile.getAbsoluteFile(), swiftFactory);
		checkConfig(swiftConfig);
		return swiftConfig;
	}

	/**
	 * Validate the config and output all the found errors on the command line.
	 *
	 * @param swiftConfig Config to check.
	 */
	private static void checkConfig(final ApplicationConfig swiftConfig) {
		final List<String> errorList = SwiftConfig.validateSwiftConfig(swiftConfig);
		if (errorList.size() > 0) {
			FileUtilities.err("WARNING: The configuration file has issues, Swift may not function correctly:");
			for (final String error : errorList) {
				FileUtilities.err("\t" + error);
			}
		}
	}

	@Override
	public ExitCode runSwiftCommand(final SwiftCommandLine cmdLine) {
		this.commandLine = cmdLine;
		final SwiftCommand command = getCommand(commandLine.getCommand());

		if (command == null) {
			throw new MprcException("Unknown command: " + commandLine.getCommand() + "\nSupported: " + listSupportedCommands());
		}

		configFile = commandLine.getInstallFile();

		return command.run(this);
	}

	@Override
	public List<String> getParameters() {
		return commandLine.getParameters();
	}

	/**
	 * @param commandName Name of a command to find.
	 * @return The command to be executed or null if no such command exists.
	 */
	private SwiftCommand getCommand(final String commandName) {
		for (final SwiftCommand command : commands) {
			if (command.getName().equalsIgnoreCase(commandName)) {
				return command;
			}
		}
		return null;
	}

	/**
	 * @return A comma-separated list of command names.
	 */
	private String listSupportedCommands() {
		final StringBuilder supportedCommands = new StringBuilder(commands.size() * 10);
		for (final SwiftCommand command : commands) {
			supportedCommands.append(command.getName());
			supportedCommands.append(", ");
		}
		return supportedCommands.substring(0, supportedCommands.length() - 2);
	}

	public FileTokenFactory getFileTokenFactory() {
		return fileTokenFactory;
	}

	public void setFileTokenFactory(final FileTokenFactory fileTokenFactory) {
		this.fileTokenFactory = fileTokenFactory;
	}

	public Daemon.Factory getDaemonFactory() {
		return daemonFactory;
	}

	public void setDaemonFactory(final Daemon.Factory factory) {
		this.daemonFactory = factory;
	}

	public MultiFactory getSwiftFactory() {
		return swiftFactory;
	}

	public void setSwiftFactory(final MultiFactory swiftFactory) {
		dependencyResolver = new DependencyResolver(swiftFactory);
		this.swiftFactory = swiftFactory;
	}

	public List<SwiftCommand> getCommands() {
		return commands;
	}

	public void setCommands(final List<SwiftCommand> commands) {
		this.commands = commands;
	}

	public DaemonConnectionFactory getDaemonConnectionFactory() {
		return daemonConnectionFactory;
	}

	public void setDaemonConnectionFactory(DaemonConnectionFactory daemonConnectionFactory) {
		this.daemonConnectionFactory = daemonConnectionFactory;
	}

	@Override
	public DaemonConfig getDaemonConfig() {
		if (daemonConfig == null) {
			if (configFile != null) {
				daemonConfig = SwiftConfig.getUserSpecifiedDaemonConfig(commandLine.getDaemonId(), getApplicationConfig());
				SwiftConfig.setupFileTokenFactory(getApplicationConfig(), daemonConfig, getFileTokenFactory());
			}
		}

		return daemonConfig;
	}

	private void setApplicationConfig(ApplicationConfig applicationConfig) {
		this.applicationConfig = applicationConfig;
		monitor.initialize(applicationConfig);
	}

	@Override
	public ApplicationConfig getApplicationConfig() {
		if (applicationConfig == null) {
			setApplicationConfig(loadSwiftConfig(configFile, getSwiftFactory()));
			initDaemonConnectionFactory();
		}
		return applicationConfig;
	}

	private void initDaemonConnectionFactory() {
		final List<ResourceConfig> brokers = getApplicationConfig().getModulesOfConfigType(MessageBroker.Config.class);
		if (brokers.size() > 0) {
			MessageBroker.Config config = (MessageBroker.Config) brokers.get(0);
			daemonConnectionFactory.setBrokerUrl(config.getBrokerUrl());
		}
	}

	@Override
	public Daemon createDaemon(final DaemonConfig config) {
		return daemonFactory.create(config, dependencyResolver);
	}

	@Override
	public Object createResource(final ResourceConfig resourceConfig) {
		return dependencyResolver.createSingleton(resourceConfig);
	}

	@Override
	public DaemonConnection getConnection(final ServiceConfig service) {
		final Object singleton = dependencyResolver.createSingleton(service);
		if (singleton instanceof DaemonConnection) {
			return (DaemonConnection) singleton;
		} else {
			ExceptionUtilities.throwCastException(singleton, DaemonConnection.class);
			return null;
		}
	}

	@Override
	public File getConfigFile() {
		return configFile;
	}

	@Override
	public OptionParser getOptionParser() {
		return commandLine.getParser();
	}

	@Override
	public SwiftSearcher.Config getSwiftSearcher() {
		final List<ResourceConfig> searchers = getDaemonConfig().getApplicationConfig().getModulesOfConfigType(SwiftSearcher.Config.class);
		if (searchers.size() != 1) {
			throw new MprcException("More than one Swift Searcher defined in this Swift install");
		}
		return (SwiftSearcher.Config) searchers.get(0);
	}

	public SwiftMonitor getMonitor() {
		return monitor;
	}

	public void setMonitor(SwiftMonitor monitor) {
		this.monitor = monitor;
	}
}
