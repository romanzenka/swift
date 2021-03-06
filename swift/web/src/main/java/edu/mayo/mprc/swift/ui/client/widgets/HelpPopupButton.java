package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;

/**
 * A help button (displaying a question mark) that popups a help message when clicked.
 */
public final class HelpPopupButton extends Label implements ClickHandler {
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
			setVisible(false);
			return;
		}
		setText("?");
		addStyleName("help-popup-button");
		helpHtml = new HTMLPanel(helpHtmlString);
		helpHtml.setStyleName("help-html-popup");
		helpHtml.setWidth(WIDTH + "px");
		addClickHandler(this);
	}

	@Override
	public void onClick(final ClickEvent event) {
		final DialogBox dialogBox = new DialogBox(true, true);
		dialogBox.setWidget(helpHtml);
		// Make sure we do not run out of the screen on the right side
		int left = getAbsoluteLeft();
		if (left + WIDTH > Window.getClientWidth()) {
			left = Math.max(Window.getClientWidth() - WIDTH, 0);
		}
		dialogBox.setPopupPosition(left, getAbsoluteTop() + 15);
		dialogBox.show();
	}
}
