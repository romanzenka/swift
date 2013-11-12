package edu.mayo.mprc.swift.ui.client.widgets.validation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import edu.mayo.mprc.swift.ui.client.dialogs.ErrorDialog;
import edu.mayo.mprc.swift.ui.client.rpc.InitialPageData;
import edu.mayo.mprc.swift.ui.client.service.Service;
import edu.mayo.mprc.swift.ui.client.service.ServiceAsync;

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
			@Override
			public void onUncaughtException(final Throwable throwable) {
				ErrorDialog.handleGlobalError(throwable);
			}
		});

		initConnection();

		panel = new SimpleParamsEditorPanel(serviceAsync, pageData);

	}

	public static SimpleParamsEditorPanel getPanel() {
		return panel;
	}
}
