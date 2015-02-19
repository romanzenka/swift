package edu.mayo.mprc.swift.params2;

import edu.mayo.mprc.config.Lifecycle;
import edu.mayo.mprc.config.RuntimeInitializer;
import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.database.Dao;
import edu.mayo.mprc.unimod.ModSet;
import edu.mayo.mprc.workspace.User;

import java.util.List;

public interface ParamsDao extends Dao, RuntimeInitializer, Lifecycle {
	/**
	 * @return List of all ion series currently configured (and not deprecated). The series should be treated as read-only.
	 */
	List<IonSeries> ionSeries();

	/**
	 * Add a new ion series. The series must not previously exist.
	 *
	 * @param ionSeries Ion series to be created.
	 * @param creation  Reason for the creation.
	 */
	void addIonSeries(IonSeries ionSeries, Change creation);

	/**
	 * Update or add new ion series.
	 *
	 * @param ionSeries New Ion series to be created.
	 * @param creation  Reason for the creation.
	 */
	IonSeries updateIonSeries(IonSeries ionSeries, Change creation);

	/**
	 * "Deletes" an ion series.
	 *
	 * @param ionSeries
	 * @param deletion
	 */
	void deleteIonSeries(IonSeries ionSeries, Change deletion);

	List<Instrument> instruments();

	Instrument getInstrumentByName(String name);

	Instrument addInstrument(Instrument instrument, Change change);

	Instrument updateInstrument(Instrument instrument, Change change);

	void deleteInstrument(Instrument instrument, Change change);

	List<Protease> proteases();

	Protease getProteaseByName(String name);

	void addProtease(Protease protease, Change change);

	Protease updateProtease(Protease protease, Change change);

	void deleteProtease(Protease protease, Change change);

	StarredProteins addStarredProteins(StarredProteins starredProteins);

	ExtractMsnSettings addExtractMsnSettings(ExtractMsnSettings extractMsnSettings);

	ScaffoldSettings addScaffoldSettings(ScaffoldSettings scaffoldSettings);

	/**
	 * Adds a search engine config. If the config already exists, the object gets updated.
	 * Returns a serialized version of the config.
	 *
	 * @param config Search engine config.
	 */
	SearchEngineConfig addSearchEngineConfig(SearchEngineConfig config);

	EnabledEngines addEnabledEngineSet(Iterable<SearchEngineConfig> searchEngineConfigs);

	EnabledEngines addEnabledEngines(EnabledEngines engines);

	/**
	 * Modification sets are not {@link edu.mayo.mprc.database.Evolvable} because they are never to be modified...
	 * only new ones get created.
	 */
	ModSet updateModSet(ModSet modSet);

	SearchEngineParameters addSearchEngineParameters(SearchEngineParameters parameters);

	/**
	 * @param key Id of the search engine parameter set.
	 * @return The search engine parameters from the database.
	 */
	SearchEngineParameters getSearchEngineParameters(int key);

	List<SavedSearchEngineParameters> savedSearchEngineParameters();

	/**
	 * Fast way of listing search engine parameters without the parameters themselves
	 */
	List<SavedSearchEngineParameters> savedSearchEngineParametersNoData();

	SavedSearchEngineParameters getSavedSearchEngineParameters(int key);

	SavedSearchEngineParameters findSavedSearchEngineParameters(String name);

	SavedSearchEngineParameters addSavedSearchEngineParameters(SavedSearchEngineParameters params, Change change);

	void deleteSavedSearchEngineParameters(SavedSearchEngineParameters params, Change change);

	/**
	 * Returns the best saved search engine parameters that matches the given search engine parameters.
	 * Since the same parameters can be saved under different names by different users, pick the best one based on the
	 * user name in case of ambiguity.
	 * <p/>
	 * If no saved parameter set matches, return null.
	 */
	SavedSearchEngineParameters findBestSavedSearchEngineParameters(SearchEngineParameters parameters, User user);

	/**
	 * Merge the detached parameter set back into the current session.
	 */
	SearchEngineParameters mergeParameterSet(SearchEngineParameters ps);
}
