package edu.mayo.mprc.swift.configuration.client.model;

public interface Context {
	ApplicationModel getApplicationModel();

	/**
	 * Displays general error message.
	 */
	void displayErrorMessage(String message);

	/**
	 * Displays error message with details specified by an exception.
	 */
	void displayErrorMessage(String message, Throwable t);
}
