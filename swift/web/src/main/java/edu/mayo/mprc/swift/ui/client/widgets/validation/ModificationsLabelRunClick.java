package edu.mayo.mprc.swift.ui.client.widgets.validation;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

/**
 * handles calling a modifications editor popup on a click event
 * It deals with propagating proxy values to the editor and launches the popup
 */
public final class ModificationsLabelRunClick implements ClickListener {
	private ModificationSelectionEditor editor;
	private String param;
	/**
	 * the type of modification, ie variable or fixed
	 */
	private String type;

	private ModificationsLabel proxy;
	/**
	 * used to indicate if should reset the editor selections to those in the proxy
	 */
	private boolean updateSelectedOnEditor;

	public ModificationsLabelRunClick(final ModificationsLabel proxy) {
		editor = proxy.getEditor();
		param = proxy.getParam();
		type = proxy.getType();
		this.proxy = proxy;
	}

	/**
	 * indicate if the selections on the proxy should be propagated to the modiications editor
	 *
	 * @param updateSelections - indicates if should progagate the selections
	 */
	protected void setUpdateSelectedOnEditor(final boolean updateSelections) {
		updateSelectedOnEditor = updateSelections;
	}

	@Override
	public void onClick(final Widget sender) {
		final ModificationDialog p = new ModificationDialog(editor);

		editor.setAllowedValues(proxy.getAllowedValues());
		if (updateSelectedOnEditor) {
			editor.setValueClear();
			editor.setValue(proxy.getClientValue());
		}
		p.setParam(param);
		p.setType(type);
		final OkClickListener listener = new OkClickListener(editor, proxy);
		p.setOkListener(listener);

		p.center();
		p.show();
	}

	/**
	 * handles the Ok button click on the Ok button of the Modification Editor
	 */
	private static final class OkClickListener implements ClickListener {
		private ModificationSelectionEditor editor;
		private ModificationsLabel proxy;

		OkClickListener(final ModificationSelectionEditor editor, final ModificationsLabel proxy) {
			this.editor = editor;
			this.proxy = proxy;
		}

		/**
		 * copy the selected items to the proxy
		 *
		 * @param sender - the Ok button
		 */
		@Override
		public void onClick(final Widget sender) {
			proxy.setValue(editor.getClientValue());
		}
	}

}
