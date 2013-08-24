package edu.mayo.mprc.swift.db;

import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.TaskData;
import edu.mayo.mprc.utilities.ComparisonChain;
import edu.mayo.mprc.workflow.persistence.TaskState;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Report how much CPU time was used by Swift. This is a basis for charging for Swift use.
 * We recognize several ways of calculating time used:
 * <ul>
 * <li>"Elapsed time" - time from start to end of search run. This is unfair if the jobs wait in the queue</li>
 * <li>"Productive time" - sum of times when at least one task was running</li>
 * <li>"Consumed time" - sum of all task times, ignoring that some run in parallel</li>
 * </ul>
 */
public final class TimeReport {

	private TimeReport() {
	}

	/**
	 * Searches that are still running or never started have elapsed time of 0 seconds.
	 *
	 * @param searchRun Search run to measure elapsed time
	 * @return Time elapsed by given search run(in seconds). Elapsed time is time from the beginning to the end of the search run.
	 */
	public static double elapsedTime(final SearchRun searchRun) {
		return duration(searchRun.getStartTimestamp(), searchRun.getEndTimestamp());
	}

	private static double duration(final Date start, final Date end) {
		if (end == null || start == null) {
			return 0.0;
		}
		return new Interval(start.getTime(), end.getTime()).toDurationMillis() / 1000.0;
	}

	/**
	 * Calculate consumed time of given set of tasks. Consumed time - sum of all times taken by all tasks that
	 * finished successfully.
	 *
	 * @param tasks List of tasks
	 * @return Consumed time by all the tasks.
	 */
	public static double consumedTime(final List<TaskData> tasks) {
		double total = 0.0;
		for (final TaskData task : tasks) {
			if (taskDidWork(task)) {
				total += duration(task.getStartTimestamp(), task.getEndTimestamp());
			}
		}
		return total;
	}

	/**
	 * Calculate productive time of given set of tasks. Productive time - amount of time when something was actually happening on the CPU,
	 * not counting the waiting time and periods when there was no activity.
	 * <p/>
	 * This is calculated by sorting all tasks start and end times and then sweeping through, keeping count of whether
	 * anything is running or not during a given time interval.
	 *
	 * @param tasks List of tasks
	 * @return Productive time when at least one task was actually running.
	 */
	public static double productiveTime(final List<TaskData> tasks) {
		final List<TaskTimestamp> taskTimestamps = new ArrayList<TaskTimestamp>(tasks.size() * 2);
		for (final TaskData task : tasks) {
			if (taskDidWork(task)) {
				taskTimestamps.add(TaskTimestamp.startTimestamp(task));
				taskTimestamps.add(TaskTimestamp.endTimestamp(task));
			}
		}
		Collections.sort(taskTimestamps);
		int intervalCounter = 0;
		double total = 0.0;
		Date previousTime = null;
		for (final TaskTimestamp timeStamp : taskTimestamps) {
			if (intervalCounter > 0) {
				total += duration(previousTime, timeStamp.getTime());
			}
			if (timeStamp.isStart()) {
				intervalCounter++;
			} else {
				intervalCounter--;
			}
			previousTime = timeStamp.getTime();
		}
		return total;
	}

	private static final class TaskTimestamp implements Comparable<TaskTimestamp> {
		private final boolean start;
		private final Date time;

		private TaskTimestamp(final boolean start, final Date time) {
			this.start = start;
			this.time = time;
		}

		public static TaskTimestamp startTimestamp(final TaskData task) {
			return new TaskTimestamp(true, task.getStartTimestamp());
		}

		public static TaskTimestamp endTimestamp(final TaskData task) {
			return new TaskTimestamp(false, task.getEndTimestamp());
		}

		public boolean isStart() {
			return start;
		}

		public Date getTime() {
			return time;
		}

		@Override
		public int compareTo(final TaskTimestamp o) {
			return ComparisonChain.start()
					.compare(time, o.time)
					.compare(start, o.start)
					.result();
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			final TaskTimestamp that = (TaskTimestamp) o;

			return compareTo(that)==0;
		}

		@Override
		public int hashCode() {
			int result = start ? 1 : 0;
			result = 31 * result + (time != null ? time.hashCode() : 0);
			return result;
		}
	}

	public static boolean taskDidWork(final TaskData task) {
		return TaskState.COMPLETED_SUCCESFULLY.equals(TaskState.fromText(task.getTaskState().getDescription())) &&
				task.getStartTimestamp() != null && task.getEndTimestamp() != null;
	}
}
