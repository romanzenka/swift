package edu.mayo.mprc.config.ui;

/**
 * A &lt;fix&gt; tag describing a way to fix a problem.
 * <p/>
 * The validation error messages can contain special markup that allows the user to "fix" an error. The markup is:
 * {@code
 * <fix action="whatever data">Message for the user</fix>
 * }
 * <p/>
 * When the user clicks the link, the {@link PropertyChangeListener#fixError}
 * method will be called with the particular action and property name.
 */
public final class FixTag {

	public static final String BEGINNINGTAG = "<fix";
	public static final String ENDTAG = "</fix>";
	public static final String ACTIONATTRIB = "action=\"";

	private FixTag() {
	}

	public static String getTag(final String action, final String description) {
		return BEGINNINGTAG + (action == null ? "" : " " + ACTIONATTRIB + action + "\"") + ">" + description + ENDTAG;
	}

}
