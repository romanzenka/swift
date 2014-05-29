package edu.mayo.mprc.utilities;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * This class will take a Pattern and a File and provide methods for getting the matches and performing substitutions
 *
 * @author Eric Winter
 */
public final class StreamRegExMatcher {
	private Pattern pattern;
	private File file;
	private StringBuilder contents;

	/**
	 * @param toLookFor the pattern we want to perform matches against
	 * @param toMatchIn the file we want to look for contents in
	 * @throws java.io.FileNotFoundException if the file was not found
	 * @throws java.io.IOException           if there was a problem reading the file or the file is over 500kB.
	 */
	public StreamRegExMatcher(final Pattern toLookFor, final File toMatchIn) throws IOException {
		if (!toMatchIn.exists()) {
			throw new FileNotFoundException("The file given could not be found " + toMatchIn.getAbsolutePath());
		}
		pattern = toLookFor;
		file = toMatchIn;
		contents = new StringBuilder(Files.toString(file, Charsets.UTF_8));
	}

	public StreamRegExMatcher(final String toMatchIn) {
		this(null, toMatchIn);
	}

	public StreamRegExMatcher(final Pattern toLookFor, final String toMatchIn) {
		pattern = toLookFor;
		contents = new StringBuilder(toMatchIn);
	}


	public StreamRegExMatcher(final File toMatchIn) throws IOException {
		this(null, toMatchIn);
	}

	public void replaceAll(final Map<Pattern, String> patternToReplacementMap) {
		for (final Map.Entry<Pattern, String> p : patternToReplacementMap.entrySet()) {
			setPattern(p.getKey());
			replaceAll(p.getValue());
		}
	}

	/**
	 * you can call this if you want to start applying a different pattern.
	 *
	 * @param newPattern the pattern you will now be using for matches.
	 */
	public synchronized void setPattern(final Pattern newPattern) {
		pattern = newPattern;
	}


	/**
	 * replaces all matches with the given replacement
	 *
	 * @param replacement the stirng you want to replace
	 * @return the string with all of the replacements made
	 * @see java.util.regex.Matcher#replaceAll(String)
	 */
	public synchronized void replaceAll(final String replacement) {
		contents = new StringBuilder(pattern.matcher(contents).replaceAll(replacement));
	}

	/**
	 * gets the contents of the file being worked with
	 *
	 * @return the contents of the file being worked with
	 */
	public synchronized String getContents() {
		return contents.toString();
	}

	/**
	 * Replaces all of the matches in the file and writes it out to another file.  This will only overwrite the file
	 * if it is the same as the original.
	 *
	 * @param toWriteTo the file we want to write the replacement out to
	 * @throws java.io.IOException if there was a problem writing the file or if toWriteTo already existed
	 * @see java.util.regex.Matcher#replaceAll(String)
	 */
	public synchronized void writeContentsToFile(final File toWriteTo) throws IOException {
		final boolean overwriteSameFile = file != null && toWriteTo.equals(file);
		writeContentsToFile(toWriteTo, overwriteSameFile);
	}

	/**
	 * Replaces all of the matches in the file and writes it out to another file. Overwriting is controlled by the caller.
	 *
	 * @param toWriteTo the file we want to write the replacement out to
	 * @param overwrite Whether we are allowed to overwrite existing file.
	 * @throws java.io.IOException if there was a problem writing the file or if toWriteTo already existed
	 * @see java.util.regex.Matcher#replaceAll(String)
	 */
	public synchronized void writeContentsToFile(final File toWriteTo, final boolean overwrite) throws IOException {
		FileUtilities.writeStringToFile(toWriteTo, contents.toString(), overwrite);
	}

	/**
	 * return if there are any matches for the given
	 *
	 * @return true if the pattern matched at all.
	 */
	public synchronized boolean matches() {
		return pattern.matcher(contents).matches();
	}

	/**
	 * call this method when you are done, any subsequent calls will throw a RuntimeException
	 */
	public void close() {
		//currently do nothing since we only read the contents into a string in the future a more performant implementation will need this to be called
	}
}