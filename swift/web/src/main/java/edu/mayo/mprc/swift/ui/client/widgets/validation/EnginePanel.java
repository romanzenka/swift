package edu.mayo.mprc.swift.ui.client.widgets.validation;

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.user.client.ui.HorizontalPanel;

/**
 * @author Roman Zenka
 */
public abstract class EnginePanel extends HorizontalPanel implements HasValueChangeHandlers<String> {
	public abstract void setEnabled(boolean enabled);

	public abstract void setValue(String value);

	public abstract String getValue();

	public abstract void setFocus(boolean focused);

	/**
	 * Sets the engine settings to a good default. We need to do this when the user loads a value
	 */
	public abstract void resetToDefault();
}
