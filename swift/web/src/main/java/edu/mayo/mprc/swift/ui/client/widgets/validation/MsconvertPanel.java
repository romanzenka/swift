package edu.mayo.mprc.swift.ui.client.widgets.validation;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.CheckBox;

/**
 * @author Roman Zenka
 */
public final class MsconvertPanel extends EnginePanel {
	public static final String MZML_OPTION = "--mzML";
	private CheckBox mzML;

	public MsconvertPanel() {
		mzML = new CheckBox("mzML");
		add(mzML);
		mzML.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
			@Override
			public void onValueChange(final ValueChangeEvent<Boolean> event) {
				ValueChangeEvent.fire(MsconvertPanel.this, getValue());
			}
		});
	}

	@Override
	public void setEnabled(final boolean enabled) {
		mzML.setEnabled(enabled);
	}

	@Override
	public void setValue(final String value) {
		mzML.setValue(value.contains(MZML_OPTION));
	}

	@Override
	public String getValue() {
		return Boolean.TRUE.equals(mzML.getValue()) ? MZML_OPTION : "";
	}

	@Override
	public void setFocus(final boolean focused) {
		mzML.setFocus(focused);
	}

	@Override
	public void resetToDefault() {
		setValue("");
	}

	@Override
	public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<String> handler) {
		return addHandler(handler, ValueChangeEvent.getType());
	}
}
