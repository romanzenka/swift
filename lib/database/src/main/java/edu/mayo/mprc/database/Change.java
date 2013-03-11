package edu.mayo.mprc.database;

import org.joda.time.DateTime;

/**
 * Reason for a change.
 */
public class Change {
	private Integer id;
	private String reason;
	private DateTime date;

	public Change() {
	}

	public Change(final String reason, final DateTime date) {
		this.reason = reason;
		this.date = date;
	}

	public Integer getId() {
		return id;
	}

	void setId(final Integer id) {
		this.id = id;
	}

	public String getReason() {
		return reason;
	}

	void setReason(final String reason) {
		this.reason = reason;
	}

	public DateTime getDate() {
		return date;
	}

	void setDate(final DateTime date) {
		this.date = date;
	}

	@Override
	public String toString() {
		return "Change{" +
				"reason='" + reason + '\'' +
				", date=" + date.toString() +
				'}';
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Change)) {
			return false;
		}

		final Change change = (Change) obj;

		if (getDate() != null ? !getDate().equals(change.getDate()) : change.getDate() != null) {
			return false;
		}
		if (getId() != null ? !getId().equals(change.getId()) : change.getId() != null) {
			return false;
		}
		if (getReason() != null ? !getReason().equals(change.getReason()) : change.getReason() != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = getId() != null ? getId().hashCode() : 0;
		result = 31 * result + (getReason() != null ? getReason().hashCode() : 0);
		result = 31 * result + (getDate() != null ? getDate().hashCode() : 0);
		return result;
	}
}
