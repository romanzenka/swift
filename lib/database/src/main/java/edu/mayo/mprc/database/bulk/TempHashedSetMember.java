package edu.mayo.mprc.database.bulk;

import edu.mayo.mprc.database.PersistableBase;

import java.io.Serializable;

/**
 * Stores reference to bulk loading job and order within the job (identifies
 * {@link TempHashedSet} being bulk loaded. The id is used for storing the id
 * of the member of {@link TempHashedSet}.
 *
 * @author Roman Zenka
 */
public final class TempHashedSetMember extends PersistableBase implements Serializable {
	private static final long serialVersionUID = 4456695838999858917L;

	private int job;
	private int dataOrder;

	public TempHashedSetMember() {
	}

	public TempHashedSetMember(final TempKey key, final int id) {
		setId(id);
		setJob(key.getJob());
		setDataOrder(key.getDataOrder());
	}

	public int getJob() {
		return job;
	}

	public void setJob(final int job) {
		this.job = job;
	}

	public int getDataOrder() {
		return dataOrder;
	}

	public void setDataOrder(final int dataOrder) {
		this.dataOrder = dataOrder;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof TempHashedSetMember)) return false;

		TempHashedSetMember that = (TempHashedSetMember) o;

		if (dataOrder != that.dataOrder) return false;
		if (job != that.job) return false;
		if (!getId().equals(that.getId())) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = job;
		result = 31 * result + dataOrder;
		result = 31 * result + getId();
		return result;
	}
}
