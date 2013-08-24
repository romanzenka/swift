package edu.mayo.mprc.dbcurator.model;

import edu.mayo.mprc.database.PersistableBase;
import org.apache.log4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeaderTransform extends PersistableBase {
	private static final Logger LOGGER = Logger.getLogger(HeaderTransform.class);

	private String name;
	/**
	 * A pattern that will match the headers and identify groups to be used in the substituion process
	 */
	private String groupString;

	/**
	 * identifies how the groups identified will be oriented in the result using the $1, $2,... syntax
	 */
	private String substitutionPattern;

	/**
	 * set this to true if it should be included as a selection to the users
	 */
	private Boolean common = false;

	/**
	 * Transient
	 * the comipiled pattern.
	 */
	private Pattern groupPattern = null;

	public String transform(final String toTransform) {
		if (groupString == null || substitutionPattern == null) {
			return toTransform;
		}

		if (groupPattern == null) {
			groupPattern = Pattern.compile(groupString);
		}

		final StringBuffer result = new StringBuffer();

		final Matcher match = groupPattern.matcher(toTransform);

		while (match.find()) {
			match.appendReplacement(result, substitutionPattern);
		}
		match.appendTail(result);

		if (result.toString().equalsIgnoreCase(substitutionPattern)) {
			LOGGER.info("Pattern not matched in header: " + toTransform);
			return toTransform;
		} else {
			return result.toString();
		}
	}

	public String getName() {
		return name;
	}

	public HeaderTransform setName(final String name) {
		this.name = name;
		return this;
	}

	public String getGroupString() {
		return groupString;
	}

	public HeaderTransform setGroupString(final String groupString) {
		this.groupString = groupString;
		return this;
	}

	public String getSubstitutionPattern() {
		return substitutionPattern;
	}

	public HeaderTransform setSubstitutionPattern(final String substitutionPattern) {
		this.substitutionPattern = substitutionPattern;
		return this;
	}

	public Boolean getCommon() {
		return common;
	}

	public HeaderTransform setCommon(final Boolean common) {
		this.common = common;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof HeaderTransform)) {
			return false;
		}

		final HeaderTransform that = (HeaderTransform) o;

		if (getCommon() != null ? !getCommon().equals(that.getCommon()) : that.getCommon() != null) {
			return false;
		}
		if (getGroupString() != null ? !getGroupString().equals(that.getGroupString()) : that.getGroupString() != null) {
			return false;
		}
		if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) {
			return false;
		}
		if (getSubstitutionPattern() != null ? !getSubstitutionPattern().equals(that.getSubstitutionPattern()) : that.getSubstitutionPattern() != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = getName() != null ? getName().hashCode() : 0;
		result = 31 * result + (getGroupString() != null ? getGroupString().hashCode() : 0);
		result = 31 * result + (getSubstitutionPattern() != null ? getSubstitutionPattern().hashCode() : 0);
		result = 31 * result + (getCommon() != null ? getCommon().hashCode() : 0);
		return result;
	}
}
