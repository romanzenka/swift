package edu.mayo.mprc.database;

/**
 * A user type that needs a {@link edu.mayo.mprc.database.FileTokenToDatabaseTranslator}
 * object in order to function.
 *
 * @author Roman Zenka
 */
public interface NeedsTranslator {
	void setTranslator(final FileTokenToDatabaseTranslator translator);
}
