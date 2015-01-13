package edu.mayo.mprc.swift.db;

import com.google.common.collect.ImmutableList;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.TaskData;
import edu.mayo.mprc.swift.dbmapping.TaskStateData;
import edu.mayo.mprc.workflow.persistence.TaskState;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.List;

public final class TestTimeReport {

	public static DateTime start;

	@BeforeClass
	public static void setUp() {
		start = randomTime();
	}

	@Test
	public static void searchRunTime() {
		Assert.assertEquals(TimeReport.elapsedTime(makeSearchRun(start, start.plusMinutes(12))), 12 * 60.0);
		Assert.assertEquals(TimeReport.elapsedTime(makeSearchRun(start, start.plusMillis(123))), 0.123);
		Assert.assertEquals(TimeReport.elapsedTime(makeSearchRun(start, null)), 0.0);
		Assert.assertEquals(TimeReport.elapsedTime(makeSearchRun(null, start)), 0.0);
	}


	@Test
	public static void noTasks() {
		final List<TaskData> tasks = new ImmutableList.Builder<TaskData>()
				.build();
		Assert.assertEquals(TimeReport.consumedTime(tasks), 0.0);
		Assert.assertEquals(TimeReport.productiveTime(tasks), 0.0);
	}

	@Test
	public static void singleTask() {
		final List<TaskData> tasks = new ImmutableList.Builder<TaskData>()
				.add(task(start, 3, 10, 22, TaskState.COMPLETED_SUCCESFULLY))
				.build();
		Assert.assertEquals(TimeReport.consumedTime(tasks), 12.0);
		Assert.assertEquals(TimeReport.productiveTime(tasks), 12.0);
	}

	@Test
	public static void twoTasks() {
		final List<TaskData> tasks = new ImmutableList.Builder<TaskData>()
				.add(task(start, 1, 5, 7, TaskState.COMPLETED_SUCCESFULLY))
				.add(task(start, 25, 30, 35, TaskState.COMPLETED_SUCCESFULLY))
				.build();
		Assert.assertEquals(TimeReport.consumedTime(tasks), 7.0);
		Assert.assertEquals(TimeReport.productiveTime(tasks), 7.0);
	}

	@Test
	public static void twoOverlappingTasks() {
		final List<TaskData> tasks = new ImmutableList.Builder<TaskData>()
				.add(task(start, 1, 5, 35, TaskState.COMPLETED_SUCCESFULLY))
				.add(task(start, 18, 20, 40, TaskState.COMPLETED_SUCCESFULLY))
				.build();
		Assert.assertEquals(TimeReport.consumedTime(tasks), 30.0 + 20.0);
		Assert.assertEquals(TimeReport.productiveTime(tasks), 40.0 - 5.0);
	}

	@Test
	public static void threeOverlappingTasks() {
		final List<TaskData> tasks = new ImmutableList.Builder<TaskData>()
				.add(task(start, 1, 5, 35, TaskState.COMPLETED_SUCCESFULLY))
				.add(task(start, 1, 10, 37, TaskState.COMPLETED_SUCCESFULLY))
				.add(task(start, 18, 20, 40, TaskState.COMPLETED_SUCCESFULLY))
				.build();
		Assert.assertEquals(TimeReport.consumedTime(tasks), 30.0 + 20.0 + 27.0);
		Assert.assertEquals(TimeReport.productiveTime(tasks), 40.0 - 5.0);
	}

	@Test
	public static void failedTask() {
		final List<TaskData> tasks = new ImmutableList.Builder<TaskData>()
				.add(task(start, 1, 5, 35, TaskState.COMPLETED_SUCCESFULLY))
				.add(task(start, 1, 10, 57, TaskState.RUN_FAILED))
				.add(task(start, 18, 20, 40, TaskState.COMPLETED_SUCCESFULLY))
				.build();
		Assert.assertEquals(TimeReport.consumedTime(tasks), 30.0 + 20.0);
		Assert.assertEquals(TimeReport.productiveTime(tasks), 40.0 - 5.0);
	}

	@Test
	public static void nullEndTask() {
		final List<TaskData> tasks = new ImmutableList.Builder<TaskData>()
				.add(task(start, 1, 5, 35, TaskState.COMPLETED_SUCCESFULLY))
				.add(task(start, 1, 10, 57, TaskState.RUN_FAILED))
				.add(task(start, 18, 20, 0, TaskState.COMPLETED_SUCCESFULLY))
				.build();
		Assert.assertEquals(TimeReport.consumedTime(tasks), 30.0);
		Assert.assertEquals(TimeReport.productiveTime(tasks), 30.0);
	}

	@Test
	public static void zeroLengthTasks() {
		final List<TaskData> tasks = new ImmutableList.Builder<TaskData>()
				.add(task(start, 1, 5, 5, TaskState.COMPLETED_SUCCESFULLY))
				.add(task(start, 18, 20, 20, TaskState.COMPLETED_SUCCESFULLY))
				.build();
		Assert.assertEquals(TimeReport.consumedTime(tasks), 0.0);
		Assert.assertEquals(TimeReport.productiveTime(tasks), 0.0);
	}


	@Test
	public static void taskDidWork() {
		Assert.assertTrue(TimeReport.taskDidWork(taskData(TaskState.COMPLETED_SUCCESFULLY)));
		Assert.assertFalse(TimeReport.taskDidWork(
				new TaskData("test", new Date(), new Date(), null, null, new TaskStateData(TaskState.COMPLETED_SUCCESFULLY.getText()), "desc")));
		Assert.assertFalse(TimeReport.taskDidWork(
				new TaskData("test", new Date(), null, new Date(), null, new TaskStateData(TaskState.COMPLETED_SUCCESFULLY.getText()), "desc")));
		Assert.assertFalse(TimeReport.taskDidWork(taskData(TaskState.INIT_FAILED)));
		Assert.assertFalse(TimeReport.taskDidWork(taskData(TaskState.READY)));
		Assert.assertFalse(TimeReport.taskDidWork(taskData(TaskState.RUN_FAILED)));
		Assert.assertFalse(TimeReport.taskDidWork(taskData(TaskState.READY)));
	}

	private static TaskData taskData(final TaskState state) {
		return new TaskData("test", new Date(), new Date(), new Date(), null, new TaskStateData(state.getText()), "desc");
	}

	private static TaskStateData taskStateData(final TaskState state) {
		return new TaskStateData(state.getText());
	}

	@Test
	public static void productiveFailedTask() {
		final List<TaskData> tasks = new ImmutableList.Builder<TaskData>()
				.add(task(start, 1, 5, 7, TaskState.RUN_FAILED))
				.add(task(start, 25, 30, 35, TaskState.COMPLETED_SUCCESFULLY))
				.build();
		Assert.assertEquals(TimeReport.productiveTime(tasks), 5.0);
	}

	private static TaskData task(final DateTime start, final int queueOffset, final int startOffset, final int endOffset, final TaskState state) {
		return new TaskData(
				"mascot",
				start.plusSeconds(queueOffset).toDate(),
				startOffset == 0 ? null : start.plusSeconds(startOffset).toDate(),
				endOffset == 0 ? null : start.plusSeconds(endOffset).toDate(),
				null, taskStateData(state),
				"Mascot search");
	}

	private static DateTime randomTime() {
		return new DateTime(2011, 8, 24, 10, 20, 33);
	}

	private static SearchRun makeSearchRun(final DateTime start, final DateTime end) {
		return new SearchRun("Test", null, null,
				start == null ? null : start.toDate(), end == null ? null : end.toDate(), 0, "no error", 10, 0, 0, 10, false, null);
	}
}
