package edu.mayo.mprc.daemon;

import java.util.Map;

/**
 * An object that provides user interface configuration
 * in the form of key/value pairs.
 * <p/>
 * It will take some existing user configuration and enrich it by its own data.
 * This way the configuration can be created as a consensus of multiple entities working together.
 *
 * @author Roman Zenka
 */
public interface UiConfigurationProvider {
	/**
	 * Is given current configuration. Can read it and modify it.
	 *
	 * @param currentConfiguration Configuration to be enriched with this object's settings.
	 */
	void provideConfiguration(Map<String, String> currentConfiguration);
}
