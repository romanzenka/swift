package edu.mayo.mprc.swift.benchmark;

import edu.mayo.mprc.swift.dbmapping.TaskData;
import edu.mayo.mprc.swift.dbmapping.TaskStateData;
import edu.mayo.mprc.workflow.persistence.TaskState;
import junit.framework.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class TestBenchmark {

	private static final TaskData MASCOT_SEARCH = new TaskData(
			"Mascot search",
			new Date(12345678L),
			new Date(12345678L + 1000L),
			new Date(12345678L + 3500L), // Runs for 2.5 seconds
			null,
			new TaskStateData(TaskState.COMPLETED_SUCCESFULLY.getText()),
			"Mascot search of <file>test.txt</file>");

	private static final TaskData SEQUEST_SEARCH = new TaskData(
			"Sequest search",
			new Date(12345678L),
			new Date(12345678L + 2000L),
			new Date(12345678L + 3000L), // Runs for 1 second
			null,
			new TaskStateData(TaskState.COMPLETED_SUCCESFULLY.getText()),
			"Sequest search of <file>test.txt</file>");

	private static final TaskData SEQUEST_FAIL_SEARCH = new TaskData(
			"Sequest search",
			new Date(12345678L),
			new Date(12345678L + 2000L),
			null,
			null,
			new TaskStateData(TaskState.RUN_FAILED.getText()),
			"Sequest search of <file>test.txt</file>");

	private final List<TaskData> tasks = new ArrayList<TaskData>();

	@BeforeMethod
	public void setup() {
		tasks.clear();
	}

	@Test
	public void shouldOutputEmptyTable() throws IOException {
		checkResult(tasks, "");
	}

	@Test
	public void shouldOutputSingleTask() throws IOException {
		tasks.add(MASCOT_SEARCH);
		checkResult(tasks, "Mascot search\n2.5\n");
	}

	@Test
	public void shouldIgnoreNonComplete() throws IOException {
		tasks.add(MASCOT_SEARCH);
		tasks.add(SEQUEST_FAIL_SEARCH);
		checkResult(tasks, "Mascot search\n2.5\n");
	}

	@Test
	public void shouldOutputTwoTasks() throws IOException {
		tasks.add(MASCOT_SEARCH);
		tasks.add(SEQUEST_FAIL_SEARCH);
		tasks.add(SEQUEST_SEARCH);
		checkResult(tasks, "Mascot search,Sequest search\n2.5,1.0\n");
	}


	private void checkResult(final List<TaskData> tasks, final String expected) throws IOException {
		final Stream outputStream = new Stream();
		Benchmark.printTaskTable(outputStream, tasks);
		Assert.assertEquals(outputStream.getOutput(), expected);
	}

	private static final class Stream extends ServletOutputStream {
		private final StringBuilder output = new StringBuilder(1000);

		public String getOutput() {
			return output.toString();
		}

		@Override
		public void print(final String s) throws IOException {
			output.append(s);
		}

		@Override
		public void println(final String s) throws IOException {
			output.append(s).append("\n");
		}

		@Override
		public void println() throws IOException {
			output.append("\n");
		}

		public void write(final int b) throws IOException {
			output.append((char) b);
		}
	}
}
