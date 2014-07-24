package edu.mayo.mprc.swift.search;

import edu.mayo.mprc.swift.dbmapping.LogData;
import edu.mayo.mprc.swift.dbmapping.TaskData;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.util.UUID;

/**
 * @author Roman Zenka
 */
public final class LogMapTest {
	private LogMap map;

	private TaskData task1;
	private UUID id1;
	private LogData log1;

	private TaskData task2;
	private UUID id2;
	private LogData log2;

	@BeforeTest
	public void setup() {
		map = new LogMap();

		task1 = new TaskData();
		task1.setTaskName("task1");
		id1 = UUID.randomUUID();
		log1 = new LogData(task1, null, new File("out"), new File("err"));

		task2 = new TaskData();
		task2.setTaskName("task2");
		id2 = UUID.randomUUID();
		log2 = new LogData(task2, null, new File("out"), new File("err"));
	}

	@Test
	public void shouldAddProperly() {
		// A logmap
		// Given a task, log id and saved log data

		// When log data is added
		map.addLogData(id1, log1);
		map.addLogData(id2, log2);

		// The map will return the same data
		final LogData result = map.getLogData(task1, id1);
		Assert.assertEquals(result, log1, "Must get what I put in");
	}

	@Test
	public void shouldNotMixTasks() {
		// A logmap
		// Given a task, log id and saved log data

		// When log data is added
		map.addLogData(id1, log1);
		map.addLogData(id2, log2);

		// The map will return null for a different task but same id

		final LogData result = map.getLogData(task2, id1);
		Assert.assertNull(result, "Tasks do not mix up their logs");
	}

	@Test
	public void shouldCleanupWhenTaskDone() {
		// When log data is added
		map.addLogData(id1, log1);
		map.addLogData(id2, log2);

		map.removeTask(task1);
		Assert.assertNull(map.getLogData(task1, id1), "Task got wiped");
	}

}
