package edu.mayo.mprc.searchdb.dao;

import com.google.common.base.Objects;

import java.io.Serializable;

/**
 * @author Roman Zenka
 */
public final class BiologicalSampleId implements Serializable {

	private static final long serialVersionUID = 7132897039247713138L;

	private String sampleName;

	/**
	 * Category of the sample. This is usually set to "none", but sometimes it can contain useful information.
	 */
	private String category;

	public BiologicalSampleId(final String sampleName, final String category) {
		this.category = category;
		this.sampleName = sampleName;
	}

	public String getCategory() {
		return category;
	}

	public String getSampleName() {
		return sampleName;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(sampleName, category);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final BiologicalSampleId other = (BiologicalSampleId) obj;
		return Objects.equal(this.sampleName, other.sampleName)
				&& Objects.equal(this.category, other.category);
	}
}
