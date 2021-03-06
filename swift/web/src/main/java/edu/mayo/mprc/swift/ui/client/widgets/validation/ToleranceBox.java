package edu.mayo.mprc.swift.ui.client.widgets.validation;

import edu.mayo.mprc.swift.ui.client.rpc.ClientTolerance;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValue;

import java.util.List;

/**
 * Displays/edits a Tolerance.
 */
public final class ToleranceBox extends ValidatableTextBox {

	public ToleranceBox(final String param) {
		super(param);
		setVisibleLength(8);
	}

	@Override
	protected ClientValue getValueFromString(final String value) {
		if ((value == null) || (value.isEmpty())) {
			return null;
		}
		return new ClientTolerance(value);

	}

	@Override
	protected String setValueAsString(final ClientValue object) {
		final ClientTolerance du = (ClientTolerance) object;
		return du.getValue();
	}

	public void updateIfHasFocus() {
		// ignore.
	}

	@Override
	public void setAllowedValues(final List<? extends ClientValue> values) {
		// ignore.
	}

	@Override
	public boolean needsAllowedValues() {
		return false;
	}
}
