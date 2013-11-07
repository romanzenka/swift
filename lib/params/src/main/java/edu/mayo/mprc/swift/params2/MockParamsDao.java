package edu.mayo.mprc.swift.params2;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.unimod.ModSet;
import edu.mayo.mprc.workspace.User;

import java.util.List;
import java.util.Map;

/**
 * Static version of ParamsDao - does not enable any modifications, only serves default test values.
 */
public final class MockParamsDao implements ParamsDao {


	@Override
	public List<IonSeries> ionSeries() {
		return IonSeries.getInitial();
	}

	private void notModifiable() {
		throw new MprcException("MockParamsDao is not modifiable, method not implemented");
	}

	@Override
	public void addIonSeries(final IonSeries ionSeries, final Change creation) {
		notModifiable();
	}

	@Override
	public IonSeries updateIonSeries(final IonSeries ionSeries, final Change creation) {
		notModifiable();
		return null;
	}

	@Override
	public void deleteIonSeries(final IonSeries ionSeries, final Change deletion) {
		notModifiable();
	}

	@Override
	public List<Instrument> instruments() {
		return Instrument.getInitial();
	}

	@Override
	public Instrument getInstrumentByName(final String name) {
		for (final Instrument instrument : Instrument.getInitial()) {
			if (instrument.getName().equals(name)) {
				return instrument;
			}
		}
		return null;
	}

	@Override
	public Instrument addInstrument(final Instrument instrument, final Change change) {
		notModifiable();
		return null;
	}

	@Override
	public Instrument updateInstrument(final Instrument instrument, final Change change) {
		notModifiable();
		return null;
	}

	@Override
	public void deleteInstrument(final Instrument instrument, final Change change) {
		notModifiable();
	}

	@Override
	public List<Protease> proteases() {
		return Protease.getInitial();
	}

	@Override
	public Protease getProteaseByName(final String name) {
		for (final Protease protease : Protease.getInitial()) {
			if (protease.getName().equals(name)) {
				return protease;
			}
		}
		return null;
	}

	@Override
	public void addProtease(final Protease protease, final Change change) {
		notModifiable();
	}

	@Override
	public Protease updateProtease(final Protease protease, final Change change) {
		notModifiable();
		return null;
	}

	@Override
	public void deleteProtease(final Protease protease, final Change change) {
		notModifiable();
	}

	@Override
	public StarredProteins addStarredProteins(final StarredProteins starredProteins) {
		notModifiable();
		return null;
	}

	@Override
	public ExtractMsnSettings addExtractMsnSettings(final ExtractMsnSettings extractMsnSettings) {
		notModifiable();
		return null;
	}

	@Override
	public ScaffoldSettings addScaffoldSettings(final ScaffoldSettings scaffoldSettings) {
		notModifiable();
		return null;
	}

	@Override
	public ModSet updateModSet(final ModSet modSet) {
		notModifiable();
		return null;
	}

	@Override
	public SearchEngineParameters addSearchEngineParameters(final SearchEngineParameters parameters) {
		notModifiable();
		return null;
	}

	@Override
	public SearchEngineParameters getSearchEngineParameters(final int key) {
		return null;
	}

	@Override
	public List<SavedSearchEngineParameters> savedSearchEngineParameters() {
		return null;
	}

	@Override
	public SavedSearchEngineParameters getSavedSearchEngineParameters(final int key) {
		return null;
	}

	@Override
	public SavedSearchEngineParameters findSavedSearchEngineParameters(final String name) {
		return null;
	}

	@Override
	public SavedSearchEngineParameters addSavedSearchEngineParameters(final SavedSearchEngineParameters params, final Change change) {
		notModifiable();
		return null;
	}

	@Override
	public void deleteSavedSearchEngineParameters(final SavedSearchEngineParameters params, final Change change) {
		notModifiable();
	}

	@Override
	public SavedSearchEngineParameters findBestSavedSearchEngineParameters(final SearchEngineParameters parameters, final User user) {
		return null;
	}

	@Override
	public SearchEngineParameters mergeParameterSet(final SearchEngineParameters ps) {
		return null;
	}

	@Override
	public void begin() {
	}

	@Override
	public void commit() {
	}

	@Override
	public void rollback() {
	}

	@Override
	public String check() {
		return null;
	}

	@Override
	public void install(Map<String, String> params) {
	}
}
