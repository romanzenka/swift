package edu.mayo.mprc.database;

import java.io.File;

/**
 * Anything that can translate a database token into a file and back.
 */
public interface FileTokenToDatabaseTranslator {

	/**
	 * Gets tokenPath of this file relative to database Daemon.
	 *
	 * @param file
	 * @return
	 */
	String fileToDatabaseToken(File file);

	/**
	 * Gets File from tokenPath relative to database Daemon.
	 *
	 * @param tokenPath
	 * @return
	 */
	File databaseTokenToFile(String tokenPath);
}
