package edu.mayo.mprc.utilities.log;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

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

	private static AtomicInteger logsCreated = new AtomicInteger(0);
	private AtomicInteger childrenCreated = new AtomicInteger(0);
	private String path = "";

	public SimpleParentLog() {
		this("log" + logsCreated.incrementAndGet());
	}

	private SimpleParentLog(final String path) {
		this.path = path;
	}

	@Override
	public ChildLog createChildLog() {
		final int childId = childrenCreated.incrementAndGet();
		return new SimpleParentLog(path + '.' + childId);
	}

	@Override
	public void startLogging() {
		LOGGER.debug(path + " started logging");
	}

	@Override
	public void stopLogging() {
		LOGGER.debug(path + " stopped logging");
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
