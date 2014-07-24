package edu.mayo.mprc.utilities.log;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

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
	}

	@Override
	public void stopLogging() {
		LOGGER.debug(id.toString() + " stopped logging");
	}

	@Override
	public Logger getOutputLogger() {
		return LOGGER;
	}

	@Override
	public Logger getErrorLogger() {
		return LOGGER;
	}

}
