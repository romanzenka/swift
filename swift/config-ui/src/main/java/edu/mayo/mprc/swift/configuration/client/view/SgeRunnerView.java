package edu.mayo.mprc.swift.configuration.client.view;

import com.google.gwt.user.client.ui.SimplePanel;
import edu.mayo.mprc.swift.configuration.client.model.ResourceModel;

import java.util.HashMap;

public final class SgeRunnerView extends SimplePanel {

	public static final String DEFAULT_WRAPPER_SCRIPT = "bin/util/sgeWrapper.sh";
	public static final String DEFAULT_SHARED_WORKING_FOLDER = ".";
	public static final String DEFAULT_QUEUE_NAME = "all.q";
	private PropertyList propertyList;
	private ResourceModel resourceModel;

	public static final String DEFAULT_SHARED_TEMP_FOLDER = "var/tmp";

	private static final String QUEUE_NAME = "queueName";
	private static final String MEMORY_REQUIREMENT = "memoryRequirement";
	private static final String NATIVE_SPECIFICATION = "nativeSpecification";
	private static final String SHARED_TEMP_DIRECTORY = "sharedTempDirectory";
	private static final String WRAPPER_SCRIPT = "wrapperScript";
	private static final String LOG_OUTPUT_FOLDER = "logOutputFolder";

	public SgeRunnerView(final Context context, final ResourceModel model) {
		final GwtUiBuilder builder = new GwtUiBuilder(context, model);

		builder.start()
				.property(QUEUE_NAME, "Queue Name", "SGE queue name.<p>There is usually a queue called <tt>all.q</tt> that sends requests to any queue available.").required().defaultValue(DEFAULT_QUEUE_NAME)
				.property(LOG_OUTPUT_FOLDER, "Shared Log Folder", "This is a shared folder within the SGE environment. Output folder where standard out log file and error out log files are stored. If not entered, the default per daemon will be used.").defaultValue("")
				.property(WRAPPER_SCRIPT, "Wrapper Script", "The command is executed through this script that servers as a wrapper. We typically use the wrapper to set umask or produce some log messages. Empty field means the command will be executed directly, with no wrapping.").defaultValue(DEFAULT_WRAPPER_SCRIPT)
				.property(NATIVE_SPECIFICATION, "Native Specification", "SGE native specification, for example, -p for running task in pvm.")
				.property(MEMORY_REQUIREMENT, "Memory Requirement", "SGE memory requirement for jobs running in this queue.<p>Integer, in MB.</p><p>Will set <tt>-l s_vmem=</tt> memory soft limit option for SGE.</p>");
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
		propertyList.loadUI(resourceModel.getProperties());
	}
}
