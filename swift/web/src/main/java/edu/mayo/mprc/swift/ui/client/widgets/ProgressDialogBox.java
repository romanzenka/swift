package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;

public final class ProgressDialogBox extends DialogBox {

	private double widthFraction = 0.5;
	private double heightFraction = 0.5;
	private int minWidth = 200;
	private int minHeight = 150;
	private static final String PROGRESS_STYLE = "submit-progress";
	private static final double MIN_FRACTION = 0.001;
	private static final double MAX_FRACTION = 1;

	public ProgressDialogBox() {
		super(false, true);
	}

	public void setProgressMessage(final String text) {
		addStyleName(PROGRESS_STYLE);
		setText(text);
		final Button cancel = new Button("Cancel");
		cancel.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				hide();
			}
		});
		setWidget(cancel);
	}

	public void setRelativeSize(final double widthFraction, final double heightFraction) {
		assert widthFraction >= MIN_FRACTION && widthFraction <= MAX_FRACTION : "The width fraction must be in [" + MIN_FRACTION + ", " + MAX_FRACTION + "] range";
		assert heightFraction >= MIN_FRACTION && heightFraction <= MAX_FRACTION : "The height fraction must be in [" + MIN_FRACTION + ", " + MAX_FRACTION + "] range";

		this.widthFraction = widthFraction;
		this.heightFraction = heightFraction;
	}

	public void setMinimumSize(final int minWidth, final int minHeight) {
		this.minWidth = minWidth;
		this.minHeight = minHeight;
	}

	public void showModal() {
		positionDialog();
//		final LightBox lightBox = new LightBox(this);
//		try {
//			lightBox.show();
//		} catch (Exception ignore) {
		show();
//		}
	}

	private void positionDialog() {
		final int clientWidth = Window.getClientWidth();
		final int clientHeight = Window.getClientHeight();
		final int popupWidth = (int) Math.max(clientWidth * widthFraction, minWidth);
		final int popupHeight = (int) Math.max(clientHeight * heightFraction, minHeight);
		final int posX = (clientWidth - popupWidth) / 2;
		final int posY = (clientHeight - popupHeight) / 2;
		setSize(popupWidth + "px", popupHeight + "px");
		setPopupPosition(posX, posY);
	}
}
