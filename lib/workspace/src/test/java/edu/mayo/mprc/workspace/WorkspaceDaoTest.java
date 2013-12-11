package edu.mayo.mprc.workspace;

import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.database.DaoTest;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

public final class WorkspaceDaoTest extends DaoTest {
	private WorkspaceDao workspaceDao;

	@BeforeClass
	public void setup() {
		final WorkspaceDaoHibernate workspaceDaoHibernate = new WorkspaceDaoHibernate();
		workspaceDao = workspaceDaoHibernate;

		initializeDatabase(Arrays.asList(workspaceDaoHibernate));
	}

	@AfterClass
	public void teardown() {
		teardownDatabase();
	}

	@Test
	public void getUserNamesTest() throws Throwable {
		workspaceDao.begin();
		try {
			final List<User> users = workspaceDao.getUsers(false);
			workspaceDao.commit();
			Assert.assertTrue((users != null && users.isEmpty()), "no user names should be found");
		} catch (Exception t) {
			workspaceDao.rollback();
			throw t;
		}
	}

	@Test(dependsOnMethods = "getUserNamesTest")
	public void listUsersTest() throws Throwable {
		workspaceDao.begin();
		try {
			final Change change = new Change("Test user added", new DateTime());
			workspaceDao.addNewUser("Roman", "Zenka", "zenka.roman@mayo.edu", change);
			final List<User> list = workspaceDao.getUsers(false);
			workspaceDao.commit();
			Assert.assertEquals(list.size(), 1, "One user has to be defined");
			final User user = list.get(0);
			Assert.assertEquals(user.getFirstName(), "Roman");
		} catch (Exception t) {
			workspaceDao.rollback();
			throw t;
		}
	}

	@Test(dependsOnMethods = "listUsersTest")
	public void preferencesTest() throws Throwable {
		workspaceDao.begin();
		try {
			final List<User> list = workspaceDao.getUsers(false);
			final User user = list.get(0);
			user.addPreference("likes", "coffee");
			workspaceDao.commit();
		} catch (Exception t) {
			workspaceDao.rollback();
			throw t;
		}

		workspaceDao.begin();
		try {
			final List<User> list = workspaceDao.getUsers(false);
			final User user = list.get(0);
			Assert.assertEquals(user.getPreferences().get("likes"), "coffee");
			workspaceDao.commit();
		} catch (Exception t) {
			workspaceDao.rollback();
			throw t;
		}
	}

	@Test
	public void shouldOrderById() {
		User u1 = new User();
		u1.setId(1);
		User u2 = new User();
		u2.setId(2);
		User u3 = new User();

		List<User> list1 = Arrays.asList(u1, u2, u3);
		List<User> list2 = Arrays.asList(u3, u1, u2);
		List<User> list3 = Arrays.asList(u2, u1);

		Assert.assertEquals(User.BY_ID.min(list1), u3);
		Assert.assertEquals(User.BY_ID.max(list1), u2);
		Assert.assertEquals(User.BY_ID.min(list2), u3);
		Assert.assertEquals(User.BY_ID.max(list2), u2);
		Assert.assertEquals(User.BY_ID.min(list3), u1);
		Assert.assertEquals(User.BY_ID.max(list3), u2);
	}
}
