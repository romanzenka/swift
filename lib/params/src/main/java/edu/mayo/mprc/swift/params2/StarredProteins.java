package edu.mayo.mprc.swift.params2;

import edu.mayo.mprc.database.PersistableBase;

/**
 * Defines a list of starred proteins.
 */
public class StarredProteins extends PersistableBase {
	private String starred;
	private String delimiter;
	private boolean regularExpression;
	private boolean matchName;

	public StarredProteins() {
	}

	public StarredProteins(final String starred, final String delimiter, final boolean regularExpression, final boolean matchName) {
		this.starred = starred;
		this.delimiter = delimiter;
		this.regularExpression = regularExpression;
		this.matchName = matchName;
	}

	/**
	 * @return Object that can decide whether given accession number belongs to a starred protein.
	 */
	public StarMatcher getMatcher() {
		return new StarMatcher(starred, delimiter, regularExpression, matchName);
	}

	public String getStarred() {
		return starred;
	}

	public void setStarred(final String starred) {
		this.starred = starred;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(final String delimiter) {
		this.delimiter = delimiter;
	}

	public boolean isRegularExpression() {
		return regularExpression;
	}

	public void setRegularExpression(final boolean regularExpression) {
		this.regularExpression = regularExpression;
	}

	public boolean isMatchName() {
		return matchName;
	}

	public void setMatchName(final boolean matchName) {
		this.matchName = matchName;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !(o instanceof StarredProteins)) {
			return false;
		}

		final StarredProteins that = (StarredProteins) o;

		if (isMatchName() != that.isMatchName()) {
			return false;
		}
		if (isRegularExpression() != that.isRegularExpression()) {
			return false;
		}
		if (getDelimiter() != null ? !getDelimiter().equals(that.getDelimiter()) : that.getDelimiter() != null) {
			return false;
		}
		if (getStarred() != null ? !getStarred().equals(that.getStarred()) : that.getStarred() != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = getStarred() != null ? getStarred().hashCode() : 0;
		result = 31 * result + (getDelimiter() != null ? getDelimiter().hashCode() : 0);
		result = 31 * result + (isRegularExpression() ? 1 : 0);
		result = 31 * result + (isMatchName() ? 1 : 0);
		return result;
	}
}
