package edu.mayo.mprc.swift.ui.client.rpc.files;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.TreeItem;

public final class TreeCheckBox extends CheckBox {
	private TreeItem treeItem;

	public TreeCheckBox() {
	}

	public TreeCheckBox(final String s) {
		super(s);
	}

	public TreeCheckBox(final String s, final boolean b) {
		super(s, b);
	}

	public TreeCheckBox(final Element element) {
		super(element);
	}

	public TreeItem getTreeItem() {
		return treeItem;
	}

	public void setTreeItem(final TreeItem treeItem) {
		this.treeItem = treeItem;
	}
}
