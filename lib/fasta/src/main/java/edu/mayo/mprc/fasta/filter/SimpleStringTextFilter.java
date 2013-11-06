package edu.mayo.mprc.fasta.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * A TextFilter that takes a simple string upon creation and then when matches() is called it sees if the simple string
 * is contained within the test string.  This filter will be case insensitive.
 *
 * @author Eric J. Winter Date: Apr 6, 2007 Time: 1:51:32 PM
 */
public final class SimpleStringTextFilter implements TextFilter {
	/**
	 * the string you want to see is contained within a larger string when you call matches
	 */
	private List<String> criteria;

	/**
	 * the mode for matching defaulting to any
	 */
	private MatchMode mode = MatchMode.ANY;

	/**
	 * the number of criteria kept around to minimize calls to get the length of criteria list
	 */
	private int tokenCount = 0;

	/**
	 * Constructor that takes the String you want to search for in test strings
	 *
	 * @param filterString the string to search for in the strings passed int matches()
	 */
	public SimpleStringTextFilter(final String filterString) {
		//we are ignoring case to lowercase every thing
		final String lower = filterString.toLowerCase(Locale.ENGLISH);

		//break up the filter into each word
		final StringTokenizer tokenizer = new StringTokenizer(lower);

		//create and populate the list of criteria strings
		criteria = new ArrayList<String>(tokenizer.countTokens());
		while (tokenizer.hasMoreTokens()) {
			criteria.add(tokenizer.nextToken());
		}

		//get the size in order to reduce the calls to criteria.size() in the future
		tokenCount = criteria.size();
	}

	/**
	 * tests to see if the given criteria is contained within the toTest
	 *
	 * @param toTest the string you want to see if the criteria is contained within
	 * @return {@code true} if toTest contains the criteria else {@code false}
	 */
	@Override
	public boolean matches(final String toTest) {

		//we want to ignore case so make lower case
		final String lower = toTest.toLowerCase(Locale.ENGLISH);

		//go through and count the number of matches
		int matchCount = 0;
		for (final String s : criteria) {
			if (lower.contains(s)) {
				matchCount++;
			}
		}

		//if any of the following are true then return false else return true
		// - mode is any and count is 0
		// - mode is all and match ount is less then the number of criteria
		// - mode is none and we have a match
		if ((mode == MatchMode.ANY) && (matchCount == 0)) {
			return false;
		} else if ((mode == MatchMode.ALL) && (matchCount < tokenCount)) {
			return false;
		} else if ((mode == MatchMode.NONE) && (matchCount > 0)) {
			return false;
		}

		return true;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setMatchMode(final MatchMode mode) {
		this.mode = mode;
	}

	/**
	 * Tests the to see if the criteria specified in the constructor is valid.  If so we just return a string indicating
	 * sucess.  There shouldn't be any cases were a simple text filter will be invalid as long as it is a valid String
	 *
	 * @return a message indicating any problem so it should always return TextFilter.VALID
	 */
	@Override
	public String testCriteria() {
		if (criteria == null || criteria.isEmpty()) {
			return "Enter a criteria string";
		}
		return TextFilter.VALID;
	}
}
