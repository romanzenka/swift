package edu.mayo.mprc.fastadb;

import edu.mayo.mprc.database.PersistableBase;

/**
 * @author Roman Zenka
 */
public final class ProteinDescription extends PersistableBase {
	private String description;
	private static final int MAX_DESCRIPTION_LENGTH = 200;

	public ProteinDescription() {
		description = "";
	}

	public ProteinDescription(final String description) {
		setDescription(description);
	}

	public String getDescription() {
		return description;
	}

	void setDescription(final String description) {
		final String trimmed = description.trim();
		if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
			this.description = trimmed.substring(0, MAX_DESCRIPTION_LENGTH);
		} else {
			this.description = trimmed;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ProteinDescription)) return false;

		ProteinDescription that = (ProteinDescription) o;

		if (!description.equals(that.description)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return description.hashCode();
	}
}
