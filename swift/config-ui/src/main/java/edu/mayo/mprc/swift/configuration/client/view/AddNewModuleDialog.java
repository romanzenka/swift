package edu.mayo.mprc.swift.configuration.client.view;

import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.swift.configuration.client.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialog for adding a new module. Lets the user pick one of all the daemons where the module can go.
 */
public final class AddNewModuleDialog extends DialogBox {
	private final Map<DaemonModel, RadioButton> daemonRadios = new HashMap<DaemonModel, RadioButton>(3);
	private final Map<String, RadioButton> typeRadios = new HashMap<String, RadioButton>(3);
	private final Button okButton = new Button("Ok");
	private final Button cancelButton = new Button("Cancel");
	private final Context errorDisplay;

	public AddNewModuleDialog(final ApplicationModel model, final List<String> types, final NewModuleCreatedCallback callback, final Context errorDisplay) {
		super(false, true);
		this.errorDisplay = errorDisplay;
		setTitle("Add new module");

		final Panel radioPanel = new HorizontalPanel();

		{
			final Panel daemonPanel;
			daemonPanel = new VerticalPanel();
			boolean wasChecked = false;
			for (final DaemonModel daemonModel : model.getDaemons()) {
				final RadioButton radio = new RadioButton("daemonRadioGroup", "Add to " + daemonModel.getName() + " daemon.");
				if (!wasChecked) {
					radio.setChecked(true);
					wasChecked = true;
				}
				daemonPanel.add(radio);
				daemonRadios.put(daemonModel, radio);
			}

			if (daemonRadios.size() > 1) {
				radioPanel.add(daemonPanel);
			}
		}

		{
			final Panel typePanel = new VerticalPanel();
			boolean wasChecked = false;
			for (final String type : types) {
				if (!UiBuilderClient.NONE_TYPE.equals(type)) {
					final RadioButton radio = new RadioButton("typeRadioGroup", "Create " + type + " module.");
					if (!wasChecked) {
						radio.setChecked(true);
						wasChecked = true;
					}
					typePanel.add(radio);
					typeRadios.put(type, radio);
				}
			}

			if (typeRadios.size() > 1) {
				radioPanel.add(typePanel);
			}
		}

		// If the user has no choice, just do the job
		if (skipDisplay()) {
			okClicked(callback);
			return;
		}

		final Panel buttonPanel = new HorizontalPanel();
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);

		okButton.addClickListener(new ClickListener() {
			@Override
			public void onClick(final Widget widget) {
				okClicked(callback);
			}
		});

		cancelButton.addClickListener(new ClickListener() {
			@Override
			public void onClick(final Widget widget) {
				hide();
			}
		});

		final Panel allPanel = new VerticalPanel();
		allPanel.add(radioPanel);
		allPanel.add(buttonPanel);
		add(allPanel);
	}

	/**
	 * @return true if there is no point displaying this dialog.
	 */
	public boolean skipDisplay() {
		return typeRadios.size() <= 1 && daemonRadios.size() <= 1;
	}

	private void okClicked(final NewModuleCreatedCallback callback) {
		DaemonModel modelToAddTo = null;
		for (final Map.Entry<DaemonModel, RadioButton> entry : daemonRadios.entrySet()) {
			if (entry.getValue().isChecked()) {
				modelToAddTo = entry.getKey();
				break;
			}
		}
		assert modelToAddTo != null;

		String type = null;
		for (final Map.Entry<String, RadioButton> entry : typeRadios.entrySet()) {
			if (entry.getValue().isChecked()) {
				type = entry.getKey();
				break;
			}
		}
		assert type != null;

		modelToAddTo.addNewResource(type, callback, errorDisplay);

		hide();
	}
}
