package edu.mayo.mprc.quameterdb.dao;

import com.google.common.base.Objects;
import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.EvolvableBase;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Stores information about a named protein group.
 * <p/>
 * A protein group has a short, user-assigned name, and a regex that matches all the accession numbers
 * for proteins in this group.
 * <p/>
 * The group is stored in the database so it can be referenced by {@link QuameterResult} objects.
 * These store spectral counts for each protein group.
 *
 * @author Roman Zenka
 */
public final class QuameterProteinGroup extends EvolvableBase implements Cloneable {
	private String name;
	private String regex;
	private transient Pattern compiledRegex;

	public QuameterProteinGroup() {
	}

	public QuameterProteinGroup(final String name, final String regex) {
		this.name = name;
		setRegex(regex);
	}

	@Override
	public Criterion getEqualityCriteria() {
		return Restrictions.conjunction()
				.add(DaoBase.nullSafeEq("name", getName()))
				.add(DaoBase.nullSafeEq("regex", getRegex()));
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name, regex);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final QuameterProteinGroup other = (QuameterProteinGroup) obj;
		return Objects.equal(this.name, other.name) && Objects.equal(this.regex, other.regex);
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(final String regex) {
		this.regex = regex;
		this.compiledRegex = Pattern.compile(this.regex, Pattern.CASE_INSENSITIVE);
	}

	/**
	 * @param accnums List of accession numbers in a protein group
	 * @return True if any of the accession numbers match regex for this group. false otherwise
	 */
	public boolean matches(final Collection<String> accnums) {
		for (final String accnum : accnums) {
			if (compiledRegex.matcher(accnum).matches()) {
				return true;
			}
		}
		return false;
	}

	public QuameterProteinGroup clone() {
		return new QuameterProteinGroup(getName(), getRegex());
	}
}
