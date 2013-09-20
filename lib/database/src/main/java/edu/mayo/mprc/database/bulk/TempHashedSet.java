package edu.mayo.mprc.database.bulk;

import edu.mayo.mprc.database.DaoBase;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Roman Zenka
 */
public final class TempHashedSet {
	private TempKey tempKey;
	private Integer newId;
	private long hash;

	private Collection<TempHashedSetMember> members = new HashSet<TempHashedSetMember>();

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

	public void calculateHash() {
		setHash(DaoBase.calculateHash(members));
	}

	public void setHash(long hash) {
		this.hash = hash;
	}

	public Collection<TempHashedSetMember> getMembers() {
		return members;
	}

	public void setMembers(Collection<TempHashedSetMember> members) {
		this.members = members;
	}
}
