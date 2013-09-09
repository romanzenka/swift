package edu.mayo.mprc.heme;

import edu.mayo.mprc.heme.dao.HemeTest;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import org.joda.time.Interval;

import java.util.Date;

/**
 * An entry for the list of tests.
 *
 * @author Roman Zenka
 */
public final class HemeEntry {
	public static final int PERCENT = 100;
	private HemeTest test;

	private HemeTestStatus status;

	/**
	 * For how long did the search run.
	 */
	private Interval duration;

	/**
	 * In case the search is running, how far did it get.
	 */
	private int progressPercent;

	public HemeEntry(final HemeTest test) {
		this.test = test;
		final HemeTestStatus status;
		final SearchRun searchRun = test.getSearchRun();
		Interval duration = null;
		if (searchRun == null) {
			status = HemeTestStatus.NOT_STARTED;
		} else {
			if (searchRun.isCompleted()) {
				status = searchRun.getErrorMessage() == null ? HemeTestStatus.SUCCESS : HemeTestStatus.FAILED;
				if (searchRun.getStartTimestamp() != null && searchRun.getEndTimestamp() != null) {
					duration = new Interval(searchRun.getStartTimestamp().getTime(), searchRun.getEndTimestamp().getTime());
				}
			} else {
				status = HemeTestStatus.RUNNING;
				if (searchRun.getStartTimestamp() != null) {
					duration = new Interval(searchRun.getStartTimestamp().getTime(), new Date().getTime());
				}
				progressPercent = PERCENT * searchRun.getTasksCompleted() / searchRun.getNumTasks();
			}
		}
		this.status = status;
		this.duration = duration;
	}

	public HemeTest getTest() {
		return test;
	}

	public HemeTestStatus getStatus() {
		return status;
	}

	public Interval getDuration() {
		return duration;
	}

	public int getProgressPercent() {
		return progressPercent;
	}
}
