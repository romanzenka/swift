package edu.mayo.mprc.swift;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.ReleaseInfoCore;
import edu.mayo.mprc.swift.commands.*;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;

/**
 * Central Swift entry point. Based on the command line options, instantiates either normal
 * or SGE Swift instance and runs it.
 */
public final class Swift {
	public static final String DEFAULT_NEW_CONFIG_FILE = "var/conf/swift.conf";
	private static final Logger LOGGER = Logger.getLogger(Swift.class);

	public static final String CONFIG_FILE_NAME = "conf/swift.conf";

	private Swift() {
	}

	public static void main(final String[] args) {
		try {
			final ExitCode result = runSwift(args);
			result.exit();
		} catch (Throwable t) {
			// SWALLOWED: Dump the exception on the stderr, not much we can do
			FileUtilities.err(MprcException.getDetailedMessage(t));
		} finally {
			ExitCode.Error.exit();
		}
	}

	/**
	 * Actually executes Swift.
	 *
	 * @param args Command line arguments.
	 * @return Requested exit code.
	 */
	static ExitCode runSwift(final String... args) {
		MainFactoryContext.initialize();

		LOGGER.info("Swift " + ReleaseInfoCore.buildVersion());
		final CommandLineParser parser = new CommandLineParser(args);
		final SwiftCommandLine commandLine = parser.getCommandLine();
		if (commandLine.getError() != null) {
			FileUtilities.err(commandLine.getError() + "\nUse --" + DisplayHelp.COMMAND + " for more information.");
			return ExitCode.Error;
		}

		final SwiftEnvironment swiftEnvironment = MainFactoryContext.getSwiftEnvironment();
		try {
			return swiftEnvironment.runSwiftCommand(commandLine);
		} catch (Exception e) {
			LOGGER.error(MprcException.getDetailedMessage(e));
			return ExitCode.Error;
		}
	}
}
