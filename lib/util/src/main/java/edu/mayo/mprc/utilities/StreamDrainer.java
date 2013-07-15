package edu.mayo.mprc.utilities;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;

/**
 * The drainer is consuming a stream line by line.
 * With each line, several actions are done:
 * <ul>
 * <li>Specifiable amount of lines is retained and available using {@link #getLog()}.</li>
 * <li>If you specify a logger, each line is logged using logLevel you specified</li>
 * <li>total line count is kept</li>
 * </ul>
 */
public final class StreamDrainer implements Runnable {
	/**
	 * How many characters per line are typically there.
	 */
	private static final int AVG_CHARS_PER_LINE = 20;
	public static final String ESCAPE = "\u001B";

	private final Logger logger;
	private final InputStream inputStream;
	private final Level logLevel;
	private final String[] loggedLines;
	private final LogMonitor logMonitor;

	private int lastLogLine;
	private int totalLines;

	/**
	 * Default amount of lines to be retained.
	 */
	public static final int DEFAULT_RETAIN_SIZE = 100;

	/**
	 * Constructor.
	 *
	 * @param inputStream InputStream object to read data from.
	 * @param logger      Logger object which will log data read from the input stream object.
	 * @param logLevel    Log level for the logger.
	 * @param retainLines How many lines max to retain. Do not go too wild with this parameter, an array is being allocated. If non-positive, default size is used. If zero, nothing gets retained.
	 */
	public StreamDrainer(final InputStream inputStream, final Logger logger, final Level logLevel, final int retainLines, final LogMonitor logMonitor) {
		this.logger = logger;
		this.inputStream = inputStream;
		this.logLevel = logLevel;
		this.logMonitor = logMonitor;
		loggedLines = new String[retainLines < 0 ? DEFAULT_RETAIN_SIZE : retainLines];
	}

	@Override
	public void run() {
		if (inputStream != null) {

			final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			try {
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					addLine(line);
				}
			} catch (IOException ex) {
				//SWALLOWED
				logger.error("Reading line from input stream.", ex);
			} finally {
				FileUtilities.closeQuietly(bufferedReader);
			}
		}
	}

	/**
	 * Add a line to the log.
	 *
	 * @param line Line to be added.
	 */
	void addLine(final String line) {
		// We started with an escape sequence, ignore this line
		if (line.indexOf(ESCAPE) == 0) {
			return;
		}
		if (loggedLines.length > 0) {
			loggedLines[lastLogLine] = line;
			lastLogLine++;
			lastLogLine %= loggedLines.length;
		}
		totalLines++;
		if (logger != null) {
			logger.log(logLevel, line);
		}
		if (logMonitor != null) {
			logMonitor.line(line);
		}
	}

	/**
	 * @return Returns abridged log that was collected - up to 100 last lines.
	 */
	public String getLog() {
		final StringBuilder log;
		final boolean raw = totalLines <= loggedLines.length;
		if (raw) {
			log = new StringBuilder(totalLines * AVG_CHARS_PER_LINE);
			for (int i = 0; i < totalLines; i++) {
				log.append(loggedLines[i]).append('\n');
			}
		} else {
			log = new StringBuilder(loggedLines.length * AVG_CHARS_PER_LINE);
			log.append(MessageFormat.format("Most recent {0} of {1} total log lines:\n", loggedLines.length, totalLines));

			for (int i = lastLogLine; i < lastLogLine + loggedLines.length; i++) {
				log.append("\t")
						.append(loggedLines[i % loggedLines.length])
						.append('\n');
			}
		}
		return log.toString();
	}
}