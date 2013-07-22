package edu.mayo.mprc.swift.params2;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.EvolvableBase;
import edu.mayo.mprc.utilities.ComparisonChain;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Represents an endoprotease enzyme. The enzyme itself is immutable.
 */
public class Protease extends EvolvableBase implements Serializable, Comparable<Protease> {
	private static final long serialVersionUID = 20111119L;
	public static final Protease TRYPSIN_P = new Protease("Trypsin (allow P)", "KR", "");
	private String name;
	private String rnminus1;
	private String rn;
	private static final int MAX_PROTEASE_NAME_LENGTH = 50;
	private static final int MAX_RN_LENGTH = 40;

	/**
	 * Hibernate-only constructor.
	 */
	public Protease() {
	}

	public Protease(final String name, final String rnminus1, final String rn) {
		this.name = name;
		this.rnminus1 = rnminus1;
		this.rn = rn;
	}

	public String getName() {
		return name;
	}

	void setName(final String name) {
		if (name != null && name.length() > MAX_PROTEASE_NAME_LENGTH) {
			throw new MprcException("Protease name " + name + "is too long");
		}
		this.name = Strings.nullToEmpty(name);
	}

	public String getRn() {
		return rn;
	}

	void setRn(final String rn) {
		if (rn != null && rn.length() > MAX_RN_LENGTH) {
			throw new MprcException("Protease Rn-1 " + rn + "is too long");
		}
		this.rn = Strings.nullToEmpty(rn);
	}

	public String getRnminus1() {
		return rnminus1;
	}

	void setRnminus1(final String rnminus1) {
		if (rnminus1 != null && rnminus1.length() > MAX_RN_LENGTH) {
			throw new MprcException("Protease Rn-1 " + rnminus1 + "is too long");
		}
		this.rnminus1 = Strings.nullToEmpty(rnminus1);
	}

	public boolean equals(final Object o) {
		if (!(o instanceof Protease)) {
			return false;
		}
		final Protease p = (Protease) o;
		return Objects.equal(p.getName(), getName()) &&
				Objects.equal(p.getRnminus1(), getRnminus1());
	}

	public int hashCode() {
		return (getName() != null ? getName().hashCode() : 0) + (getRnminus1() != null ? getRnminus1().hashCode() : 0)
				+ (getRn() != null ? getRn().hashCode() : 0);
	}

	public String toString() {
		return getName() + ": " + rnminus1 + " " + rn;
	}

	public int compareTo(final Protease o) {
		return ComparisonChain.start().nullsFirst()
				.compare(getName(), o.getName())
				.compare(getRnminus1(), o.getRnminus1())
				.compare(getRn(), o.getRn())
				.result();
	}

	public Protease copy() {
		final Protease protease = new Protease(getName(), getRnminus1(), getRn());
		protease.setId(getId());
		return protease;
	}

	/**
	 * Provides initial fixed list of allowed enzymes.  This list was
	 * taken from /usr/local/mascot/config/enzymes.  We
	 * removed those that aren't representable on the other
	 * engines: such as semi-specific cleavage (NTT<2)
	 * and those with multiple cleavage events (like CNBr + Trypsin)
	 * <p/>
	 * Briefly, an endoprotease is modeled by a single amino acid on either
	 * side of a siscile bond, eg AArnminus1 - siscile bind - AArn.
	 * <p/>
	 * A list of amino acids allowed in each position is specified by single letter code;
	 * ! preceding a list of amino acids inverts that list, matching anything but
	 * the listed residues; and empty list matches all amino acids.
	 * <p/>
	 * See Voet and Voet Biochemistry pg 111 for a more thorough explanation of this
	 * nomenclature, from whence it is taken.
	 */
	public static List<Protease> getInitial() {
		return Arrays.asList(
				new Protease("Arg-C", "R", "!P"),
				new Protease("Asp-N", "", "BD"),
				new Protease("Asp-N_ambic", "", "DE"),
				new Protease("Chymotrypsin", "FYWL", "!P"),
				new Protease("CNBr", "M", ""),
				new Protease("Formic_acid", "D", ""),
				new Protease("Lys-C (restrict P)", "K", "!P"),
				new Protease("Lys-C (allow P)", "K", ""),
				new Protease("PepsinA", "FL", ""),
				new Protease("Tryp-CNBr", "KRM", "!P"),
				new Protease("TrypChymo", "FYWLKR", "!P"),
				new Protease("TrypChymoKRWFYnoP", "KRWFY", ""),
				TRYPSIN_P,
				new Protease("Trypsin (restrict P)", "KR", "!P"),
				new Protease("V8-DE", "BDEZ", "!P"),
				new Protease("V8-E", "EZ", "!P"),
				new Protease("ChymoAndGluC", "FYWLE", ""),
				new Protease("Non-Specific", "", ""));
	}
}
