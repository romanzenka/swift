package edu.mayo.mprc.swift.configuration.client.view;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.swift.configuration.client.model.*;

import java.util.*;

public final class DaemonWrapper extends SimplePanel {
	private final DaemonModel daemonModel;
	private final ListBox newModulePicker;
	private Button newModuleButton;
	private final AvailableModules availableModules;
	private Map<ResourceModel, ModuleConfigUis> uis = new HashMap<ResourceModel, ModuleConfigUis>();
	private DaemonView daemonConfigUI;
	private Context context;

	private static class InfoComparator implements Comparator<AvailableModules.Info> {
		@Override
		public int compare(AvailableModules.Info o1, AvailableModules.Info o2) {
			return o1.getName().compareToIgnoreCase(o2.getName());
		}
	}

	private class ModuleConfigUis {
		private ModuleConfigUis(final ModuleWrapper moduleWrapper, final RunnerView runner) {
			ui = moduleWrapper;
			this.runner = runner;
		}

		public ModuleWrapper ui;
		public RunnerView runner;
	}

	public DaemonWrapper(final DaemonModel daemonModel, final Context context) {
		this.context = context;
		addStyleName("module-wrapper");
		final FlowPanel panel = new FlowPanel();
		panel.addStyleName("module");
		final Label label = new Label("Daemon");
		label.addStyleName("module-label");
		panel.add(label);
		final HTML desc = new HTML("A daemon is a process that a provides portion of Swift functionality. " +
				"<ul><li>A daemon consists of multiple modules.</li>" +
				"<li>Communication between daemons is done using a message broker, " +
				"shared filesystem and for the UI and main searcher, also using a database. All these means of communication are available and configurable as modules.</li>" +
				"<li>Typically, you would like to have one daemon per physical machine, but that is not a requirement</li>" +
				"</ul>" +
				"<p>The properties below are mostly used for simplifying the configuration process. The only mandatory fields are daemon name and host name, in case you want to run multiple daemons, you must also set up the shared file space.</p>");
		desc.addStyleName("module-description");
		panel.add(desc);

		this.daemonModel = daemonModel;
		availableModules = context.getApplicationModel().getAvailableModules();

		this.daemonModel.addListener(new MyDaemonModelListener());

		newModulePicker = new ListBox(false);
		final Collection<AvailableModules.Info> moduleInfos = availableModules.getModuleInfos();
		final List<AvailableModules.Info> moduleInfoList = new ArrayList<AvailableModules.Info>(moduleInfos);
		Collections.sort(moduleInfoList, new InfoComparator());

		for (final AvailableModules.Info info : moduleInfoList) {
			// if (info.isModule()) {
			newModulePicker.addItem(info.getName(), info.getType());
			// }
		}

		newModuleButton = new Button("Add new module");
		newModuleButton.addStyleName("btn");
		newModuleButton.addStyleName("new-module-button");
		newModuleButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				final int index = newModulePicker.getSelectedIndex();
				final String type = newModulePicker.getValue(index);
				DaemonWrapper.this.daemonModel.addNewResource(type, null, context);
			}
		});

		daemonConfigUI = new DaemonView();
		daemonConfigUI.initializeUi(this.daemonModel, this.context);
		panel.add(daemonConfigUI.getWidget());

		final Panel moduleAdding = new HorizontalPanel();
		moduleAdding.add(newModulePicker);
		moduleAdding.add(newModuleButton);
		panel.add(moduleAdding);

		final ArrayList<ResourceModel> resources = new ArrayList<ResourceModel>(this.daemonModel.getChildren());
		for (final ResourceModel resource : resources) {
			addUiForResource(resource);
		}
		setWidget(panel);
	}

	public ModuleWrapper getUiForResource(final ResourceModel resourceModel) {
		final DaemonWrapper.ModuleConfigUis configUis = uis.get(resourceModel);
		return configUis == null ? null : configUis.ui;
	}

	public RunnerView getRunnerUiForModule(final ModuleModel moduleModel) {
		return uis.get(moduleModel).runner;
	}

	private class MyDaemonModelListener implements ResourceModelListener {
		@Override
		public void initialized(final ResourceModel model) {
		}

		@Override
		public void nameChanged(final ResourceModel model) {
		}

		@Override
		public void childAdded(final ResourceModel child, final ResourceModel addedTo) {
			addUiForResource(child);
		}

		@Override
		public void childRemoved(final ResourceModel child, final ResourceModel removedFrom) {
			removeUiForResource(child);
		}

		@Override
		public void propertyChanged(final ResourceModel model, final String propertyName, final String newValue) {
			if (model instanceof DaemonModel) {
				daemonConfigUI.loadUI((DaemonModel) model);
			} else {
				throw new RuntimeException("Programmer error: type mismatch");
			}
		}
	}

	/**
	 * The model is already in the list. Just add the ui.
	 */
	private void addUiForResource(final ResourceModel resource) {
		final ModuleWrapper ui = createNewModuleConfigUI(resource);
		ui.getModule().loadUI(resource.getProperties());

		RunnerView runner = null;
		if (resource instanceof ModuleModel) {
			runner = createNewRunnerUi(((ModuleModel) resource).getRunner());
		}

		final ModuleConfigUis t = new ModuleConfigUis(ui, runner);
		uis.put(resource, t);
	}

	private ModuleWrapper createNewModuleConfigUI(final ResourceModel module) {
		final String type = module.getType();
		final ModuleView ui = null;
		if ("database".equals(type)) {
			// Special case. Database UI is currently not supported by UiBuilder, so it is created manually
			final GwtUiBuilder builder = new GwtUiBuilder(context, module);

			final DatabaseView databaseView = new DatabaseView(builder, module);
			return new ModuleWrapper(availableModules.getModuleNameForType(type), databaseView, availableModules.getDescriptionForType(type));
		} else {
			final GwtUiBuilder builder = new GwtUiBuilder(context, module);
			builder.start();
			module.getReplayer().replay(builder);
			return new ModuleWrapper(availableModules.getModuleNameForType(type), builder.end(), availableModules.getDescriptionForType(type));
		}
	}

	private RunnerView createNewRunnerUi(final ResourceModel model) {
		final RunnerView runner = new RunnerView(context, model);
		runner.setStyleName("runner-wrapper");
		return runner;
	}

	private void removeUiForResource(final ResourceModel module) {
		for (final Map.Entry<ResourceModel, ModuleConfigUis> ui : uis.entrySet()) {
			if (ui.getKey().equals(module)) {
				uis.remove(ui.getKey());
				break;
			}
		}
	}

}
