package edu.mayo.mprc.swift.ui.client.widgets.validation;

import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.common.client.ExceptionUtilities;
import edu.mayo.mprc.swift.ui.client.rpc.ClientScaffoldSettings;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValue;
import edu.mayo.mprc.swift.ui.client.widgets.ValidatedDoubleTextBox;
import edu.mayo.mprc.swift.ui.client.widgets.ValidatedIntegerTextBox;

import java.util.List;

public final class ScaffoldSettingsEditor extends Composite implements Validatable, ChangeListener, ClickListener {
	private ClientScaffoldSettings scaffoldSettings;
	private ChangeListenerCollection changeListenerCollection = new ChangeListenerCollection();
	private HorizontalPanel panel;
	private ValidatedDoubleTextBox proteinProbability;
	private ValidatedIntegerTextBox minPeptideCount;
	private ValidatedDoubleTextBox peptideProbability;

	private Label minNTTLabel;
	private ValidatedIntegerTextBox minNTT;

	private CheckBox starredCheckbox;
	private Anchor starEditor;
	private StarredProteinsDialog starredDialog;

	private CheckBox goAnnotations;
	private Label saveSpectraLabel;
	private ListBox saveSpectra;

	public ScaffoldSettingsEditor() {
		panel = new HorizontalPanel();
		panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

		proteinProbability = new ValidatedDoubleTextBox(0, 100, 95);
		proteinProbability.setVisibleLength(5);
		proteinProbability.addChangeListener(this);
		panel.add(proteinProbability);

		minPeptideCount = new ValidatedIntegerTextBox(1, 100, 2);
		minPeptideCount.setVisibleLength(2);
		minPeptideCount.addChangeListener(this);
		panel.add(minPeptideCount);

		peptideProbability = new ValidatedDoubleTextBox(0, 100, 95);
		peptideProbability.setVisibleLength(5);
		peptideProbability.addChangeListener(this);
		panel.add(peptideProbability);

		minNTTLabel = new Label("NTT>=");
		minNTTLabel.setStyleName("scaffold-setting-group");
		panel.add(minNTTLabel);
		minNTT = new ValidatedIntegerTextBox(0, 2, 1);
		minNTT.setVisibleLength(2);
		minNTT.addChangeListener(this);
		panel.add(minNTT);

		starredCheckbox = new CheckBox("Stars");
		starredCheckbox.setStyleName("scaffold-setting-group");
		starredCheckbox.addClickListener(this);
		panel.add(starredCheckbox);

		starEditor = new Anchor("Edit");
		starEditor.addClickListener(this);
		panel.add(starEditor);

		goAnnotations = new CheckBox("GO Annotations");
		goAnnotations.setStyleName("scaffold-setting-group");
		goAnnotations.addClickListener(new ClickListener() {
			public void onClick(final Widget sender) {
				onChange(sender);
			}
		});
		panel.add(goAnnotations);

		saveSpectraLabel = new Label("Save spectra:", false);
		saveSpectraLabel.addStyleName("scaffold-setting-group");
		panel.add(saveSpectraLabel);
		saveSpectra = new ListBox();
		saveSpectra.addItem("All", "all");
		saveSpectra.addItem("Identified", "id");
		saveSpectra.addItem("None", "none");
		saveSpectra.addChangeListener(this);
		panel.add(saveSpectra);

		starredDialog = new StarredProteinsDialog();
		starredDialog.setOkListener(new ClickListener() {
			public void onClick(final Widget sender) {
				onChange(sender);
			}
		});

		this.initWidget(panel);
	}

	public ClientValue getClientValue() {
		return scaffoldSettings;
	}

	public void setValue(final ClientValue value) {
		if (!(value instanceof ClientScaffoldSettings)) {
			ExceptionUtilities.throwCastException(value, ClientScaffoldSettings.class);
			return;
		}
		scaffoldSettings = (ClientScaffoldSettings) value;
		proteinProbability.setText(String.valueOf(scaffoldSettings.getProteinProbability() * 100.0));
		minPeptideCount.setText(String.valueOf(scaffoldSettings.getMinimumPeptideCount()));
		peptideProbability.setText(String.valueOf(scaffoldSettings.getPeptideProbability() * 100.0));
		minNTT.setText(String.valueOf(scaffoldSettings.getMinimumNonTrypticTerminii()));
		starredCheckbox.setChecked(scaffoldSettings.getStarredProteins() != null);
		goAnnotations.setChecked(scaffoldSettings.isAnnotateWithGOA());
		saveSpectra.setSelectedIndex(scaffoldSettings.isSaveNoSpectra() ? 2 : (scaffoldSettings.isSaveOnlyIdentifiedSpectra() ? 1 : 0));
		starredDialog.setValue(scaffoldSettings);
	}

	public void focus() {
		proteinProbability.setFocus(true);
	}

	public void setValidationSeverity(final int validationSeverity) {
		ValidationController.setValidationSeverity(validationSeverity, this);
	}

	public boolean needsAllowedValues() {
		return false;
	}

	public void setAllowedValues(final List<? extends ClientValue> values) {
		// ignore
	}

	public void setEnabled(final boolean enabled) {
		proteinProbability.setEnabled(enabled);
		minPeptideCount.setEnabled(enabled);
		peptideProbability.setEnabled(enabled);
		minNTT.setEnabled(enabled);
		starredCheckbox.setEnabled(enabled);
		goAnnotations.setEnabled(enabled);
		saveSpectra.setEnabled(enabled);
	}

	public void addChangeListener(final ChangeListener changeListener) {
		changeListenerCollection.add(changeListener);
	}

	public void removeChangeListener(final ChangeListener changeListener) {
		changeListenerCollection.remove(changeListener);
	}

	public void onChange(final Widget widget) {
		scaffoldSettings.setProteinProbability(proteinProbability.getDoubleValue() / 100.0);
		scaffoldSettings.setMinimumPeptideCount(minPeptideCount.getIntegerValue());
		scaffoldSettings.setPeptideProbability(peptideProbability.getDoubleValue() / 100.0);
		scaffoldSettings.setMinimumNonTrypticTerminii(minNTT.getIntegerValue());
		scaffoldSettings.setAnnotateWithGOA(goAnnotations.isChecked());
		scaffoldSettings.setConnectToNCBI(goAnnotations.isChecked());
		scaffoldSettings.setSaveNoSpectra("none".equals(saveSpectra.getValue(saveSpectra.getSelectedIndex())));
		scaffoldSettings.setSaveOnlyIdentifiedSpectra("id".equals(saveSpectra.getValue(saveSpectra.getSelectedIndex())));
		starredCheckbox.setChecked(scaffoldSettings.getStarredProteins() != null);
		fireChange();
	}

	private void fireChange() {
		changeListenerCollection.fireChange(this);
	}

	public void onClick(final Widget sender) {
		if (starredCheckbox.equals(sender)) {
			if (starredCheckbox.isChecked()) {
				scaffoldSettings.setStarredProteins(starredDialog.getLastValue());
			} else {
				scaffoldSettings.setStarredProteins(null);
			}
		} else if (starEditor.equals(sender)) {
			starredDialog.center();
			starredDialog.show();
		}
		onChange(sender);
	}
}
