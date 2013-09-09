package edu.mayo.mprc.heme.dao;

import com.google.common.base.Preconditions;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.DaoBase;
import org.hibernate.FetchMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Zenka
 */
@Repository("hemeDao")
public final class HemeDaoHibernate extends DaoBase implements HemeDao {
	@Override
	public Collection<String> getHibernateMappings() {
		return Arrays.asList("edu/mayo/mprc/heme/dao/HemeTest.hbm.xml");
	}

	@Override
	public List<HemeTest> getAllTests() {
		return (List<HemeTest>) getSession()
				.createCriteria(HemeTest.class)
				.setFetchMode("searchRun", FetchMode.JOIN)
				.addOrder(Order.desc("date"))
				.addOrder(Order.asc("name"))
				.list();
	}

	/**
	 * The two objects are equivalent iff their date and path match.
	 * Name does not have to match since it is contained in the path.
	 * Also, the path is what matters.
	 *
	 * @param test Test to check for equality.
	 * @return Hibernate criteria matching all records equal to the provided one.
	 */
	private Criterion getHemeTestEqualityCriteria(final HemeTest test) {
		return Restrictions.and(
				DaoBase.nullSafeEq("date", test.getDate()),
				DaoBase.nullSafeEq("path", test.getPath()));
	}

	@Override
	public HemeTest addTest(final HemeTest test) {
		try {
			Preconditions.checkNotNull(test, "test must not be null");
			return save(test, getHemeTestEqualityCriteria(test), true);
		} catch (Exception e) {
			throw new MprcException("Could not add " + test, e);
		}
	}

	@Override
	public void removeTest(final HemeTest test) {
		final Object o = getSession().createCriteria(HemeTest.class).add(getHemeTestEqualityCriteria(test)).uniqueResult();
		if (o != null) {
			getSession().delete(o);
		}
	}

	@Override
	public long countTests() {
		return rowCount(HemeTest.class);
	}

	@Override
	public void saveOrUpdate(HemeTest test) {
		getSession().saveOrUpdate(test);
	}

	@Override
	public HemeTest getTestForId(final int testId) {
		final Object result = getSession().get(HemeTest.class, testId);
		if (!(result instanceof HemeTest)) {
			throw new MprcException(MessageFormat.format("Could not find {0} object of id {1}", HemeTest.class.getSimpleName(), testId));
		}
		return (HemeTest) result;
	}

	@Override
	public String check(final Map<String, String> params) {
		// Nothing to do
		return null;
	}

	@Override
	public void initialize(final Map<String, String> params) {
		// Nothing to do
	}
}
