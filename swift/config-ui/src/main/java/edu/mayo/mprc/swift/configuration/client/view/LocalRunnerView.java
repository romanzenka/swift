package edu.mayo.mprc.swift.configuration.client.view;

import com.google.gwt.user.client.ui.SimplePanel;
import edu.mayo.mprc.swift.configuration.client.model.Context;
import edu.mayo.mprc.swift.configuration.client.model.ResourceModel;

import java.util.HashMap;

public final class LocalRunnerView extends SimplePanel {

	private static final int MAX_THREADS = 256;
	private PropertyList propertyList;
	private ResourceModel resourceModel;

	public static final String THREAD_NUMBER = "Number of Threads";
	public static final String LOG_OUTPUT_FOLDER = "Log Output Folder";


	public LocalRunnerView(final Context context, final ResourceModel model) {
		final GwtUiBuilder builder = new GwtUiBuilder(context, model);

		builder.start();
		builder.property("numThreads", THREAD_NUMBER, "Number of simultaneously executing threads.").integerValue(1, MAX_THREADS).defaultValue("1").required();
		builder.property("logOutputFolder", LOG_OUTPUT_FOLDER, "Output folder where standard out log file and error out log files are stored.<p>If you do not enter any value, the setting per daemon will be used.");
		propertyList = builder.end();

		add(propertyList);
		setModel(model);
	}

	public void fireValidations() {
		propertyList.fireValidations();
	}

	public ResourceModel getModel() {
		final HashMap<String, String> properties = propertyList.saveUI();

		resourceModel.setProperties(properties);
		return resourceModel;
	}

	public void setModel(final ResourceModel model) {
		resourceModel = model;

		// Clean up potentially missing log folder
		if (model.getProperty("logOutputFolder") == null) {
			model.setProperty("logOutputFolder", "");
		}

		propertyList.loadUI(resourceModel.getProperties());
	}
}
