package edu.mayo.mprc.swift.configuration.client.view;

/**
 * Listens to fix action trigger by the Widget generated in the makeWidget(......) method of the FixTag class.
 */
public interface FixTagActionListener {
	/**
	 * Method is called when a fix action is initiated by user.
	 * <p/>
	 * After the action ends, the listener should indicate success using the provided {@link ValidationPanel#showSuccess()}
	 */
	void onFix(String action, ValidationPanel validationPanel);
}
