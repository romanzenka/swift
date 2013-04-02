package edu.mayo.mprc.config;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;

import java.io.*;

/**
 * Reads configs written with {@link AppConfigWriter}.
 *
 * @author Roman Zenka
 */
public final class AppConfigReader implements Closeable {
	private BufferedReader reader;
	private MultiFactory multiFactory;
	private DependencyResolver dependencyResolver;

	public AppConfigReader(File configFile, MultiFactory multiFactory) {
		try {
			init(new FileReader(configFile), multiFactory);
		} catch (FileNotFoundException e) {
			throw new MprcException("Cannot read config file " + configFile.getAbsolutePath(), e);
		}
	}

	private void init(Reader reader, MultiFactory multiFactory) {
		this.reader = new BufferedReader(reader);
		this.multiFactory = multiFactory;
		this.dependencyResolver = new DependencyResolver(multiFactory);
	}

	public ApplicationConfig load() {
		ApplicationConfig config = new ApplicationConfig();
		int lineNum = 0;
		try {
			while (true) {
				final String line = reader.readLine();
				lineNum++;
				if (line == null) {
					break;
				}
				final String unescapedLine = unescapeLine(line);
			}
		} catch (Exception e) {
			throw new MprcException("Error reading line: " + lineNum, e);
		} finally {
			FileUtilities.closeQuietly(this);
		}
		return config;
	}

	static String unescapeLine(final String line) {
		if (line == null) {
			return null;
		}
		int lastIndex = 0;
		int state = 0;
		final StringBuilder result = new StringBuilder(line.length());
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			switch (state) {
				case 0: // Start
					if ((' ' == c) || (c == '\t')) {
						// Keep eating spaces and tabs
					} else {
						if ('\\' == c) {
							state = 2; // Escape
						} else if ('#' == c) {
							state = 3; // Comment
						} else {
							state = 1; // Collecting characters
							result.append(c);
							lastIndex++;
						}
					}
					break;
				case 1: // Collect characters
					if ('\\' == c) {
						state = 2; // Escape
					} else if ('#' == c) {
						state = 3; // Comment
					} else {
						result.append(c);
						if (c != ' ' && c != '\t') {
							lastIndex = result.length();
						}
					}
					break;
				case 2: // Escape
					switch (c) {
						case 'n':
							result.append('\n');
							lastIndex++;
							break;
						case 'r':
							result.append('\r');
							lastIndex++;
							break;
						default:
							result.append(c);
							if (c != ' ' && c != '\t') {
								lastIndex = result.length();
							}
							break;
					}
					state = 1;
				case 3: // Comment - we never escape this case
					break;

			}
		}
		return result.substring(0, lastIndex);
	}

	@Override
	public void close() throws IOException {
		FileUtilities.closeQuietly(reader);
	}
}
