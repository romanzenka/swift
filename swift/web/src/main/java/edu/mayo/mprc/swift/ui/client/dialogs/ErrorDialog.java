package edu.mayo.mprc.swift.ui.client.dialogs;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.common.client.GWTServiceException;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValidation;

/**
 * Simple dialog for showing errors.
 */
public final class ErrorDialog extends DialogBox {

	private VerticalPanel verticalPanel;
	private Widget widget;

	private ErrorDialog(final Throwable t) {
		super(true);
		if (t instanceof GWTServiceException) {
			init(ClientValidation.SEVERITY_ERROR, t.getMessage(), ((GWTServiceException) t).getStackTraceAsString());
		} else {
			init(ClientValidation.SEVERITY_ERROR, t.getMessage(), null);
		}
		show();
	}

	private ErrorDialog(final ClientValidation cv, final Widget validationWidget) {
		super(true);
		init(cv.getSeverity(), cv.getMessage(), cv.getThrowableMessage());
		widget = validationWidget;
		setWidth(validationWidget.getOffsetWidth() + " px");
		setHeight("100px");
		setPopupPositionAndShow(new MyPositionCallback());
	}

	public static void show(final ClientValidation cv, final Widget validationWidget) {
		final ErrorDialog dialog = new ErrorDialog(cv, validationWidget);
		dialog.show();
	}

	public static void show(final Throwable t) {
		final ErrorDialog dialog = new ErrorDialog(t);
		dialog.show();
	}

	/**
	 * Deal with errors not associated with any specific widget.
	 * TODO Does this really belong here?
	 */
	public static void handleGlobalError(final Throwable t) {
		show(t);
	}

	private void init(final int severity, final String shortMessage, final String detailedMessage) {
		setText(ValidationPanel.getSeverityName(severity));
		setStyleName("errorDialog");
		verticalPanel = new VerticalPanel();
		final HorizontalPanel hp = new HorizontalPanel();
		hp.add(ValidationPanel.getImageForSeverity(severity));
		hp.add(new Label(shortMessage));
		verticalPanel.add(hp);
		final ScrollPanel pane = new ScrollPanel();
		pane.setSize("700px", "300px");
		pane.setStyleName("errorPane");
		final HTML html = new HTML("<br/><pre>" + detailedMessage + "</pre>");
		pane.add(html);
		verticalPanel.add(pane);
		setWidget(verticalPanel);
	}

	class MyPositionCallback implements PositionCallback {
		@Override
		public void setPosition(final int width, final int height) {
			final int clientHeight = Window.getClientHeight();

			setPopupPosition(widget.getAbsoluteLeft(), widget.getAbsoluteTop() + widget.getOffsetHeight());
			if (height > clientHeight) {
				setHeight(clientHeight - getAbsoluteTop() + " px");
			}
		}
	}
}
