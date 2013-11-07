package edu.mayo.mprc.workspace;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.RuntimeInitializer;
import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.Database;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository("workspaceDao")
public final class WorkspaceDaoHibernate extends DaoBase implements WorkspaceDao, RuntimeInitializer {
	private static final Logger LOGGER = Logger.getLogger(WorkspaceDaoHibernate.class);

	public WorkspaceDaoHibernate() {
	}

	public WorkspaceDaoHibernate(final Database database) {
		super(database);
	}

	@Override
	public Collection<String> getHibernateMappings() {
		final List<String> list = new ArrayList<String>(Arrays.asList("edu/mayo/mprc/workspace/User.hbm.xml"));
		list.addAll(super.getHibernateMappings());
		return list;
	}

	/**
	 * We are sorting users by first and then last name. Display the names in "first last" order for a natural, easy to
	 * scan list.
	 *
	 * @param withPreferences
	 * @return List of users in "first last" order.
	 */
	@Override
	public List<User> getUsers(final boolean withPreferences) {
		try {
			final Criteria criteria = allCriteria(User.class)
					.addOrder(Order.asc("firstName"))
					.addOrder(Order.asc("lastName"))
					.setReadOnly(true);
			if (withPreferences) {
				criteria.setFetchMode("preferences", FetchMode.SELECT);
			}
			return listAndCast(criteria);
		} catch (Exception t) {
			throw new MprcException("Cannot obtain list of users", t);
		}
	}

	@Override
	public User getUserByEmail(final String email) {
		try {
			return get(User.class, Restrictions.eq("userName", email).ignoreCase());
		} catch (Exception t) {
			throw new MprcException("Cannot find user with e-mail [" + email + "]", t);
		}
	}


	@Override
	public User addNewUser(final String firstName, final String lastName, final String email, final Change change) {
		try {
			User user = new User(firstName, lastName, email, "database");
			user = save(user, getUserEqualityCriteria(user), true);
			return user;
		} catch (Exception t) {
			throw new MprcException("Cannot create new user " + firstName + " " + lastName, t);
		}
	}

	private Criterion getUserEqualityCriteria(final User user) {
		return Restrictions.eq("userName", user.getUserName());
	}

	@Override
	public String check() {
		if (countAll(User.class) == 0) {
			return "At least one user has to be defined";
		}
		if (!getUsersNoInitials().isEmpty()) {
			return "There are users with no initials defined";
		}
		return null;
	}

	@Override
	public void install(Map<String, String> params) {
		if (countAll(User.class) == 0) {
			final User user = new User("Mprc", "Test", "mprctest@localhost", "mt", "database");
			save(user, new Change("Creating a test user - no users were defined", new DateTime()), getUserEqualityCriteria(user), true);
		}

		addUserInitials();
	}

	private void addUserInitials() {
		// Update initials
		final List<User> users = getUsersNoInitials();
		if (!users.isEmpty()) {
			LOGGER.info("Updating user initials");
			int count = 0;

			for (final User user : users) {
				user.setInitials((user.getFirstName().charAt(0) + "" + user.getLastName().charAt(0)).toLowerCase(Locale.ENGLISH));
				LOGGER.info("User " + (++count) + " updated. User initials: " + user.getInitials());
			}
		}
	}

	private List<User> getUsersNoInitials() {
		try {
			return listAndCast(allCriteria(User.class)
					.add(Restrictions.isNull("initials")));
		} catch (Exception t) {
			throw new MprcException("Cannot obtain list of users", t);
		}
	}
}
