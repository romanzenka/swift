package edu.mayo.mprc.swift.ui.client.widgets.validation;

import edu.mayo.mprc.swift.ui.client.rpc.ClientInstrument;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValue;

import java.util.Collections;
import java.util.List;

/**
 * Display a list of {@link edu.mayo.mprc.swift.ui.client.rpc.ClientSequenceDatabase} objects.
 */
public final class InstrumentListBox extends ValidatableListBox {
	private static final List<ClientValue> EMPTY_VALUE = Collections.emptyList();

	public InstrumentListBox(final String param) {
		super(param, false);
	}

	public String getStringValue(final ClientValue value) {
		if (value == null) {
			return "";
		}
		if (!(value instanceof ClientInstrument)) {
			throw new RuntimeException("Expected a ClientInstrument");
		}
		final ClientInstrument csd = (ClientInstrument) value;
		return csd.getName();
	}

	public ClientValue bundle(final List<? extends ClientValue> selected) {
		return null;//unused
	}

	public List<? extends ClientValue> unbundle(final ClientValue value) {
		return EMPTY_VALUE; // unused
	}

	/**
	 * We only fetch the list of instruments once, because it never changes.
	 *
	 * @return null if we do not want new list of allowed values (instruments).
	 */
	public boolean needsAllowedValues() {
		return allowedValues == null || allowedValues.isEmpty();
	}
}