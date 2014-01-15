package edu.mayo.mprc.swift.ui.client.widgets.validation;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.ui.HasVisibility;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import edu.mayo.mprc.swift.ui.client.dialogs.Validatable;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValue;

/**
 * An abstract check box that can validate the users's input server side in conjuction with a ValidationController
 */
public abstract class ValidatableTextBox implements Validatable, HasVisibility, IsWidget {
	private final TextBox textBox = new TextBox();
	private final String param;
	private final EventBus eventBus = new SimpleEventBus();

	public ValidatableTextBox(final String param) {
		this.param = param;
		textBox.addKeyUpHandler(
				new KeyUpHandler() {
					@Override
					public void onKeyUp(final KeyUpEvent event) {
						if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
							ValueChangeEvent.fire(ValidatableTextBox.this, getValue());
						}
					}
				}
		);
		textBox.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(final ChangeEvent event) {
				ValueChangeEvent.fire(ValidatableTextBox.this, getValue());
			}
		});
	}

	public String getParam() {
		return param;
	}

	protected abstract ClientValue getValueFromString(String value);

	@Override
	public ClientValue getValue() {
		return getValueFromString(textBox.getValue());
	}

	@Override
	public void setValue(final ClientValue value) {
		if (value == null) {
			return;
		}
		textBox.setText(setValueAsString(value));
	}

	@Override
	public void setValue(final ClientValue value, final boolean fireEvents) {
		ClientValueUtils.setValue(this, value, fireEvents);
	}

	protected abstract String setValueAsString(ClientValue object);

	@Override
	public void focus() {
		textBox.setFocus(true);
	}

	@Override
	public void setValidationSeverity(final int validationSeverity) {
		ValidationController.setValidationSeverity(validationSeverity, textBox);
	}

	@Override
	public void setEnabled(final boolean enabled) {
		textBox.setEnabled(enabled);
	}

	public boolean isVisible() {
		return textBox.isVisible();
	}

	public void setVisible(final boolean visible) {
		textBox.setVisible(visible);
	}

	public Widget asWidget() {
		return textBox.asWidget();
	}

	public int getVisibleLength() {
		return textBox.getVisibleLength();
	}

	public void setVisibleLength(final int length) {
		textBox.setVisibleLength(length);
	}

	@Override
	public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<ClientValue> handler) {
		return eventBus.addHandler(ValueChangeEvent.getType(), handler);
	}

	@Override
	public void fireEvent(final GwtEvent<?> event) {
		eventBus.fireEventFromSource(event, this);
	}
}
