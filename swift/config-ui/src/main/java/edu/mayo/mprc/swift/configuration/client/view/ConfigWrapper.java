package edu.mayo.mprc.swift.configuration.client.view;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.swift.configuration.client.ConfigurationService;
import edu.mayo.mprc.swift.configuration.client.model.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Main panel holding all the configuration for all daemons and modules.
 */
public final class ConfigWrapper extends SimplePanel {
	private ApplicationModel model;
	private PushButton newDaemonButton;
	private Map<DaemonModel, DaemonUi> daemonMap = new HashMap<DaemonModel, DaemonUi>();
	private Tree configTree;
	private TreeItem daemonsItem;
	private SimplePanel uiPanel;
	private SimplePanel runnerPanel;
	private Context context;

	public ConfigWrapper(final Context context) {
		this.context = context;
		final FlowPanel mainPanel = new FlowPanel();
		configTree = new Tree();
		configTree.setStyleName("config-tree");

		final HorizontalPanel daemonsPanel = new HorizontalPanel();
		daemonsPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		daemonsPanel.add(new Label("Daemons"));

		newDaemonButton = new PushButton("Add new");
		newDaemonButton.addStyleName("tree-item-pusbutton");
		newDaemonButton.addClickListener(new ClickListener() {
			@Override
			public void onClick(final Widget widget) {
				ConfigurationService.App.getInstance().createChild(model.getId(), "daemon", new AsyncCallback<ResourceModel>() {
					@Override
					public void onFailure(final Throwable throwable) {
						context.displayErrorMessage("Cannot create new daemon", throwable);
					}

					@Override
					public void onSuccess(final ResourceModel model) {
						if (model instanceof DaemonModel) {
							ConfigWrapper.this.context.getApplicationModel().addDaemon((DaemonModel) model);
						} else {
							context.displayErrorMessage("Wrong type returned when adding new daemon");
						}
					}
				});
			}
		});
		daemonsPanel.add(newDaemonButton);

		daemonsItem = configTree.addItem(daemonsPanel);

		final HorizontalPanel treeAndItems = new HorizontalPanel();
		model = context.getApplicationModel();
		model.addListener(new MyApplicationModelListener());
		initFromData();

		configTree.addTreeListener(new TreeListener() {
			@Override
			public void onTreeItemSelected(final TreeItem item) {
				if (item != null) {
					if (isDaemonItem(item)) {
						uiPanel.setWidget((DaemonWrapper) item.getUserObject());
						runnerPanel.clear();
					} else if (isDaemonResource(item)) {
						// Parent of module is a daemon
						final DaemonWrapper wrapper = (DaemonWrapper) item.getParentItem().getUserObject();
						final ResourceModel resourceModel = (ResourceModel) item.getUserObject();
						uiPanel.setWidget(wrapper.getUiForResource(resourceModel));
						if (resourceModel instanceof ModuleModel) {
							runnerPanel.setWidget(wrapper.getRunnerUiForModule((ModuleModel) resourceModel));
						} else {
							runnerPanel.clear();
						}
					} else {
						uiPanel.clear();
						runnerPanel.clear();
					}
				}
			}

			@Override
			public void onTreeItemStateChanged(final TreeItem item) {
			}
		});

		uiPanel = new SimplePanel();
		runnerPanel = new SimplePanel();
		treeAndItems.add(configTree);
		treeAndItems.add(uiPanel);
		treeAndItems.add(runnerPanel);

		mainPanel.add(treeAndItems);
		setWidget(mainPanel);

		// Open the daemons item
		daemonsItem.setState(true);
	}

	private boolean isDaemonResource(final TreeItem item) {
		return item.getUserObject() instanceof ResourceModel;
	}

	private boolean isDaemonItem(final TreeItem item) {
		if (item.getParentItem() == null) {
			return false;
		}
		return item.getParentItem().equals(daemonsItem);
	}

	private void initFromData() {
		for (final DaemonModel daemon : model.getDaemons()) {
			addDaemonUi(daemon);
		}
	}

	private static final class DaemonUi {
		DaemonUi(final DaemonModel model, final TreeItem treeItem, final DaemonWrapper wrapper) {
			this.model = model;
			this.treeItem = treeItem;
			this.wrapper = wrapper;
		}

		public DaemonModel model;
		public TreeItem treeItem;
		public DaemonWrapper wrapper;
	}

	private class MyApplicationModelListener implements ResourceModelListener {

		@Override
		public void initialized(final ResourceModel model) {
		}

		@Override
		public void nameChanged(final ResourceModel model) {
		}

		@Override
		public void childAdded(final ResourceModel child, final ResourceModel addedTo) {
			addDaemonUi((DaemonModel) child);
		}

		@Override
		public void childRemoved(final ResourceModel child, final ResourceModel removedFrom) {
			final DaemonModel daemon = (DaemonModel) child;
			final DaemonUi ui = daemonMap.get(daemon);
			if (ui.treeItem.isSelected()) {
				uiPanel.clear();
				runnerPanel.clear();
			}

			ui.treeItem.remove();

			for (final ResourceModel module : daemon.getChildren()) {
				final ModuleWrapper forModule = ui.wrapper.getUiForResource(module);
				if (forModule.equals(uiPanel.getWidget())) {
					uiPanel.clear();
					runnerPanel.clear();
					return;
				}
			}
		}

		@Override
		public void propertyChanged(final ResourceModel model, final String propertyName, final String newValue) {
		}
	}

	private void addDaemonUi(final DaemonModel daemon) {
		final HorizontalPanel daemonItem = new HorizontalPanel();
		daemonItem.add(new Label(daemon.getName()));
		final DeleteButton removeDaemonButton = new DeleteButton("Do you really want to remove this daemon?");
		removeDaemonButton.addClickListener(new ClickListener() {
			@Override
			public void onClick(final Widget sender) {
				ConfigurationService.App.getInstance().removeChild(daemon.getId(), new AsyncCallback<Void>() {
					@Override
					public void onFailure(final Throwable throwable) {
						context.displayErrorMessage("Could not remove daemon " + daemon.getName(), throwable);
					}

					@Override
					public void onSuccess(final Void aVoid) {
						model.removeDaemon(daemon);
					}
				});
			}
		});
		daemonItem.add(removeDaemonButton);

		final TreeItem treeItem = new TreeItem(daemonItem);
		final DaemonWrapper wrapper = new DaemonWrapper(daemon, context);

		treeItem.setUserObject(wrapper);
		daemonsItem.addItem(treeItem);
		daemonMap.put(daemon, new DaemonUi(daemon, treeItem, wrapper));

		daemon.addListener(new MyDaemonModelListener(treeItem));

		for (final ResourceModel resource : daemon.getChildren()) {
			addTreeItemForResource(daemon, resource, treeItem);
		}

		treeItem.setState(true);
	}

	private class MyDaemonModelListener implements ResourceModelListener {

		private final TreeItem daemonTreeItem;

		MyDaemonModelListener(final TreeItem daemonTreeItem) {
			this.daemonTreeItem = daemonTreeItem;
		}

		@Override
		public void initialized(final ResourceModel model) {
		}

		@Override
		public void nameChanged(final ResourceModel model) {
			changeTreeItemLabel(daemonTreeItem, model.getName());
		}

		@Override
		public void childAdded(final ResourceModel child, final ResourceModel addedTo) {
			addTreeItemForResource((DaemonModel) addedTo, child, daemonTreeItem);
		}

		@Override
		public void childRemoved(final ResourceModel child, final ResourceModel removedFrom) {
			removeTreeItemForResource(child, daemonTreeItem);
			final ModuleWrapper removedModuleWrapper = daemonMap.get((DaemonModel) removedFrom).wrapper.getUiForResource(child);
			if (uiPanel.getWidget().equals(removedModuleWrapper)) {
				uiPanel.clear();
				runnerPanel.clear();
			}
			daemonsItem.getTree().setSelectedItem(daemonsItem);
		}

		@Override
		public void propertyChanged(final ResourceModel model, final String propertyName, final String newValue) {
		}
	}

	private void addTreeItemForResource(final DaemonModel daemon, final ResourceModel resource, final TreeItem daemonTreeItem) {
		final HorizontalPanel panel = new HorizontalPanel();
		panel.add(new Label(resource.getName()));
		final DeleteButton deleteButton = new DeleteButton("Do you really want to remove this module?");
		deleteButton.addClickListener(new ClickListener() {
			@Override
			public void onClick(final Widget sender) {
				ConfigurationService.App.getInstance().removeChild(resource.getId(), new AsyncCallback<Void>() {
					@Override
					public void onFailure(final Throwable throwable) {
						context.displayErrorMessage("Could not remove module " + resource.getName(), throwable);
					}

					@Override
					public void onSuccess(final Void aVoid) {
						daemon.removeChild(resource);
					}
				});
			}
		});
		panel.add(deleteButton);
		final TreeItem item = new TreeItem(panel);
		item.setState(true);
		item.setUserObject(resource);
		daemonTreeItem.addItem(item);
	}

	private void changeTreeItemLabel(final TreeItem treeItem, final String newLabel) {
		final HorizontalPanel itemPanel = (HorizontalPanel) treeItem.getWidget();
		((Label) itemPanel.getWidget(0)).setText(newLabel);
	}

	private void removeTreeItemForResource(final ResourceModel resource, final TreeItem daemonTreeItem) {
		for (int row = 0; row < daemonTreeItem.getChildCount(); row++) {
			final TreeItem child = daemonTreeItem.getChild(row);
			if (child.getUserObject().equals(resource)) {
				daemonTreeItem.removeItem(child);
				break;
			}
		}
	}

}

