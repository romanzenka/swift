package edu.mayo.mprc.database;

import org.hibernate.criterion.Criterion;

/**
 * A class that can produce a hibernate criterion matching all equal classes in the database.
 * Equal means equal for business purposes (not including the ID of the object for instance).
 * This can be used to cut on database use by storing each distinct object only once.
 *
 * @author Roman Zenka
 */
public interface EqualityCriteria {
	/**
	 * @return Hibernate equality criteria that will match classes equal to this one in the database.
	 * Not all objects need to implement this - throw an exception if you never want to
	 * query this object by its contents.
	 */
	Criterion getEqualityCriteria();
}
