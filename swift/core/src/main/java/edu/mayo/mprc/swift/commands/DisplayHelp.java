package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.ReleaseInfoCore;
import edu.mayo.mprc.swift.ExitCode;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;

/**
 * @author Roman Zenka
 */
public class DisplayHelp implements SwiftCommand {
	private static final Logger LOGGER = Logger.getLogger(DisplayHelp.class);
	public static final String COMMAND = "help";

	@Override
	public String getName() {
		return COMMAND;
	}

	@Override
	public String getDescription() {
		return "Display Swift help";
	}

	@Override
	public ExitCode run(final SwiftEnvironment environment) {
		try {
			FileUtilities.out(ReleaseInfoCore.buildVersion());
			FileUtilities.out("");
			FileUtilities.out("This command starts Swift with the provided configuration parameters.");
			FileUtilities.out("If you do not have a configuration file yet, please run Swift's web configuration first.");
			FileUtilities.out("");
			FileUtilities.out("Usage:");
			environment.getOptionParser().printHelpOn(System.out);
			return ExitCode.Ok;
		} catch (Exception t) {
			LOGGER.fatal("Could not display help message.", t);
			return ExitCode.Error;
		}
	}
}
