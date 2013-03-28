package edu.mayo.mprc.swift.params2.mapping;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.dbcurator.model.persistence.CurationDao;
import edu.mayo.mprc.swift.params2.Instrument;
import edu.mayo.mprc.swift.params2.IonSeries;
import edu.mayo.mprc.swift.params2.ParamsDao;
import edu.mayo.mprc.swift.params2.Protease;
import edu.mayo.mprc.unimod.ModSpecificity;
import edu.mayo.mprc.unimod.Unimod;
import edu.mayo.mprc.unimod.UnimodDao;

import java.util.*;

/**
 * Metadata information about search engine parameters. Contains list of allowed values, default values, etc.
 * The values are fetched on demand and cached indefinitely.
 * TODO: Merge this cache with the DAOs to ensure that it gets invalidated when the user changes something.
 */
public final class ParamsInfoImpl extends ParamsInfo {
	private CurationDao curationDao;
	private UnimodDao unimodDao;
	private ParamsDao paramsDao;

	private List<Protease> enzymes;

	private Unimod unimod;
	private Map<String, IonSeries> ions;

	private Instrument defInst;
	private List<Instrument> insts;
	private final Map<String, Instrument> instsHash = new HashMap<String, Instrument>();

	/**
	 * @param curationDao Access to the database with a list of supported curations (allows us to translate database names to files)
	 * @param unimodDao   Access to unimod.
	 */
	public ParamsInfoImpl(final CurationDao curationDao, final UnimodDao unimodDao, final ParamsDao paramsDao) {
		this.curationDao = curationDao;
		this.unimodDao = unimodDao;
		this.paramsDao = paramsDao;
	}

	@Override
	public List<Curation> getDatabaseAllowedValues() {
		final List<Curation> matchingCurations = curationDao.getAllCurations();
		final List<Curation> dbs = new ArrayList<Curation>();
		for (final Curation c : matchingCurations) {
			if (!c.hasBeenRun()) {
				continue;
			}
			dbs.add(c);
		}
		Collections.sort(dbs, new Comparator<Curation>() {
			public int compare(final Curation o1, final Curation o2) {
				final String name1 = o1.getShortName();
				final String name2 = o2.getShortName();
				return name1.compareToIgnoreCase(name2);
			}
		});
		return dbs;
	}

	private void initializeEnzymes() {
		if (enzymes == null) {
			this.enzymes = paramsDao.proteases();
		}
	}

	@Override
	public List<Protease> getEnzymeAllowedValues() {
		initializeEnzymes();
		return enzymes;
	}

	private void initializeUnimod() {
		if (unimod == null) {
			try {
				this.unimod = unimodDao.load();
			} catch (Exception t) {
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

	private void initializeInstruments() {
		if (insts == null) {
			this.insts = paramsDao.instruments();
			for (final Instrument instrument : insts) {
				if (instrument.getMascotName() != null && instrument.getMascotName().equals(Instrument.ORBITRAP.getMascotName())) {
					defInst = instrument;
					break;
				}
			}
			if (defInst == null) {
				defInst = insts.get(0);
			}
			for (final Instrument instrument : insts) {
				instsHash.put(instrument.getName(), instrument);
			}
		}
	}

	@Override
	public List<Instrument> getInstrumentAllowedValues() {
		initializeInstruments();
		return insts;
	}

	@Override
	public Map<String, Instrument> getInstruments() {
		initializeInstruments();
		return instsHash;
	}

	private void initializeIons() {
		if (ions == null) {
			final List<IonSeries> listIons = paramsDao.ionSeries();
			ions = new HashMap<String, IonSeries>();
			for (final IonSeries ionSeries : listIons) {
				ions.put(ionSeries.getName(), ionSeries);
			}
		}
	}

	@Override
	public Map<String, IonSeries> getIons() {
		initializeIons();
		return ions;
	}
}
