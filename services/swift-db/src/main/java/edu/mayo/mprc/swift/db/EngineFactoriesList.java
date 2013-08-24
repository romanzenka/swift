package edu.mayo.mprc.swift.db;

import com.google.common.collect.Maps;
import edu.mayo.mprc.searchengine.EngineFactory;
import edu.mayo.mprc.searchengine.EngineMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/**
 * A list of all engines we can possibly configure and support.
 *
 * @author Roman Zenka
 */
@Component("engineFactoriesList")
public final class EngineFactoriesList {
	Map<String/*code*/, EngineMetadata> engineMetadata;
	Collection<EngineFactory> engineFactories;

	public EngineFactoriesList() {
	}

	/**
	 * @return List of all engine metadata that describe the kind of engines creatable by our factories.
	 */
	public Collection<EngineMetadata> getEngineMetadata() {
		return engineMetadata.values();
	}

	/**
	 * @return List of all available engine factories.
	 */
	public Collection<EngineFactory> getEngineFactories() {
		return engineFactories;
	}

	@Autowired
	public void setEngineFactories(Collection<EngineFactory> engineFactories) {
		this.engineFactories = engineFactories;
		engineMetadata = Maps.newHashMap();
		for (final EngineFactory factory : engineFactories) {
			final EngineMetadata metadata = factory.getEngineMetadata();
			engineMetadata.put(metadata.getCode(), metadata);
		}
	}

	public EngineMetadata getEngineMetadataForCode(final String code) {
		return engineMetadata.get(code);
	}
}
