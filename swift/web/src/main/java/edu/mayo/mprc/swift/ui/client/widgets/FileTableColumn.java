package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.user.client.ui.Widget;

class FileTableColumn {
	private int column;
	private String title;
	private Widget widget;
	private String columnStyle;

	FileTableColumn(final int column, final String title, final Widget widget) {
		this(column, title, widget, null);
	}

	FileTableColumn(final int column, final String title, final Widget widget, final String columnStyle) {
		this.column = column;
		this.title = title;
		this.widget = widget;
		this.columnStyle = columnStyle;
	}

	public void init(final FileTable table) {
		if (widget != null) {
			table.setWidget(table.getHeaderRowIndex(), column, widget);
		} else {
			table.setHTML(table.getHeaderRowIndex(), column, title);
		}
		if (columnStyle != null) {
			table.getColumnFormatter().addStyleName(column, columnStyle);
		}
	}

	public Widget getWidget() {
		return widget;
	}
}
