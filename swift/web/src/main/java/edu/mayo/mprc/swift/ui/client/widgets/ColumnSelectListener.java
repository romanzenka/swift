package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CheckBox;

public final class ColumnSelectListener implements ClickHandler {
	private final int column;
	private final FileTable table;

	public ColumnSelectListener(final int column, final FileTable table) {
		this.column = column;
		this.table = table;
	}

	@Override
	public void onClick(final ClickEvent event) {
		final CheckBox mainCheckBox = (CheckBox) event.getSource();
		for (int row = table.getFirstDataRow(); row < table.getRowCount(); row++) {
			table.setChecked(row, column, Boolean.TRUE.equals(mainCheckBox.getValue()));
		}
	}
}
