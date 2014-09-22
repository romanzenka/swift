package edu.mayo.mprc.swift.configuration.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.swift.configuration.client.model.*;
import edu.mayo.mprc.swift.configuration.client.view.ConfigWrapper;

public final class Configuration implements EntryPoint, Context {

	private RootPanel configurationPanel;
	private RootPanel progressPanel;
	private RootPanel errorPanel;
	private VerticalPanel multiErrorPanel;
	private Button saveConfigurationButton;
	private ApplicationModel model = new ApplicationModel();
	private ConfigWrapper configWrapper;

	@Override
	public void onModuleLoad() {
		progressPanel = RootPanel.get("progress");

		if (progressPanel != null) {

			displayProgressMessage("Loading...");
			ConfigurationService.App.getInstance().loadConfiguration(new AsyncCallback<ApplicationModel>() {
				@Override
				public void onFailure(final Throwable throwable) {
					displayErrorMessage(throwable.getMessage());
				}

				@Override
				public void onSuccess(final ApplicationModel applicationModel) {
					displayProgressMessage(null);
					model = applicationModel;
					configWrapper = new ConfigWrapper(Configuration.this);
					configurationPanel.add(configWrapper);
					RootPanel.get("saveButtonPlaceholder").add(saveConfigurationButton);
				}

			});
			configurationPanel = RootPanel.get("main");
			errorPanel = RootPanel.get("error");
			errorPanel.add(multiErrorPanel = new VerticalPanel());

			saveConfigurationButton = new Button("Save configuration");
			saveConfigurationButton.addStyleName("btn");
			saveConfigurationButton.addStyleName("btn-primary");
			saveConfigurationButton.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(final ClickEvent event) {
					clearErrorMessages();
					displayProgressMessage("Saving...");
					save();
				}
			});

		}

		// displayProgressMessage("Initializing");
	}

	/**
	 * Hide all error messages
	 */
	private void clearErrorMessages() {
		multiErrorPanel.clear();
	}

	private void save() {
		ConfigurationService.App.getInstance().saveConfiguration(new UiChangesReplay());
	}

	private void displayProgressMessage(final String message) {
		if (message != null) {
			progressPanel.clear();
			progressPanel.add(new HTML(message));
			progressPanel.setVisible(true);
		} else {
			progressPanel.setVisible(false);
		}
	}

	@Override
	public ApplicationModel getApplicationModel() {
		return model;
	}

	@Override
	public void displayErrorMessage(final String message) {
		if (message != null) {
			final Panel panel = new FlowPanel();

			final HTML errorMessage = new HTML(message);
			errorMessage.addStyleName("error");
			panel.add(errorMessage);

			final Button clearErrorButton = new Button("Clear Error");
			clearErrorButton.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(final ClickEvent event) {
					multiErrorPanel.remove(panel);
				}
			});
			panel.add(clearErrorButton);

			multiErrorPanel.add(panel);
		}
	}

	@Override
	public void displayErrorMessage(final String message, final Throwable t) {
		displayErrorMessage(message + ": " + t.getMessage());
	}

	private class UiChangesReplay implements AsyncCallback<UiChangesReplayer> {
		@Override
		public void onFailure(final Throwable throwable) {
			displayProgressMessage(null);
			displayErrorMessage(throwable.getMessage());
		}

		@Override
		public void onSuccess(final UiChangesReplayer changes) {
			displayProgressMessage(null);
			final StringBuilder message = new StringBuilder();

			changes.replay(new UiChanges() {
				private static final long serialVersionUID = -5054481989230026680L;

				@Override
				public void setProperty(final String resourceId, final String propertyName, final String newValue) {
				}

				@Override
				public void displayPropertyError(final String resourceId, final String propertyName, final String error) {
					if (error == null) {
						message.setLength(0);
					} else {
						message.append("<li>")
								.append(error)
								.append("</li>");
					}
				}
			});
			if (message.length() > 0) {
				displayErrorMessage("<ul>" + message.toString() + "</ul>");
			}
		}
	}
}
