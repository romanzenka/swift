package edu.mayo.mprc.utilities.log;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

/**
 * A simple implementation of {@link ParentLog} that logs name of each logger as they get created,
 * the logs themselves go to the standard logger.
 *
 * @author Roman Zenka
 */
public final class SimpleParentLog implements ChildLog {
	private static final Logger LOGGER = Logger.getLogger(SimpleParentLog.class);
	private static final long serialVersionUID = -291080842199803090L;

	private final UUID id = UUID.randomUUID();
	private File file;
	private FileWriter writer;

	public SimpleParentLog() {

	}

	@Override
	public UUID getLogId() {
		return id;
	}

	@Override
	public ChildLog createChildLog() {
		return new SimpleParentLog();
	}

	@Override
	public ChildLog createChildLog(String outputLogFilePath, String errorLogFilePath) {
		return createChildLog();
	}

	@Override
	public void startLogging() {
		LOGGER.debug(id.toString() + " started logging");
		try {
			file = File.createTempFile("test", ".log");
			writer = new FileWriter(file, true);
		} catch (IOException e) {
			throw new MprcException(e);
		}
	}

	@Override
	public void stopLogging() {
		LOGGER.debug(id.toString() + " stopped logging");
		FileUtilities.closeQuietly(writer);
		FileUtilities.cleanupTempFile(file);
	}

	@Override
	public Logger getOutputLogger() {
		return LOGGER;
	}

	@Override
	public Logger getErrorLogger() {
		return LOGGER;
	}

	@Override
	public void close() {
		// Do nothing
	}

}
