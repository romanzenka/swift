package edu.mayo.mprc.swift.ui.client.dialogs;

import edu.mayo.mprc.swift.ui.client.widgets.validation.ModificationSelectionEditor;

/**
 * popup panel container for the mods selection editor
 */
public final class ModificationDialog extends FrameDialog {

	private ModificationSelectionEditor editor;
	private String param;
	private String type;

	public ModificationDialog(final ModificationSelectionEditor editor) {
		super("", true, true, false, false);

		setContent(editor);
	}


	public ModificationSelectionEditor getEditor() {
		return editor;
	}

	public void setEditor(final ModificationSelectionEditor editor) {
		this.editor = editor;
		setWidget(this.editor);
	}

	public String getParam() {
		return param;
	}

	public void setParam(final String param) {
		this.param = param;
	}

	public String getType() {
		return type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	@Override
	protected void okay() {
		// clear the description textarea
		hide();
	}

	@Override
	protected void cancel() {
		// clear the description textarea
		hide();
	}


}
