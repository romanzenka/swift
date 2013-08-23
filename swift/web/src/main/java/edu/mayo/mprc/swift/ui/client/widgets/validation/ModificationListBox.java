package edu.mayo.mprc.swift.ui.client.widgets.validation;

import edu.mayo.mprc.swift.ui.client.rpc.ClientModSpecificity;
import edu.mayo.mprc.swift.ui.client.rpc.ClientModSpecificitySet;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows multiple-selection of modifications.
 */
public final class ModificationListBox extends ValidatableListBox {
	public ModificationListBox(final String param, final boolean allowMultiple) {
		super(param, allowMultiple);
	}

	@Override
	public ClientValue bundle(final List<? extends ClientValue> selected) {
		final List<ClientModSpecificity> specs = new ArrayList<ClientModSpecificity>(selected.size());
		for (final ClientValue value : selected) {
			specs.add(ClientModSpecificity.cast(value));
		}
		return new ClientModSpecificitySet(specs);
	}

	@Override
	public String getStringValue(final ClientValue value) {
		return ClientModSpecificity.cast(value).getName();
	}

	@Override
	public List<? extends ClientValue> unbundle(final ClientValue value) {
		return ClientModSpecificitySet.cast(value).getModSpecificities();
	}

	/**
	 * @return We have our own mechanism of fetching allowed values, because there are many modifications and they never change.
	 *         We never request update of allowed values.
	 */
	@Override
	public boolean needsAllowedValues() {
		return false;
	}
}
