package edu.mayo.mprc.swift.ui.client.widgets;

/**
 * Configuration for the user interface.
 * <p/>
 * The configuration is collected from the Swift configuration file itself (webUi is the main class) as well
 * as all the plugins that provide user interface configuration elements
 * (marked with UserInterfaceConfigurationProvider interface).
 * <p/>
 * The configuration is then shipped to Swift application in a hashmap. The application can access it afterwards
 * via this interface.
 *
 * @author Roman Zenka
 */
public interface UiConfiguration {

	/**
	 * Return a setting for the user interface.
	 *
	 * @param key Name of the setting.
	 * @return The setting itself. If no setting is present, returns null.
	 */
	String getConfigurationSetting(String key);
}
