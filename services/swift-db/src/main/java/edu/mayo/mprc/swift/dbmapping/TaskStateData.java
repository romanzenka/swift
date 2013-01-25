package edu.mayo.mprc.swift.dbmapping;

import edu.mayo.mprc.database.PersistableBase;

public class TaskStateData extends PersistableBase {
	private String description;

	public TaskStateData() {
	}

	public TaskStateData(final String description) {
		this.description = description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !(o instanceof TaskStateData)) {
			return false;
		}

		final TaskStateData that = (TaskStateData) o;

		if (getDescription() != null ? !getDescription().equals(that.getDescription()) : that.getDescription() != null) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		final int result;
		result = (getDescription() != null ? getDescription().hashCode() : 0);
		return result;
	}


	public String toString() {
		return "TaskStateData{" +
				"id=" + getId() +
				", description='" + getDescription() + '\'' +
				'}';
	}
}
