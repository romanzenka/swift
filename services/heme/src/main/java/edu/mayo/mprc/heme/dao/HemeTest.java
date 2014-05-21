package edu.mayo.mprc.heme.dao;

import edu.mayo.mprc.database.DaoBase;
import edu.mayo.mprc.database.PersistableBase;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import java.util.Date;

/**
 * A test performed by the laboratory. Identifies the subject and date + additional parameters (expected mass delta).
 *
 * @author Roman Zenka
 */
public class HemeTest extends PersistableBase {
	/**
	 * Name of the patient (coded). This is equivalent to the name of the search and should be the last folder
	 * of the path as well, but there can be exceptions. This is just to make searches easier.
	 */
	private String name;

	/**
	 * Date of the test.
	 */
	private Date date;

	/**
	 * Path to the test folder, relative to the root. Includes the patient name (coded) as well as the date.
	 * In theory this is obsolete, in practice it would allow us to hack in special cases that do not follow
	 * the prescription.
	 */
	private String path;

	/**
	 * Expected mass delta in Daltons.
	 */
	private double massDelta;

	/**
	 * The mass delta tolerance (in Daltons). Any mass +- the tolerance is to be considered.
	 */
	private double massDeltaTolerance;

	/**
	 * Search run that is associated with this test.
	 */
	private SearchRun searchRun;

	public HemeTest() {
	}

	public HemeTest(final String name, final Date date, final String path, final double massDelta, final double massDeltaTolerance) {
		this.name = name;
		this.date = date;
		this.path = path;
		this.massDelta = massDelta;
		this.massDeltaTolerance = massDeltaTolerance;
	}

	public String getPath() {
		return path;
	}

	public void setPath(final String path) {
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(final Date date) {
		this.date = date;
	}

	public double getMassDelta() {
		return massDelta;
	}

	public void setMassDelta(final double massDelta) {
		this.massDelta = massDelta;
	}

	public double getMassDeltaTolerance() {
		return massDeltaTolerance;
	}

	public void setMassDeltaTolerance(final double massDeltaTolerance) {
		this.massDeltaTolerance = massDeltaTolerance;
	}

	public SearchRun getSearchRun() {
		return searchRun;
	}

	public void setSearchRun(SearchRun searchRun) {
		this.searchRun = searchRun;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (!(o instanceof HemeTest)) return false;

		final HemeTest hemeTest = (HemeTest) o;

		if (date != null ? !date.equals(hemeTest.date) : hemeTest.date != null) return false;
		if (path != null ? !path.equals(hemeTest.path) : hemeTest.path != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = date != null ? date.hashCode() : 0;
		result = 31 * result + (path != null ? path.hashCode() : 0);
		return result;
	}

	/**
	 * The two objects are equivalent iff their date and path match.
	 * Name does not have to match since it is contained in the path.
	 * Also, the path is what matters.
	 *
	 * @return Hibernate criteria matching all records equal to the provided one.
	 */
	@Override
	public Criterion getEqualityCriteria() {
		return Restrictions.and(
				DaoBase.nullSafeEq("date", getDate()),
				DaoBase.nullSafeEq("path", getPath()));
	}


	@Override
	public String toString() {
		return "HemeTest{" +
				"name='" + name + '\'' +
				", date=" + date +
				", path='" + path + '\'' +
				", massDelta=" + massDelta +
				", massDeltaTolerance=" + massDeltaTolerance +
				'}';
	}
}
