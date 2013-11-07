package edu.mayo.mprc.swift.commands;

import edu.mayo.mprc.swift.ExitCode;

/**
 * @author Roman Zenka
 */
public interface SwiftCommand {
	/**
	 * @return A longer description of the command.
	 */
	String getDescription();

	/**
	 * Executes the command within Swift's environment.
	 *
	 * @param environment Swift environment to run the command within.
	 */
	ExitCode run(SwiftEnvironment environment);
}
