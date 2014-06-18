package edu.mayo.mprc.swift.configuration.client.model;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import edu.mayo.mprc.common.client.GWTServiceException;

public interface ConfigurationService extends RemoteService {

	/**
	 * Save current configuration to a file.
	 *
	 * @return How to modify the UI - a list of validation messages in this case.
	 * @throws GWTServiceException
	 */
	UiChangesReplayer saveConfiguration() throws GWTServiceException;

	ApplicationModel loadConfiguration() throws GWTServiceException;

	ResourceModel createChild(String parentId, String type) throws GWTServiceException;

	void removeChild(String childId) throws GWTServiceException;

	void changeRunner(String serviceId, String newRunnerType) throws GWTServiceException;

	/**
	 * Any time property changes on the client, this method is called. Validation results are returned.
	 *
	 * @param onDemand The user specifically demands validation of the property by clicking on demand button.
	 */
	UiChangesReplayer propertyChanged(String modelId, String propertyName, String newValue, boolean onDemand) throws GWTServiceException;

	/**
	 * The client asked to fix a validation error for given property name, by running a specified action.
	 */
	void fix(String moduleId, String propertyName, String action) throws GWTServiceException;

	/**
	 * Shut down the whole server.
	 */
	void terminateProgram();

	/**
	 * Utility/Convenience class.
	 * Use ConfigurationService.App.getInstance() to access static instance of ConfigurationServiceAsync
	 */
	class App {
		private static final ConfigurationServiceAsync OUR_INSTANCE;

		private App() {
		}

		static {
			OUR_INSTANCE = (ConfigurationServiceAsync) GWT.create(ConfigurationService.class);
			((ServiceDefTarget) OUR_INSTANCE).setServiceEntryPoint(GWT.getModuleBaseURL() + "ConfigurationService");
		}

		public static ConfigurationServiceAsync getInstance() {
			return OUR_INSTANCE;
		}
	}
}
