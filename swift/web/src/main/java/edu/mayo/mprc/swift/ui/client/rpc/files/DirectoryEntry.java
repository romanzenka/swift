package edu.mayo.mprc.swift.ui.client.rpc.files;

import com.google.gwt.user.client.ui.TreeItem;

/**
 * A directory.
 *
 * @author: Roman Zenka
 */
public final class DirectoryEntry extends Entry {
	private static final long serialVersionUID = 20101221L;

	public DirectoryEntry() {
	}

	public DirectoryEntry(final String name) {
		super(name);
	}

	@Override
	public TreeItem createTreeItem() {
		final TreeCheckBox checkBox = new TreeCheckBox(getName(), false);
		final TreeItem newItem = new TreeItem(checkBox);
		checkBox.setTreeItem(newItem);
		newItem.setUserObject(this);
		if (getChildrenList().isEmpty()) {
			newItem.addItem("empty");
		}
		return newItem;
	}
}
