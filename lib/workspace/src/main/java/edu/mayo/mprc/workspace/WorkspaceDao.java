package edu.mayo.mprc.workspace;

import edu.mayo.mprc.config.RuntimeInitializer;
import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.database.Dao;
import edu.mayo.mprc.database.TestDataProvider;

import java.util.List;

public interface WorkspaceDao extends Dao, RuntimeInitializer, TestDataProvider {
	/**
	 * @return List of users recognized by the system. The user list should be treated as read-only - you cannot
	 *         modify the user information.
	 * @param withPreferences When set to true, the preference list is fetched as well
	 */
	List<User> getUsers(boolean withPreferences);

	/**
	 * @param email
	 * @return User with e-mail equal to given string, or null if no such user is defined. The user data is read-only.
	 */
	User getUserByEmail(String email);

	/**
	 * Adds a new user with specified information and returns the user object. The resulting object is read-only.
	 * In case there is already an user with same email in the database, an exception is thrown.
	 *
	 * @param firstName First name.
	 * @param lastName  Last name.
	 * @param email     Email - must be unique.
	 * @return Newly added user.
	 */
	User addNewUser(String firstName, String lastName, String email, Change change);
}
