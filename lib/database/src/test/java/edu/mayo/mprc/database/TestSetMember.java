package edu.mayo.mprc.database;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

public class TestSetMember extends PersistableBase {

	private String memberName;

	public TestSetMember() {
	}

	public TestSetMember(final String memberName) {
		this.memberName = memberName;
	}

	public String getMemberName() {
		return memberName;
	}

	public void setMemberName(final String memberName) {
		this.memberName = memberName;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !(o instanceof TestSetMember)) {
			return false;
		}

		final TestSetMember that = (TestSetMember) o;

		if (getMemberName() != null ? !getMemberName().equals(that.getMemberName()) : that.getMemberName() != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return getMemberName() != null ? getMemberName().hashCode() : 0;
	}

	@Override
	public Criterion getEqualityCriteria() {
		return Restrictions.conjunction()
				.add(DaoBase.nullSafeEq("memberName", getMemberName()));
	}
}
