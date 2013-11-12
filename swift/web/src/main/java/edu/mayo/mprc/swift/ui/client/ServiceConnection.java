package edu.mayo.mprc.swift.ui.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import edu.mayo.mprc.swift.ui.client.service.Service;
import edu.mayo.mprc.swift.ui.client.service.ServiceAsync;

public final class ServiceConnection {
	private static ServiceAsync browsingService;

	private ServiceConnection() {
	}

	public static synchronized ServiceAsync instance() {
		if (browsingService == null) {
			browsingService = (ServiceAsync) GWT.create(Service.class);

			final ServiceDefTarget endpoint = (ServiceDefTarget) browsingService;
			final String moduleRelativeURL = GWT.getModuleBaseURL() + "Service";
			endpoint.setServiceEntryPoint(moduleRelativeURL);
		}
		return browsingService;
	}
}
