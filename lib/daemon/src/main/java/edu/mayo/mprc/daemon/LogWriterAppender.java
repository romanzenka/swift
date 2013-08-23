package edu.mayo.mprc.daemon;

import org.apache.log4j.*;
import org.apache.log4j.spi.LoggingEvent;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * Class appends filtered log to a given file.
 */
class LogWriterAppender extends WriterAppender {

	private String allowedMDCKey;
	private Object mdcValue;
	private Set<String> allowedThreadNames;
	private Set<Level> allowedLevels;
	private Set<Level> notAllowedLevels;

	private static Layout appenderLayout;

	static {
		final Enumeration appenders = Logger.getRootLogger().getAllAppenders();

		if (appenders.hasMoreElements()) {
			final Appender appender = (Appender) appenders.nextElement();
			appenderLayout = appender.getLayout();
		} else {
			appenderLayout = new SimpleLayout();
		}
	}

	public LogWriterAppender(final OutputStream outputStream) {
		this(new OutputStreamWriter(outputStream));
	}

	public LogWriterAppender(final Writer writer) {
		super(appenderLayout, writer);

		allowedThreadNames = new HashSet<String>(4);
		allowedLevels = new HashSet<Level>(10);
		notAllowedLevels = new HashSet<Level>(10);
	}

	public void setAllowedMDCKey(final String key, final Object value) {
		this.allowedMDCKey = key;
		this.mdcValue = value;
	}

	public void clearAllowedMDCKey() {
		this.allowedMDCKey = null;
		this.mdcValue = null;
	}

	public void addAllowedThreadName(final String threadName) {
		allowedThreadNames.add(threadName);
	}

	public void removeAllowedThreadName(final String threadName) {
		allowedThreadNames.remove(threadName);
	}

	public void addAllowedLevel(final Level level) {
		allowedLevels.add(level);
	}

	public void removeAllowedLevel(final Level level) {
		allowedLevels.remove(level);
	}

	public void addNotAllowedLevel(final Level level) {
		notAllowedLevels.add(level);
	}

	public void removeNotAllowedLevel(final Level level) {
		notAllowedLevels.remove(level);
	}

	@Override
	public void append(final LoggingEvent loggingEvent) {
		if (allowedThreadNames.isEmpty() || allowedThreadNames.contains(loggingEvent.getThreadName())) {
			if ((allowedLevels.isEmpty() || allowedLevels.contains(loggingEvent.getLevel())) && (notAllowedLevels.isEmpty() || !notAllowedLevels.contains(loggingEvent.getLevel()))) {
				if (allowedMDCKey == null || mdcValue.equals(loggingEvent.getMDC(allowedMDCKey))) {
					super.append(loggingEvent);
				}
			}
		}
	}
}
