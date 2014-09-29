package edu.mayo.mprc.swift.ui.client.widgets.validation;

import edu.mayo.mprc.swift.ui.client.rpc.ClientString;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValue;

import java.util.List;

/**
 * @author Roman Zenka
 */
public final class TitleSuffixTextBox extends ValidatableTextBox {
	public TitleSuffixTextBox(String param) {
		super(param);
	}

	@Override
	protected ClientValue getValueFromString(String value) {
		return new ClientString(value);
	}

	@Override
	protected String setValueAsString(ClientValue object) {
		if (object instanceof ClientString) {
			return ((ClientString) object).getValue();
		}
		return "";
	}

	@Override
	public boolean needsAllowedValues() {
		return false;
	}

	@Override
	public void setAllowedValues(List<? extends ClientValue> values) {
	}
}
