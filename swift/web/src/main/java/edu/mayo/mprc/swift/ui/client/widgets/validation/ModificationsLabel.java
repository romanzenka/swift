package edu.mayo.mprc.swift.ui.client.widgets.validation;

import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.swift.ui.client.rpc.ClientModSpecificity;
import edu.mayo.mprc.swift.ui.client.rpc.ClientModSpecificitySet;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValue;

import java.util.*;


/**
 * This is a label used to display a list of modifications (there names).
 * It also provides a hyperlink to launch the Modifcations Editor Popup
 */
public final class ModificationsLabel extends Composite implements Validatable {

	private boolean enabled = true;
	private List<? extends ClientValue> allowedValues;
	private Set<ClientModSpecificity> selectedValues = new HashSet<ClientModSpecificity>();

	private String param;
	private String type;

	private Label modsText;

	private PushButton editCmd;

	private ChangeListenerCollection listeners = new ChangeListenerCollection();


	private Panel container;
	private ModificationSelectionEditor editor;

	private String buttonName;
	public static final List<ClientModSpecificity> NO_SPECIFICITIES = new ArrayList<ClientModSpecificity>(0);


	public ModificationsLabel(final String param, final String buttonName) {
		this.setParam(param);
		this.setType(getType(param));
		this.setButtonName(buttonName);
	}

	public void setEditor(final ModificationSelectionEditor editor) {
		this.editor = editor;
		this.createModificationsLabel();
	}
	// TODO
	//  this widget needs to register a listener with the Modificatons Editor so that  when it closes with OK status
	// the ModificationsListBox here will be updated.

	/*
		* Note on the first update the allowed values are set.
		* TODO
		* After that assume they do not change, so don't reset the editor
		* TODO
		* The selected values will be reset each time the editor is opened
		 */
	private ModificationsLabelRunClick popupLauncher;

	private void createContainer() {
		setContainer(new FlowPanel());
		this.setModsText(new Label());
		this.getModsText().setStyleName("mods-label-text");
		// need a click listener to call the modifications editor popup
		this.setPopupLauncher(new ModificationsLabelRunClick(this));

		setEditCmd(new PushButton("Edit", getPopupLauncher()));
		getEditCmd().setStylePrimaryName("editModsButton");
		// We will enable the editor as soon as our modification list loads
		getEditCmd().setEnabled(false);
		getPopupLauncher().setUpdateSelectedOnEditor(true);
		getContainer().add(this.getModsText());
		getContainer().add(this.getEditCmd());
		this.resetText();
	}

	private void createModificationsLabel() {
		this.createContainer();

		this.initWidget(this.getContainer());
	}

	public void clear() {
		selectedValues.clear();
	}

	public void addModifications(final List<ClientModSpecificity> specs) {
		selectedValues.clear();
		selectedValues.addAll(specs);
		resetText();
	}

	private void resetText() {
		final StringBuilder text = new StringBuilder();
		if (selectedValues.size() == 0) {
			text.append("(none)");
		} else {
			final Iterator<ClientModSpecificity> it = selectedValues.iterator();
			while (true) {
				final ClientModSpecificity mod = it.next();
				text.append(mod.toString());
				if (it.hasNext()) {
					text.append("; ");
				} else {
					break;
				}
			}
		}
		this.getModsText().setText(text.toString());
	}

	private static String getType(final String param) /* throws GWTServiceException */ {
		if (param.equals(ModificationSelectionEditor.FIXED_PARAM_NAME)) {
			return ModificationSelectionEditor.FIXED_MOD_TYPE;
		} else if (param.equals(ModificationSelectionEditor.VARIABLE_PARAM_NAME)) {
			return ModificationSelectionEditor.VARIABLE_MOD_TYPE;

		}

		// TODO - find out how to propagate exceptions in GWT
		//throw GWTServiceExceptionFactory.createException("invalid parameter="+param, new MprcException("invalid parameter="+param));
		return null;
	}

	public void focus() {
		this.getEditor().focus();
	}

	public ClientValue getClientValue() {
		final List<ClientValue> items = new ArrayList<ClientValue>(selectedValues);
		return bundle(items);
	}

	/**
	 * sets the selected values
	 *
	 * @param value
	 */
	public void setValue(final ClientValue value) {
		if (value != null) {
			final List<? extends ClientValue> selected = unbundle(value);
			for (final ClientValue sel : selected) {
				selectedValues.add((ClientModSpecificity) sel);
			}

			this.addModifications(getValues(value));
			this.resetText();
			if (isEnabled()) {
				listeners.fireChange(this);
			}
		}
	}

	public void setValidationSeverity(final int validationSeverity) {
		ValidationController.setValidationSeverity(validationSeverity, this.getEditCmd());
	}

	/**
	 * Once the allowed values arrive, we can enable the editor
	 *
	 * @param values Allowed values.
	 */
	public void setAllowedValues(final List<? extends ClientValue> values) {
		allowedValues = values;
		if (values != null && values.size() > 0) {
			getEditCmd().setEnabled(true);
		}
	}

	public List<? extends ClientValue> getAllowedValues() {
		return allowedValues;
	}

	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}

/* end PROXY interface  */

	public static List<ClientModSpecificity> getValues(final ClientValue value) {
		if (value == null) {
			return NO_SPECIFICITIES;
		}
		return ClientModSpecificitySet.cast(value).getModSpecificities();
	}

	public static ClientValue bundle(final List<? extends ClientValue> selected) {
		final List<ClientModSpecificity> specs = new ArrayList<ClientModSpecificity>(selected.size());
		final ClientModSpecificitySet cmss = new ClientModSpecificitySet(specs);
		for (final ClientValue value : selected) {
			specs.add(ClientModSpecificity.cast(value));
		}
		return cmss;
	}

	public static List<? extends ClientValue> unbundle(final ClientValue value) {
		return ClientModSpecificitySet.cast(value).getModSpecificities();
	}

	/**
	 * @return null - we will get the allowed values fetched by an independent mechanism
	 */
	public boolean needsAllowedValues() {
		return false;
	}

	public void addChangeListener(final ChangeListener changeListener) {
		listeners.add(changeListener);
	}

	public void removeChangeListener(final ChangeListener changeListener) {
		listeners.remove(changeListener);
	}

	/**
	 * valid choices are
	 * <p>
	 * <ul>
	 * <li> @ModificationSelectionEditor.VARIABLE_PARAM_NAME
	 * <li> @ModificationSelectionEditor.FIXED_PARAM_NAME
	 * </ul>
	 * </p>
	 */
	public String getParam() {
		return param;
	}

	public void setParam(final String param) {
		this.param = param;
	}

	/**
	 * type is needed by the modification  selection editor
	 */
	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	/**
	 * displays the names in a concatenated form
	 * an example
	 * <p>
	 * 'Variable Modifications; Carbamidomethyl (C); Oxidized (M)'
	 * or
	 * Variable Modifications: (none)
	 * </p>
	 */
	public Label getModsText() {
		return modsText;
	}

	public void setModsText(final Label modsText) {
		this.modsText = modsText;
	}

	public PushButton getEditCmd() {
		return editCmd;
	}

	public void setEditCmd(final PushButton editCmd) {
		this.editCmd = editCmd;
	}

	/**
	 * the container for the widgets
	 */
	public Panel getContainer() {
		return container;
	}

	public void setContainer(final Panel container) {
		this.container = container;
	}

	/**
	 * the modifications selection editor
	 */
	public ModificationSelectionEditor getEditor() {
		return editor;
	}

	/**
	 * the name of the button that will be used to call the modificatons selectoin editor popup
	 */
	public String getButtonName() {
		return buttonName;
	}

	public void setButtonName(final String buttonName) {
		this.buttonName = buttonName;
	}

	public ModificationsLabelRunClick getPopupLauncher() {
		return popupLauncher;
	}

	public void setPopupLauncher(final ModificationsLabelRunClick popupLauncher) {
		this.popupLauncher = popupLauncher;
	}
}



