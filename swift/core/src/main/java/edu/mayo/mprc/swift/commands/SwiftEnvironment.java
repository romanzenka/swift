package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.config.*;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.swift.Swift;
import edu.mayo.mprc.swift.search.SwiftSearcher;
import joptsimple.OptionParser;

import java.io.File;
import java.util.List;

/**
 * An environment for the running Swift instance. Knows all about the Swift configuration and about the command line parameters.
 * Can be initialized either from command line, or from a servlet configuration.
 *
 * @author Roman Zenka
 */
public interface SwiftEnvironment extends RunningApplicationContext, Lifecycle {
	/**
	 * Runs a Swift command within the Swift environment. The environment changes based on the
	 * command line provided (e.g. current daemon config is set, {@link edu.mayo.mprc.daemon.files.FileTokenFactory} is set up for it etc.
	 * Then the command is executed.
	 *
	 * @param cmdLine Parsed command line.
	 */
	ExitCode runSwiftCommand(final SwiftCommandLine cmdLine);

	/**
	 * Run a Swift command directly.
	 *
	 * @param command    Command to run.
	 * @param configFile Configuration file to use for the environment.
	 * @return Exit code of the command.
	 */
	ExitCode runSwiftCommand(final SwiftCommand command, final File configFile);

	/**
	 * @return Parameters for the Swift command.
	 */
	List<String> getParameters();

	/**
	 * User specifies which environment to run within using the --daemon command line switch.
	 * Daemons are configured in the main Swift configuration file, by default in {@link Swift#CONFIG_FILE_NAME}.
	 *
	 * @return Configuration of the current daemon. A daemon specifies a particular environment
	 *         and a list of services to run in that environment.
	 */
	DaemonConfig getDaemonConfig();

	/**
	 * Manually override the daemon config.
	 *
	 * @param config Daemon config to be used.
	 */
	void setDaemonConfig(DaemonConfig config);

	/**
	 * @return Configuration of the entire application.
	 */
	ApplicationConfig getApplicationConfig();

	/**
	 * For testing purposes mainly.
	 *
	 * @param config The config of the application
	 */
	void setApplicationConfig(ApplicationConfig config);

	/**
	 * Creates a daemon of given configuration.
	 */
	Daemon createDaemon(DaemonConfig config);

	Object createResource(ResourceConfig resourceConfig);

	/**
	 * Provides a connection to a runner. The parameter you pass comes from your worker configuration.
	 *
	 * @param service Configuration of a service to connect to.
	 * @return Connection to the runner performing the particular service.
	 */
	DaemonConnection getConnection(ServiceConfig service);

	/**
	 * @return XML configuration file for entire Swift.
	 */
	File getConfigFile();

	/**
	 * @return The parser that was used to parse the command line parameters.
	 */
	OptionParser getOptionParser();

	/**
	 * Shortcut method for obtaining the one Swift searcher. Fails if none or more are defined.
	 *
	 * @return Configuration for the Swift Searcher (core Swift module).
	 */
	SwiftSearcher.Config getSwiftSearcher();

	/**
	 * Register an additional command.
	 *
	 * @param name    Name of the command without the -command suffix
	 * @param command Command to be added to the list
	 */
	void registerCommand(String name, SwiftCommand command);

	/**
	 * @return A comma-separated list of command names.
	 */
	String listSupportedCommands();
}
