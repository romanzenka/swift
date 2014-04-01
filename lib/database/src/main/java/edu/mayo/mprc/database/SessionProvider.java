package edu.mayo.mprc.database;

import org.hibernate.Session;
import org.hibernate.dialect.Dialect;

/**
 * @author Roman Zenka
 */
public interface SessionProvider {
	/**
	 * @return Current database session.
	 */
	Session getSession();

	/**
	 * @return Current database dialect.
	 */
	Dialect getDialect();

	/**
	 * Return fully qualified name for given table (using schema)
	 */
	String qualifyTableName(String table);
}
