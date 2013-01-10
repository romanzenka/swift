package edu.mayo.mprc.utilities.progress;

/**
 * A reporter that can be used to provide updates about how the work is going.
 * <p/>
 * This is a part of {@link ProgressReporter} that can also report when work starts/ends/fails.
 *
 * @author Roman Zenka
 */
public interface UserProgressReporter {
	/**
	 * Reports progress of the worker. The progress can be anything serializable, for instance an Integer containing
	 * amount of percent. The grid engine daemon implementation for instance sends back the assigned grid engine number as a specific
	 * progress report.
	 *
	 * @param progressInfo Information about the progress, e.g. an Integer containing the amount of percent done.
	 */
	void reportProgress(ProgressInfo progressInfo);
}
