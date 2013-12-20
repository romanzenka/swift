package edu.mayo.mprc.swift.commands;

import joptsimple.OptionParser;

import java.io.File;
import java.util.List;

/**
 * Encapsulates information passed to Swift through the command line.
 *
 * @author Roman Zenka
 */
public final class SwiftCommandLine {
	public static final String RUN_OR_CONFIG = "run-or-config";
	public static final String DEFAULT_RUN_COMMAND = "run-swift";
	public static final String CONFIG_COMMAND = "config";

	private final String command;
	private final List<String> parameters;
	private final File installFile;
	private final String daemonId;
	private final String error;
	private final OptionParser parser;

	public SwiftCommandLine(final String command, final List<String> parameters, final File installFile, final String daemonId, final String error, final OptionParser parser) {
		this.command = command;
		this.parameters = parameters;
		this.installFile = installFile;
		this.daemonId = daemonId;
		this.error = error;
		this.parser = parser;
	}

	/**
	 * @return The command the user wants to invoke.
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * @return List of the parameters the users want to invoke.
	 */
	public List<String> getParameters() {
		return parameters;
	}

	/**
	 * @return Configuration file for Swift.
	 */
	public File getInstallFile() {
		return installFile;
	}

	/**
	 * @return ID of the daemon the user wants to run.
	 */
	public String getDaemonId() {
		return daemonId;
	}

	public String getError() {
		return error;
	}

	/**
	 * @return Parser that parsed all the command-line options.
	 */
	public OptionParser getParser() {
		return parser;
	}
}
