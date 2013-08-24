package edu.mayo.mprc.swift.params2.mapping;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.swift.params2.Instrument;
import edu.mayo.mprc.swift.params2.IonSeries;
import edu.mayo.mprc.swift.params2.ParamName;
import edu.mayo.mprc.swift.params2.Protease;
import edu.mayo.mprc.unimod.MockUnimodDao;
import edu.mayo.mprc.unimod.ModSpecificity;
import edu.mayo.mprc.unimod.Unimod;

import java.util.*;

/**
 * Static information about available and allowed parameters. Used for testing to prevent dependency on the database.
 */
public class MockParamsInfo extends ParamsInfo {
	private Unimod unimod;

	@Override
	public List<Curation> getDatabaseAllowedValues() {
		final List<Curation> list = new ArrayList<Curation>();

		addCuration(list, "Current_SP");
		addCuration(list, "Current_SP_Human");
		addCuration(list, "Mprc Test");
		addCuration(list, "NamMar09");
		addCuration(list, "SPhu011910_intstd");
		addCuration(list, "SPhuisrv2008");
		addCuration(list, "SPrabit081117A");
		addCuration(list, "ShortTest");
		addCuration(list, "Sp081031hurv");
		addCuration(list, "Sprorev+IgMv");
		addCuration(list, "Sprot081031");
		addCuration(list, "SprotHum");
		addCuration(list, "SprotHumRev");
		addCuration(list, "SprotHumRev2");
		addCuration(list, "SprotHumRev3");
		addCuration(list, "SprotRev");
		addCuration(list, "Sprot_humx_20070724");
		addCuration(list, "sprot_human");
		addCuration(list, "yeast17");
		addCuration(list, "yeast_15");
		addCuration(list, "PlagAltSp_20070529");
		addCuration(list, "yeast_five");
		addCuration(list, "SPmous081031");
		addCuration(list, "SPhumorarab2");
		addCuration(list, "AltDb100708");
		addCuration(list, "sprot_rev3");
		addCuration(list, "IPI_Human");
		addCuration(list, "18mix092106");
		addCuration(list, "SPrabit");
		addCuration(list, "humbovvac_20070117");
		addCuration(list, "SprotHumRand");
		addCuration(list, "SPmorv090731");
		addCuration(list, "SP090817hbiv");
		addCuration(list, "SP090817hbi2");
		addCuration(list, "SPmammal0907");
		addCuration(list, "SP_20090817");
		addCuration(list, "SP090817hurv");
		addCuration(list, "UP_mouse");
		addCuration(list, "SP_090817hur");
		addCuration(list, "AltDb20100224");
		addCuration(list, "SPmamistd");
		addCuration(list, "NCBInr100720");
		addCuration(list, "SP_090817hrB");
		addCuration(list, "SPhurv090731");
		addCuration(list, "NCBInr042409");

		return list;
	}

	private void addCuration(final List<Curation> list, final String shortName) {
		final Curation curation = new Curation();
		curation.setShortName(shortName);
		curation.setTitle("Test curation");
		list.add(curation);
	}

	@Override
	public List<Protease> getEnzymeAllowedValues() {
		return Protease.getInitial();
	}

	private void initializeUnimod() {
		if (unimod == null) {
			final MockUnimodDao dao = new MockUnimodDao();
			try {
				dao.begin();
				unimod = dao.load();
				dao.commit();
			} catch (Exception t) {
				dao.rollback();
				throw new MprcException("Could not load unimod data from the database", t);
			}
		}
	}

	@Override
	public Set<ModSpecificity> getVariableModsAllowedValues(final boolean includeHidden) {
		initializeUnimod();
		return unimod.getAllSpecificities(includeHidden);
	}

	@Override
	public Set<ModSpecificity> getFixedModsAllowedValues(final boolean includeHidden) {
		initializeUnimod();
		return unimod.getAllSpecificities(includeHidden);
	}

	@Override
	public Unimod getUnimod() {
		initializeUnimod();
		return unimod;
	}

	@Override
	public List<Instrument> getInstrumentAllowedValues() {
		return Instrument.getInitial();
	}

	@Override
	public Map<String, Instrument> getInstruments() {
		final Map<String, Instrument> result = new TreeMap<String, Instrument>();
		for (final Instrument instrument : getInstrumentAllowedValues()) {
			result.put(instrument.getName(), instrument);
		}
		return result;
	}

	@Override
	public Map<String, IonSeries> getIons() {
		final Map<String, IonSeries> result = new TreeMap<String, IonSeries>();
		for (final IonSeries ionSeries : IonSeries.getInitial()) {
			result.put(ionSeries.getName(), ionSeries);
		}
		return result;
	}

	@Override
	public Iterable getAllowedValues(final ParamName param) {
		return null; //TODO: implement me
	}
}