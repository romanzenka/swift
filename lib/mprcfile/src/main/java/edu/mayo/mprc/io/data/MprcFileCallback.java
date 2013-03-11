package edu.mayo.mprc.io.data;

import java.sql.ResultSet;

/**
 * Callback for MPRC files - gets an already created result set and processes it. To be used internally.
 */
public interface MprcFileCallback {
	/**
	 * @param rs ResultSet to process.
	 * @return Any object, will be returned from {@link MprcFile#processResultSet}.
	 */
	Object processResultSet(ResultSet rs);
}
