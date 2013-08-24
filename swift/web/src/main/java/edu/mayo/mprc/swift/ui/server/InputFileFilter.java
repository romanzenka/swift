package edu.mayo.mprc.swift.ui.server;

import java.io.File;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter to find files that are not hidden and end with one of the extensions from a given list.
 */
public final class InputFileFilter implements FilenameFilter, Serializable {
	private static final long serialVersionUID = 20101221L;

	private final Pattern filePattern;
	private final Pattern dirPattern;
	private static final int PATTERN_BUILDER_CAPACITY = 20;
	private boolean dirsToo;

	public InputFileFilter() {
		filePattern = Pattern.compile(".*");
		dirPattern = Pattern.compile("^$");
	}

	/**
	 * @param allowedExtensions    Allowed extensions, including the dot (such as ".RAW" or ".mgf")
	 * @param allowedDirExtensions Which directory extensions make the directories be treated as files
	 * @param dirsToo              consider directories too
	 */
	public InputFileFilter(final String allowedExtensions, final String allowedDirExtensions, final boolean dirsToo) {
		filePattern = getPattern(allowedExtensions);
		dirPattern = getPattern(allowedDirExtensions);
		this.dirsToo = dirsToo;
	}

	private Pattern getPattern(final String e) {
		final String[] extensions = e.split("\\|");
		final StringBuilder pattern = new StringBuilder(PATTERN_BUILDER_CAPACITY);
		// We build a pattern like this: .*\\.ext$|.*\\.ext2$|...
		// It should match any file name that ENDS in given extension.
		for (final String extension : extensions) {
			pattern.append("|.*");
			pattern.append(Pattern.quote(extension));
			pattern.append('$');
		}

		return Pattern.compile(pattern.substring(1));
	}

	@Override
	public boolean accept(final File dir, final String name) {
		boolean result = false;
		final File file = new File(dir, name);
		if (!file.isHidden()) {
			if (dirsToo && file.isDirectory() && (dirPattern == null || dirPattern.matcher(name).matches())) {
				result = true;
			} else if (file.isFile()) {
				final Matcher match = filePattern.matcher(name);
				if (match.matches()) {
					result = true;
				}
			}
		}
		return result;
	}
}