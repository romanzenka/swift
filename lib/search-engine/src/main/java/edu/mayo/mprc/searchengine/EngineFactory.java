package edu.mayo.mprc.searchengine;

import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.daemon.worker.WorkerFactory;

/**
 * A factory capable of creating a search engine.
 * Provides metadata about the engine to be created.
 *
 * @author Roman Zenka
 */
public interface EngineFactory<C extends ResourceConfig, W extends Worker> extends WorkerFactory<C, W> {
	/**
	 * @return Information about the search engine being created by this class.
	 */
	EngineMetadata getEngineMetadata();
}
