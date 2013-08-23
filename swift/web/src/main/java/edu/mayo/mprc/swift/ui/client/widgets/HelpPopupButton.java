package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;

/**
 * A help button (displaying a question mark) that popups a help message when clicked.
 */
public final class HelpPopupButton extends Label implements ClickListener {
	private HTMLPanel helpHtml;
	private static final int WIDTH = 500;

	public HelpPopupButton() {
		initialize(null);
	}

	public HelpPopupButton(final String helpHtmlString) {
		initialize(helpHtmlString);
	}

	private void initialize(final String helpHtmlString) {
		if (helpHtmlString == null) {
			this.setVisible(false);
			return;
		}
		this.setText("?");
		this.addStyleName("help-popup-button");
		this.helpHtml = new HTMLPanel(helpHtmlString);
		this.helpHtml.setStyleName("help-html-popup");
		this.helpHtml.setWidth(WIDTH + "px");
		this.addClickListener(this);
	}

	@Override
	public void onClick(final Widget widget) {
		final DialogBox dialogBox = new DialogBox(true, true);
		dialogBox.setWidget(helpHtml);
		// Make sure we do not run out of the screen on the right side
		int left = this.getAbsoluteLeft();
		if (left + WIDTH > Window.getClientWidth()) {
			left = Math.max(Window.getClientWidth() - WIDTH, 0);
		}
		dialogBox.setPopupPosition(left, this.getAbsoluteTop() + 15);
		dialogBox.show();
	}
}
