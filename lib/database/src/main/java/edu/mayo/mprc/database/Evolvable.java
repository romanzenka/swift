package edu.mayo.mprc.database;

/**
 * Swift is storing parameters into a long term storage, so it is possible to go back and see exactly what was done.
 * <p/>
 * The values however evolve over time. This interface allows marking old values as deprecated, and provide a reason why.
 * Since it is possible to evolve objects, these objects should not be mutable.
 */
public interface Evolvable extends EqualityCriteria {
	/**
	 * Set id for hibernate. This is not to be used by the code itself.
	 *
	 * @param id
	 */
	void setId(Integer id);

	/**
	 * @return Hibernate id. Do not use this value for anything.
	 */
	Integer getId();

	/**
	 * @return Information about when and why was the object created.
	 */
	Change getCreation();

	/**
	 * @param creation Reason for the object being created.
	 */
	void setCreation(Change creation);

	/**
	 * @return The reason why this object is deprecated. If null, the object is in active use.
	 */
	Change getDeletion();

	/**
	 * @param deletion The reason why this object is deprecated. One such reason can be shared with many objects.
	 */
	void setDeletion(Change deletion);
}
