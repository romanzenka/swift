package edu.mayo.mprc.database.bulk;

import edu.mayo.mprc.database.DaoTest;
import edu.mayo.mprc.database.TestList;
import edu.mayo.mprc.database.TestSetMember;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Roman Zenka
 */
public final class BulkLoadSetTest extends DaoTest {
	public static final int VALUES = 100;
	private TestBulkDaoBaseImpl dao;

	@BeforeMethod
	public void setup() {
		dao = new TestBulkDaoBaseImpl();
		initializeDatabase(Arrays.asList(dao));
	}

	@AfterMethod
	public void teardown() {
		teardownDatabase();
	}


	@Test
	public void shouldBulkLoadSets() {
		dao.begin();
		// We save the members first, independently, so they have ids
		final List<TestSetMember> members = new ArrayList<TestSetMember>(VALUES / 10);
		for (int i = 0; i < VALUES / 10; i++) {
			TestSetMember member = new TestSetMember("member #" + i);
			member = dao.save(member, testMemberEqualityCriteria(member), true);
			members.add(member);
		}
		dao.commit();

		final List<TestList> testSets = new ArrayList<TestList>(VALUES);
		for (int i = 0; i < VALUES; i++) {
			final TestList testSet = new TestList();
			testSets.add(testSet);
			for (int j = 0; j < i / 10; j++) {
				final TestSetMember member = members.get(j);
				testSet.add(member);
			}
		}

		// Here we expect the set members to be already saved
		dao.begin();
		final TestSetLoader loader = new TestSetLoader(dao, dao);
		loader.addObjects(testSets);

		for (final TestList item : testSets) {
			Assert.assertNotNull(item.getId());
			for (final TestSetMember member : item.getList()) {
				Assert.assertNotNull(member.getId());
			}
		}

		dao.commit();
	}

	private Criterion testMemberEqualityCriteria(final TestSetMember member) {
		return Restrictions.conjunction()
				.add(Restrictions.eq("memberName", member.getMemberName()));
	}
}
