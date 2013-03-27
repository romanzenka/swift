package edu.mayo.mprc.searchengine;

import edu.mayo.mprc.daemon.WorkerFactory;

/**
 * A factory capable of creating a search engine.
 * Provides metadata about the engine to be created.
 *
 * @author Roman Zenka
 */
public interface EngineFactory extends WorkerFactory {
	/**
	 * @return Information about the search engine being created by this class.
	 */
	EngineMetadata getEngineMetadata();
}
