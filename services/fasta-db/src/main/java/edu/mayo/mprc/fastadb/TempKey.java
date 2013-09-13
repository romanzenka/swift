package edu.mayo.mprc.fastadb;

import java.io.Serializable;

/**
 * @author Roman Zenka
 */
public final class TempKey implements Serializable {
	private int job;
	private int dataOrder;

	public TempKey() {
	}

	public TempKey(int job, int dataOrder) {
		this.job = job;
		this.dataOrder = dataOrder;
	}

	public int getJob() {
		return job;
	}

	public void setJob(int job) {
		this.job = job;
	}

	public int getDataOrder() {
		return dataOrder;
	}

	public void setDataOrder(int dataOrder) {
		this.dataOrder = dataOrder;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof TempKey)) return false;

		TempKey tempKey = (TempKey) o;

		if (dataOrder != tempKey.dataOrder) return false;
		if (job != tempKey.job) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = job;
		result = 31 * result + dataOrder;
		return result;
	}
}
