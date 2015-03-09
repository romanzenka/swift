package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * @author Roman Zenka
 */
@Component("help-command")
public class DisplayHelp implements SwiftCommand {
	private static final Logger LOGGER = Logger.getLogger(DisplayHelp.class);
	public static final String COMMAND = "help";

	@Override
	public String getDescription() {
		return "Display Swift help";
	}

	@Override
	public ExitCode run(final SwiftEnvironment environment) {
		try {
			FileUtilities.out("");
			FileUtilities.out("This command starts Swift with the provided configuration parameters.");
			FileUtilities.out("If you do not have a configuration file yet, please run Swift's web configuration first.");
			FileUtilities.out("");
			FileUtilities.out("Usage:");
			environment.getOptionParser().printHelpOn(System.out);
			FileUtilities.out("Supported Swift commands:");
			FileUtilities.out(environment.listSupportedCommands());
			return ExitCode.Ok;
		} catch (Exception t) {
			// SWALLOWED: Not much we can do
			LOGGER.fatal("Could not display help message.", t);
			return ExitCode.Error;
		}
	}
}
