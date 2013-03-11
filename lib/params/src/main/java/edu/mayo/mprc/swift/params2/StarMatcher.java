package edu.mayo.mprc.swift.params2;

import java.util.regex.Pattern;

/**
 * Determines whether given accession number belongs to a starred protein or not.
 *
 * @author Roman Zenka
 */
public final class StarMatcher {
	private Pattern pattern;

	public StarMatcher(String starred, String delimiter, boolean regularExpression, boolean matchName) {
		final String[] items = starred.split(delimiter);
		final StringBuilder regex = new StringBuilder(100);
		for (final String item : items) {
			if (regex.length() > 0) {
				regex.append("|");
			}
			if (regularExpression) {
				regex.append(item.trim());
			} else {
				regex.append(Pattern.quote(item.trim()));
			}
		}
		pattern = Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
	}

	public boolean matches(String accNum) {
		return pattern.matcher(accNum).matches();
	}
}
