package edu.mayo.mprc.workflow.engine;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.workflow.persistence.TaskState;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Common stuff for our tasks.
 * This is a critical class because it is being used in a multithreaded environment. Few operations are truly safe.
 */
public abstract class TaskBase implements Task {
	// The engine the task belongs to
	private WorkflowEngine engine;

	// List of dependencies
	private List<Task> inputs = new ArrayList<Task>();
	private List<Task> outputs = new ArrayList<Task>();

	// Counting dependencies
	private AtomicInteger numInputsDone = new AtomicInteger(0);
	private AtomicInteger numInputsFailed = new AtomicInteger(0);

	// Task state information
	// All of these objects are guarded by stateLock
	private final Object stateLock = new Object();
	private TaskState state = TaskState.UNINITIALIZED;
	private String description;
	private String name;
	private String id;
	private Date becameReady;
	private Date executionStarted;
	private Date executionFinished;
	private Integer taskDataId;

	private String executedOnHost = null; // host enqueued on
	private Throwable lastError;
	private String lastWarning;

	private int priority;

	/**
	 * Set to true by {@link #completeWhenFilesAppear}
	 */
	protected AtomicBoolean waitingForFileToAppear = new AtomicBoolean(false);

	public TaskBase() {
		this.id = null;
	}

	public void setWorkflowEngine(final WorkflowEngine engine) {
		this.engine = engine;
		synchronized (stateLock) {
			if (this.id == null && this.name != null) {
				this.id = engine.getNewTaskId(this.getName());
			}
		}
	}

	public WorkflowEngine getEngine() {
		return engine;
	}

	public String getDescription() {
		synchronized (stateLock) {
			return description;
		}
	}

	public void setDescription(final String description) {
		synchronized (stateLock) {
			this.description = description;
		}
		if (engine != null) {
			engine.taskDescriptionChange(this);
		}
	}

	public void setName(final String name) {
		final WorkflowEngine engine = this.engine;
		synchronized (stateLock) {
			if (this.id == null && engine != null) {
				this.id = engine.getNewTaskId(name);
			}
			this.name = name;
		}
		if (engine != null) {
			engine.taskNameChange(this);
		}
	}

	public String getName() {
		synchronized (stateLock) {
			return this.name;
		}
	}

	public Date getBecameReady() {
		synchronized (stateLock) {
			return becameReady;
		}
	}

	public Date getExecutionStarted() {
		synchronized (stateLock) {
			return executionStarted;
		}
	}

	public Date getExecutionFinished() {
		synchronized (stateLock) {
			return executionFinished;
		}
	}

	public Integer getTaskDataId() {
		synchronized (stateLock) {
			return taskDataId;
		}
	}

	public void setTaskDataId(final Integer taskDataId) {
		synchronized (stateLock) {
			this.taskDataId = taskDataId;
		}
	}

	public String getExecutedOnHost() {
		return executedOnHost;
	}

	public void setExecutedOnHost(final String executedOnHost) {
		this.executedOnHost = executedOnHost;
	}

	void assertValidStateChange(final TaskState oldState, final TaskState newState) {
		if (oldState == newState) {
			return;
		}
		final String currentTransition =
				(oldState == null ? "null" : oldState.getText())
						+ " -> "
						+ (newState == null ? "null" : newState.getText()) + " prohibited: ";
		if (oldState == null) {
			assert TaskState.UNINITIALIZED == newState : currentTransition + "null -> " + TaskState.UNINITIALIZED.getText() + " is the only allowed transition.";
		} else {
			switch (oldState) {
				case COMPLETED_SUCCESFULLY:
				case COMPLETED_WARNING:
				case RUN_FAILED:
				case INIT_FAILED:
					assert false : currentTransition + " once task succeeds or fails, it must not change its state";
					break;
				case READY:
					assert newState == TaskState.RUNNING || newState == TaskState.RUN_FAILED || newState == TaskState.RUNNING_WARN
							: currentTransition + " ready task can only start running or fail before the packet gets even sent";
					break;
				case RUNNING:
					assert newState == TaskState.COMPLETED_SUCCESFULLY ||
							newState == TaskState.RUNNING_WARN ||
							newState == TaskState.COMPLETED_WARNING ||
							newState == TaskState.RUN_FAILED
							: currentTransition + " running task can only succeed or fail";
					break;
				case UNINITIALIZED:
					assert newState == TaskState.INIT_FAILED || newState == TaskState.READY :
							currentTransition + " uninitialized task can only fail initialization or become ready";
					break;
				case RUNNING_WARN:
					assert newState == TaskState.COMPLETED_WARNING || newState == TaskState.RUN_FAILED : currentTransition + " running warn task can only complete with warning or fail";
					break;
				default:
					assert false : "State not supported " + oldState.getText();
			}
		}
	}

	public void setState(final TaskState newState) {
		assert engine != null : "Cannot change task state if the task is not associtated with an engine" + this.getName() + " " + this.getDescription();
		TaskState oldState = null;
		synchronized (stateLock) {
			oldState = state;
			if (oldState == newState) {
				// No change - do nothing
				return;
			}
			// Check whether this is a valid transition
			assertValidStateChange(oldState, newState);
			state = newState;
			// Record timestamps
			if (state == TaskState.COMPLETED_SUCCESFULLY ||
					state == TaskState.RUN_FAILED) {
				executionFinished = new Date();
			} else if (state == TaskState.READY) {
				becameReady = new Date();
			} else if (state == TaskState.RUNNING) {
				executionStarted = new Date();
			}
		}
		// Notify the search engine about this change
		engine.afterTaskStateChange(this, oldState, newState);
	}

	public TaskState getState() {
		synchronized (stateLock) {
			return state;
		}
	}

	public boolean isFailed() {
		synchronized (stateLock) {
			return TaskState.INIT_FAILED == state ||
					TaskState.RUN_FAILED == state;
		}
	}

	public boolean isSuccessful() {
		synchronized (stateLock) {
			return TaskState.COMPLETED_SUCCESFULLY == state ||
					TaskState.COMPLETED_WARNING == state;
		}
	}

	public boolean isDone() {
		synchronized (stateLock) {
			return TaskState.COMPLETED_SUCCESFULLY == state ||
					TaskState.COMPLETED_WARNING == state ||
					TaskState.INIT_FAILED == state ||
					TaskState.RUN_FAILED == state;
		}
	}

	public boolean stateEquals(final TaskState checkAgainst) {
		synchronized (stateLock) {
			return checkAgainst == state;
		}
	}

	public void setError(final Throwable error) {
		synchronized (stateLock) {
			this.lastError = error;
			setState(TaskState.RUN_FAILED);
		}
	}

	public void setWarning(final String warning) {
		synchronized (stateLock) {
			this.lastWarning = warning;
			if (state == TaskState.RUNNING) {
				setState(TaskState.RUNNING_WARN);
			}
		}
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(final int priority) {
		this.priority = priority;
	}

	public Throwable getLastError() {
		synchronized (stateLock) {
			return lastError;
		}
	}

	public String getLastWarning() {
		synchronized (stateLock) {
			return lastWarning;
		}
	}

	public void completeWhenFilesAppear(final File... files) {
		waitingForFileToAppear.set(true);
		try {
			FileUtilities.waitForFiles(Sets.<File>newHashSet(files));
			setComplete();
		} catch (MprcException e) {
			setError(new MprcException("The files [" + Joiner.on("], [").join(files) + "] did not appear even after 2 minutes."));
		}
	}

	private void setComplete() {
		setState(TaskState.COMPLETED_SUCCESFULLY);
	}

	public void addDependency(final Task task) {
		if (task != null) {
			if (!task.getOutputs().contains(this)) {
				inputs.add(task);
				task.getOutputs().add(this);
			}
		}
	}

	public void inputDone(final Task input) {
		assert getState() == TaskState.UNINITIALIZED : "Only uninitialized tasks are interested in their inputs. This task is in state " + getState().getText();
		// If the input failed, keep a note
		if (input.getState() != TaskState.COMPLETED_SUCCESFULLY) {
			numInputsFailed.incrementAndGet();
		}
		// If all inputs are done, we change our own state
		if (numInputsDone.incrementAndGet() == inputs.size()) {
			if (numInputsFailed.get() > 0) {
				this.setState(TaskState.INIT_FAILED);
			} else {
				this.setState(TaskState.READY);
			}
		}
	}

	public void afterProgressInformationReceived(final Object progressInfo) {
		if (engine != null) {
			engine.afterProgressInformationReceived(this, progressInfo);
		}
	}

	public List<Task> getInputs() {
		return inputs;
	}

	public List<Task> getOutputs() {
		return outputs;
	}

	public String getFullId() {
		final WorkflowEngine engine = this.engine;
		synchronized (stateLock) {
			return engine.getId() + "." + id;
		}
	}
}
