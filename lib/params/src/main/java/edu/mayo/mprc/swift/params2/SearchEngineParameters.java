package edu.mayo.mprc.swift.params2;

import com.google.common.base.Objects;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.PersistableBase;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.unimod.ModSet;

/**
 * Describes a set of parameters for running a database search.
 * The search is defined as several of these parameters applied to input data and particular workflow setup.
 * All the evolvable parameters have to be persisted prior to creation of this class (e.g. instrument, protease, etc.)
 * Otherwise it would be impossible to save the class to database.
 */
public class SearchEngineParameters extends PersistableBase {

	/**
	 * The list of proteins to be considered for the search.
	 */
	private Curation database;

	/**
	 * Protease used to digest the proteins to peptides.
	 */
	private Protease protease;

	/**
	 * How many cleavages have to be due to the protease?
	 * <p/>
	 * <table>
	 * <tr><th>2</th><td>a proper cleavage on both ends of the peptide (default)</td></tr>
	 * <tr><th>1</th><td>semitryptic cleavage</td></tr>
	 * <tr><th>0</th><td>non-specific cleavage (protease no longer matters)</td></tr>
	 * </table>
	 * <p/>
	 * This is similar to numTrypticTerminii, however, the protease in question does not have
	 * to be Trypsin.
	 */
	private int minTerminiCleavages;

	/**
	 * Maximum amount of missed cleavages to be considered when searching.
	 */
	private int missedCleavages;

	/**
	 * Set of fixed modifications (must occur for all peptides)
	 */
	private ModSet fixedModifications;

	/**
	 * Set of variable modifications (can, but do not have to occur)
	 */
	private ModSet variableModifications;

	/**
	 * How far can be the theoretical peptide mass from the mass reported by the instrument.
	 * Typically 10 ppm for an Orbitrap.
	 */
	private Tolerance peptideTolerance;

	/**
	 * How far can be a fragment ion from its theoretical position.
	 */
	private Tolerance fragmentTolerance;

	/**
	 * What instrument was used to produce the spectrum ions (determines which ion series are considered)
	 */
	private Instrument instrument;

	/**
	 * The results of the search are influenced by extract_msn settings, that is why these settings
	 * are saved alongside the rest.
	 */
	private ExtractMsnSettings extractMsnSettings;

	/**
	 * The results of the search are influenced by Scaffold settings (thresholds).
	 */
	private ScaffoldSettings scaffoldSettings;

	public SearchEngineParameters() {
	}

	public SearchEngineParameters(final Curation database, final Protease protease, final Integer minTerminiCleavages, final int missedCleavages, final ModSet fixed, final ModSet variable, final Tolerance peptideTolerance, final Tolerance fragmentTolerance, final Instrument instrument, final ExtractMsnSettings extractMsnSettings, final ScaffoldSettings scaffoldSettings) {
		this.database = database;
		this.protease = protease;
		this.minTerminiCleavages = minTerminiCleavages;
		this.missedCleavages = missedCleavages;
		fixedModifications = fixed;
		variableModifications = variable;
		this.peptideTolerance = peptideTolerance;
		this.fragmentTolerance = fragmentTolerance;
		this.instrument = instrument;
		this.extractMsnSettings = extractMsnSettings;
		this.scaffoldSettings = scaffoldSettings;
	}

	public Curation getDatabase() {
		return database;
	}

	public void setDatabase(final Curation database) {
		if (checkImmutability(getDatabase(), database)) {
			return;
		}
		this.database = database;
	}

	private boolean checkImmutability(final Object oldValue, final Object newValue) {
		if (getId() != null) {
			if (oldValue == null && newValue == null || (oldValue != null && oldValue.equals(newValue))) {
				return true;
			}

			throw new MprcException("Search engine parameters are immutable once saved");
		}
		return false;
	}

	public Protease getProtease() {
		return protease;
	}

	public void setProtease(final Protease protease) {
		if (checkImmutability(getProtease(), protease)) {
			return;
		}

		this.protease = protease;
	}

	public int getMinTerminiCleavages() {
		return minTerminiCleavages;
	}

	public void setMinTerminiCleavages(final int minTerminiCleavages) {
		if (checkImmutability(getMinTerminiCleavages(), minTerminiCleavages)) {
			return;
		}
		this.minTerminiCleavages = minTerminiCleavages;
	}

	public int getMissedCleavages() {
		return missedCleavages;
	}

	public void setMissedCleavages(final int missedCleavages) {
		if (checkImmutability(getMissedCleavages(), missedCleavages)) {
			return;
		}
		this.missedCleavages = missedCleavages;
	}

	public ModSet getFixedModifications() {
		return fixedModifications;
	}

	public void setFixedModifications(final ModSet fixedModifications) {
		if (checkImmutability(getFixedModifications(), fixedModifications)) {
			return;
		}
		this.fixedModifications = fixedModifications;
	}

	public ModSet getVariableModifications() {
		return variableModifications;
	}

	public void setVariableModifications(final ModSet variableModifications) {
		if (checkImmutability(getVariableModifications(), variableModifications)) {
			return;
		}
		this.variableModifications = variableModifications;
	}

	public Tolerance getPeptideTolerance() {
		return peptideTolerance;
	}

	public void setPeptideTolerance(final Tolerance peptideTolerance) {
		if (checkImmutability(getPeptideTolerance(), peptideTolerance)) {
			return;
		}
		this.peptideTolerance = peptideTolerance;
	}

	public Tolerance getFragmentTolerance() {
		return fragmentTolerance;
	}

	public void setFragmentTolerance(final Tolerance fragmentTolerance) {
		if (checkImmutability(getFragmentTolerance(), fragmentTolerance)) {
			return;
		}
		this.fragmentTolerance = fragmentTolerance;
	}

	public Instrument getInstrument() {
		return instrument;
	}

	public void setInstrument(final Instrument instrument) {
		if (checkImmutability(getInstrument(), instrument)) {
			return;
		}
		this.instrument = instrument;
	}

	public ExtractMsnSettings getExtractMsnSettings() {
		return extractMsnSettings;
	}

	public void setExtractMsnSettings(final ExtractMsnSettings extractMsnSettings) {
		this.extractMsnSettings = extractMsnSettings;
	}

	public ScaffoldSettings getScaffoldSettings() {
		return scaffoldSettings;
	}

	public void setScaffoldSettings(final ScaffoldSettings scaffoldSettings) {
		this.scaffoldSettings = scaffoldSettings;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SearchEngineParameters)) {
			return false;
		}

		final SearchEngineParameters other = (SearchEngineParameters) obj;

		if (getId() != null && other.getId() != null) {
			return getId().equals(other.getId());
		}

		if (!Objects.equal(getMinTerminiCleavages(), other.getMinTerminiCleavages())) {
			return false;
		}
		if (!Objects.equal(getMissedCleavages(), other.getMissedCleavages())) {
			return false;
		}
		if (!Objects.equal(getDatabase(), other.getDatabase())) {
			return false;
		}
		if (!Objects.equal(getFixedModifications(), other.getFixedModifications())) {
			return false;
		}
		if (!Objects.equal(getFragmentTolerance(), other.getFragmentTolerance())) {
			return false;
		}
		if (!Objects.equal(getInstrument(), other.getInstrument())) {
			return false;
		}
		if (!Objects.equal(getPeptideTolerance(), other.getPeptideTolerance())) {
			return false;
		}
		if (!Objects.equal(getProtease(), other.getProtease())) {
			return false;
		}
		if (!Objects.equal(getVariableModifications(), other.getVariableModifications())) {
			return false;
		}
		if (!Objects.equal(getExtractMsnSettings(), other.getExtractMsnSettings())) {
			return false;
		}
		if (!Objects.equal(getScaffoldSettings(), other.getScaffoldSettings())) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = getDatabase().hashCode();
		result = 31 * result + getProtease().hashCode();
		result = 31 * result + getMinTerminiCleavages();
		result = 31 * result + getMissedCleavages();
		result = 31 * result + getFixedModifications().hashCode();
		result = 31 * result + getVariableModifications().hashCode();
		result = 31 * result + getPeptideTolerance().hashCode();
		result = 31 * result + getFragmentTolerance().hashCode();
		result = 31 * result + getInstrument().hashCode();
		result = 31 * result + getExtractMsnSettings().hashCode();
		result = 31 * result + getScaffoldSettings().hashCode();
		return result;
	}

	/**
	 * @return Deep copy, independent on hibernate to be stored in the session cache.
	 */
	public SearchEngineParameters copy() {
		final Curation database1 = getDatabase().copyFull();
		// Retain the database ID
		database1.setId(getDatabase().getId());
		return new SearchEngineParameters(
				database1,
				getProtease().copy(),
				getMinTerminiCleavages(),
				getMissedCleavages(),
				getFixedModifications().copy(),
				getVariableModifications().copy(),
				getPeptideTolerance().copy(),
				getFragmentTolerance().copy(),
				getInstrument().copy(),
				getExtractMsnSettings().copy(),
				getScaffoldSettings().copy());
	}

	public void setValue(final ParamName name, final Object o) {
		switch (name) {
			case PeptideTolerance:
				setPeptideTolerance((Tolerance) o);
				break;
			case FragmentTolerance:
				setFragmentTolerance((Tolerance) o);
				break;
			case MinTerminiCleavages:
				setMinTerminiCleavages((Integer) o);
				break;
			case MissedCleavages:
				setMissedCleavages((Integer) o);
				break;
			case Database:
				setDatabase((Curation) o);
				break;
			case Enzyme:
				setProtease((Protease) o);
				break;
			case VariableMods:
				setVariableModifications((ModSet) o);
				break;
			case FixedMods:
				setFixedModifications((ModSet) o);
				break;
			case Instrument:
				setInstrument((Instrument) o);
				break;
			case ExtractMsnSettings:
				setExtractMsnSettings((ExtractMsnSettings) o);
				break;
			case ScaffoldSettings:
				setScaffoldSettings((ScaffoldSettings) o);
				break;
			default:
				break;
		}
	}

	public Object getValue(final ParamName paramName) {
		switch (paramName) {
			case PeptideTolerance:
				return getPeptideTolerance();
			case FragmentTolerance:
				return getFragmentTolerance();
			case MinTerminiCleavages:
				return getMinTerminiCleavages();
			case MissedCleavages:
				return getMissedCleavages();
			case Database:
				return getDatabase();
			case Enzyme:
				return getProtease();
			case VariableMods:
				return getVariableModifications();
			case FixedMods:
				return getFixedModifications();
			case Instrument:
				return getInstrument();
			case ExtractMsnSettings:
				return getExtractMsnSettings();
			case ScaffoldSettings:
				return getScaffoldSettings();
			default:
				throw new MprcException("Unknown parameter name " + paramName.getName());
		}
	}
}
