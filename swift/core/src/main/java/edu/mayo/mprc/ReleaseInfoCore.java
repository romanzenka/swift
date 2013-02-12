package edu.mayo.mprc;

import java.io.Serializable;
import java.util.ResourceBundle;

/**
 * A java file that will have some revision information inserted before being compiled.  This will allow the application
 * to access this information at run-time.
 */
public final class ReleaseInfoCore implements Serializable {
	private static final long serialVersionUID = 20080128;

	public static String buildVersion() {
		return getProperty("build.version");
	}

	public static String buildTimestamp() {
		return getProperty("build.timestamp");
	}

	public static String buildRevision() {
		return getProperty("build.revision");
	}

	public static String buildLink() {
		return getProperty("build.link");
	}

	private static String getProperty(final String key) {
		final ResourceBundle bundle = ResourceBundle.getBundle("build");
		final String buildNumber = bundle.getString(key);
		return buildNumber;
	}


	public static void main(final String[] args) {
		System.out.println(ReleaseInfoCore.buildVersion());
	}

}
