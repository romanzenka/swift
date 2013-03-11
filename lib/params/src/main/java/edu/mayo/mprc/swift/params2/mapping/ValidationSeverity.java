package edu.mayo.mprc.swift.params2.mapping;

public enum ValidationSeverity {
	NONE(0), INFO(1), WARNING(2), ERROR(3);

	private int rank;

	ValidationSeverity(final int rank) {
		this.setRank(rank);
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}
}
