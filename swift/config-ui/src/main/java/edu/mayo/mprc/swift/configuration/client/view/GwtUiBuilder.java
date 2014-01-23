package edu.mayo.mprc.swift.configuration.client.view;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.swift.configuration.client.model.*;
import edu.mayo.mprc.swift.configuration.client.validation.local.IntegerValidator;
import edu.mayo.mprc.swift.configuration.client.validation.local.RequiredFieldValidator;
import edu.mayo.mprc.swift.configuration.client.validation.local.Validator;

import java.util.Arrays;

/**
 * A library of methods that create various UI components to be shared for the configuration.
 * <p/>
 * The key method is {@link #property}. Other methods modify the function of the call that is to come (e.g. required().property())
 * or they modify the property that was defined before (e.g. property().existing). The state is therefore split into two parts
 * - preceding (pertaining to the preceding property call) and future (pertaining to the property call that is to come).
 * <p/>
 * GwtUiBuilder makes a {@link PropertyList} control.
 * All controls are set up to send any change to {@link ConfigurationService#propertyChanged}.
 */
public final class GwtUiBuilder implements UiBuilderClient {
	private final ResourceModel model;
	private PropertyList propertyList;
	private int row;

	// Information about property that is currently being built.
	private String name;
	private String displayName;
	private Widget editor;
	private String description;
	private MultiValidator validator; // Validator for the current "property" call. E.g. property().fileExists()
	private String defaultValue;

	private ApplicationModel applicationModel;
	private Widget nativeInterface;

	private Context context;

	public GwtUiBuilder(final Context context, final ResourceModel model) {
		this.model = model;
		applicationModel = context.getApplicationModel();
		this.context = context;
		row = 0;
	}

	public GwtUiBuilder start() {
		propertyList = new PropertyList();
		propertyList.getColumnFormatter().addStyleName(1, "editor-column");
		editor = null;
		row = 0;
		validator = null;
		return this;
	}

	public Widget getNativeInterface() {
		return nativeInterface;
	}

	public PropertyList end() {
		if (editor != null) {
			endPropertyUi();
		}
		return propertyList;
	}

	public PropertyList getPropertyList() {
		return propertyList;
	}

	@Override
	public GwtUiBuilder nativeInterface(final String className) {
		if ("database".equals(className)) {
			nativeInterface = new DatabaseView(this, model);
		} else {
			throw new RuntimeException("Unsupported native inteface: " + className);
		}
		return this;
	}

	/**
	 * A property method triggers output of the UI for the previous property (which is by now complete).
	 *
	 * @param name        New property name.
	 * @param displayName Property display name (user-friendly text)
	 * @param description Property description.
	 */
	@Override
	public GwtUiBuilder property(final String name, final String displayName, final String description) {
		if (editor != null) {
			endPropertyUi();
		}

		startPropertyUi(name, displayName, description);
		makeTextBoxEditor();

		return this;
	}

	private void makeTextBoxEditor() {
		editor = new TextBox();
		editor.addStyleName("property-text-box");
	}

	/**
	 * Starts creation of a new property UI.
	 *
	 * @param name        New property name.
	 * @param displayName New property display name.
	 * @param description New property description.
	 */
	private void startPropertyUi(final String name, final String displayName, final String description) {
		validator = new MultiValidator(model, name);

		this.name = name;
		this.displayName = displayName;
		this.description = description;
	}

	/**
	 * Adds UI for one property. Generates a validation panel just above the property.
	 * Hooks the editor to fire a validation on change, which in turns sends the new value to the server.
	 */
	private void endPropertyUi() {
		// Add validation panel
		final ValidationPanel validationPanel = addValidationPanel(propertyList, row);
		row++;

		// Add aditor and validator (+asterisk) and editor
		final String asteriskIfRequired = validator.isRequiredField() ? "<span class=\"required\">*</span>" : "";
		final String displayNameHardSpaces = displayName.replaceAll(" ", "&nbsp;");
		final HTML html = new HTML(displayNameHardSpaces + asteriskIfRequired);
		html.addStyleName("property-caption");
		propertyList.setWidget(row, 0, html);
		validator.setValidationPanel(validationPanel);

		if (validator.hasOnDemandValidation()) {
			final HorizontalPanel panel = createOnDemandValidationPanel(editor, validator, validationPanel);
			propertyList.setWidget(row, 1, panel);
		} else {
			propertyList.setWidget(row, 1, editor);
		}

		// Set the default value if specified - this is mostly used for the special DatabaseView module.
		if (defaultValue != null) {
			if (editor instanceof TextBox) {
				((TextBox) editor).setText(defaultValue);
			} else if (editor instanceof CheckBox) {
				((CheckBox) editor).setValue("true".equals(defaultValue));
			}
			defaultValue = null;
		}

		propertyList.getFlexCellFormatter().addStyleName(row, 1, "property-editor");

		addEditorValidator(editor, validator, validationPanel);
		row++;

		// Add property description under
		propertyList.getFlexCellFormatter().setColSpan(row, 0, 2);
		propertyList.setWidget(row, 0, new HTML(description));
		propertyList.getFlexCellFormatter().addStyleName(row, 0, "property-description");
		row++;

		// Register the new property with the property list
		propertyList.registerProperty(name, editor, validator);
	}

	private void addEditorValidator(final Widget editor, final MultiValidator validator, final ValidationPanel validationPanel) {
		if (editor instanceof HasChangeHandlers) {
			((HasChangeHandlers) editor).addChangeHandler(new ChangeHandler() {
				@Override
				public void onChange(final ChangeEvent event) {
					validator.validate(PropertyList.getEditorValue((Widget) event.getSource()));
				}
			});
		} else if (editor instanceof HasValueChangeHandlers) {
			((HasValueChangeHandlers) editor).addValueChangeHandler(new ValueChangeHandler() {
				@Override
				public void onValueChange(final ValueChangeEvent event) {
					validator.validate(PropertyList.getEditorValue((Widget) event.getSource()));
				}
			});
		} else if (editor instanceof HasClickHandlers) {
			((HasClickHandlers) editor).addClickHandler(new ClickHandler() {
				@Override
				public void onClick(final ClickEvent event) {
					validator.validate(PropertyList.getEditorValue((Widget) event.getSource()));
				}
			});
		} else {
			throw new RuntimeException("Unsupported type of editor: " + editor.getClass().getName());
		}
	}

	/**
	 * @param editor          Value editor
	 * @param validator       Validator for the editor
	 * @param validationPanel Validation panel to display validation results
	 * @return Validation panel consisting of the editor and "Test" button that runs the on-demand validation.
	 */
	private HorizontalPanel createOnDemandValidationPanel(final Widget editor, final MultiValidator validator, final ValidationPanel validationPanel) {
		final HorizontalPanel panel = new HorizontalPanel();
		panel.add(this.editor);

		final Button test = new Button("Test");
		test.addStyleName("btn");
		panel.add(test);

		final HTML testProgress = new HTML("&nbsp;");
		testProgress.setVisible(false);
		testProgress.addStyleName("progress");
		panel.add(testProgress);

		final HTML successIndicator = new HTML("&nbsp;");
		successIndicator.setVisible(false);
		successIndicator.addStyleName("successIndicator");
		panel.add(successIndicator);

		validationPanel.setTestButton(test);
		validationPanel.setTestProgress(testProgress);
		validationPanel.setSuccessIndicator(successIndicator);
		test.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				validator.runOnDemandValidation(PropertyList.getEditorValue(editor), validationPanel);
			}
		});
		return panel;
	}

	public Widget getPrecedingEditor() {
		return editor;
	}

	/**
	 * Adds change handler to the preceeding property. If the preceeding property is
	 * represented by a TextBox, the listner must be of the type ChangeListener. If the
	 * preceeding property is represented by a CheckBox, the listner must be of the type ClickHandler.
	 */
	public GwtUiBuilder addEventHandler(final EventHandler handler) {
		if (editor instanceof TextBox) {
			if (!(handler instanceof ChangeHandler)) {
				throw new RuntimeException("The event handler for TextBox must be a change handler");
			}
			((TextBox) editor).addChangeHandler((ChangeHandler) handler);
		} else if (editor instanceof CheckBox) {
			if (!(handler instanceof ClickHandler)) {
				throw new RuntimeException("The event handler for CheckBox must be a click handler");
			}
			((CheckBox) editor).addClickHandler((ClickHandler) handler);
		}
		return this;
	}

	@Override
	public GwtUiBuilder required() {
		validator.addValidator(new RequiredFieldValidator());
		return this;
	}

	/**
	 * Default values are displayed in the fields. The user is still responsible
	 * to set the default values in the {@link ResourceModel} objects, otherwise
	 * the default might be overriden on load.
	 *
	 * @param value Default value for the current property.
	 */
	@Override
	public GwtUiBuilder defaultValue(final String value) {
		defaultValue = value;
		return this;
	}

	@Override
	public GwtUiBuilder validateOnDemand() {
		validator.addOnDemandValidator();
		return this;
	}

	private ValidationPanel addValidationPanel(final FlexTable flexTable, final int nextTableRow) {
		final ValidationPanel validationMessagePanel = new ValidationPanel();

		flexTable.setWidget(nextTableRow, 0, validationMessagePanel);
		flexTable.getFlexCellFormatter().setColSpan(nextTableRow, 0, 3);
		return validationMessagePanel;
	}


	public GwtUiBuilder validator(final Validator validator) {
		this.validator.addValidator(validator);
		return this;
	}

	@Override
	public GwtUiBuilder boolValue() {
		editor = new CheckBox();
		return this;
	}

	@Override
	public GwtUiBuilder integerValue(final Integer minimum, final Integer maximum) {
		final IntegerValidator v = new IntegerValidator(minimum, maximum);
		validator.addValidator(v);
		return this;
	}


	@Override
	public GwtUiBuilder reference(final String... type) {
		editor = new ReferenceListBox(Arrays.asList(type), applicationModel, context);
		return this;
	}


	@Override
	public GwtUiBuilder enable(final String propertyName, final boolean synchronous) {
		if (editor instanceof CheckBox) {
			final CheckBox checkBox = (CheckBox) editor;
			checkBox.addClickHandler(new ClickHandler() {

				@Override
				public void onClick(final ClickEvent event) {
					final Widget sender = (Widget) event.getSource();
					if (sender instanceof CheckBox) {
						final CheckBox checkBox = (CheckBox) sender;
						final boolean enableLink = Boolean.TRUE.equals(checkBox.getValue()) == synchronous;
						final Widget editor = propertyList.getWidgetForName(propertyName);
						if (editor != null && editor instanceof FocusWidget) {
							((FocusWidget) editor).setEnabled(enableLink);
						} else {
							throw new RuntimeException("The property " + propertyName + " does not correspond to an editor that can be enabled/disabled.");
						}
					}
				}
			});
		} else {
			throw new RuntimeException("Only a checkbox can be set to enable/disable other properties");
		}
		return this;
	}

}
