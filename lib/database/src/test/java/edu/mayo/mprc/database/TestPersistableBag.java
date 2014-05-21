package edu.mayo.mprc.database;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Test of persistable bags
 *
 * @author Roman Zenka
 */
public class TestPersistableBag {
	@Test
	public void emptyListsAreEqual() {
		final ObjectBag list1 = new ObjectBag();
		final ObjectBag list2 = new ObjectBag();
		Assert.assertEquals(list1, list2, "Empty lists must be equal");
	}

	@Test
	public void orderDoesNotMatter() {
		final TestPersistable t1 = new TestPersistable(1);
		final TestPersistable t2 = new TestPersistable(2);

		final ObjectBag list1 = new ObjectBag();
		list1.add(t1);
		list1.add(t2);

		final ObjectBag list2 = new ObjectBag();
		list2.add(t2);
		list2.add(t1);

		Assert.assertTrue(list1.equals(list2), "Order of elements does not matter");
	}

	@Test
	public void differentCollectionsAreOk() {
		final TestPersistable t1 = new TestPersistable(1);
		final TestPersistable t2 = new TestPersistable(2);

		final ObjectBag list1 = new ObjectBag();
		list1.add(t1);
		list1.add(t2);

		final ObjectBag list2 = new ObjectBag();
		final List<TestPersistable> myList = new ArrayList<TestPersistable>(2);
		myList.add(t2);
		myList.add(t1);
		list2.setList(myList);

		Assert.assertTrue(list1.equals(list2), "We can use different implementation of a list and still get equality");
	}


	private class TestPersistable extends PersistableBase {
		private int value;

		private TestPersistable(final int value) {
			this.value = value;
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || !(o instanceof TestPersistable)) {
				return false;
			}

			final TestPersistable that = (TestPersistable) o;

			if (value != that.value) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			return value;
		}

		@Override
		public Criterion getEqualityCriteria() {
			return Restrictions.disjunction();
		}
	}

	private class ObjectBag extends PersistableBagBase<TestPersistable> {
		private ObjectBag() {
		}

		private ObjectBag(final int initialCapacity) {
			super(initialCapacity);
		}

		private ObjectBag(final Collection<TestPersistable> items) {
			super(items);
		}
	}

}
