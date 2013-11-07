package edu.mayo.mprc.swift.params2;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.Database;
import edu.mayo.mprc.unimod.ModSet;
import edu.mayo.mprc.workspace.User;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository("paramsDao")
public final class ParamsDaoHibernate extends DaoBase implements ParamsDao {
	private static final Logger LOGGER = Logger.getLogger(ParamsDaoHibernate.class);

	private static final String PARAMS_FOLDER = "edu/mayo/mprc/swift/params2/";

	public ParamsDaoHibernate() {
	}

	public ParamsDaoHibernate(final Database database) {
		super(database);
	}

	@Override
	public Collection<String> getHibernateMappings() {
		final Collection<String> list = new ArrayList<String>(Arrays.asList(
				"edu/mayo/mprc/dbcurator/model/Curation.hbm.xml",
				"edu/mayo/mprc/dbcurator/model/SourceDatabaseArchive.hbm.xml",
				"edu/mayo/mprc/dbcurator/model/curationsteps/CurationStep.hbm.xml",
				"edu/mayo/mprc/dbcurator/model/curationsteps/DataSource.hbm.xml",
				"edu/mayo/mprc/dbcurator/model/curationsteps/HeaderTransform.hbm.xml",
				"edu/mayo/mprc/unimod/Mod.hbm.xml",
				"edu/mayo/mprc/unimod/ModSet.hbm.xml",
				"edu/mayo/mprc/unimod/ModSpecificity.hbm.xml",
				"edu/mayo/mprc/workspace/User.hbm.xml",
				PARAMS_FOLDER + "IonSeries.hbm.xml",
				PARAMS_FOLDER + "Instrument.hbm.xml",
				PARAMS_FOLDER + "Protease.hbm.xml",
				PARAMS_FOLDER + "SearchEngineParameters.hbm.xml",
				PARAMS_FOLDER + "SavedSearchEngineParameters.hbm.xml",
				PARAMS_FOLDER + "ExtractMsnSettings.hbm.xml",
				PARAMS_FOLDER + "ScaffoldSettings.hbm.xml",
				PARAMS_FOLDER + "StarredProteins.hbm.xml"
		));
		list.addAll(super.getHibernateMappings());
		return list;
	}

	@Override
	public List<IonSeries> ionSeries() {
		return listAll(IonSeries.class);
	}

	/**
	 * @return Criteria that selects ion series equal to given one.
	 */
	private static Criterion getIonSeriesEqCriteria(final IonSeries ionSeries) {
		return Restrictions.eq("name", ionSeries.getName());
	}

	@Override
	public void addIonSeries(final IonSeries ionSeries, final Change creation) {
		try {
			save(ionSeries, creation, getIonSeriesEqCriteria(ionSeries), true/*Must be new*/);
		} catch (Exception t) {
			throw new MprcException("Cannot add new ion series '" + ionSeries.getName() + "'", t);
		}
	}

	@Override
	public IonSeries updateIonSeries(final IonSeries ionSeries, final Change creation) {
		try {
			return save(ionSeries, creation, getIonSeriesEqCriteria(ionSeries), false/*Can already exist*/);
		} catch (Exception t) {
			throw new MprcException("Cannot update ion series '" + ionSeries.getName() + "'", t);
		}
	}

	@Override
	public void deleteIonSeries(final IonSeries ionSeries, final Change deletion) {
		try {
			delete(ionSeries, deletion);
		} catch (Exception t) {
			throw new MprcException("Cannot delete ion series '" + ionSeries.getName() + "'", t);
		}
	}

	@Override
	public List<Instrument> instruments() {
		return listAll(Instrument.class);
	}

	@Override
	public Instrument getInstrumentByName(final String name) {
		try {
			return
					get(Instrument.class, Restrictions.eq("name", name));
		} catch (Exception t) {
			throw new MprcException("Cannot find instrument " + name, t);
		}
	}

	private static SimpleExpression getInstrumentEqCriteria(final Instrument instrument) {
		return Restrictions.eq("name", instrument.getName());
	}

	@Override
	public Instrument addInstrument(final Instrument instrument, final Change change) {
		try {
			final Instrument newInstrument = updateInstrumentIonSeries(instrument, change);
			save(newInstrument, change, getInstrumentEqCriteria(instrument), true/*Must be new*/);
			return newInstrument;
		} catch (Exception t) {
			throw new MprcException("Cannot add new instrument '" + instrument.getName() + "'", t);
		}
	}

	private Instrument updateInstrumentIonSeries(final Instrument instrument, final Change change) {
		final Set<IonSeries> newSeries = new HashSet<IonSeries>();
		for (final IonSeries series : instrument.getSeries()) {
			final IonSeries updateSeries = updateIonSeries(series, change);
			newSeries.add(updateSeries);
		}
		return new Instrument(instrument.getName(), newSeries, instrument.getMascotName());
	}

	@Override
	public Instrument updateInstrument(final Instrument instrument, final Change change) {
		try {
			final Instrument newInstrument = updateInstrumentIonSeries(instrument, change);
			save(newInstrument, change, getInstrumentEqCriteria(instrument), false/*Update existing*/);
			return newInstrument;
		} catch (Exception t) {
			throw new MprcException("Cannot update instrument '" + instrument.getName() + "'", t);
		}
	}

	@Override
	public void deleteInstrument(final Instrument instrument, final Change change) {
		try {
			delete(instrument, change);
		} catch (Exception t) {
			throw new MprcException("Cannot delete instrument '" + instrument.getName() + "'", t);
		}
	}

	@Override
	public List<Protease> proteases() {
		return listAll(Protease.class);
	}

	private static SimpleExpression getProteaseEqCriteria(final Protease protease) {
		return Restrictions.eq("name", protease.getName());
	}

	@Override
	public Protease getProteaseByName(final String name) {
		return get(Protease.class, Restrictions.eq("name", name));
	}

	@Override
	public void addProtease(final Protease protease, final Change change) {
		try {
			save(protease, change, getProteaseEqCriteria(protease), true/*Must be new*/);
		} catch (Exception t) {
			throw new MprcException("Cannot add new protease '" + protease.getName() + "'", t);
		}
	}

	@Override
	public Protease updateProtease(final Protease protease, final Change change) {
		try {
			return save(protease, change, getProteaseEqCriteria(protease), false/*Can already exist*/);
		} catch (Exception t) {
			throw new MprcException("Cannot update protease '" + protease.getName() + "'", t);
		}
	}

	@Override
	public void deleteProtease(final Protease protease, final Change change) {
		try {
			delete(protease, change);
		} catch (Exception t) {
			throw new MprcException("Cannot delete protease '" + protease.getName() + "'", t);
		}
	}

	private static Criterion getExtractMsnSettingsEqualityCriteria(final ExtractMsnSettings extractMsnSettings) {
		return Restrictions.conjunction()
				.add(nullSafeEq("commandLineSwitches", extractMsnSettings.getCommandLineSwitches()))
				.add(nullSafeEq("command", extractMsnSettings.getCommand()));
	}

	@Override
	public ExtractMsnSettings addExtractMsnSettings(final ExtractMsnSettings extractMsnSettings) {
		try {
			return save(extractMsnSettings, getExtractMsnSettingsEqualityCriteria(extractMsnSettings), false);
		} catch (Exception t) {
			throw new MprcException("Could not add extract msn settings", t);
		}
	}

	private static Criterion getStarredProteinsEqualityCriteria(final StarredProteins starredProteins) {
		return Restrictions.conjunction()
				.add(nullSafeEq("starred", starredProteins.getStarred()))
				.add(nullSafeEq("delimiter", starredProteins.getDelimiter()))
				.add(nullSafeEq("regularExpression", starredProteins.isRegularExpression()));
	}

	private static Criterion getScaffoldSettingsEqualityCriteria(final ScaffoldSettings scaffoldSettings) {
		return Restrictions.conjunction()
				.add(Restrictions.between("proteinProbability", scaffoldSettings.getProteinProbability() - ScaffoldSettings.PROBABILITY_PRECISION, scaffoldSettings.getProteinProbability() + ScaffoldSettings.PROBABILITY_PRECISION))
				.add(Restrictions.between("peptideProbability", scaffoldSettings.getPeptideProbability() - ScaffoldSettings.PROBABILITY_PRECISION, scaffoldSettings.getPeptideProbability() + ScaffoldSettings.PROBABILITY_PRECISION))
				.add(Restrictions.eq("minimumPeptideCount", scaffoldSettings.getMinimumPeptideCount()))
				.add(Restrictions.eq("minimumNonTrypticTerminii", scaffoldSettings.getMinimumNonTrypticTerminii()))
				.add(Restrictions.eq("saveOnlyIdentifiedSpectra", scaffoldSettings.isSaveOnlyIdentifiedSpectra()))
				.add(Restrictions.eq("saveNoSpectra", scaffoldSettings.isSaveNoSpectra()))
				.add(Restrictions.eq("connectToNCBI", scaffoldSettings.isConnectToNCBI()))
				.add(Restrictions.eq("annotateWithGOA", scaffoldSettings.isAnnotateWithGOA()))
				.add(Restrictions.eq("useFamilyProteinGrouping", scaffoldSettings.isUseFamilyProteinGrouping()))
				.add(Restrictions.eq("useIndependentSampleGrouping", scaffoldSettings.isUseIndependentSampleGrouping()))
				.add(associationEq("starredProteins", scaffoldSettings.getStarredProteins()));
	}

	@Override
	public ScaffoldSettings addScaffoldSettings(final ScaffoldSettings scaffoldSettings) {
		try {
			if (scaffoldSettings.getStarredProteins() != null) {
				scaffoldSettings.setStarredProteins(addStarredProteins(scaffoldSettings.getStarredProteins()));
			}
			return save(scaffoldSettings, getScaffoldSettingsEqualityCriteria(scaffoldSettings), false);
		} catch (Exception t) {
			throw new MprcException("Could not add extract msn settings", t);
		}
	}

	@Override
	public StarredProteins addStarredProteins(final StarredProteins starredProteins) {
		try {
			return save(starredProteins, getStarredProteinsEqualityCriteria(starredProteins), false);
		} catch (Exception t) {
			throw new MprcException("Could not add starred proteins", t);
		}
	}

	@Override
	public ModSet updateModSet(final ModSet modSet) {
		if (modSet.getId() != null) {
			// ModSet is unchangeable once saved
			return modSet;
		}
		return updateCollection(modSet, modSet.getModifications(), "modifications");
	}

	@Override
	public SearchEngineParameters addSearchEngineParameters(final SearchEngineParameters parameters) {
		final Session session = getSession();

		if (parameters.getDatabase() != null && parameters.getDatabase().getId() == null) {
			throw new MprcException("The database must be persisted before it is assigned to search engine parameters");
		}
		if (parameters.getProtease().getId() == null) {
			throw new MprcException("The enzyme must be persisted before it is assigned to search engine parameters");
		}
		if (parameters.getInstrument().getId() == null) {
			throw new MprcException("The instrument must be persisted before it is assigned to search engine parameters");
		}

		// Update modifications sets
		parameters.setFixedModifications(updateModSet(parameters.getFixedModifications()));
		parameters.setVariableModifications(updateModSet(parameters.getVariableModifications()));
		parameters.setExtractMsnSettings(addExtractMsnSettings(parameters.getExtractMsnSettings()));
		parameters.setScaffoldSettings(addScaffoldSettings(parameters.getScaffoldSettings()));

		final Criteria criteria = session.createCriteria(SearchEngineParameters.class);
		if (parameters.getDatabase() != null) {
			criteria.add(Restrictions.eq("database.id", parameters.getDatabase().getId()));
		} else {
			criteria.add(Restrictions.isNull("database.id"));
		}
		criteria.add(associationEq("protease", parameters.getProtease()))
				.add(Restrictions.eq("missedCleavages", parameters.getMissedCleavages()))
				.add(associationEq("fixedModifications", parameters.getFixedModifications()))
				.add(associationEq("variableModifications", parameters.getVariableModifications()))
				.add(Restrictions.eq("peptideTolerance", parameters.getPeptideTolerance()))
				.add(Restrictions.eq("fragmentTolerance", parameters.getFragmentTolerance()))
				.add(associationEq("instrument", parameters.getInstrument()))
				.add(associationEq("extractMsnSettings", parameters.getExtractMsnSettings()))
				.add(associationEq("scaffoldSettings", parameters.getScaffoldSettings()))
		;
		final List<SearchEngineParameters> parameterList = listAndCast(criteria);
		final SearchEngineParameters existing =
				parameterList.isEmpty() ? null : parameterList.get(0);

		if (existing != null) {
			if (parameters.equals(existing)) {
				// Item equals the saved object, bring forth the additional parameters that do not participate in equality.
				parameters.setId(existing.getId());
				return (SearchEngineParameters) session.merge(parameters);
			}
		}

		parameters.setId(null);
		session.save(parameters);
		return parameters;
	}

	@Override
	public SearchEngineParameters getSearchEngineParameters(final int key) {
		return (SearchEngineParameters) getSession().get(SearchEngineParameters.class, key);
	}

	@Override
	public List<SavedSearchEngineParameters> savedSearchEngineParameters() {
		try {
			return listAndCast(
					allCriteria(SavedSearchEngineParameters.class)
							.addOrder(Order.asc("name").ignoreCase()));

		} catch (Exception t) {
			throw new MprcException("Cannot list all saved search parameters", t);
		}
	}

	@Override
	public SavedSearchEngineParameters getSavedSearchEngineParameters(final int key) {
		return (SavedSearchEngineParameters) getSession().get(SavedSearchEngineParameters.class, key);
	}

	@Override
	public SavedSearchEngineParameters findSavedSearchEngineParameters(final String name) {
		return get(SavedSearchEngineParameters.class, Restrictions.eq("name", name));
	}

	@Override
	public SavedSearchEngineParameters addSavedSearchEngineParameters(final SavedSearchEngineParameters params, final Change change) {
		try {
			// Make sure our parameters are normalized
			params.setParameters(addSearchEngineParameters(params.getParameters()));
			return save(params, change, Restrictions.eq("name", params.getName()), false);
		} catch (Exception t) {
			throw new MprcException("Could not save search engine parameters ", t);
		}
	}

	@Override
	public void deleteSavedSearchEngineParameters(final SavedSearchEngineParameters params, final Change change) {
		try {
			delete(params, change);
		} catch (Exception t) {
			throw new MprcException("Could not delete saved parameters [" + params.getName() + "] by " + params.getUser().getFirstName() + " " + params.getUser().getLastName(), t);
		}
	}

	@Override
	public SavedSearchEngineParameters findBestSavedSearchEngineParameters(final SearchEngineParameters parameters, final User user) {
		try {
			final List<SavedSearchEngineParameters> list = listAndCast(
					allCriteria(SavedSearchEngineParameters.class)
							.add(Restrictions.eq("parameters", parameters)));
			if (list.isEmpty()) {
				return null;
			}
			for (final SavedSearchEngineParameters p : list) {
				if (user.equals(p.getUser())) {
					return p;
				}
			}
			return list.get(0);
		} catch (Exception t) {
			throw new MprcException("Could not find saved search engine parameters matching specified ones", t);
		}
	}

	@Override
	public SearchEngineParameters mergeParameterSet(final SearchEngineParameters ps) {
		return (SearchEngineParameters) getSession().merge(ps);
	}

	@Override
	public String check() {
		if (countAll(IonSeries.class) == 0) {
			return "No ion series defined";
		}
		if (countAll(Instrument.class) == 0) {
			return "No instruments defined";
		}
		if (countAll(Protease.class) == 0) {
			return "No proteases defined";
		}
		return null;
	}

	@Override
	public void install(Map<String, String> params) {
		installIonSeries();
		installInstruments();
		installProteases();
	}

	private void installIonSeries() {
		if (countAll(IonSeries.class) == 0) {
			final Change change = new Change("Installing initial ion series", new DateTime());
			LOGGER.info(change.getReason());
			final List<IonSeries> list = IonSeries.getInitial();
			for (final IonSeries series : list) {
				updateIonSeries(series, change);
			}
		}
	}

	private void installInstruments() {
		if (countAll(Instrument.class) == 0) {
			final Change change = new Change("Installing initial instruments", new DateTime());
			LOGGER.info(change.getReason());
			final List<Instrument> list = Instrument.getInitial();
			for (final Instrument instrument : list) {
				updateInstrument(instrument, change);
			}
		}
	}

	private void installProteases() {
		if (countAll(Protease.class) == 0) {
			final Change change = new Change("Installing initial proteases", new DateTime());
			LOGGER.info(change.getReason());
			final List<Protease> list = Protease.getInitial();
			for (final Protease protease : list) {
				updateProtease(protease, change);
			}
		}
	}
}
