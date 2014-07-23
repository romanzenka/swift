package edu.mayo.mprc.utilities.progress;

import edu.mayo.mprc.utilities.log.ParentLog;
import edu.mayo.mprc.utilities.log.SimpleParentLog;
import org.apache.log4j.Logger;

/**
 * Simple implementation for tests.
 *
 * @author Roman Zenka
 */
public final class TestProgressReporter implements UserProgressReporter {
	private static final Logger LOGGER = Logger.getLogger(TestProgressReporter.class);

	@Override
	public void reportProgress(final ProgressInfo progressInfo) {
		LOGGER.debug(progressInfo);
	}

	@Override
	public ParentLog getParentLog() {
		return new SimpleParentLog();
	}
}
