package edu.mayo.mprc.workflow.persistence;

public enum TaskState {
	/**
	 * Nobody started anything.
	 */
	UNINITIALIZED("Uninitialized"),
	/**
	 * All our inputs are ready, we can run.
	 */
	READY("Ready"),
	/**
	 * We are running right now.
	 */
	RUNNING("Running"),
	/**
	 * We are running in a warning state.
	 */
	RUNNING_WARN("Running Warning"),
	/**
	 * The execution failed.
	 */
	RUN_FAILED("Run Failed"),
	/**
	 * Success!
	 */
	COMPLETED_SUCCESFULLY("Completed Successfully"),
	/**
	 * Success with warnings.
	 */
	COMPLETED_WARNING("Completed with Warning"),
	/**
	 * Could not even start running, because inputs failed.
	 */
	INIT_FAILED("InitializationFailed");

	TaskState(final String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public static TaskState fromText(final String text) {
		for (final TaskState state : TaskState.values()) {
			if (state.getText().equals(text)) {
				return state;
			}
		}
		return null;
	}

	private final String text;
}
