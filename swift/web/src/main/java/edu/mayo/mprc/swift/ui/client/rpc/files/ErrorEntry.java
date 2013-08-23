package edu.mayo.mprc.swift.ui.client.rpc.files;

import com.google.gwt.user.client.ui.TreeItem;

/**
 * This contains error message (for instance caused by attempting to list directory contents). It is treated as a normal tree node.
 *
 * @author: Roman Zenka
 */
public final class ErrorEntry extends Entry {
	private static final long serialVersionUID = 20101221L;
	private String errorMessage;

	public ErrorEntry() {
		super("");
		this.errorMessage = "Unknown error occured.";
	}

	public ErrorEntry(final String message) {
		super("");
		this.errorMessage = message;
	}

	@Override
	public TreeItem createTreeItem() {
		return new TreeItem(errorMessage);
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(final String errorMessage) {
		this.errorMessage = errorMessage;
	}

}
