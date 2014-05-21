package edu.mayo.mprc.dbcurator.model;

import edu.mayo.mprc.database.PersistableBase;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

public class FastaSource extends PersistableBase {
	private String name;
	private String url;

	/**
	 * the transform that can be automatically applied to sequences from this data source
	 */
	private HeaderTransform transform;

	/**
	 * set to true if you want this to be displayed in drop downs
	 */
	private Boolean common = false;

	public FastaSource() {
	}

	public FastaSource(final String name, final String url) {
		this.name = name;
		this.url = url;
	}

	public String getName() {
		return name;
	}

	public FastaSource setName(final String name) {
		this.name = name;
		return this;
	}

	public String getUrl() {
		return url;
	}

	public FastaSource setUrl(final String url) {
		this.url = url;
		return this;
	}

	public HeaderTransform getTransform() {
		return transform;
	}

	public FastaSource setTransform(final HeaderTransform transform) {
		this.transform = transform;
		return this;
	}

	public Boolean getCommon() {
		return common;
	}

	public FastaSource setCommon(final Boolean common) {
		this.common = common;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof FastaSource)) {
			return false;
		}

		final FastaSource that = (FastaSource) o;

		if (getCommon() != null ? !getCommon().equals(that.getCommon()) : that.getCommon() != null) {
			return false;
		}
		if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) {
			return false;
		}
		if (getTransform() != null ? !getTransform().equals(that.getTransform()) : that.getTransform() != null) {
			return false;
		}
		if (getUrl() != null ? !getUrl().equals(that.getUrl()) : that.getUrl() != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = getName() != null ? getName().hashCode() : 0;
		result = 31 * result + (getUrl() != null ? getUrl().hashCode() : 0);
		result = 31 * result + (getTransform() != null ? getTransform().hashCode() : 0);
		result = 31 * result + (getCommon() != null ? getCommon().hashCode() : 0);
		return result;
	}

	@Override
	public Criterion getEqualityCriteria() {
		return Restrictions.eq("name", getName());
	}
}
