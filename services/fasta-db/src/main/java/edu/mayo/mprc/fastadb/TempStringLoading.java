package edu.mayo.mprc.fastadb;

import edu.mayo.mprc.database.bulk.TempKey;

/**
 * @author Roman Zenka
 */
public class TempStringLoading {
	private TempKey tempKey;
	private Integer newId;

	private String data;

	public TempStringLoading() {
	}

	public TempStringLoading(final TempKey tempKey, final String data) {
		this.tempKey = tempKey;
		this.data = data;
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

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}
