package edu.mayo.mprc.unimod;

import edu.mayo.mprc.config.RuntimeInitializer;
import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.database.Dao;

public interface UnimodDao extends Dao, RuntimeInitializer {
	/**
	 * @return Current version of unimod. Since this is an expensive operation, you should store the unimod around
	 *         instead of calling this method over and over. Also, the resulting Mods are not managed by Hibernate to
	 *         keep the session cache small.
	 */
	Unimod load();

	/**
	 * Upgrade the unimod database/install a fresh copy of unimod. An upgrade will retain all previously existing objects,
	 * so no references will be broken.
	 *
	 * @param unimod  Unimod to install.
	 * @param request Change request.
	 */
	UnimodUpgrade upgrade(Unimod unimod, Change request);

	/**
	 * TODO: This needs to be a config file outside of Swift.
	 *
	 * @return Default, initial version of unimod, as embedded in Swift.
	 */
	Unimod getDefaultUnimod();
}
