package edu.mayo.mprc.swift.commands;

/**
 * Swift exit codes. Support exiting with the particular code for convenience.
 */
public enum ExitCode {
	/**
	 * Successful execution.
	 */
	Ok(0),

	/**
	 * Swift failed.
	 */
	Error(1),

	/**
	 * Swift should restart (the configuration changed).
	 */
	Restart(2);

	private final int exitCode;

	ExitCode(final int exitCode) {
		this.exitCode = exitCode;
	}

	/**
	 * Call {@code System.exit} with this exit code
	 */
	public void exit() {
		System.exit(exitCode);
	}
}
