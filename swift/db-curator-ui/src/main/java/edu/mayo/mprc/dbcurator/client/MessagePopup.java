package edu.mayo.mprc.dbcurator.client;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;

class MessagePopup extends PopupPanel {
	public MessagePopup(final String msg, final int xPosition, final int yPosition) {
		super(true);
		setPopupPosition(xPosition, yPosition);
		setStyleName("curator-message-popup");
		setWidget(new Label(msg));
	}

	public void show(final int msToDisplay) {
		super.show();
		if (msToDisplay > 0) {
			final Timer hideTimer = new Timer() {
				@Override
				public void run() {
					hide();
				}
			};
			hideTimer.schedule(msToDisplay);
		}
	}

	@Override
	public void show() {
		show(2500);
	}
}
