package edu.mayo.mprc.workflow.engine;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.exceptions.CompositeException;
import edu.mayo.mprc.utilities.progress.ProgressReport;
import edu.mayo.mprc.workflow.persistence.TaskState;
import org.apache.log4j.Logger;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A generic workflow engine! :)
 */
public final class WorkflowEngine {
	private static final Logger LOGGER = Logger.getLogger(WorkflowEngine.class);

	private boolean initialized;
	private final List<Task> allTasks = new ArrayList<Task>();
	private final LinkedBlockingQueue<Task> tasksToProcess = new LinkedBlockingQueue<Task>();
	private ProgressReport previousProgressReport;
	private final CombinedMonitor monitor = new CombinedMonitor();
	private Resumer resumer;
	private AtomicInteger succeededTasks = new AtomicInteger(0);  // How many tasks succeeded (including with warning)
	private AtomicInteger warningTasks = new AtomicInteger(0); // How many have warning (when they fail, they are no longer warning, they are in error)
	private AtomicInteger failedTasks = new AtomicInteger(0); // Failed with our without warning
	private AtomicInteger initFailedTasks = new AtomicInteger(0);
	private AtomicInteger runningTasks = new AtomicInteger(0);
	private boolean done;
	private String id;
	private AtomicInteger taskId = new AtomicInteger(0);

	// We use this lock to check the size of the queue and add a new element atomically
	private final Object resumeLock = new Object();

	public WorkflowEngine(final String id) {
		this.id = id;
		done = false;
		initialized = false;
		previousProgressReport = null;
		resumer = null;
	}

	/**
	 * @return Workflow engine id.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Add new task to be executed.
	 *
	 * @param task Task to be executed.
	 */
	public void addTask(final Task task) {
		allTasks.add(task);
	}

	/**
	 * Add a list of tasks to be executed.
	 *
	 * @param tasks Tasks to be executed.
	 */
	public void addAllTasks(final Collection<? extends Task> tasks) {
		allTasks.addAll(tasks);
	}

	/**
	 * @return Dump of all the current tasks, their dependencies and states in the form of a .dot file.
	 */
	public String dumpDot() {
		final StringBuilder dot = new StringBuilder();
		dot.append("digraph template { \n");

		for (int i = 0; i < allTasks.size(); i++) {
			final Task task = allTasks.get(i);
			String color = "white";
			switch (task.getState()) {
				case COMPLETED_SUCCESFULLY:
					color = "green";
					break;
				case INIT_FAILED:
					color = "darkorange1";
					break;
				case READY:
					color = "beige";
					break;
				case RUNNING:
					color = "greenyellow";
					break;
				case RUN_FAILED:
					color = "red";
					break;
				case UNINITIALIZED:
					color = "grey";
					break;
				case RUNNING_WARN:
					color = "orange";
					break;
				case COMPLETED_WARNING:
					color = "green";
					break;
				default:
					throw new MprcException("Unsupported task state: " + task.getState().name());
			}
			dot.append(
					MessageFormat.format(
							"\"node_{0}\" [label=\"{1}\\n{2}\\n{3}\", color={4}, style=filled, shape=box, fontsize=14];\n",
							i,
							task.getName(),
							task.getDescription(),
							task.getState().getText(),
							color));
		}

		for (int i = 0; i < allTasks.size(); i++) {
			final Task task = allTasks.get(i);
			for (final Task output : task.getOutputs()) {
				int index = -1;
				for (int j = 0; j < allTasks.size(); j++) {
					if (allTasks.get(j).equals(output)) {
						index = j;
						break;
					}
				}
				if (index != -1) {
					dot.append(
							MessageFormat.format("\"node_{0}\" -> \"node_{1}\"\n",
									i, index));
				}
			}
		}

		dot.append("}\n");
		return dot.toString();
	}

	/**
	 * Add a monitor that watches the execution.
	 *
	 * @param monitor Monitor for the execution.
	 */
	public void addMonitor(final SearchMonitor monitor) {
		this.monitor.addMonitor(monitor);
	}

	/**
	 * @return Total amount of tasks within this engine.
	 */
	public int getNumTasks() {
		return allTasks.size();
	}

	private void initialize() {
		// We will start processing all tasks without dependencies
		synchronized (resumeLock) {
			for (final Task task : allTasks) {
				task.setWorkflowEngine(this);
				if (task.getInputs().size() == 0) {
					task.setState(TaskState.READY);
				}
			}
		}
	}

	/**
	 * Runs next step of the search.
	 * After the run returns, you should check {@link #isDone()} and {@link #isWorkAvailable()} to see whether you should
	 * terminate the loop or immediatelly continue with the next iteration. If there is no work available and work is not done,
	 * you call {@link #resumeOnWork} with a {@link edu.mayo.mprc.workflow.engine.Resumer} that will get triggered as soon as there is more work to do.
	 */
	public void run() {
		try {
			// If not initialized, run the first step.
			if (!initialized) {
				initialize();
				updateProgress();
				initialized = true;
			}

			// Drain all we collected in the queue so we can start processing it properly
			final List<Task> taskList = new ArrayList<Task>();
			synchronized (resumeLock) {
				tasksToProcess.drainTo(taskList);
			}

			// Task is in the queue because it can actually run - it is either ready or running (running tasks can be called multiple times)
			// All its dependencies are resolved, it was not run before, etc. Thenrefore we do not have to do any checking.
			for (final Task task : taskList) {
				assert task.getState() != TaskState.UNINITIALIZED : "The task " + task.getName() + " must be ready to run";
				if (task.getState() == TaskState.READY) {
					task.setState(TaskState.RUNNING);
				}
				try {
					task.run();
				} catch (Exception e) {
					task.setError(e);
				}
			}

			// We update the progress information
			done = updateProgress();

			// If we are completely done, we report it.
			if (done) {
				int numFailed = 0;
				final CompositeException exception = new CompositeException("Search failed");
				for (final Task task : allTasks) {
					if (!task.stateEquals(TaskState.COMPLETED_SUCCESFULLY)) {
						if (task.getLastError() != null) {
							exception.addCause(task.getLastError());
						}
						numFailed++;
					}
				}
				if (numFailed != 0) {
					// Throw an exception - failure
					if (exception.getCauses().size() == 1) {
						final Throwable t = exception.getCauses().iterator().next();
						if (t instanceof MprcException) {
							throw (MprcException) t;
						} else {
							throw new MprcException("Task failed", t);
						}
					} else {
						throw exception;
					}
				}
			}
		} catch (MprcException e) {
			// Let everyone interested know about the search failure
			monitor.error(e);
			// And rethrow the exception
			throw e;
		} catch (Exception t) {
			// Wrap the throwable into MprcException if not caught by the clause above
			monitor.error(t);
			throw new MprcException(t);
		}
	}

	/**
	 * @return True if all work is done.
	 */
	public boolean isDone() {
		return done;
	}

	/**
	 * @return True if there is some work to do immediatelly available in the queue.
	 */
	public boolean isWorkAvailable() {
		synchronized (resumeLock) {
			return tasksToProcess.size() > 0;
		}
	}

	/**
	 * Will call {@link edu.mayo.mprc.workflow.engine.Resumer#resume()} when there is some work available in the queue.
	 *
	 * @param resumer Resumer to call when work appears in the queue.
	 */
	public void resumeOnWork(final Resumer resumer) {
		boolean runResumer = false;

		synchronized (resumeLock) {
			this.resumer = null;

			if (tasksToProcess.size() > 0) {
				runResumer = true;
			} else {
				this.resumer = resumer;
			}
		}
		if (runResumer) {
			resumer.resume();
		}
	}

	/**
	 * @return True if everything is done.
	 */
	private boolean updateProgress() {
		// Notify all the monitors about the updated progress statistics
		final ProgressReport report = new ProgressReport(
				allTasks.size(),
				0,
				0,
				runningTasks.get(),
				0,
				succeededTasks.get(),
				warningTasks.get(),
				failedTasks.get(),
				initFailedTasks.get());

		// Only if there is a change
		if (!report.equals(previousProgressReport)) {
			for (final SearchMonitor m : monitor.getMonitors()) {
				m.updateStatistics(report);
			}
			this.previousProgressReport = report;
		}

		return succeededTasks.get() + failedTasks.get() == allTasks.size();
	}

	/**
	 * The task notifies the engine when the description changes. Call only from the task.
	 */
	public void taskDescriptionChange(final TaskBase taskBase) {
		monitor.taskChange(taskBase);
	}

	/**
	 * The task notifies the engine when the name changes. Call only from the task.
	 */
	public void taskNameChange(final TaskBase taskBase) {
		monitor.taskChange(taskBase);
	}

	/**
	 * Task notifies the engine when its state changes. This is done AFTER the task flips the state.
	 * Call only from the task.
	 */
	public void afterTaskStateChange(final TaskBase taskBase, final TaskState oldState, final TaskState currentState) {
		if (oldState == currentState) {
			return;
		}

		switch (currentState) {
			case COMPLETED_SUCCESFULLY:
				succeededTasks.incrementAndGet();
				runningTasks.decrementAndGet();
				break;
			case INIT_FAILED:
				initFailedTasks.incrementAndGet();
				failedTasks.incrementAndGet();
				break;
			case RUNNING:
				runningTasks.incrementAndGet();
				break;
			case RUN_FAILED:
				failedTasks.incrementAndGet();
				runningTasks.decrementAndGet();
				break;
			case READY:
			case UNINITIALIZED:
				break;
			case RUNNING_WARN:
				warningTasks.incrementAndGet();
				break;
			case COMPLETED_WARNING:
				runningTasks.decrementAndGet();
				succeededTasks.incrementAndGet();
				break;
			default:
				throw new MprcException("Unknown task state " + currentState.getText());
		}
		monitor.taskChange(taskBase);

		// Make the state transition, propagate it to other tasks and if we need to be resumed, signal that

		// If we are switching to completed/failed or init failed states, the tasks dependent on us have to know.
		if (currentState == TaskState.COMPLETED_SUCCESFULLY ||
				currentState == TaskState.RUN_FAILED ||
				currentState == TaskState.INIT_FAILED) {
			// Tell all of our outputs that we are done
			for (final Task dependent : taskBase.getOutputs()) {
				dependent.inputDone(taskBase);
			}

			// We update the progress information
			final boolean allDone = updateProgress();

			// If absolutely everything is done, we do one last resume to report this.
			if (allDone) {
				reportResume();
			}
		} else if (currentState == TaskState.READY) {
			tasksToProcess.add(taskBase);
			// The initial adding of tasks does not cause a resume
			if (initialized) {
				reportResume();
			}
		}
	}

	public void afterProgressInformationReceived(final TaskBase task, final Object progressInfo) {
		monitor.taskProgress(task, progressInfo);
	}

	public void workflowFailure(final Throwable e) {
		monitor.error(e);
	}

	/**
	 * Report that we want to be resumed. This is done only once per resumer.
	 */
	private void reportResume() {
		synchronized (resumeLock) {
			if (resumer != null) {
				// Resume just once
				resumer.resume();
				resumer = null;
			}
		}
	}

	public String getNewTaskId(final String name) {
		if (name == null) {
			return null;
		}
		return name.replaceAll("\\s", "_") + ":" + taskId.incrementAndGet();
	}

	/**
	 * Reports an external error like if it occured within the workflow engine.
	 *
	 * @param t Exception to report.
	 */
	public void reportError(final Throwable t) {
		try {
			monitor.error(t);
		} catch (Exception e) {
			LOGGER.debug("Engine monitor error", e);
			// SWALLOWED: Monitor failure does not count
		}
	}
}
