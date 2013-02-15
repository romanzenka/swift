package edu.mayo.mprc.workflow.engine;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.exceptions.CompositeException;
import edu.mayo.mprc.workflow.persistence.TaskState;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.text.MessageFormat;
import java.util.*;

public final class EngineTest {
	private static final Logger LOGGER = Logger.getLogger(EngineTest.class);

	private static final int NUM_TASKS = 10000;
	private static final int MAX_DEPENDENCIES = 100;

	private static List<Task> pausedTasks = Collections.synchronizedList(new ArrayList<Task>());

	private enum FailMode {
		Never,
		LastTask,
		FirstTask,
		RandomTasks
	}

	@Test
	public void shouldRunWhenSuccessful() {
		runSucceedingTest(FailMode.Never);
	}

	@Test
	public void shouldRunWhenLastFails() {
		runSucceedingTest(FailMode.LastTask);
	}

	@Test
	public void shouldRunWhenFirstFails() {
		runSucceedingTest(FailMode.FirstTask);
	}

	@Test
	public void shouldRunWhenRandomlyFails() {
		runSucceedingTest(FailMode.RandomTasks);
	}

	@Test
	public void shouldPauseWhenSuccessful() throws InterruptedException {
		runPausingTest(FailMode.Never);
	}

	@Test(dependsOnMethods = {"shouldPauseWhenSuccessful"})
	public void shouldPauseWhenLastFails() throws InterruptedException {
		runPausingTest(FailMode.LastTask);
	}

	@Test(dependsOnMethods = {"shouldPauseWhenLastFails"})
	public void shouldPauseWhenFirstFails() throws InterruptedException {
		runPausingTest(FailMode.FirstTask);
	}

	@Test(dependsOnMethods = {"shouldPauseWhenFirstFails"})
	public void shouldPauseWhenRandomlyFails() throws InterruptedException {
		runPausingTest(FailMode.RandomTasks);
	}

	private static void runSucceedingTest(final FailMode failMode) {
		final Random random = new Random(1);
		final WorkflowEngine engine = fillEngineWithTasks(failMode, false, random);

		final Date workStart = new Date();
		boolean finished = false;
		int iterations;
		for (iterations = 0; iterations < NUM_TASKS; iterations++) {
			runEngine(failMode, engine);
			if (engine.isDone()) {
				finished = true;
				break;
			}
			if (!engine.isWorkAvailable()) {
				Assert.fail("The work must be always available if engine is not done - the tasks are not pausing");
			}
		}
		Assert.assertTrue(finished, "The engine must be finished in amount of steps equal to number of tasks");
		final Date workEnd = new Date();
		logOutput(failMode, workStart, iterations, workEnd, "immediate tasks");
	}

	private static void runEngine(final FailMode failMode, final WorkflowEngine engine) {
		try {
			engine.run();
		} catch (CompositeException error) {
			Assert.assertEquals(failMode, FailMode.RandomTasks, "Composite exception is thrown only if several tasks fail " + error.getMessage());
		} catch (Exception e) {
			if (failMode == FailMode.LastTask || failMode == FailMode.FirstTask) {
				Assert.assertEquals(e.getMessage(), "Task failed as expected", "We expect a single task to fail");
			} else {
				LOGGER.error("Unexpected exception", e);
				Assert.fail("There should never be an exception thrown");
			}
		}
	}

	private static void logOutput(final FailMode failMode, final Date workStart, final int iterations, final Date workEnd, final String tasks) {
		LOGGER.debug(MessageFormat.format("{0}\t{5},\t{4},\tmax {1} dependencies/task,\ttook {2} ms,\t{3} iterations.",
				NUM_TASKS,
				MAX_DEPENDENCIES,
				workEnd.getTime() - workStart.getTime(),
				iterations,
				"fail: " + failMode.name(),
				tasks));
	}

	private static void runPausingTest(final FailMode failMode) throws InterruptedException {
		pausedTasks.clear();
		final Random random = new Random(1);
		final WorkflowEngine engine = fillEngineWithTasks(failMode, true, random);

		final MyResumer r = new MyResumer();
		final UnPauser unpauser = new UnPauser(pausedTasks, random);
		final Thread t = new Thread(unpauser, "Un-pauser");
		t.start();

		final Date workStart = new Date();
		boolean finished = false;
		int iterations;
		for (iterations = 0; iterations < NUM_TASKS; iterations++) {
			runEngine(failMode, engine);
			if (engine.isDone()) {
				finished = true;
				break;
			}
			if (!engine.isWorkAvailable()) {
				// There is no work, pause until there is some.
				r.reset();
				engine.resumeOnWork(r);
				r.waitForResume();
			}
		}
		unpauser.stop();
		Assert.assertTrue(finished, "The engine must be finished in amount of steps lower than number of tasks");
		final Date workEnd = new Date();
		logOutput(failMode, workStart, iterations, workEnd, "pausing tasks");
	}

	private static WorkflowEngine fillEngineWithTasks(final FailMode failMode, final boolean pause, final Random random) {
		final WorkflowEngine engine = new WorkflowEngine("engine test");
		final List<Task> tasks = new ArrayList<Task>();
		for (int i = 0; i < NUM_TASKS; i++) {
			boolean fail = false;
			switch (failMode) {
				case FirstTask:
					if (i == 0) {
						fail = true;
					}
					break;
				case LastTask:
					if (i == NUM_TASKS - 1) {
						fail = true;
					}
					break;
				case Never:
					break;
				case RandomTasks:
					fail = random.nextBoolean();
					break;
			}
			final SimpleTask newTask = new SimpleTask(pause, fail);
			addRandomDependencies(random, tasks, newTask);
			tasks.add(newTask);
		}
		engine.addAllTasks(tasks);
		return engine;
	}

	private static void addRandomDependencies(final Random random, final List<Task> tasks, final SimpleTask newTask) {
		final int deps = random.nextInt(Math.max(Math.min(MAX_DEPENDENCIES, tasks.size()), 1));
		for (int j = 0; j < deps; j++) {
			final int k = random.nextInt(tasks.size());
			if (newTask.declareDependency(tasks.get(k))) {
				newTask.addDependency(tasks.get(k));
			}
		}
	}

	private static final class SimpleTask extends TaskBase {
		private List<Task> dependencies;
		private boolean pause;
		private boolean fail;

		public SimpleTask(final boolean pause, final boolean fail) {
			this.pause = pause;
			this.fail = fail;
			this.dependencies = new ArrayList<Task>();
		}

		public void run() {
			if (!pause) {
				if (fail) {
					throw new MprcException("Task failed as expected");
				}
				setState(TaskState.COMPLETED_SUCCESFULLY);
				for (final Task dep : dependencies) {
					Assert.assertEquals(dep.getState(), TaskState.COMPLETED_SUCCESFULLY, "All dependencies must be resolved before this task runs.");
				}
			} else {
				pausedTasks.add(this);
			}
			// Next time we run, we will succeed
			pause = false;
		}

		public boolean declareDependency(final Task task) {
			if (dependencies.contains(task)) {
				return false;
			}
			dependencies.add(task);
			return true;
		}
	}

	private static final class MyResumer implements Resumer {
		private final Object lock = new Object();
		private volatile boolean resumed;
		private volatile boolean usedForWait;
		private volatile boolean usedForResume;

		public MyResumer() {
			resumed = false;
			usedForWait = false;
			usedForResume = false;
		}

		public void resume() {
			synchronized (lock) {
				Assert.assertFalse(usedForResume, "resume() can be called only once between resets");
				usedForResume = true;
				resumed = true;
				lock.notifyAll();
			}
		}

		public void waitForResume() throws InterruptedException {
			synchronized (lock) {
				Assert.assertFalse(usedForWait, "waitForResume() can be called once between resets");
				usedForWait = true;
				int totalWait = 0;
				while (!resumed && totalWait < 30000) {
					lock.wait(1000);
					totalWait += 1000;
				}
				Assert.assertTrue(resumed, "waitForResume timed out.");
			}
		}

		public void reset() {
			synchronized (lock) {
				resumed = false;
				usedForWait = false;
				usedForResume = false;
			}
		}
	}

	// Randomly unpauses tasks from a list
	private static final class UnPauser implements Runnable {
		private volatile List<Task> pausedTasks;
		private volatile boolean stop;
		private Random random;

		public UnPauser(final List<Task> pausedTasks, final Random random) {
			this.pausedTasks = pausedTasks;
			this.random = random;
			this.stop = false;
		}

		public void stop() {
			this.stop = true;
		}

		public void run() {
			try {
				while (!stop) {
					if (pausedTasks.size() > 0) {
						final int index = random.nextInt(pausedTasks.size());
						final Task task = pausedTasks.get(index);
						pausedTasks.remove(index);
						task.setState(TaskState.COMPLETED_SUCCESFULLY);
						Thread.sleep(0);
					} else {
						Thread.sleep(10);
					}
				}
			} catch (InterruptedException e) {
				// SWALLOWED: We stop on interrupt
				LOGGER.debug("Interrupted unpauser", e);
				stop = true;
			}
		}

	}
}
