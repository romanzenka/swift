package edu.mayo.mprc.swift.configuration.client.view;

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.mayo.mprc.swift.configuration.client.model.Context;
import edu.mayo.mprc.swift.configuration.client.model.DaemonModel;

import java.util.HashMap;
import java.util.Map;

public final class DaemonView extends SimplePanel implements ModuleView {
	private PropertyList propertyList;

	private static final String DAEMON_NAME = "daemonName";

	private DaemonModel daemonModel;

	public DaemonView() {
	}

	public void initializeUi(final DaemonModel daemonModel, final Context context) {
		this.daemonModel = daemonModel;

		final GwtUiBuilder builder = new GwtUiBuilder(context, daemonModel);
		builder.start();

		final PropertyChangeListener listener = new PropertyChangeListener();
		builder.property(DAEMON_NAME, "Daemon Name", "Descriptive name used when referring to this daemon.").required()
				.addEventListener(listener)
				.property(DaemonModel.HOST_NAME, "Host Name", "Host system network name or ip address.").required()
				.addEventListener(listener)
				.property(DaemonModel.OS_NAME, "Operating System", "Host system operating system name, for example, Windows or Linux.")
				.addEventListener(listener)
				.property(DaemonModel.OS_ARCH, "Architecture", "Host system architecture, for example, <tt>x86</tt>, <tt>x86_64</tt>, .")
				.addEventListener(listener)
				.property(DaemonModel.SHARED_FILE_SPACE_PATH, "Shared File Space", "" +
						"<p>" +
						"If you plan to run only one daemon, leave this value empty.")
				.property(DaemonModel.TEMP_FOLDER_PATH, "Temporary Folder", "Temporary folder that can be used for caching. Transferred files from other daemons with no shared file system " +
						"with this daemon are cached to this folder. " +
						"<p>" +
						"If daemon runs any of its modules in a SGE setting, this temporary folder most be accessible from all SGE nodes.").defaultValue("var/tmp")
				.property(DaemonModel.LOG_OUTPUT_FOLDER, "Log Folder", "A folder where all the services can write their logs." +
						"<p>" +
						"If daemon runs any of its modules in a SGE setting, the log folder most be accessible from all SGE nodes.").defaultValue("var/log")
				.property(DaemonModel.DUMP_ERRORS, "Dump errors", "When an error happens, the daemon will dump the original request information that failed. This allows the user to isolate and debug the particular error.")
				.boolValue().defaultValue("false")
				.property(DaemonModel.DUMP_FOLDER_PATH, "Dump folder path", "Where should the dump files be stored.").defaultValue("var/tmp/dumps");

		setWidget(propertyList = builder.end());

		loadUI(daemonModel);
	}

	@Override
	public Widget getModuleWidget() {
		return this;
	}

	/**
	 * Loads UI values from properties values in the daemon model.
	 *
	 * @param daemonModel
	 */
	public void loadUI(final DaemonModel daemonModel) {
		this.daemonModel = daemonModel;

		final HashMap<String, String> values = new HashMap(daemonModel.getProperties());
		values.put(DAEMON_NAME, daemonModel.getName());
		propertyList.loadUI(values);
	}

	@Override
	public void loadUI(final Map<String, String> values) {
		propertyList.loadUI(values);
	}

	@Override
	public HashMap<String, String> saveUI() {
		final HashMap<String, String> values = propertyList.saveUI();
		setDaemonModelProperties(values);
		return values;
	}

	private void setDaemonModelProperties(final HashMap<String, String> values) {
		daemonModel.setName(values.get(DAEMON_NAME));
		daemonModel.setProperties(values);
	}

	private class PropertyChangeListener implements ChangeListener {
		@Override
		public void onChange(final Widget widget) {
			saveUI();
		}
	}
}
