package edu.mayo.mprc.swift.params2.mapping;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.swift.params2.Instrument;
import edu.mayo.mprc.swift.params2.IonSeries;
import edu.mayo.mprc.swift.params2.ParamName;
import edu.mayo.mprc.swift.params2.Protease;
import edu.mayo.mprc.unimod.ModSpecificity;
import edu.mayo.mprc.unimod.Unimod;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ParamsInfo {

	/**
	 * @return The list of allowed curations has to be extricated from Hibernate, as we will be using it later in the
	 * program when we are no longer in hibernate session.
	 */
	public abstract List<Curation> getDatabaseAllowedValues();

	public abstract List<Protease> getEnzymeAllowedValues();

	public abstract Set<ModSpecificity> getVariableModsAllowedValues(boolean includeHidden);

	public abstract Set<ModSpecificity> getFixedModsAllowedValues(boolean includeHidden);

	/**
	 * @return Unimod modification set.
	 */
	public abstract Unimod getUnimod();

	/**
	 * @return Fixed list of allowed instruments, based on the Mascot provided ion series
	 * that are available in other search engines.  This is pretty cheesy but I
	 * spent a bunch of time thinking about how to do this and the limited ion series
	 * support in other engines makes it less attractive to try to, for example,
	 * automatically import the mascot instruments...
	 */
	public abstract List<Instrument> getInstrumentAllowedValues();

	/**
	 * @return Map of all supported instruments.
	 */
	public abstract Map<String, Instrument> getInstruments();

	/**
	 * @return List of supported ions.
	 */
	public abstract Map<String, IonSeries> getIons();

	/**
	 * @param param Name of the parameter to get allowed values for.
	 * @return List of allowed values.
	 */
	public Iterable<?> getAllowedValues(final ParamName param) {
		switch (param) {
			case Database:
				return getDatabaseAllowedValues();
			case Enzyme:
				return getEnzymeAllowedValues();
			case FixedMods:
				return getFixedModsAllowedValues(true);
			case VariableMods:
				return getVariableModsAllowedValues(true);
			case Instrument:
				return getInstrumentAllowedValues();
			case FragmentTolerance:
			case MinTerminiCleavages:
			case MissedCleavages:
			case PeptideTolerance:
			case ExtractMsnSettings:
			case ScaffoldSettings:
			case EnabledEngines:
			case TitleSuffix:
				return null;
			default:
				throw new MprcException("Unsupported parameter " + param.toString());
		}

	}
}
