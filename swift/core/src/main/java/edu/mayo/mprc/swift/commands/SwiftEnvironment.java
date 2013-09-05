package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.config.*;
import edu.mayo.mprc.daemon.Daemon;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.MessageBroker;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.swift.ExitCode;
import edu.mayo.mprc.swift.Swift;
import edu.mayo.mprc.swift.SwiftMonitor;
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
public interface SwiftEnvironment extends RunningApplicationContext {
	/**
	 * Runs a Swift command within the Swift environment. The environment changes based on the
	 * command line provided (e.g. current daemon config is set, {@link FileTokenFactory} is set up for it etc.
	 * Then the command is executed.
	 *
	 * @param cmdLine Parsed command line.
	 */
	ExitCode runSwiftCommand(final SwiftCommandLine cmdLine);

	/**
	 * @return Parameters for the Swift command.
	 */
	List<String> getParameters();

	/**
	 * User specifies which environment to run within using the --daemon command line switch.
	 * Daemons are configured in the main Swift configuration file, by default in {@link Swift#CONFIG_FILE_NAME}.
	 * <p/>
	 * The side effect of this function is initialization of the {@link FileTokenFactory}. This can be done only
	 * if the daemon is known.
	 *
	 * @return Configuration of the current daemon. A daemon specifies a particular environment
	 *         and a list of services to run in that environment.
	 */
	DaemonConfig getDaemonConfig();

	/**
	 * @return Configuration of the entire application.
	 */
	ApplicationConfig getApplicationConfig();

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
	 * @return Configuration for the Swift Searcher (core Swift module).
	 */
	SwiftSearcher.Config getSwiftSearcher();

	/**
	 * Shortcut method for obtaining the message broker config. Fails if none or more are defined.
	 */
	MessageBroker.Config getMessageBroker();

	/**
	 * Creates a Swift monitor instance on demand (returns an existing one if one exists).
	 * @return Swift monitor (periodically pings all daemon services, captures their responses in a status report).
	 */
	SwiftMonitor getMonitor();
}
