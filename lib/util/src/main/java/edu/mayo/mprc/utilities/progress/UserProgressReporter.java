package edu.mayo.mprc.utilities.progress;

import edu.mayo.mprc.utilities.log.ParentLog;

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

	/**
	 * When reporting progress, you can get access to the parent log, which allows you to spawn a child log
	 * in case you are spawning children. The fact that a child was spawned
	 * will be communicated to the caller as a progress message.
	 *
	 * @return An object that allows you to spawn a child log.
	 */
	ParentLog getParentLog();
}
