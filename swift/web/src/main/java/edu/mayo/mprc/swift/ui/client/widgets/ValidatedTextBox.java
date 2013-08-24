package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.*;

/*
 *      Copyright 2008 Battams, Derek
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 */

public abstract class ValidatedTextBox extends TextBox {

	/**
	 * An abstract class that defines the minimal functionality of validators for ValidatedTextBox objects; isValueValid() will be called to validate the data in each instance
	 */
	public abstract static class TextBoxValidator {
		private String lastError = null;

		/**
		 * Test the given data for it's validity
		 *
		 * @param data The data to test
		 * @return True if the data is valid or false otherwise; sets the getLastError() string to null if returning true or sets it to the reason the data isn't valid if returning false
		 */
		public abstract boolean isValueValid(String data);

		/**
		 * Set the last error encountered or null if the last check for validity deemed the data valid
		 *
		 * @param err The error message or null
		 */
		public void setLastError(final String err) {
			lastError = err;
		}

		/**
		 * Return the last error encountered by the validator or null if the last check for validity deemed the data valid
		 *
		 * @return The last error, as a string, or null
		 */
		public String getLastError() {
			return lastError;
		}
	}

	/**
	 * The FocusListener automatically attached to each instance
	 */
	private static class ValidatedTextBoxFocusListener extends FocusListenerAdapter {
		private PopupPanel p = new PopupPanel();

		@Override
		public void onLostFocus(final Widget sender) {
			final ValidatedTextBox vTxtBox = (ValidatedTextBox) sender;
			if (!vTxtBox.isValueValid()) {
				vTxtBox.setInvalid(true);
				p.setWidget(new Label(vTxtBox.getLastError()));
				p.setStyleName("validation-popup");
				p.setPopupPosition(sender.getAbsoluteLeft() + sender.getOffsetWidth() + 5, sender.getAbsoluteTop());
				p.show();
				DeferredCommand.addCommand(new Command() {
					@Override
					public void execute() {
						vTxtBox.setFocus(true);
					}
				});
			} else {
				p.hide();
				vTxtBox.setInvalid(false);
			}
		}
	}

	private static ValidatedTextBoxFocusListener focusListener = new ValidatedTextBoxFocusListener(); // Single instance can be attached to every outer widget instance!

	private TextBoxValidator validator;

	/**
	 * Constructor; use the TextBoxValidator argument as the validator for this instance
	 *
	 * @param v The validator to be used for this widget instance
	 */
	public ValidatedTextBox(final TextBoxValidator v) {
		validator = v;
		super.addFocusListener(focusListener);
	}

	/**
	 * Set the state of the widget to invalid (apply dependent style name "invalid")
	 *
	 * @param b True to set the widget invalid or false to remove invalid state
	 */
	public final void setInvalid(final boolean b) {
		if (b && !isInvalid()) {
			addStyleDependentName("invalid");
		} else if (!b && isInvalid()) {
			removeStyleDependentName("invalid");
		}
		return;
	}

	/**
	 * Test the validity of the input in this widget
	 *
	 * @return True if the input is valid (as determined by the TextBoxValidator given at construction) or false otherwise
	 */
	public final boolean isValueValid() {
		return validator.isValueValid(getText());
	}

	/**
	 * Get the last error reported by the associated validator or null if the last check deemed the input to be valid
	 *
	 * @return The last error encountered by the validator or null if the last check deemed the input to be valid
	 */
	public final String getLastError() {
		return validator.getLastError();
	}

	/**
	 * Check if the widget is set to an invalid state (has GWT dependent style of "invalid" applied at this moment)
	 *
	 * @return True if the widget is in the invalid state or false otherwise
	 */
	public final boolean isInvalid() {
		return getStyleName().contains("-invalid");
	}

	@Override
	public final void addFocusListener(final FocusListener listener) {
		return;
	}
}
	