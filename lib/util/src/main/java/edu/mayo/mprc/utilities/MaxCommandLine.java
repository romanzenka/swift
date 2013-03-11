package edu.mayo.mprc.utilities;

import com.google.common.base.Splitter;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class MaxCommandLine {

	public static final Logger LOGGER = Logger.getLogger(MaxCommandLine.class);
	/**
	 * This is a conservative estimate of valid command line length. This number is used when the valid command line length cannot be
	 * determined automatically. It should be valid on all potential platforms.
	 */
	public static final int MIN_VALID_MAX_CALL_LENGTH = 5000;

	private MaxCommandLine() {
	}

	/**
	 * Find the max call length allowed on this system
	 * The approach used to is continuously double the length until if fails
	 * and/or halve it until it succeeds
	 *
	 * @param tryLength  - the length to start with
	 * @param inCommCall - the call to use for testing the maximum call length, for example 'echo'
	 * @return maximum call length
	 */
	public static long findMaxCallLength(final long tryLength, final String inCommCall) {
		final String baseCommand = inCommCall == null ? "echo " : inCommCall + ' ';

		final String arg = "abcdefghij";

		if (!runCommand(baseCommand + arg)) {
			LOGGER.warn("Could not determine maximum command line length for sequest. Assuming conservative " + MIN_VALID_MAX_CALL_LENGTH);
			return MIN_VALID_MAX_CALL_LENGTH;
		}

		long startLength = tryLength;

		if (startLength == 0) {
			startLength = 1000 * 1000;
		}
		long overShot = 0;
		long underShot = 0;

		while (true) {
			final String call = buildCallOfLength(baseCommand, startLength, arg);
			if (runCommand(call)) {
				startLength = startLength * 2;
				underShot = call.length();
			} else {
				// We found a failing command length
				overShot = call.length();
				break;
			}
		}

		// We do not know shortest working command - we failed right away
		while (underShot == 0) {
			final String call = buildCallOfLength(baseCommand, startLength, arg);
			if (runCommand(call)) {
				underShot = call.length();
				break;
			} else {
				overShot = call.length();
				startLength = startLength / 2;
			}
		}

		while (overShot - underShot > 100) {
			startLength = underShot + (overShot - underShot) / 2;

			final String call = buildCallOfLength(baseCommand, startLength, arg);
			if (runCommand(call)) {
				underShot = call.length();
			} else {
				overShot = call.length();
			}
		}
		return underShot;

	}

	/**
	 * see if the command will run under this environment.
	 */

	public static boolean runCommand(final String cmd) {
		final Iterable<String> splitCmd = Splitter.on(' ').split(cmd);
		final List<String> list = new ArrayList<String>(5);
		for (final String s : splitCmd) {
			list.add(s);
		}
		final ProcessBuilder builder = new ProcessBuilder(list);
		final ProcessCaller caller = new ProcessCaller(builder);
		caller.setRetainLogs(false);
		caller.setLogToConsole(false);

		try {
			caller.run();
			return true;
		} catch (Exception ignore) {
			// Failed call means we ran too long command
			return false;
		}
	}

	/**
	 * get a call string of given length, starting with a given command
	 */
	public static String buildCallOfLength(final String prefix, final long length, final String piece) {
		final StringBuilder result = new StringBuilder(prefix);
		while (result.length() + piece.length() < length) {
			result.append(piece);
		}

		return result.toString();
	}
}
