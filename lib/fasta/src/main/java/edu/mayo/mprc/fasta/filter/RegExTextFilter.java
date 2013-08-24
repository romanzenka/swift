package edu.mayo.mprc.fasta.filter;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A @see TextFilter that takes a Perl style regular expression (RE) upon construction and looks for this RE when
 * matches() is called.
 * <p/>
 *
 * @author Eric J. Winter 4/9/07
 */
public final class RegExTextFilter implements TextFilter {

	/**
	 * the mode that should be used when determining if a string contains this regular expression for regular expression
	 * matching it is typically ANY or None since only 1 regular expression is normally included in the criteria.  A none
	 * will invert the result of matches so if any would return one value a setting of none would return the opposite value.
	 */
	private MatchMode mode = MatchMode.ANY;

	/**
	 * The regular expression that should be searched for
	 */
	private final String searchRegEx;

	/**
	 * the pattern that will be compiled and used repeatably once this object is created.
	 */
	private Pattern pattern;

	/**
	 * createa a new regextextfilter with a given expression.
	 *
	 * @param regex the pattern you want to look for
	 * @throws PatternSyntaxException
	 */
	public RegExTextFilter(final String regex) {
		searchRegEx = regex;
	}


	/**
	 * A method to see if toMatch contains text that matches this filters criteria.
	 *
	 * @param toMatch the String that you want to test
	 * @return true if this filter matches at least one segment of the toMatch String
	 */
	@Override
	public boolean matches(final String toMatch) {
		compilePattern();

		boolean result = pattern.matcher(toMatch).find();
		if (mode == MatchMode.NONE) {
			result = !result;
		}
		return result;
	}

	/**
	 * sets the mode that should be used when running this filter such as ALL, ANY, or NONE
	 *
	 * @param mode the mode that we want to use
	 */
	@Override
	public void setMatchMode(final MatchMode mode) {
		this.mode = mode;
	}

	/**
	 * Tests to see if the currently set criteria is a valid search expression.  It will check to see if the regular
	 * expression is compileable.  If not then the error message from the compile attempt is returned.
	 *
	 * @return if the criteria is valid the VALID String is returned else a short string indicating where the problem
	 *         may be will be given
	 */
	@Override
	public String testCriteria() {
		try {
			compilePattern();
		} catch (PatternSyntaxException pse) {
			return pse.getMessage();
		}
		return VALID;
	}

	private void compilePattern() {
		if (pattern == null) {
			pattern = Pattern.compile(searchRegEx, Pattern.CASE_INSENSITIVE);
		}
	}
}
