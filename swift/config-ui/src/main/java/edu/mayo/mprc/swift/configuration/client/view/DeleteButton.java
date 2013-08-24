package edu.mayo.mprc.swift.configuration.client.view;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

/**
 * A delete button that support confirmation.
 */
public final class DeleteButton extends PushButton {
	private String deleteMessage;

	public DeleteButton(final String deleteMessage) {
		super();
		addStyleName("delete-button");
		this.deleteMessage = deleteMessage;
	}

	public String getDeleteMessage() {
		return deleteMessage;
	}

	public void setDeleteMessage(final String deleteMessage) {
		this.deleteMessage = deleteMessage;
	}

	@Override
	public void addClickListener(final ClickListener listener) {
		super.addClickListener(new ConfirmationClickListener(listener));
	}

	private class ConfirmationClickListener implements ClickListener {
		private ClickListener wrappedClickListener;

		private ConfirmationClickListener(final ClickListener wrappedClickListener) {
			this.wrappedClickListener = wrappedClickListener;
		}

		@Override
		public void onClick(final Widget sender) {
			if (Window.confirm(deleteMessage)) {
				wrappedClickListener.onClick(sender);
			}
		}
	}
}
