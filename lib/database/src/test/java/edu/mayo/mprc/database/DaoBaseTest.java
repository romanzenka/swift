package edu.mayo.mprc.database;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Arrays;

public final class DaoBaseTest extends DaoTest {
	private DaoBase base;
	private static final Logger LOGGER = Logger.getLogger(DaoBaseTest.class);

	@BeforeClass(alwaysRun = true)
	public void setUp() {

		LOGGER.getParent().setLevel(Level.DEBUG);
		base = new TestDaoBaseImpl();
		initializeDatabase(Arrays.asList(base));
		base.begin();
	}

	@AfterClass
	public void tearDown() {
		base.commit();
	}

	private TestSet s(final TestSet set) {
		LOGGER.debug("  // Saving set " + set.getSetName());
		base.save(set, Restrictions.eq("setName", set.getSetName()), true);
		LOGGER.debug("  \\ Saved set " + set.getSetName());
		return set;
	}

	private TestSetMember s(final TestSetMember m) {
		LOGGER.debug("    // Saving member " + m.getMemberName());
		base.save(m, Restrictions.eq("memberName", m.getMemberName()), false);
		LOGGER.debug("    \\ Saved member " + m.getMemberName());
		return m;
	}

	private TestSet makeSet(final String name, final int firstMember, final int lastMember) {
		TestSet set = new TestSet();
		set.setSetName(name);
		for (int i = firstMember; i <= lastMember; i++) {
			set.add(s(new TestSetMember("Member " + i)));
		}

		LOGGER.debug("// Updating set " + set.getSetName());
		set = base.updateCollection(set, set.getMembers(), "members");
		LOGGER.debug("\\ Updated set " + set.getSetName());
		return set;
	}

	@Test
	public void testSaveSet() {
		final TestSet s1 = makeSet("s1", 1, 7);

		Assert.assertNotNull(s1.getId());

		final TestSet s2 = makeSet("s2", 1, 12);
		Assert.assertNotSame(s2.getId(), s1.getId(), "Sets differ in elements, two different objects");

		final TestSet s3 = makeSet("s3", 1, 7);
		Assert.assertEquals(s3, s1, "These sets are meant to be the same");
		Assert.assertEquals(s3.getId(), s1.getId(), "Same set with same contents, save as same object");
	}

	@Test
	public void shouldSaveEmptySets() {
		final TestSet s1 = makeSet("e1", 1, 0);

		Assert.assertNotNull(s1.getId());

		final TestSet s2 = makeSet("e2", 1, 0);
		Assert.assertEquals(s2.getId(), s1.getId(), "Sets differ in name, save as same object");

		final TestSet s3 = makeSet("e3", 1, 0);
		Assert.assertEquals(s3, s1, "These sets are meant to be the same");
		Assert.assertEquals(s3.getId(), s1.getId(), "Same set with same contents, save as same object");
	}

	@Test
	public void shouldHonorWritesWithinTransaction() {
		final long initialCount = (Long) base.getSession().createQuery("select count(t) from TestSet as t").uniqueResult();
		final TestSet e1 = makeSet("extra1", 1, 2);
		final TestSet e2 = makeSet("extra2", 1, 3);
		final long finalCount = (Long) base.getSession().createQuery("select count(t) from TestSet as t").uniqueResult();
		Assert.assertEquals(finalCount, initialCount + 2, "One item has to be added");
	}

	/**
	 * Save same object twice, only one gets written out. Double equality has ranges.
	 */
	@Test
	public void shouldIdempotentlySaveDoubles() {
		TestDouble d1 = new TestDouble(10, 20);
		d1 = base.save(d1, testDoubleEqualityCriteria(d1), false);
		nextTransaction();
		TestDouble d2 = new TestDouble(10.0001, 19.9999);
		d2 = base.save(d2, testDoubleEqualityCriteria(d2), false);
		Assert.assertEquals(d2.getId(), d1.getId(), "Must be the same object");
		TestDouble d3 = new TestDouble(10.0001, Double.NaN);
		d3 = base.save(d3, testDoubleEqualityCriteria(d3), false);
		Assert.assertNotSame(d2.getId(), d3.getId(), "Must not be the same object");
	}

	/**
	 * Save same object twice, only one gets written out. Double NaN is handled properly.
	 */
	@Test
	public void shouldIdempotentlySaveNaNDoubles() {
		TestDouble d1 = new TestDouble(10, Double.NaN);
		d1 = base.save(d1, testDoubleEqualityCriteria(d1), false);
		nextTransaction();
		TestDouble d2 = new TestDouble(10.0001, Double.NaN);
		d2 = base.save(d2, testDoubleEqualityCriteria(d2), false);
		Assert.assertEquals(d2.getId(), d1.getId(), "Must be the same object");
		TestDouble d3 = new TestDouble(Double.NaN, Double.NaN);
		d3 = base.save(d3, testDoubleEqualityCriteria(d3), false);
		Assert.assertNotSame(d2.getId(), d3.getId(), "Must not be the same object");
	}

	private Criterion testDoubleEqualityCriteria(final TestDouble d1) {
		return Restrictions.conjunction()
				.add(DaoBase.doubleEq("value1", d1.getValue1(), 0.1))
				.add(DaoBase.doubleEq("value2", d1.getValue2(), 0.1));
	}

	@Test
	public void shouldSaveDateTimes() {
		TestDate d1 = new TestDate(new DateTime(2012, 2, 29, 10, 20, 30, 0), new DateTime(2010, 10, 30, 13, 45, 59, 0));
		d1 = base.save(d1, testDateEqualityCriteria(d1), false);
		nextTransaction();
		TestDate d2 = new TestDate(new DateTime(2012, 2, 29, 10, 20, 30, 0), new DateTime(2010, 10, 30, 13, 45, 59, 0));
		d2 = base.save(d2, testDateEqualityCriteria(d2), false);
		Assert.assertEquals(d2.getId(), d1.getId(), "Must be the same object");
	}

	private Criterion testDateEqualityCriteria(final TestDate d1) {
		return Restrictions.conjunction()
				.add(Restrictions.eq("value1", d1.getValue1()))
				.add(Restrictions.eq("value2", d1.getValue2()));
	}


}
