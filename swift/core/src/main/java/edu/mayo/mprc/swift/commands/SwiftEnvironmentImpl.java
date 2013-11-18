package edu.mayo.mprc.swift.commands;

import com.google.common.base.Joiner;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.swift.resources.ResourceTable;
import edu.mayo.mprc.swift.resources.SwiftConfig;
import edu.mayo.mprc.swift.search.SwiftSearcher;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import joptsimple.OptionParser;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;

/**
 * Swift environment - knows of all the resources Swift has. Given a command line,
 * it can initialize itself using the various switches and then run Swift using
 * {@link #runSwiftCommand}.
 */
@Component("swiftEnvironment")
public final class SwiftEnvironmentImpl implements SwiftEnvironment, ApplicationContextAware {
	public static final String COMMAND_SUFFIX = "-command";
	private Daemon.Factory daemonFactory;
	private ResourceTable swiftFactory;

	private ApplicationConfig applicationConfig;
	private DaemonConfig daemonConfig;
	private File configFile;
	private SwiftCommandLine commandLine;
	private ApplicationContext applicationContext;
	private Map<String, SwiftCommand> extraCommands = new HashMap<String, SwiftCommand>(1);

	public SwiftEnvironmentImpl() {
	}

	/**
	 * Load Swift configuration from a given file.
	 *
	 * @param configFile   Swift config file to load config from.
	 * @param swiftFactory A factory of all objects supported by Swift.
	 * @return Loaded Swift configuration.
	 */
	private void loadSwiftConfig(final File configFile, final MultiFactory swiftFactory) {
		if (configFile == null) {
			return;
		}
		ApplicationConfig.load(applicationConfig, configFile.getAbsoluteFile(), swiftFactory);
		checkConfig(applicationConfig);
	}

	/**
	 * Validate the config and output all the found errors on the command line.
	 *
	 * @param swiftConfig Config to check.
	 */
	private static void checkConfig(final ApplicationConfig swiftConfig) {
		final List<String> errorList = SwiftConfig.validateSwiftConfig(swiftConfig);
		if (!errorList.isEmpty()) {
			FileUtilities.err("WARNING: The configuration file has issues, Swift may not function correctly:");
			for (final String error : errorList) {
				FileUtilities.err("\t" + error);
			}
		}
	}

	@Override
	public ExitCode runSwiftCommand(final SwiftCommandLine cmdLine) {
		commandLine = cmdLine;
		final SwiftCommand command = getCommand(commandLine.getCommand());

		if (command == null) {
			throw new MprcException("Unknown command: " + commandLine.getCommand() + "\nSupported: " + listSupportedCommands());
		}

		configFile = commandLine.getInstallFile();

		// We initialize the environment on demand before we run the command
		if (!isRunning()) {
			start();
		}

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
		final String beanName = getBeanNameForCommand(commandName);

		final SwiftCommand swiftCommand = extraCommands.get(beanName);
		if (swiftCommand != null) {
			return swiftCommand;
		}

		final Object bean = applicationContext.getBean(beanName);
		if (bean instanceof SwiftCommand) {
			return (SwiftCommand) bean;
		}
		return null;
	}

	private static String getBeanNameForCommand(final String commandName) {
		return commandName + COMMAND_SUFFIX;
	}

	/**
	 * @return A comma-separated list of command names.
	 */
	private String listSupportedCommands() {
		final String[] commands = applicationContext.getBeanNamesForType(SwiftCommand.class);
		Arrays.sort(commands);
		for (int i = 0; i < commands.length; i++) {
			if (commands[i].endsWith(COMMAND_SUFFIX)) {
				commands[i] = commands[i].substring(0, commands[i].length() - COMMAND_SUFFIX.length());
			}
		}
		return Joiner.on(", ").join(commands);
	}

	public Daemon.Factory getDaemonFactory() {
		return daemonFactory;
	}

	@Resource(name = "daemonFactory")
	public void setDaemonFactory(final Daemon.Factory factory) {
		daemonFactory = factory;
	}

	public MultiFactory getSwiftFactory() {
		return swiftFactory;
	}

	@Resource(name = "resourceTable")
	public void setSwiftFactory(final ResourceTable swiftFactory) {
		this.swiftFactory = swiftFactory;
	}

	public void setDaemonConfig(DaemonConfig daemonConfig) {
		this.daemonConfig = daemonConfig;
	}

	@Override
	public DaemonConfig getDaemonConfig() {
		if (daemonConfig == null) {
			if (configFile != null) {
				daemonConfig = SwiftConfig.getUserSpecifiedDaemonConfig(commandLine.getDaemonId(), getApplicationConfig());
			}
		}

		return daemonConfig;
	}

	public void setApplicationConfig(final ApplicationConfig applicationConfig) {
		this.applicationConfig = applicationConfig;
	}

	@Override
	public ApplicationConfig getApplicationConfig() {
		if (applicationConfig == null) {
			setApplicationConfig(new ApplicationConfig());
		}
		if (applicationConfig.getDaemons().size() == 0) {
			loadSwiftConfig(configFile, getSwiftFactory());
		}
		return applicationConfig;
	}

	@Override
	public Daemon createDaemon(final DaemonConfig config) {
		return daemonFactory.create(config, config.getApplicationConfig().getDependencyResolver());
	}

	@Override
	public Object createResource(final ResourceConfig resourceConfig) {
		return applicationConfig.getDependencyResolver().createSingleton(resourceConfig);
	}

	@Override
	public <T extends ResourceConfig> T getSingletonConfig(final Class<T> clazz) {
		final List<T> matchingConfigs = new ArrayList<T>(2);
		addMatchingToList(clazz, getDaemonConfig().getResources(), matchingConfigs);
		addMatchingToList(clazz, getDaemonConfig().getServices(), matchingConfigs);

		final T singleMatchingResource = getSingleMatchingResource(clazz, matchingConfigs);
		if (singleMatchingResource != null) {
			return singleMatchingResource;
		}
		return getSingleMatchingResource(clazz, getApplicationConfig().getModulesOfConfigType(clazz));
	}

	private <T extends ResourceConfig> void addMatchingToList(final Class<T> clazz, final List<? extends ResourceConfig> resources, final List<T> matchingConfigs) {
		for (final ResourceConfig resource : resources) {
			if (clazz.isInstance(resource)) {
				matchingConfigs.add((T) resource);
			}
		}
	}

	private <T extends ResourceConfig> T getSingleMatchingResource(final Class<T> clazz, final List<T> modulesOfConfigType) {
		if (modulesOfConfigType.size() > 1) {
			throw new MprcException("There is more than one resource of type " + clazz.getName() + " defined");
		}
		if (modulesOfConfigType.isEmpty()) {
			return null;
		}
		return modulesOfConfigType.get(0);
	}

	@Override
	public DaemonConnection getConnection(final ServiceConfig service) {
		final Object singleton = applicationConfig.getDependencyResolver().createSingleton(service);
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
		final List<SwiftSearcher.Config> searchers = getDaemonConfig().getApplicationConfig().getModulesOfConfigType(SwiftSearcher.Config.class);
		if (searchers.size() != 1) {
			throw new MprcException("More than one Swift Searcher defined in this Swift install");
		}
		return searchers.get(0);
	}

	@Override
	public void registerCommand(final String name, final SwiftCommand command) {
		extraCommands.put(getBeanNameForCommand(name), command);
	}

	@Override
	public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public boolean isRunning() {
		return getDaemonFactory().isRunning();
	}

	@Override
	public void start() {
		if (!isRunning()) {
			getDaemonFactory().start();
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			getDaemonFactory().stop();
		}
	}
}
