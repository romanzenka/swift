package edu.mayo.mprc.swift.ui.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

/**
 * Hosts SimpleParamsEditorPanel for testing.
 * Not an entry point-called manually from SwiftApp.
 */
public final class ParamsEditorApp {

	private static ServiceAsync serviceAsync;
	private static SimpleParamsEditorPanel panel;

	private ParamsEditorApp() {
	}
	//public int changeTimeout = 5000; // Validation delay after modifying text box in msecs.

	private static void initConnection() {
		serviceAsync = (ServiceAsync) GWT.create(Service.class);

		final String moduleRelativeURL = GWT.getModuleBaseURL() + "Service";
		((ServiceDefTarget) serviceAsync).setServiceEntryPoint(moduleRelativeURL);
	}


	/**
	 * * This is the entry point method.
	 */
	public static void onModuleLoad(InitialPageData pageData) {
		GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
			public void onUncaughtException(final Throwable throwable) {
				SimpleParamsEditorPanel.handleGlobalError(throwable);
			}
		});

		initConnection();

		panel = new SimpleParamsEditorPanel(serviceAsync, pageData);

	}

	public static SimpleParamsEditorPanel getPanel() {
		return panel;
	}
}
