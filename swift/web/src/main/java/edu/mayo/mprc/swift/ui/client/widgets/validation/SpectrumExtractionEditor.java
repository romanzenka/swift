package edu.mayo.mprc.swift.ui.client.widgets.validation;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import edu.mayo.mprc.common.client.ExceptionUtilities;
import edu.mayo.mprc.swift.ui.client.dialogs.Validatable;
import edu.mayo.mprc.swift.ui.client.rpc.ClientExtractMsnSettings;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SpectrumExtractionEditor extends Composite implements Validatable, ValueChangeHandler<String>, ChangeHandler {
	private ClientExtractMsnSettings settings;
	private HorizontalPanel panel;
	private ListBox engineName;

	private final EnginePanel extractMsnPanel = new ExtractMsnEnginePanel();

	private final EnginePanel msconvertPanel = new MsconvertPanel();

	private Map<String, EnginePanel> engineToPanel = new HashMap<String, EnginePanel>(2);

	public SpectrumExtractionEditor() {
		panel = new HorizontalPanel();
		panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		engineName = new ListBox();
		engineName.addItem("extract_msn", "extract_msn");
		engineToPanel.put("extract_msn", extractMsnPanel);
		engineName.addItem("msconvert", "msconvert");
		engineToPanel.put("msconvert", msconvertPanel);

		engineName.setSelectedIndex(1);
		engineName.addChangeHandler(this);
		panel.add(engineName);

		msconvertPanel.setVisible(false);
		msconvertPanel.addValueChangeHandler(this);
		panel.add(msconvertPanel);

		extractMsnPanel.setVisible(false);
		extractMsnPanel.addValueChangeHandler(this);
		panel.add(extractMsnPanel);

		initWidget(panel);
	}

	@Override
	public ClientValue getValue() {
		if (settings == null) {
			return null;
		}
		// We filter the command-line switches if engine is not extract_msn
		return new ClientExtractMsnSettings(
				currentEnginePanel().getValue(),
				settings.getCommand());
	}

	@Override
	public void setValue(final ClientValue value) {
		if (!(value instanceof ClientExtractMsnSettings)) {
			ExceptionUtilities.throwCastException(value, ClientExtractMsnSettings.class);
			return;
		}
		settings = (ClientExtractMsnSettings) value;
		for (int i = 0; i < engineName.getItemCount(); i++) {
			if (engineName.getValue(i).equalsIgnoreCase(settings.getCommand())) {
				engineName.setSelectedIndex(i);
				break;
			}
		}
		updateInterface();
		// Only the current engine gets the value set. Everybody else resets their value
		for (EnginePanel enginePanel : engineToPanel.values()) {
			enginePanel.resetToDefault();
		}
		currentEnginePanel().setValue(settings.getCommandLineSwitches());
	}

	@Override
	public void setValue(final ClientValue value, final boolean fireEvents) {
		ClientValueUtils.setValue(this, value, fireEvents);
	}

	@Override
	public void focus() {
		currentEnginePanel().setFocus(true);
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
		// Not supported
	}

	@Override
	public void setEnabled(final boolean enabled) {
		for (final EnginePanel panel : engineToPanel.values()) {
			panel.setEnabled(enabled);
		}
		engineName.setEnabled(enabled);
	}

	private void updateSettings() {
		settings.setCommandLineSwitches(currentEnginePanel().getValue());
		final String engine = currentEngineName();
		settings.setCommand(engine);
		updateInterface();
	}

	private String currentEngineName() {
		return engineName.getValue(engineName.getSelectedIndex());
	}

	private void updateInterface() {
		enablePanel(currentEngineName());
	}

	private void enablePanel(final String engine) {
		for (final HorizontalPanel tohide : engineToPanel.values()) {
			tohide.setVisible(false);
		}
		enginePanel(engine).setVisible(true);
	}

	private EnginePanel currentEnginePanel() {
		return enginePanel(currentEngineName());
	}

	private EnginePanel enginePanel(final String engine) {
		return engineToPanel.get(engine);
	}

	@Override
	public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<ClientValue> handler) {
		return addHandler(handler, ValueChangeEvent.getType());
	}

	private void valueChanged() {
		updateSettings();
		updateInterface();
		ValueChangeEvent.fire(this, getValue());
	}

	@Override
	public void onValueChange(final ValueChangeEvent<String> event) {
		valueChanged();
	}

	@Override
	public void onChange(ChangeEvent event) {
		valueChanged();
	}
}
