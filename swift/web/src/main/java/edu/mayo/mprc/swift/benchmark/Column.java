package edu.mayo.mprc.swift.benchmark;

import java.util.ArrayList;

final class Column implements Comparable<Column> {

	private final String title;
	private final ArrayList<String> data = new ArrayList<String>(10);

	Column(final String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public void addData(final String data) {
		this.data.add(data);
	}

	public String getData(final int index) {
		return index < data.size() ? data.get(index) : "";
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}

		final Column column = (Column) obj;

		if (title != null ? !title.equals(column.getTitle()) : column.getTitle() != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return title != null ? title.hashCode() : 0;
	}

	@Override
	public int compareTo(final Column o) {
		return getTitle().compareTo(o.getTitle());
	}
}
