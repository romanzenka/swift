package edu.mayo.mprc.swift.params2;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.PersistableBase;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * User-editable information about a supported search engine.
 * Right now we do not store any additional information, the class is used only to store sets of engines to perform
 * searches with.
 */
public class SearchEngineConfig extends PersistableBase {

	/**
	 * Unique text identifier for the engine (e.g. <c>MASCOT</c>).
	 */
	private String code;
	/**
	 * Version of the engine.
	 */
	private String version;

	public SearchEngineConfig() {
	}

	public SearchEngineConfig(final String code, final String version) {
		setCode(code);
		setVersion(version);
	}

	public String getCode() {
		return code;
	}

	public void setCode(final String code) {
		if (code == null) {
			throw new MprcException("Search engine code cannot be null");
		}
		this.code = code;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(final String version) {
		if (version == null) {
			throw new MprcException("Search engine version cannot be null");
		}
		this.version = version;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SearchEngineConfig)) {
			return false;
		}

		final SearchEngineConfig that = (SearchEngineConfig) o;

		if (!code.equals(that.code)) {
			return false;
		}
		if (!version.equals(that.version)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = code.hashCode();
		result = 31 * result + version.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "SearchEngineConfig{" +
				"code='" + code + '\'' +
				", version='" + version + '\'' +
				'}';
	}

	@Override
	public Criterion getEqualityCriteria() {
		return Restrictions.and(
				DaoBase.nullSafeEq("code", getCode()),
				DaoBase.nullSafeEq("version", getVersion())
		);
	}
}
