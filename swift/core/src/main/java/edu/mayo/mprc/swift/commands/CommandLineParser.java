package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.swift.Swift;
import edu.mayo.mprc.utilities.CommandLine;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A parsed Swift command - what did the user ask Swift to do.
 * <p/>
 * Includes environment setup.
 *
 * @author Roman Zenka
 */
public final class CommandLineParser {
	private OptionParser parser;
	private final SwiftCommandLine commandLine;

	/**
	 * @return Option parser for Swift's command line.
	 */
	OptionParser setupParser() {
		parser = new OptionParser();
		parser.accepts("run", "A Swift command to run. When missing, Swift will run all workers configured for the specified daemon.")
				.withRequiredArg().describedAs("Swift command").ofType(String.class);
		parser.accepts("install", "Installation config file. Default is " + Swift.CONFIG_FILE_NAME + ". Please run the Swift configuration to obtain this file.")
				.withRequiredArg().ofType(File.class);
		parser.accepts("daemon", "Specify the daemon (this describes the environment of the current run) as it was set up during the configuration. When no name is given, the configuration has to contain exactly one daemon, otherwise an error is produced.")
				.withOptionalArg().ofType(String.class).describedAs("name");
		parser.accepts("sge", "Process a single work packet and exit. Used for jobs submitted through the Sun Grid Engine (SGE). The file contains all input parameters encoded in XML format.")
				.withRequiredArg().describedAs("XML file").ofType(String.class);
		parser.acceptsAll(Arrays.asList("help", "?"), "Show this help screen");
		return parser;
	}

	public CommandLineParser(final String[] args) {
		setupParser();
		final OptionSet options = parser.parse(args);

		String command = null;
		List<String> parameters = new ArrayList<String>(0);
		String error = null;

		if (options.has("?")) {
			command = DisplayHelp.COMMAND;
		} else if (!options.has("daemon") && !options.has("sge") && !options.has("run")) {
			error = "You must specify either the --daemon, --sge or --run options.";
			command = DisplayHelp.COMMAND;
		} else if (options.has("sge") && options.has("run")) {
			error = "--sge and --run options are mutually exclusive.";
			command = DisplayHelp.COMMAND;
		} else if (options.has("sge")) {
			command = RunSge.COMMAND;
			parameters = Arrays.asList((String) options.valueOf("sge"));
		} else {
			if (options.has("run")) {
				command = (String) options.valueOf("run");
				parameters = options.nonOptionArguments();
			} else {
				command = SwiftCommandLine.DEFAULT_RUN_COMMAND;
			}
		}
		File installFile = null;
		if (!DisplayHelp.COMMAND.equals(command)) {
			installFile = CommandLine.findFile(options, "install", "installation config file", Swift.CONFIG_FILE_NAME);
		}
		final String daemonId = (String) options.valueOf("daemon");
		commandLine = new SwiftCommandLine(command, parameters, installFile, daemonId, error, parser);
	}

	public SwiftCommandLine getCommandLine() {
		return commandLine;
	}

	public OptionParser getOptionParser() {
		return parser;
	}
}
