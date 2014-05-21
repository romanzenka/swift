package edu.mayo.mprc.database;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;

/**
 * A date holder.
 *
 * @author Roman Zenka
 */
public class TestDate extends PersistableBase {
	private DateTime value1;
	private DateTime value2;

	public TestDate() {
	}

	public TestDate(final DateTime value1, final DateTime value2) {
		this.value1 = value1;
		this.value2 = value2;
	}

	public DateTime getValue1() {
		return value1;
	}

	public void setValue1(final DateTime value1) {
		this.value1 = value1;
	}

	public DateTime getValue2() {
		return value2;
	}

	public void setValue2(final DateTime value2) {
		this.value2 = value2;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !(o instanceof TestDate)) {
			return false;
		}

		final TestDate testDate = (TestDate) o;

		if (value1 != null ? !value1.equals(testDate.value1) : testDate.value1 != null) {
			return false;
		}
		if (value2 != null ? !value2.equals(testDate.value2) : testDate.value2 != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = value1 != null ? value1.hashCode() : 0;
		result = 31 * result + (value2 != null ? value2.hashCode() : 0);
		return result;
	}

	@Override
	public Criterion getEqualityCriteria() {
		return Restrictions.conjunction()
				.add(Restrictions.eq("value1", getValue1()))
				.add(Restrictions.eq("value2", getValue2()));
	}
}
