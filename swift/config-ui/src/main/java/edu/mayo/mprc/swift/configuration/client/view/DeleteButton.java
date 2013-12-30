package edu.mayo.mprc.swift.configuration.client.view;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.PushButton;

/**
 * A delete button that supports confirmation.
 */
public final class DeleteButton extends PushButton {
	private final String deleteMessage;

	public DeleteButton(final String deleteMessage) {
		addStyleName("delete-button");
		this.deleteMessage = deleteMessage;
	}

	@Override
	public HandlerRegistration addClickHandler(final ClickHandler handler) {
		return super.addClickHandler(new ConfirmationClickHandler(handler));
	}

	private class ConfirmationClickHandler implements ClickHandler {
		private ClickHandler wrappedClickHandler;

		private ConfirmationClickHandler(final ClickHandler wrappedClickHandler) {
			this.wrappedClickHandler = wrappedClickHandler;
		}

		@Override
		public void onClick(final ClickEvent event) {
			if (Window.confirm(deleteMessage)) {
				wrappedClickHandler.onClick(event);
			}
		}
	}
}
