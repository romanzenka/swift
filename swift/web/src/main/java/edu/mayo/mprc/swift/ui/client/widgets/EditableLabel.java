/*
 * Copyright 2006 Robert Hanson <iamroberthanson AT gmail.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Modified by Roman Zenka - placed in a different package.
 */

package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.*;


/**
 * Editable Label class, funcionality displays a Label UI
 * Element until clicked on, then if element is set to be
 * editable (default) then an editable area and Buttons are
 * displayed instead.
 * <p/>
 * If the Label is not set to be word wrapped (default) then
 * the editable area is a Text Box and clicking the OK button
 * or hitting return key in the TextBox will display the Label with
 * the updated text.
 * <p/>
 * If the Label is set to be word wrapped, using the setWordWrap(boolean)
 * method, then the editable area is a Text Area and clicking the OK
 * button will display the Label with the updated text.
 * <p/>
 * In both cases, clicking Cancel button or hitting Escape key in the
 * TextBox/TextArea then the Label is displayed with original text.
 *
 * @author Adam Tacy
 * @version 0.0.2
 *          <p/>
 *          Changes since version 0.0.1
 *          + made cancelLabelChange public [ref request id: 1518134]
 *          + made originalText have default value of empty string [to support ref request id: 1518134]
 *          *End*
 */
public class EditableLabel extends Composite implements HasWordWrap, HasText {
	/**
	 * TextBox element to enable text to be changed if Label is not word wrapped
	 */
	private TextBox changeText;

	/**
	 * TextArea element to enable text to be changed if Label is wordwrapped
	 */
	private TextArea changeTextArea;


	/**
	 * Label element, which is initially is diplayed.
	 */
	private Label text;

	/**
	 * String element that contains the original text of a
	 * Label prior to it being edited.
	 */
	private String originalText;

	/**
	 * Simple button to confirm changes
	 */
	private Widget confirmChange;

	/**
	 * Simple button to cancel changes
	 */
	private Widget cancelChange;

	/**
	 * Flag to indicate that label can be edited.
	 */
	private boolean isEditable = true;

	/**
	 * Local copy of the update class passed in to the constructor.
	 */
	private ChangeListener updater = null;

	/**
	 * Default String value for OK button
	 */
	private String defaultOkButtonText = "OK";

	/**
	 * Default String value for Cancel button
	 */
	private String defaultCancelButtonText = "Cancel";

	/**
	 * Change the displayed label to be a TextBox and copy label
	 * text into the TextBox.
	 */
	private void changeTextLabel() {
		if (isEditable) {
			// Set up the TextBox
			originalText = text.getText();

			// Change the view from Label to TextBox and Buttons
			text.setVisible(false);
			confirmChange.setVisible(true);
			cancelChange.setVisible(true);

			if (text.getWordWrap()) {
				// If Label word wrapped use the TextArea to edit
				changeTextArea.setText(originalText);
				changeTextArea.setVisible(true);
				changeTextArea.setFocus(true);
			} else {
				// Otherwise use the TextBox to edit.
				changeText.setText(originalText);
				changeText.setVisible(true);
				changeText.setFocus(true);
			}
		}
	}

	/**
	 * Restores visibility of Label and hides the TextBox and Buttons
	 */
	private void restoreVisibility() {
		// Change appropriate visibilities
		text.setVisible(true);
		confirmChange.setVisible(false);
		cancelChange.setVisible(false);
		if (text.getWordWrap()) {
			// If Label is word wrapped hide the TextArea
			changeTextArea.setVisible(false);
		} else {
			// Otherwise hide the TextBox
			changeText.setVisible(false);
		}
	}

	/**
	 * Sets the Label text to the new value, restores the
	 * display and calls the update method.
	 */
	private void setTextLabel() {
		if (text.getWordWrap()) {
			// Set the Label to be the text in the Text Box
			text.setText(changeTextArea.getText());
		} else {
			// Set the Label to be the text in the Text Box
			text.setText(changeText.getText());
		}
		// Set the object back to display label rather than TextBox and Buttons
		restoreVisibility();

		// Call the update method provided in the Constructor
		// (this could be anything from alerting the user through to
		// Making an AJAX call to store the data.
		updater.onChange(this);
	}

	/**
	 * Sets the Label text to the original value, restores the display.
	 */
	public void cancelLabelChange() {
		// Set the Label text back to what it was originally
		text.setText(originalText);
		// Set the object back to display Label rather than TextBox and Buttons
		restoreVisibility();
	}

	/**
	 * Creates the Label, the TextBox and Buttons.  Also associates
	 * the update method provided in the constructor with this instance.
	 *
	 * @param labelText        The value of the initial Label.
	 * @param onUpdate         The class that provides the update method called when the Label has been updated.
	 * @param okButtonText     The text diplayed in the OK button.
	 * @param cancelButtonText The text displayed in the Cancel button.
	 */
	private void createEditableLabel(final String labelText, final ChangeListener onUpdate,
	                                 final String okButtonText, final String cancelButtonText) {
		// Put everything in a VerticalPanel
		final FlowPanel instance = new FlowPanel();

		// Create the Label element and add a ClickHandler to call out Change method when clicked
		text = new Label(labelText);
		text.setStyleName("editableLabel-label");

		text.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				changeTextLabel();
			}
		});

		// Create the TextBox element used for non word wrapped Labels
		// and add a KeyboardListener for Return and Esc key presses
		changeText = new TextBox();
		changeText.setStyleName("editableLabel-textBox");

		changeText.addKeyPressHandler(new KeyPressHandler() {
			@Override
			public void onKeyPress(final KeyPressEvent event) {
				// If return then save, if Esc cancel the change, otherwise do nothing
				switch (event.getUnicodeCharCode()) {
					case KeyCodes.KEY_ENTER:
						setTextLabel();
						break;
					case KeyCodes.KEY_ESCAPE:
						cancelLabelChange();
						break;
				}
			}
		});

		// Create the TextAre element used for word-wrapped Labels
		// and add a KeyboardListener for Esc key presses (not return in this case)

		changeTextArea = new TextArea();
		changeTextArea.setStyleName("editableLabel-textArea");

		changeTextArea.addKeyPressHandler(new KeyPressHandler() {
			@Override
			public void onKeyPress(final KeyPressEvent event) {
				// If Esc then cancel the change, otherwise do nothing
				switch (event.getUnicodeCharCode()) {
					case KeyCodes.KEY_ESCAPE:
						cancelLabelChange();
						break;
				}
			}
		});


		// Set up Confirmation Button
		confirmChange = createConfirmButton(okButtonText);

		if (!(confirmChange instanceof HasClickHandlers)) {
			throw new RuntimeException("Confirm change button must allow for click events");
		}

		((HasClickHandlers) confirmChange).addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				setTextLabel();
			}
		});

		// Set up Cancel Button
		cancelChange = createCancelButton(cancelButtonText);
		if (!(cancelChange instanceof HasClickHandlers)) {
			throw new RuntimeException("Cancel change button must allow for click events");
		}

		((HasClickHandlers) cancelChange).addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				cancelLabelChange();
			}
		});

		// Put the buttons in a panel
		final FlowPanel buttonPanel = new FlowPanel();
		buttonPanel.setStyleName("editableLabel-buttonPanel");
		buttonPanel.add(confirmChange);
		buttonPanel.add(cancelChange);

		// Add panels/widgets to the widget panel
		instance.add(text);
		instance.add(changeText);
		instance.add(changeTextArea);
		instance.add(buttonPanel);

		// Set initial visibilities.  This needs to be after
		// adding the widgets to the panel because the FlowPanel
		// will mess them up when added.
		text.setVisible(true);
		changeText.setVisible(false);
		changeTextArea.setVisible(false);
		confirmChange.setVisible(false);
		cancelChange.setVisible(false);

		// Set the updater method.
		updater = onUpdate;

		// Assume that this is a non word wrapped Label unless explicitly set otherwise
		text.setWordWrap(false);

		// Set the widget that this Composite represents
		initWidget(instance);
	}

	/**
	 * @param cancelButtonText
	 */
	protected Widget createCancelButton(final String cancelButtonText) {
		final Button result = new Button();
		result.setStyleName("editableLabel-buttons");
		result.addStyleName("editableLabel-cancel");
		result.setText(cancelButtonText);
		return result;
	}

	/**
	 * @param okButtonText
	 */
	protected Widget createConfirmButton(final String okButtonText) {
		final Button result = new Button();
		result.setStyleName("editableLabel-buttons");
		result.addStyleName("editableLabel-confirm");
		result.setText(okButtonText);
		return result;
	}


	/**
	 * Set the word wrapping on the label (if word wrapped then the editable
	 * field becomes a TextArea, if not then the editable field is a TextBox.
	 *
	 * @param b Boolean value, true means Label is word wrapped, false means it is not.
	 */
	@Override
	public void setWordWrap(final boolean b) {
		text.setWordWrap(b);
	}

	/**
	 * Return whether the Label is word wrapped or not.
	 */
	@Override
	public boolean getWordWrap() {
		return text.getWordWrap();
	}

	/**
	 * Return the text value of the Label
	 */
	@Override
	public String getText() {
		return text.getText();
	}

	/**
	 * Set the text value of the Label
	 */
	@Override
	public void setText(final String newText) {
		text.setText(newText);
	}

	/**
	 * Constructor that uses default text values for buttons.
	 *
	 * @param labelText The initial text of the label.
	 * @param onUpdate  Handler object for performing actions once label is updated.
	 */
	public EditableLabel(final String labelText, final ChangeListener onUpdate) {
		createEditableLabel(labelText, onUpdate, defaultOkButtonText, defaultCancelButtonText);
	}
}

