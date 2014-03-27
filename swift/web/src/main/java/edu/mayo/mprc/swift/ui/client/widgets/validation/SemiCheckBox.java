package edu.mayo.mprc.swift.ui.client.widgets.validation;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import edu.mayo.mprc.swift.ui.client.dialogs.Validatable;
import edu.mayo.mprc.swift.ui.client.rpc.ClientInteger;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValue;

import java.util.List;

/**
 * A checkbox allowing semi-tryptic searches (minTerminiCleavages == 1 instead of 2)
 *
 * @author Roman Zenka
 */
public final class SemiCheckBox implements Validatable, IsWidget {

	private CheckBox checkBox;

	public SemiCheckBox() {
		checkBox = new CheckBox("Semi");
	}

	@Override
	public ClientValue getValue() {
		return null;  // TODO: Implement this method
	}

	@Override
	public void setValue(final ClientValue value) {
		if (value instanceof ClientInteger) {
			checkBox.setValue(1 == ((ClientInteger) value).getValue());
		} else {
			checkBox.setValue(false);
		}
	}

	@Override
	public void setValue(final ClientValue value, final boolean fireEvents) {
		ClientValueUtils.setValue(this, value, fireEvents);
	}

	@Override
	public void focus() {
		checkBox.setFocus(true);
	}

	@Override
	public void setValidationSeverity(final int validationSeverity) {
		ValidationController.setValidationSeverity(validationSeverity, asWidget());
	}

	@Override
	public boolean needsAllowedValues() {
		return false;
	}

	@Override
	public void setAllowedValues(final List<? extends ClientValue> values) {

	}

	@Override
	public void setEnabled(final boolean enabled) {
		checkBox.setEnabled(enabled);
	}

	@Override
	public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<ClientValue> handler) {
		return checkBox.addHandler(handler, ValueChangeEvent.getType());
	}

	@Override
	public void fireEvent(final GwtEvent<?> event) {
		checkBox.fireEvent(event);
	}

	@Override
	public Widget asWidget() {
		return checkBox;
	}
}
