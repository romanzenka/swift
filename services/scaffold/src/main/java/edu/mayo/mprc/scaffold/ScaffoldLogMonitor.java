package edu.mayo.mprc.scaffold;

import edu.mayo.mprc.utilities.LogMonitor;
import edu.mayo.mprc.utilities.progress.PercentDone;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;

public class ScaffoldLogMonitor implements LogMonitor {
	private static final int TIME_BETWEEN_UPDATES_MS = 1000;
	private final UserProgressReporter progressReporter;
	private long lastTimeMs = System.currentTimeMillis();
	private boolean staleFile;


	public ScaffoldLogMonitor(final UserProgressReporter progressReporter) {
		this.progressReporter = progressReporter;
	}

	@Override
	public void line(final String line) {
		// Watch for progress
		if (line.length() > 2 && line.charAt(0) == '%' && line.charAt(line.length() - 1) == '%') {
			final long time = System.currentTimeMillis();
			if (time - lastTimeMs < TIME_BETWEEN_UPDATES_MS) {
				return;
			}
			lastTimeMs = time;
			// Percent complete line.
			final String substring = line.substring(1, line.length() - 1);
			try {
				final float percentComplete = Float.parseFloat(substring);
				progressReporter.reportProgress(new PercentDone(percentComplete));
			} catch (NumberFormatException ignore) {
				// SWALLOWED: We ignore these exceptions - we will just not be able to report progress
			}
		}
		// Watch for important warnings - stale file handle warning means our output is incorrect
		if (line.contains("java.io.IOException: Stale file handle")) {
			staleFile = true;
		}
	}

	/**
	 * @return True if Scaffold produced stale file warning.
	 * This warning means Scaffold failed and its outputs are inconsistent, need to be wiped.
	 */
	public boolean isStaleFile() {
		return staleFile;
	}
}
