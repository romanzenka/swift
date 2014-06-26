package edu.mayo.mprc.swift.search.task;

import edu.emory.mathcs.backport.java.util.Collections;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.db.SearchEngine;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.swift.params2.EnabledEngines;
import edu.mayo.mprc.swift.params2.SearchEngineConfig;

import java.util.Collection;

/**
 * Manages a set of search engines, given search definition it can produce
 * search engine with a particular version.
 *
 * @author Roman Zenka
 */
public final class SearchEngineList {
	private final Collection<SearchEngine> searchEngines;

	private SwiftSearchDefinition searchDefinition;

	public SearchEngineList(Collection<SearchEngine> searchEngines, SwiftSearchDefinition searchDefinition) {
		this.searchEngines = searchEngines;
		this.searchDefinition = searchDefinition;
	}

	public Collection<SearchEngine> getEngines() {
		return Collections.unmodifiableCollection(searchEngines);
	}

	public SearchEngine getSearchEngine(final String code) {
		String version = "";
		for (final SearchEngineConfig config : getEnabledEngines().getEngineConfigs()) {
			if (config.getCode().equals(code)) {
				version = config.getVersion();
				break;
			}
		}

		for (final SearchEngine engine : searchEngines) {
			if (engine.getCode().equalsIgnoreCase(code) && engine.getVersion().equalsIgnoreCase(version)) {
				return engine;
			}
		}

		// Special case - the version we want is not specified. Pick the newest.
		// This happens for legacy searches and also for the QuaMeter-enabled MM and IdpQonvert
		// If there is no such engine, return null instead of throwing an exception
		if ("".equals(version)) {
			SearchEngine bestEngine = null;
			String bestVersion = "";
			for (final SearchEngine engine : searchEngines) {
				if (engine.getCode().equalsIgnoreCase(code) && engine.getVersion().compareTo(bestVersion) > 0) {
					bestVersion = version;
					bestEngine = engine;
				}
			}
			return bestEngine;
		} else {
			throw new MprcException("The search engine [" + code + "] version [" + version + "] is no longer available. Please edit the search and try again");
		}
	}

	public SearchEngine getScaffoldEngine() {
		return getSearchEngine("SCAFFOLD");
	}

	public SearchEngine getIdpQonvertEngine() {
		return getSearchEngine("IDPQONVERT");
	}

	public SearchEngine getMyrimatchEngine() {
		return getSearchEngine("MYRIMATCH");
	}

	public SearchEngine getQuameterEngine() {
		return getSearchEngine("QUAMETER");
	}

	public SearchEngine getCometEngine() {
		return getSearchEngine("COMET");
	}

	/**
	 * Enabled engines are those that the first searched file enables (for now)
	 */
	private EnabledEngines getEnabledEngines() {
		return searchDefinition.getEnabledEngines();
	}
}
