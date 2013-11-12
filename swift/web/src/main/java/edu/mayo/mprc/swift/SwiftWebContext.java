package edu.mayo.mprc.swift;

import edu.mayo.mprc.swift.resources.WebUi;

/**
 * This is a utility class for centralizing access to the Spring ApplicationContext.  Ideally this
 * class would eventually go away as we wire more and more of Swift through Spring but in reality it will take a large
 * effort (too large?) to decouple Swift enough to make full Spring wiring possible.
 */
public final class SwiftWebContext {
	private SwiftWebContext() {
	}

	/**
	 * @return Centralized configuration for all the servlets.
	 */
	public static WebUi getWebUi() {
		return MainFactoryContext.getWebUiHolder().getWebUi();
	}

	public static String getPathPrefix() {
		final String prefix = getWebUi().getFileTokenFactory().fileToDatabaseToken(
				getWebUi().getBrowseRoot());
		if (!prefix.endsWith("/")) {
			return prefix + "/";
		}
		return prefix;
	}
}
