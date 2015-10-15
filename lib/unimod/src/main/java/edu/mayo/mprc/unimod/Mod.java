package edu.mayo.mprc.unimod;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.EvolvableBase;
import edu.mayo.mprc.utilities.ComparisonChain;
import edu.mayo.mprc.utilities.ExcludeJson;
import org.hibernate.criterion.Criterion;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents a post-translational modification. The modification itself is immutable, any change (including adding specificities),
 * requires creating a clone. The setters are provided for Hibernate only.
 */
public class Mod extends EvolvableBase implements Comparable<Mod> {
	private String title;
	private String fullName;
	private Integer recordID;
	private Double massMono;
	private Double massAverage;
	private String composition;

	@ExcludeJson
	private Set<String> altNames;

	@ExcludeJson
	private Set<ModSpecificity> modSpecificities;

	private static final int MAX_COMPOSITION_LENGTH = 150;

	/**
	 * For Hibernate
	 */
	public Mod() {
	}

	public Mod(final String title, final String fullName, final Integer recordID, final Double massMono, final Double massAverage, final String composition, final Set<String> altNames, final Set<ModSpecificity> modSpecificities) {
		setVariables(title, fullName, recordID, massMono, massAverage, composition, altNames);
		setModSpecificities(modSpecificities);
	}

	public Mod(final String title, final String fullName, final Integer recordID, final Double massMono, final Double massAverage, final String composition, final Set<String> altNames, final Set<SpecificityBuilder> specificityBuilders, final boolean ignore) {
		setVariables(title, fullName, recordID, massMono, massAverage, composition, altNames);
		setModSpecificities(buildersToSpecificities(specificityBuilders));
	}

	public Mod(final String title, final String fullName, final Integer recordID, final Double massMono, final Double massAverage, final String composition, final Set<String> altNames, final SpecificityBuilder specificityBuilder) {
		setVariables(title, fullName, recordID, massMono, massAverage, composition, altNames);
		setModSpecificities(new ImmutableSet.Builder<ModSpecificity>().add(specificityBuilder.build(this)).build());
	}

	private void setVariables(final String title, final String fullName, final Integer recordID, final Double massMono, final Double massAverage, final String composition, final Set<String> altNames) {
		setTitle(title);
		setFullName(fullName);
		setRecordID(recordID);
		setMassMono(massMono);
		setMassAverage(massAverage);
		setComposition(composition);
		setAltNames(altNames);
	}

	private Set<ModSpecificity> buildersToSpecificities(final Set<SpecificityBuilder> specificityBuilders) {
		final Set<ModSpecificity> specificities = new HashSet<ModSpecificity>(specificityBuilders.size());
		for (final SpecificityBuilder builder : specificityBuilders) {
			specificities.add(builder.build(this));
		}
		return specificities;
	}

	public String toString() {
		final StringBuilder sb = new StringBuilder();

		sb.append("title=").append(title);
		sb.append("; full name=").append(fullName);
		sb.append("; recordID=").append(recordID);

		for (final ModSpecificity s : modSpecificities) {
			sb.append("Specificity: ").append(s.toString()).append("\n");
		}
		sb.append("Delta: ")
				.append("mono: ").append(massMono)
				.append("avg: ").append(massAverage)
				.append("composition: ").append(composition)
				.append("\n");

		return sb.toString();
	}

	public Set<String> getAltNames() {
		return altNames;
	}

	private void setAltNames(final Set<String> altNames) {
		this.altNames = altNames;
	}

	public String getTitle() {
		return title;
	}

	private void setTitle(final String title) {
		if (title != null && title.length() > MAX_COMPOSITION_LENGTH) {
			throw new MprcException("The modification title is too long, maximum length is 150: " + title);
		}
		this.title = title;
	}

	public String getFullName() {
		return fullName;
	}

	private void setFullName(final String fullName) {
		if (fullName != null && fullName.length() > MAX_COMPOSITION_LENGTH) {
			throw new MprcException("The modification full name is too long, maximum length is 150: " + fullName);
		}
		this.fullName = fullName;
	}

	public Integer getRecordID() {
		return recordID;
	}

	private void setRecordID(final Integer recordID) {
		this.recordID = recordID;
	}

	public Set<ModSpecificity> getModSpecificities() {
		return modSpecificities;
	}

	private void setModSpecificities(final Set<ModSpecificity> modSpecificities) {
		this.modSpecificities = modSpecificities == null ? new TreeSet<ModSpecificity>() : modSpecificities;
	}

	public Double getMassMono() {
		return massMono;
	}

	private void setMassMono(final Double massMono) {
		this.massMono = massMono;
	}

	public Double getMassAverage() {
		return massAverage;
	}

	private void setMassAverage(final Double massAverage) {
		this.massAverage = massAverage;
	}

	public String getComposition() {
		return composition;
	}

	private void setComposition(final String composition) {
		if (composition != null && composition.length() > MAX_COMPOSITION_LENGTH) {
			throw new MprcException("The modification composition is too long, maximum length is " + MAX_COMPOSITION_LENGTH + ": " + composition);
		}

		this.composition = composition;
	}

	public int hashCode() {
		return (getTitle() != null ? getTitle().hashCode() : 0) + (getMassMono() == null ? 0 : getMassMono().hashCode());
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Mod)) {
			return false;
		}

		final Mod mod = (Mod) obj;

		if (getAltNames() != null ? !getAltNames().equals(mod.getAltNames()) : mod.getAltNames() != null) {
			return false;
		}
		if (!Strings.nullToEmpty(getComposition()).equals(Strings.nullToEmpty(mod.getComposition()))) {
			return false;
		}
		if (!Strings.nullToEmpty(getFullName()).equals(Strings.nullToEmpty(mod.getFullName()))) {
			return false;
		}
		if (getMassAverage() != null ? !getMassAverage().equals(mod.getMassAverage()) : mod.getMassAverage() != null) {
			return false;
		}
		if (getMassMono() != null ? !getMassMono().equals(mod.getMassMono()) : mod.getMassMono() != null) {
			return false;
		}
		if (getRecordID() != null ? !getRecordID().equals(mod.getRecordID()) : mod.getRecordID() != null) {
			return false;
		}
		if (getModSpecificities() != null ? !getModSpecificities().equals(mod.getModSpecificities()) : mod.getModSpecificities() != null) {
			return false;
		}
		if (!Strings.nullToEmpty(getTitle()).equals(Strings.nullToEmpty(mod.getTitle()))) {
			return false;
		}
		return true;
	}

	public Mod copy() {
		Mod mod = new Mod(
				getTitle(),
				getFullName(),
				getRecordID(),
				getMassMono(),
				getMassAverage(),
				getComposition(),
				getAltNames(),
				getModSpecificities());
		mod.setId(getId());
		return mod;
	}

	/**
	 * Sort by title, full name and record ID
	 */
	@Override
	public int compareTo(final Mod o) {
		return ComparisonChain.start().nullsFirst()
				.compare(getTitle(), o.getTitle())
				.compare(getFullName(), o.getFullName())
				.compare(getRecordID(), o.getRecordID())
				.result();
	}

	@Override
	public Criterion getEqualityCriteria() {
		throw new MprcException("Mods are not saved using equality criteria");
	}
}
