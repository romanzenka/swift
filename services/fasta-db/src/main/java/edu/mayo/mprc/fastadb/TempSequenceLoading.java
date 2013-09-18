package edu.mayo.mprc.fastadb;

/**
 * @author Roman Zenka
 */
public class TempSequenceLoading {
	private TempKey tempKey;

	private Integer newId;
	private Double mass;
	private String sequence;

	public TempSequenceLoading() {
	}

	public TempSequenceLoading(final TempKey tempKey, final Sequence sequence) {
		this.tempKey = tempKey;
		this.mass = sequence.getMass();
		this.sequence = sequence.getSequence();
	}

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

	public Double getMass() {
		return mass;
	}

	public void setMass(Double mass) {
		this.mass = mass;
	}

	public String getSequence() {
		return sequence;
	}

	public void setSequence(String sequence) {
		this.sequence = sequence;
	}
}
