package edu.mayo.mprc.database;

import org.hibernate.criterion.Criterion;

/**
 * A class that can produce a hibernate criterion matching all equal classes in the database.
 *
 * @author Roman Zenka
 */
public interface EqualityCriteria {
	/**
	 * @return Hibernate equality criteria that will match classes equal to this one in the database
	 */
	Criterion getEqualityCriteria();
}
