package edu.mayo.mprc.database;

import org.hibernate.Session;

/**
 * @author Roman Zenka
 */
public interface SessionProvider {
	/**
	 * @return Current database session.
	 */
	Session getSession();
}
