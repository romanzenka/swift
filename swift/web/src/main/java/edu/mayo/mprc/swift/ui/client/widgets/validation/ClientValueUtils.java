package edu.mayo.mprc.swift.ui.client.widgets.validation;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import edu.mayo.mprc.swift.ui.client.dialogs.Validatable;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValue;

/**
 * @author Roman Zenka
 */
public final class ClientValueUtils {
	static boolean valuesNotIdentical(final ClientValue value, final ClientValue currentValue) {
		if (currentValue == null) {
			if (value != null) {
				return true;
			}
		} else {
			if (!currentValue.equals(value)) {
				return true;
			}
		}
		return false;
	}

	public static void setValue(final Validatable validatable, final ClientValue value, final boolean fireEvents) {
		if (valuesNotIdentical(value, validatable.getValue())) {
			validatable.setValue(value);
			if (fireEvents) {
				ValueChangeEvent.fire(validatable, value);
			}
		}
	}
}
