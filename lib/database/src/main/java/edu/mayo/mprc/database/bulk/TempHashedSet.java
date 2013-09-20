package edu.mayo.mprc.database.bulk;

/**
 * @author Roman Zenka
 */
public final class TempHashedSet {
	private TempKey tempKey;
	private Integer newId;
	private long hash;

	public TempKey getTempKey() {
		return tempKey;
	}

	public void setTempKey(TempKey tempKey) {
		this.tempKey = tempKey;
	}

	public Integer getNewId() {
		return newId;
	}

	public void setNewId(Integer newId) {
		this.newId = newId;
	}

	public long getHash() {
		return hash;
	}

	public void setHash(long hash) {
		this.hash = hash;
	}
}
