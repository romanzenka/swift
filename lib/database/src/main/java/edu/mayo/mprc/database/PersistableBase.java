package edu.mayo.mprc.database;

import com.google.common.collect.Ordering;
import edu.mayo.mprc.MprcException;

/**
 * For objects that are persistable into the database.
 */
public abstract class PersistableBase implements EqualityCriteria {
	/**
	 * Id for hibernate.
	 */
	private Integer id;

	public Integer getId() {
		return id;
	}

	public void setId(final Integer id) {
		if (this.id != null && id == null) {
			throw new MprcException("The id was set to null - this should not happen ever");
		}
		this.id = id;
	}

	public static final Ordering<PersistableBase> BY_ID = new Ordering<PersistableBase>() {
		@Override
		public int compare(final PersistableBase o1, final PersistableBase o2) {
			if (o1.getId() == null) {
				return o2.getId() == null ? 0 : -1;
			}
			if (o2.getId() == null) {
				return o1.getId() == null ? 0 : 1;
			}
			return o1.getId().compareTo(o2.getId());
		}
	};
}
