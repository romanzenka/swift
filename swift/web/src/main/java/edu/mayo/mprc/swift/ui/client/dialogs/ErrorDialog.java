package edu.mayo.mprc.swift.ui.client.dialogs;

import com.google.gwt.event.shared.UmbrellaException;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.common.client.GWTServiceException;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValidation;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple dialog for showing errors.
 */
public final class ErrorDialog extends DialogBox {

	private VerticalPanel verticalPanel;
	private Widget widget;

	private ErrorDialog(final Throwable t) {
		super(true);
		if (t instanceof GWTServiceException) {
			init(ClientValidation.SEVERITY_ERROR, t.getMessage(), getDetailedMessage(t));
		} else {
			init(ClientValidation.SEVERITY_ERROR, t == null ? "Unknown error" : t.getMessage(), getDetailedMessage(t));
		}
		show();
	}

	public static String getDetailedMessage(final Throwable t) {
		Throwable current = t;
		Throwable previous = null;
		final StringBuilder message = new StringBuilder();
		final List<Throwable> list = new ArrayList<Throwable>();
		while (current != null && !current.equals(previous)) {
			if (exceptionMessageDiffers(current, previous)) {
				list.add(current);
			}
			previous = current;
			current = current.getCause();
		}

		// Let's go from the innermost to outermost and skip the trivial messages.
		// Trivial message happens when you wrap
		for (int i = 0; i < list.size(); i++) {
			final Throwable curr = list.get(i);
			if (i < list.size() - 1) {
				final Throwable next = list.get(i + 1);
				// Compare current to the next throwable. Is the next just wrapping the current, preending the exception class name?
				final String nonInformativeWrap = next.getClass().getName() + ": " + next.getMessage();
				if (curr.getMessage().equalsIgnoreCase(nonInformativeWrap)) {
					continue;
				}
			}
			if (i > 0) {
				message.append("<br/>&rarr; ");
			}
			if (curr instanceof NullPointerException) {
				message.append("Null pointer exception");
			} else {
				message.append(curr.getMessage());
			}
		}
		return message.toString();
	}

	private static String getThrowableMessage(final Throwable throwable) {
		if (throwable == null) {
			return null;
		}
		if (throwable instanceof UmbrellaException) {
			final StringBuilder result = new StringBuilder();
			for (final Throwable t2 : ((UmbrellaException) throwable).getCauses()) {
				result.append(getDetailedMessage(t2));
				result.append("<br/>");
			}
			return result.toString();
		}
		return throwable.getMessage();
	}

	private static boolean exceptionMessageDiffers(final Throwable current, final Throwable previous) {

		if (previous == null) {
			return true; // We always differ from initial null.
		}
		final String currentMessage = getThrowableMessage(current);

		// Going to null message always means no difference (just a loss of signal)
		return currentMessage != null && (!currentMessage.equalsIgnoreCase(getThrowableMessage(previous)));
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
