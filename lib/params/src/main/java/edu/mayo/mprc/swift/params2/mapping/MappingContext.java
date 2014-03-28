package edu.mayo.mprc.swift.params2.mapping;

import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.swift.params2.ParamName;

/**
 * Specifies a context for mapping parameters. The context knows what we want to map, in which direction, what are the additional
 * data we might need to do so and how to report errors.
 */
public interface MappingContext {

	/**
	 * Information about abstract parameters.
	 */
	ParamsInfo getAbstractParamsInfo();

	/**
	 * Notify the context that given parameter started to be mapped. All subsequent errors/warnings should be
	 * tied to this parameter.
	 *
	 * @param paramName Name of the parameter that is being mapped.
	 */
	void startMapping(ParamName paramName);

	/**
	 * Adds a message to the mapping that is currently being processed.
	 *
	 * @param message   Error message to add.
	 * @param t         Exception, in case we have any - null otherwise.
	 * @param paramName Parameter that this error pertains to. Null - whatever we are processing now.
	 */
	void reportError(String message, Throwable t, ParamName paramName);

	/**
	 * Adds a message to the mapping that is currently being processed.
	 *
	 * @param message   Warning message to add.
	 * @param paramName Parameter that this warning pertains to. Null - whatever we are processing now.
	 */
	void reportWarning(String message, ParamName paramName);

	/**
	 * Adds a message to the mapping that is currently being processed.
	 *
	 * @param message   Informational message to add.
	 * @param paramName Parameter that this message pertains to. Null - whatever we are processing now.
	 */
	void reportInfo(String message, ParamName paramName);

	/**
	 * @return True if no errors occured since last call to a mapping method.
	 *         Use this if you want to do an action only if all mappings validated ok.
	 */
	boolean noErrors();

	/**
	 * Used only for migrating Swift params into database. When a missing database is detected,
	 * this code adds a dummy version to the database. Make sure it has the deletion set.
	 *
	 * @param legacyName Name of the curation that was missing.
	 * @deprecated Delete once Swift gets migrated to the database.
	 */
	Curation addLegacyCuration(String legacyName);
}
