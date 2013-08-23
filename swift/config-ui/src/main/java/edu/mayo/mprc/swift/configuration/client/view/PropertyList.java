package edu.mayo.mprc.swift.configuration.client.view;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import edu.mayo.mprc.swift.configuration.client.validation.local.Validator;

import java.util.HashMap;
import java.util.Map;

public final class PropertyList extends FlexTable implements ModuleView {
	private Map<String, PropertyDefinition> properties = new HashMap<String, PropertyDefinition>();

	@Override
	public void loadUI(final Map<String, String> values) {
		if (values == null) {
			return;
		}
		for (final PropertyDefinition prop : properties.values()) {
			final String value = values.get(prop.name);
			if (value != null) {
				setEditorValue(prop.editor, value);
			} else {
				// Value was not provided. Take the default, if any, and use it.
				String def = getEditorValue(prop.editor);
				if (def != null) {
					prop.validator.validate(def);
				}
			}
		}
	}

	public static String getEditorValue(final Widget editor) {
		final String value;
		if (editor instanceof TextBox) {
			value = ((TextBox) editor).getText().trim();
		} else if (editor instanceof CheckBox) {
			value = ((CheckBox) editor).isChecked() ? "true" : "false";
		} else if (editor instanceof ReferenceListBox) {
			value = ((ReferenceListBox) editor).getValue();
		} else {
			throw new RuntimeException("Unsupported editor type " + editor.getClass().getName());
		}
		return value;
	}

	public static void setEditorValue(final Widget editor, final String value) {
		if (editor instanceof TextBox) {
			((TextBox) editor).setText(value);
		} else if (editor instanceof CheckBox) {
			((CheckBox) editor).setValue("true".equals(value));
		} else if (editor instanceof ReferenceListBox) {
			((ReferenceListBox) editor).setValue(value);
		} else {
			throw new RuntimeException("Unsupported editor type " + editor.getClass().getName());
		}
	}

	@Override
	public HashMap<String, String> saveUI() {
		final HashMap<String, String> map = new HashMap<String, String>();
		for (final PropertyDefinition prop : properties.values()) {
			final String value = getEditorValue(prop.editor);
			map.put(prop.name, value);
		}
		return map;
	}

	public void registerProperty(final String name, final Widget editor, final Validator validator) {
		properties.put(name, new PropertyDefinition(name, editor, validator));
	}

	/**
	 * @param name Name of property.
	 * @return The editor for this property.
	 */
	public Widget getWidgetForName(final String name) {
		final PropertyDefinition propertyDefinition = properties.get(name);
		return propertyDefinition != null ? propertyDefinition.editor : null;
	}

	public void fireValidations() {
		for (final PropertyDefinition definition : properties.values()) {
			definition.validator.validate(getPropertyValue(definition.name));
		}
	}

	public void setPropertyValue(final String propertyName, final String value) {
		final Widget editor = getWidgetForName(propertyName);
		setEditorValue(editor, value);
	}

	public String getPropertyValue(final String propertyName) {
		final Widget editor = getWidgetForName(propertyName);
		return getEditorValue(editor);
	}

	@Override
	public Widget getModuleWidget() {
		return this;
	}

	private static final class PropertyDefinition {
		private PropertyDefinition(final String name, final Widget editor, final Validator validator) {
			this.name = name;
			this.editor = editor;
			this.validator = validator;
		}

		public final String name;
		public final Widget editor;
		public final Validator validator;
	}
}
