package edu.mayo.mprc.swift.configuration.client.model;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ConfigurationServiceAsync {
	void saveConfiguration(AsyncCallback<UiChangesReplayer> async);

	void loadConfiguration(AsyncCallback<ApplicationModel> async);

	void createChild(String parentId, String type, AsyncCallback<ResourceModel> async);

	void removeChild(String childId, AsyncCallback<Void> async);

	void propertyChanged(String modelId, String propertyName, String newValue, boolean onDemand, AsyncCallback<UiChangesReplayer> async);

	void fix(String moduleId, String propertyName, String action, AsyncCallback<Void> async);

	void changeRunner(String serviceId, String newRunnerType, AsyncCallback<Void> async);
}
