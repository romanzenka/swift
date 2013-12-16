package edu.mayo.mprc.database;

import edu.mayo.mprc.MprcException;

import java.io.File;

/**
 * A dummy file token translator that does no translation at all - it stores the full file path directly.
 *
 * @author Roman Zenka
 */
public class DummyFileTokenTranslator implements FileTokenToDatabaseTranslator {

	public static final String PREFIX = "dummy:";

	@Override
	public String fileToDatabaseToken(final File file) {
		return PREFIX + file.getAbsolutePath();
	}

	@Override
	public File databaseTokenToFile(final String tokenPath) {
		if (tokenPath == null) {
			return null;
		}
		if (tokenPath.startsWith(PREFIX)) {
			return new File(tokenPath.substring(PREFIX.length()));
		} else {
			throw new MprcException("The dummy token translator encountered a non-dummy file path: " + tokenPath);
		}
	}
}
