package edu.mayo.mprc.swift.configuration.client.view;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.TabPanel;
import edu.mayo.mprc.swift.configuration.client.ConfigurationService;
import edu.mayo.mprc.swift.configuration.client.model.ModuleModel;
import edu.mayo.mprc.swift.configuration.client.model.ResourceModel;

import java.util.HashMap;
import java.util.Map;

public final class RunnerView extends SimplePanel {
	private TabPanel runnerType;
	private LocalRunnerView localRunnerView;
	private SgeRunnerView sgeRunnerView;

	public RunnerView(final Context context, final ResourceModel model) {
		runnerType = new TabPanel();
		runnerType.addTabListener(new TabListener() {
			@Override
			public boolean onBeforeTabSelected(final SourcesTabEvents sender, final int tabIndex) {
				return true;
			}

			@Override
			public void onTabSelected(final SourcesTabEvents sender, final int tabIndex) {
				final String newType = runnerType.getTabBar().getSelectedTab() == 0 ? "localRunner" : "sgeRunner";
				if (newType.equals(model.getType())) {
					return;
				}

				// TODO: This is a hack, clean this up with proper model-view-controller
				final ModuleModel module = (ModuleModel) model.getParent();
				ConfigurationService.App.getInstance().changeRunner(module.getService().getId(), newType,
						new AsyncCallback<Void>() {
							@Override
							public void onFailure(final Throwable caught) {
							}

							@Override
							public void onSuccess(final Void result) {
							}
						});
				model.setType(newType);
				model.setName(newType);

				if ("localRunner".equals(newType)) {
					updateProperties(model, localRunnerView.getModel().getProperties());
					localRunnerView.fireValidations();
				} else {
					updateProperties(model, sgeRunnerView.getModel().getProperties());
					sgeRunnerView.fireValidations();
				}
			}
		});
		localRunnerView = new LocalRunnerView(context, model);
		runnerType.add(localRunnerView, "Run locally");
		sgeRunnerView = new SgeRunnerView(context, model);
		runnerType.add(sgeRunnerView, "Run in grid");
		this.add(runnerType);

		setModel(model);
	}

	private void updateProperties(final ResourceModel model, final HashMap<String, String> properties) {
		for (final Map.Entry<String, String> entry : properties.entrySet()) {
			model.setProperty(entry.getKey(), entry.getValue());
			model.firePropertyChange(entry.getKey(), entry.getValue());
		}
	}

	public void setModel(final ResourceModel model) {
		localRunnerView.setModel(model);
		sgeRunnerView.setModel(model);

		if ("localRunner".equals(model.getType())) {
			runnerType.selectTab(0);
		} else if ("sgeRunner".equals(model.getType())) {
			runnerType.selectTab(1);
		} else {
			throw new RuntimeException("Unsupported runner " + model.getClass().getName());
		}
	}
}
