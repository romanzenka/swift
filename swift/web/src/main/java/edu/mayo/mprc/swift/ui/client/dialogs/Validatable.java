package edu.mayo.mprc.swift.ui.client.dialogs;

import com.google.gwt.user.client.ui.SourcesChangeEvents;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValidation;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValue;

import java.util.List;

/**
 * Widgets which can get or set a single, serializable value.
 */
public interface Validatable extends SourcesChangeEvents {

	ClientValue getClientValue();

	void setValue(ClientValue value);

	/**
	 * Direct the user's focus towards this widget.
	 */
	void focus();

	/**
	 * Cause the widget to display its validation state to the user, by, for example,
	 * coloring it's background yellow if the state >= SEVERITY_WARNING.
	 *
	 * @param validationSeverity The maximum severity value from any ClientValidations for this widget.
	 * @see ClientValidation for list of possible severity values.
	 */
	void setValidationSeverity(int validationSeverity);

	/**
	 * @return True if the control wants allowed values.
	 */
	boolean needsAllowedValues();

	/**
	 * Set the list of allowed values, if any, that this Validatable widget should display.
	 * If not provided by the server-side AbstractParam, this will never be called.
	 */
	void setAllowedValues(List<? extends ClientValue> values);

	/**
	 * Set the enabled state of this widget.
	 */
	void setEnabled(boolean enabled);
}
