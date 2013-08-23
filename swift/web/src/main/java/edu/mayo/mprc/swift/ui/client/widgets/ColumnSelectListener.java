package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

public final class ColumnSelectListener implements ClickListener {
	private int column;
	private FileTable table;

	public ColumnSelectListener(final int column, final FileTable table) {
		this.column = column;
		this.table = table;
	}

	@Override
	public void onClick(final Widget widget) {
		final CheckBox mainCheckBox = (CheckBox) widget;
		for (int row = table.getFirstDataRow(); row < table.getRowCount(); row++) {
			table.setChecked(row, column, mainCheckBox.isChecked());
		}
	}
}
