package edu.mayo.mprc.swift.ui.client.widgets.validation;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ChangeListenerCollection;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import edu.mayo.mprc.swift.ui.client.dialogs.Validatable;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValue;

/**
 * An abstract check box that can validate the users's input server side in conjuction with a ValidationController
 */
public abstract class ValidatableTextBox extends TextBox implements Validatable {
	private ChangeListenerCollection listeners;
	private String param;

	public ValidatableTextBox(final String param) {
		super();
		this.param = param;
		listeners = new ChangeListenerCollection();
		addKeyUpHandler(
				new KeyUpHandler() {
					@Override
					public void onKeyUp(KeyUpEvent event) {
						if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
							listeners.fireChange((Widget) event.getSource());
						}
					}
				}
		);
		super.addChangeListener(new ChangeListener() {
			@Override
			public void onChange(final Widget widget) {
				listeners.fireChange(widget);
			}
		});
	}

	public String getParam() {
		return param;
	}

	@Override
	public ClientValue getClientValue() {
		return getValueFromString(getText());
	}

	protected abstract ClientValue getValueFromString(String value);

	@Override
	public void setValue(final ClientValue value) {
		if (value == null) {
			return;
		}
		setText(setValueAsString(value));
	}

	protected abstract String setValueAsString(ClientValue object);

	@Override
	public void focus() {
		setFocus(true);
	}

	@Override
	public void addChangeListener(final ChangeListener changeListener) {
		listeners.add(changeListener);
	}

	@Override
	public void removeChangeListener(final ChangeListener changeListener) {
		listeners.remove(changeListener);
	}

	@Override
	public void setValidationSeverity(final int validationSeverity) {
		ValidationController.setValidationSeverity(validationSeverity, this);
	}
}
