package edu.mayo.mprc.swift.dbmapping;

import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.PersistableBase;
import org.hibernate.criterion.Criterion;

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

	@Override
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

	@Override
	public int hashCode() {
		final int result;
		result = (getDescription() != null ? getDescription().hashCode() : 0);
		return result;
	}

	@Override
	public Criterion getEqualityCriteria() {
		return DaoBase.nullSafeEq("description", getDescription());
	}

	public String toString() {
		return "TaskStateData{" +
				"id=" + getId() +
				", description='" + getDescription() + '\'' +
				'}';
	}
}
