package edu.mayo.mprc.unimod;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import edu.mayo.mprc.database.PersistableBase;
import edu.mayo.mprc.utilities.ComparisonChain;

import java.util.*;

/**
 * Specifies where in a peptide a {@link Mod} is permitted (eg at an amino acid, on the N-, or C-terminus, etc).
 * <p/>
 * Once created, specificity is immutable (the only one to call the setters is hibernate).
 *
 * @author Roman Zenka
 */
public class ModSpecificity extends PersistableBase implements Comparable<ModSpecificity> {
	/**
	 * the modification that this specifies
	 */
	private Mod modification;
	/**
	 * hidden is a flag used by unimod to differentiate common (false) from uncommon(true) specificities
	 */
	private Boolean hidden;
	/**
	 * the site is the AA where the modification can occur or '*' if it can be any site (for N or C terminus)
	 */
	private Character site;
	/**
	 * At which terminus can this specificity occur. Can be Cterm, Nterm or Anywhere. Anywhere denotes modifications that can occur anywhere, not only
	 * at a terminus. See comment of this class.
	 */
	private Terminus term;
	/**
	 * If the 'term' field is set, <code>true</code> signalizes this modification can occur only at the protein C or N term.
	 * <code>false</code> means it can be any peptide C or N term.
	 */
	private Boolean proteinOnly;
	/**
	 * unimod has a handful of classifications for differentiating specificities
	 */
	private String classification;
	/**
	 * if there are specificities within the same modification with the same group then they can be grouped. All members
	 * of the same group have the same number stored.
	 */
	private Integer specificityGroup;
	private String comments;

	/**
	 * Name of the terminus as it appears in the unimod file.
	 */
	public static final String POSITION_ANYWHERE = "Anywhere";
	public static final String POSITION_ANY_N_TERM = "Any N-term";
	public static final String POSITION_PROTEIN_N_TERM = "Protein N-term";
	public static final String POSITION_ANY_C_TERM = "Any C-term";
	public static final String POSITION_PROTEIN_C_TERM = "Protein C-term";
	public static final String SITE_N_TERM = "N-term";
	public static final String SITE_C_TERM = "C-term";

	/**
	 * @param modification Modification this specificity belongs to.
	 * @param site         Amino acid where the modification appears. If null, it can be any amino acid (C/N terminus).
	 * @param terminus     If the modification happens only at the terminus, specify which one.
	 * @param proteinOnly  If the modification happens only at the terminus, can it be protein terminus only, or any peptide. Otherwise store null.
	 */
	public ModSpecificity(final Mod modification, final Character site, final Terminus terminus, final boolean proteinOnly, final boolean hidden, final String classification, final Integer specificityGroup, final String comments) {
		setModification(modification);
		setSite(site);
		setTerm(terminus);
		setProteinOnly(proteinOnly);
		setHidden(hidden);
		setClassification(classification);
		setSpecificityGroup(specificityGroup);
		setComments(comments);
	}

	public ModSpecificity() {
	}

	public Mod getModification() {
		return modification;
	}

	private void setModification(final Mod modification) {
		this.modification = modification;
	}

	/**
	 * True if this modification is one of the ones marked "hidden" in the unimod file; these modifications
	 * are less prevelant and are typically only displayed on user request.
	 */
	public Boolean getHidden() {
		return hidden != null && hidden;
	}

	private void setHidden(final Boolean hidden) {
		this.hidden = hidden;
	}

	/**
	 * Unimod "site" string specifying the amino acid where the modification is permitted; see {@link #site}
	 */
	public Character getSite() {
		return site;
	}

	private void setSite(final Character site) {
		this.site = site != null ? site : '*';
	}

	public Terminus getTerm() {
		return term;
	}

	private void setTerm(final Terminus term) {
		this.term = term;
	}

	public Boolean isProteinOnly() {
		return proteinOnly;
	}

	private void setProteinOnly(final Boolean proteinOnly) {
		this.proteinOnly = proteinOnly;
	}

	public String getClassification() {
		return classification;
	}

	private void setClassification(final String classification) {
		this.classification = classification;
	}

	public Integer getSpecificityGroup() {
		return specificityGroup;
	}

	private void setSpecificityGroup(final Integer specificityGroup) {
		this.specificityGroup = specificityGroup;
	}

	/**
	 * Only use this for mascot: it combines ModSpecificities within the same specificity group,
	 * eg "Phospho (ST)".  Constrast this with {@link #toString()}.
	 * <p/>
	 * The ?-term or Protein ?-term modifications are output as Title (N-term AA) or (N-term) to be compliant with
	 * Mascot's format.
	 */
	public String toMascotString() {
		if (modification.getTitle() == null) {
			return null;
		}

		final StringBuilder place = new StringBuilder();

		if (getSpecificityGroup() != null) {
			for (final ModSpecificity modSpecificity : groupSpecificities()) {
				addLocation(place, modSpecificity);
			}
		} else {
			addLocation(place, this);
		}

		return modification.getTitle() + " (" + place + ")";
	}

	/**
	 * Returns a list of group specificities that this particular specificity is a part of.
	 */
	public List<ModSpecificity> groupSpecificities() {
		final List<ModSpecificity> groupModSpecificities = new ArrayList<ModSpecificity>(4);
		final Set<ModSpecificity> modModSpecificitySet = modification.getModSpecificities();
		for (final ModSpecificity modSpecificity : modModSpecificitySet) {
			if (getSpecificityGroup().equals(modSpecificity.getSpecificityGroup())) {
				groupModSpecificities.add(modSpecificity);
			}
		}
		Collections.sort(groupModSpecificities);
		return groupModSpecificities;
	}

	/**
	 * Produces the string used to display modifications to the user, eg "Phospho (S)".
	 * <p/>
	 * Contrast this with {@link #toMascotString()}.
	 */
	public String toString() {
		if (modification.getTitle() == null) {
			return "";
		}
		final StringBuilder place = new StringBuilder();
		addLocation(place, this);
		return modification.getTitle() + " (" + place + ")";
	}

	private static void addLocation(final StringBuilder place, final ModSpecificity modSpecificity) {
		final StringBuilder text = new StringBuilder();
		if (modSpecificity.isPositionProteinSpecific()) {
			text.append(" Protein");
		}
		if (modSpecificity.isPositionCTerminus()) {
			text.append(' ').append(ModSpecificity.SITE_C_TERM);
		} else if (modSpecificity.isPositionNTerminus()) {
			text.append(' ').append(ModSpecificity.SITE_N_TERM);
		}
		if (modSpecificity.isSiteSpecificAminoAcid()) {
			text.append(' ').append(modSpecificity.getSite());
		}
		if (text.length() > 1) {
			place.append(text.substring(1));
		}
	}

	@Override
	public int hashCode() {
		// The hash code is simplified to the most important stuff, does not need to have the full equals suite
		return Objects.hashCode(getModification(),
				getSite(),
				getTerm(),
				isProteinOnly());
	}

	/**
	 * Two mods are considered equal, if they have everything the same, including unimportant properties like comments.
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ModSpecificity)) {
			return false;
		}

		final ModSpecificity other = (ModSpecificity) obj;

		if (getClassification() != null ? !getClassification().equals(other.getClassification()) : other.getClassification() != null) {
			return false;
		}
		if (!Strings.nullToEmpty(getComments()).equals(Strings.nullToEmpty(other.getComments()))) {
			return false;
		}
		if (getHidden() != null ? !getHidden().equals(other.getHidden()) : other.getHidden() != null) {
			return false;
		}
		if (!Strings.nullToEmpty(getModification().getTitle()).equals(Strings.nullToEmpty(other.getModification().getTitle()))) {
			return false;
		}
		if (isProteinOnly() != null ? !isProteinOnly().equals(other.isProteinOnly()) : other.isProteinOnly() != null) {
			return false;
		}
		if (getSite() != null ? !getSite().equals(other.getSite()) : other.getSite() != null) {
			return false;
		}
		if (getSpecificityGroup() != null ? !getSpecificityGroup().equals(other.getSpecificityGroup()) : other.getSpecificityGroup() != null) {
			return false;
		}
		return getTerm() == other.getTerm();

	}

	@Override
	public int compareTo(final ModSpecificity o) {
		if (o == null) {
			return -1;
		}
		return ComparisonChain.start().nullsFirst()
				.compare(this.getModification().getTitle().toLowerCase(Locale.ENGLISH), o.getModification().getTitle().toLowerCase(Locale.ENGLISH))
				.compare(this.getTerm(), o.getTerm())
				.compare(this.isProteinOnly(), o.isProteinOnly())
				.compare(this.getSite(), o.getSite())
				.compare(this.getSpecificityGroup(), o.getSpecificityGroup())
				.result();
	}

	/**
	 * Returns free text comments given in unimod, if any.
	 */
	public String getComments() {
		return comments;
	}

	private void setComments(final String comments) {
		this.comments = comments;
	}

	public boolean isPositionNTerminus() {
		return term == Terminus.Nterm;
	}

	public boolean isPositionCTerminus() {
		return term == Terminus.Cterm;
	}

	public boolean isPositionAnywhere() {
		return term == Terminus.Anywhere;
	}

	/**
	 * @return True if this mod dictates a specific amino acid it works on. False for '*' site.
	 */
	public boolean isSiteSpecificAminoAcid() {
		if (site != null) {
			return !site.equals('*');
		}
		return false;
	}

	/**
	 * @return True if this modification can occur at C terminus. This means either the C terminus is requested,
	 *         or the modification can occur anywhere in the sequence.
	 */
	public boolean isSiteCTerminus() {
		if (site != null) {
			return site.equals('*') && isPositionCTerminus();
		}

		return false;
	}

	/**
	 * @return True if this modification can occur at N terminus. This means either the N terminus is requested,
	 *         or the modification can occur anywhere in the sequence.
	 */
	public boolean isSiteNTerminus() {
		if (site != null) {
			return site.equals('*') && isPositionNTerminus();
		}

		return false;
	}

	public boolean isPositionProteinSpecific() {
		return !isPositionAnywhere() && proteinOnly;
	}

	public ModSpecificity copy() {
		return new ModSpecificity(
				getModification().copy(),
				getSite(),
				getTerm(),
				isProteinOnly(),
				getHidden(),
				getClassification(),
				getSpecificityGroup(),
				getComments());
	}

	/**
	 * @param acids      Which amino acids are allowed (specificity must be contained in the list). Empty string means - not AA specific.
	 * @param terminus   Terminus for the specificity.
	 * @param proteinEnd Is the terminus on protein end only?
	 * @return True if this specificity matches the requested parameters.
	 */
	public boolean matches(final String acids, final Terminus terminus, final boolean proteinEnd) {
		if (term != terminus) {
			return false;
		}
		if (terminus != Terminus.Anywhere && isProteinOnly() != proteinEnd) {
			return false;
		}
		if (acids.isEmpty()) {
			if (isPositionCTerminus() || isPositionNTerminus()) {
				return true;
			}
			if (!isPositionAnywhere()) {
				return false;
			}
		}
		return acids.contains(String.valueOf(getSite()));
	}
}
