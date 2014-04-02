package edu.mayo.mprc.swift.ui.client.widgets.validation;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.common.client.ExceptionUtilities;
import edu.mayo.mprc.swift.ui.client.dialogs.Validatable;
import edu.mayo.mprc.swift.ui.client.rpc.ClientScaffoldSettings;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValue;
import edu.mayo.mprc.swift.ui.client.widgets.ValidatedDoubleTextBox;
import edu.mayo.mprc.swift.ui.client.widgets.ValidatedIntegerTextBox;

import java.util.List;

public final class ScaffoldSettingsEditor extends Composite implements Validatable, ClickHandler, ChangeHandler {
	private ClientScaffoldSettings scaffoldSettings;
	private final HorizontalPanel panel;
	private final ValidatedDoubleTextBox proteinProbability;
	private final ValidatedIntegerTextBox minPeptideCount;
	private final ValidatedDoubleTextBox peptideProbability;

	private final Label minNTTLabel;
	private final ValidatedIntegerTextBox minNTT;

	private final CheckBox starredCheckbox;
	private final Anchor starEditor;
	private final StarredProteinsDialog starredDialog;

	private final CheckBox goAnnotations;
	private final Label saveSpectraLabel;
	private final ListBox saveSpectra;
	private final CheckBox useIndependentSampleGrouping;
	private final CheckBox useFamilyProteinGrouping;
	private final CheckBox mzIdentMlReport;

	public ScaffoldSettingsEditor() {
		panel = new HorizontalPanel();
		panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

		proteinProbability = new ValidatedDoubleTextBox(0, 100, 95);
		proteinProbability.setVisibleLength(5);
		proteinProbability.addChangeHandler(this);
		panel.add(proteinProbability);

		minPeptideCount = new ValidatedIntegerTextBox(1, 100, 2);
		minPeptideCount.setVisibleLength(2);
		minPeptideCount.addChangeHandler(this);
		panel.add(minPeptideCount);

		peptideProbability = new ValidatedDoubleTextBox(0, 100, 95);
		peptideProbability.setVisibleLength(5);
		peptideProbability.addChangeHandler(this);
		panel.add(peptideProbability);

		minNTTLabel = new Label("NTT>=");
		minNTTLabel.setStyleName("scaffold-setting-group");
		panel.add(minNTTLabel);
		minNTT = new ValidatedIntegerTextBox(0, 2, 1);
		minNTT.setVisibleLength(2);
		minNTT.addChangeHandler(this);
		panel.add(minNTT);

		starredCheckbox = new CheckBox("Stars");
		starredCheckbox.setStyleName("scaffold-setting-group");
		starredCheckbox.addClickHandler(this);
		panel.add(starredCheckbox);

		starEditor = new Anchor("Edit");
		starEditor.addClickHandler(this);
		panel.add(starEditor);

		goAnnotations = new CheckBox("GO Annotations");
		goAnnotations.setStyleName("scaffold-setting-group");
		goAnnotations.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				updateAndFireChange();
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
		saveSpectra.addChangeHandler(this);
		panel.add(saveSpectra);

		useIndependentSampleGrouping = new CheckBox("Independent Samples");
		useIndependentSampleGrouping.setTitle("Samples will be reported as if they were processed in independent Scaffold runs");
		useIndependentSampleGrouping.setStyleName("scaffold-setting-group");
		useIndependentSampleGrouping.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				updateAndFireChange();
			}
		});
		panel.add(useIndependentSampleGrouping);

		useFamilyProteinGrouping = new CheckBox("Protein Families");
		useFamilyProteinGrouping.setTitle("Scaffold will group proteins into families. New in Scaffold 4");
		useFamilyProteinGrouping.setStyleName("scaffold-setting-group");
		useFamilyProteinGrouping.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				updateAndFireChange();
			}
		});
		panel.add(useFamilyProteinGrouping);

		mzIdentMlReport = new CheckBox("mzIdentML");
		mzIdentMlReport.setTitle("Scaffold will report mzIdentML for perSPECtvies");
		mzIdentMlReport.setStyleName("scaffold-setting-group");
		mzIdentMlReport.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				updateAndFireChange();
			}
		});
		panel.add(mzIdentMlReport);

		starredDialog = new StarredProteinsDialog();
		starredDialog.setOkListener(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				updateAndFireChange();
			}
		});

		initWidget(panel);
	}

	@Override
	public ClientValue getValue() {
		return scaffoldSettings;
	}

	@Override
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
		starredCheckbox.setValue(scaffoldSettings.getStarredProteins() != null);
		goAnnotations.setValue(scaffoldSettings.isAnnotateWithGOA());
		saveSpectra.setSelectedIndex(scaffoldSettings.isSaveNoSpectra() ? 2 : (scaffoldSettings.isSaveOnlyIdentifiedSpectra() ? 1 : 0));
		starredDialog.setValue(scaffoldSettings);
		useIndependentSampleGrouping.setValue(scaffoldSettings.isUseIndependentSampleGrouping());
		useFamilyProteinGrouping.setValue(scaffoldSettings.isUseFamilyProteinGrouping());
	}

	@Override
	public void setValue(final ClientValue value, final boolean fireEvents) {
		ClientValueUtils.setValue(this, value, fireEvents);
	}

	@Override
	public void focus() {
		proteinProbability.setFocus(true);
	}

	@Override
	public void setValidationSeverity(final int validationSeverity) {
		ValidationController.setValidationSeverity(validationSeverity, this);
	}

	@Override
	public boolean needsAllowedValues() {
		return false;
	}

	@Override
	public void setAllowedValues(final List<? extends ClientValue> values) {
		// ignore
	}

	@Override
	public void setEnabled(final boolean enabled) {
		proteinProbability.setEnabled(enabled);
		minPeptideCount.setEnabled(enabled);
		peptideProbability.setEnabled(enabled);
		minNTT.setEnabled(enabled);
		starredCheckbox.setEnabled(enabled);
		goAnnotations.setEnabled(enabled);
		useFamilyProteinGrouping.setEnabled(enabled);
		useIndependentSampleGrouping.setEnabled(enabled);
		saveSpectra.setEnabled(enabled);
	}

	@Override
	public void onChange(final ChangeEvent event) {
		updateAndFireChange();
	}

	private void updateAndFireChange() {
		scaffoldSettings.setProteinProbability(proteinProbability.getDoubleValue() / 100.0);
		scaffoldSettings.setMinimumPeptideCount(minPeptideCount.getIntegerValue());
		scaffoldSettings.setPeptideProbability(peptideProbability.getDoubleValue() / 100.0);
		scaffoldSettings.setMinimumNonTrypticTerminii(minNTT.getIntegerValue());
		scaffoldSettings.setAnnotateWithGOA(goAnnotations.getValue());
		scaffoldSettings.setConnectToNCBI(goAnnotations.getValue());
		scaffoldSettings.setSaveNoSpectra("none".equals(saveSpectra.getValue(saveSpectra.getSelectedIndex())));
		scaffoldSettings.setSaveOnlyIdentifiedSpectra("id".equals(saveSpectra.getValue(saveSpectra.getSelectedIndex())));
		starredCheckbox.setValue(scaffoldSettings.getStarredProteins() != null);
		scaffoldSettings.setUseIndependentSampleGrouping(useIndependentSampleGrouping.getValue());
		scaffoldSettings.setUseFamilyProteinGrouping(useFamilyProteinGrouping.getValue());
		scaffoldSettings.setMzIdentMlReport(mzIdentMlReport.getValue());
		fireChange();
	}

	private void fireChange() {
		ValueChangeEvent.fire(this, getValue());
	}

	@Override
	public void onClick(final ClickEvent event) {
		final Widget sender = (Widget) event.getSource();
		if (starredCheckbox.equals(sender)) {
			if (Boolean.TRUE.equals(starredCheckbox.getValue())) {
				scaffoldSettings.setStarredProteins(starredDialog.getLastValue());
			} else {
				scaffoldSettings.setStarredProteins(null);
			}
		} else if (starEditor.equals(sender)) {
			starredDialog.center();
			starredDialog.show();
		}
		updateAndFireChange();
	}

	@Override
	public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<ClientValue> handler) {
		return addHandler(handler, ValueChangeEvent.getType());
	}
}
