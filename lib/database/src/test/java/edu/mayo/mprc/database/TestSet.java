package edu.mayo.mprc.database;

import edu.mayo.mprc.MprcException;
import org.hibernate.criterion.Criterion;

import java.util.HashSet;
import java.util.Set;

public class TestSet extends PersistableBase {

	private String setName;
	private Set<TestSetMember> members;

	public TestSet() {
		members = new HashSet<TestSetMember>();
	}

	public String getSetName() {
		return setName;
	}

	public void setSetName(final String setName) {
		this.setName = setName;
	}

	public Set<TestSetMember> getMembers() {
		return members;
	}

	public void setMembers(final Set<TestSetMember> members) {
		this.members = members;
	}

	public void add(final TestSetMember member) {
		members.add(member);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || !(obj instanceof TestSet)) {
			return false;
		}

		final TestSet testSet = (TestSet) obj;

		if (getMembers() != null ? !getMembers().equals(testSet.getMembers()) : testSet.getMembers() != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return getMembers() != null ? getMembers().hashCode() : 0;
	}

	@Override
	public Criterion getEqualityCriteria() {
		throw new MprcException("A set does not implement equality criteria");
	}
}
