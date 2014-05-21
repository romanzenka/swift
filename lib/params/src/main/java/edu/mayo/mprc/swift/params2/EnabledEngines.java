package edu.mayo.mprc.swift.params2;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.PersistableBase;
import org.hibernate.criterion.Criterion;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A set of enabled search engineConfigs. Once saved becomes immutable.
 */
public class EnabledEngines extends PersistableBase {
	private Set<SearchEngineConfig> engineConfigs = new HashSet<SearchEngineConfig>();

	public EnabledEngines() {
	}

	public EnabledEngines(final Set<SearchEngineConfig> engineConfigs) {
		setEngineConfigs(engineConfigs);
	}

	public Set<SearchEngineConfig> getEngineConfigs() {
		return engineConfigs;
	}

	void setEngineConfigs(final Set<SearchEngineConfig> engineConfigs) {
		this.engineConfigs = engineConfigs;
	}

	public int size() {
		return engineConfigs.size();
	}

	public void add(final SearchEngineConfig engineConfig) {
		if (getId() != null) {
			throw new MprcException("Enabled engine set is immutable once saved to the database");
		}
		engineConfigs.add(engineConfig);
	}

	public boolean isEnabled(final SearchEngineConfig config) {
		return engineConfigs.contains(config);
	}

	public boolean isEnabled(final String code) {
		for (final SearchEngineConfig config : engineConfigs) {
			if (config.getCode().equals(code)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param code Engine code.
	 * @return Version of the engine that is enabled, <c>null</c> if such engine does not exist.
	 */
	public String enabledVersion(final String code) {
		for (final SearchEngineConfig config : engineConfigs) {
			if (config.getCode().equals(code)) {
				return config.getVersion();
			}
		}
		return null;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || !(o instanceof EnabledEngines)) {
			return false;
		}

		final EnabledEngines that = (EnabledEngines) o;
		return Objects.equal(that.getEngineConfigs(), getEngineConfigs());
	}

	@Override
	public int hashCode() {
		return getEngineConfigs() != null ? getEngineConfigs().hashCode() : 0;
	}

	/*
	 * @param code Search engine code.
	 * @return The version of the search engine that provides the requested service, null if none found, throw an exception
	 * if more than one version of an engine provides the same service.
	 */
	public String getEngineVersion(final String code) {
		List<String> versions = Lists.newArrayList();
		for (final SearchEngineConfig config : engineConfigs) {
			if (config.getCode().equals(code)) {
				versions.add(config.getVersion());
			}
		}
		if (versions.size() == 1) {
			return versions.get(0);
		}
		if (versions.isEmpty()) {
			return null;
		}
		Collections.sort(versions);
		throw new MprcException("The search engine " + code + " has multiple matching versions: " + Joiner.on(", ").join(versions));
	}

	@Override
	public Criterion getEqualityCriteria() {
		throw new MprcException("EnabledEngines does not support equality testing");
	}

	public EnabledEngines copy() {
		return new EnabledEngines(getEngineConfigs());
	}
}
