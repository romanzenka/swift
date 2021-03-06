package edu.mayo.mprc.swift.ui.client.dialogs;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValidation;

import java.io.Serializable;
import java.util.*;

/**
 * A Widget which displays the results of (server-side) validation operations
 * associated with one or more other Widgets.  This widget reserves space in
 * the UI for a fixed number of possible Validation displays, and then
 * either scrolls, or pops up as necessary to show validations that don't fit.
 */
public final class ValidationPanel extends Composite {
	public static Image getImageForSeverity(final int severity) {
		switch (severity) {
			case ClientValidation.SEVERITY_NONE:
				return new Image("images/20pxblank.png");
			case ClientValidation.SEVERITY_INFO:
				return new Image("images/info.png");
			case ClientValidation.SEVERITY_WARNING:
				return new Image("images/warning.png");
			case ClientValidation.SEVERITY_ERROR:
				return new Image("images/error.png");
			default:
				throw new RuntimeException("Unknown severity " + severity);
		}
	}

	public static String getSeverityName(final int severity) {
		switch (severity) {
			case ClientValidation.SEVERITY_NONE:
				return "";
			case ClientValidation.SEVERITY_INFO:
				return "Info";
			case ClientValidation.SEVERITY_WARNING:
				return "Warning";
			case ClientValidation.SEVERITY_ERROR:
				return "Error";
			default:
				throw new RuntimeException("Unknown severity " + severity);
		}
	}

	private List<ClientValidation> currentValidations = new ArrayList<ClientValidation>();
	private Map<ClientValidation, Object> byValidation = new HashMap<ClientValidation, Object>();
	private Map<Object, List<ClientValidation>> byValidatable = new HashMap<Object, List<ClientValidation>>();
	private VerticalPanel vp = new VerticalPanel();
	private int numLines;
	private boolean reflowing;

	/**
	 * Create a panel that can display the given number of validation lines.
	 *
	 * @param numLines Number of validations to display in vertical lines;
	 */
	public ValidationPanel(final int numLines) {
		this.numLines = numLines;

		initWidget(vp);
	}


	public void addValidation(final ClientValidation cv, final Object v) {
		if (!currentValidations.contains(cv)) {
			currentValidations.add(cv);
			byValidation.put(cv, v);
			List<ClientValidation> al = byValidatable.get(v);
			if (al == null) {
				al = new ArrayList<ClientValidation>();
				byValidatable.put(v, al);
			}
			al.add(cv);
		}

		delayedReflow();
	}

	public void removeValidation(final ClientValidation cv) {
		currentValidations.remove(cv);
		delayedReflow();
	}

	public void removeValidationsFor(final Object v) {
		final List<ClientValidation> al = byValidatable.get(v);
		if (al == null) {
			return;
		}
		for (final ClientValidation cv : al) {
			currentValidations.remove(cv);
			byValidation.remove(cv);
		}
		byValidatable.remove(v);
		delayedReflow();
	}

	/**
	 * Schedule a reflow later.
	 */
	protected void delayedReflow() {
		// check if one is already scheduled.
		if (!reflowing) {
			reflowing = true;
			DeferredCommand.addCommand(new Command() {
				@Override
				public void execute() {
					reflow();
				}
			});
		}
	}

	private void reflow() {
		reflowing = false;
		// first, sort the list of validations by severity.
		Collections.sort(currentValidations, new ValidationComparator());
		vp.clear();  // TODO lame!
		int i = 0;
		for (int slotsLeft = numLines; slotsLeft >= 1 && i < currentValidations.size(); --slotsLeft) {
			final ClientValidation cv = currentValidations.get(i);
			vp.add(new ValidationWidget(cv));
			++i;
		}
		// TODO What do we do when all validations do not fit?
	}

	private class ValidationWidget extends Composite {
		private ClientValidation cv;
		private Image img;
		private FocusPanel fp = new FocusPanel();

		private FlowPanel hp = new FlowPanel();

		ValidationWidget(final ClientValidation cv) {
			this.cv = cv;
			img = getImageForSeverity(cv.getSeverity());
			img.addStyleName("params-validation");
			hp.add(img);
			final Label label;
			hp.add(label = new Label(cv.getMessage()));
			label.addStyleName("params-validation");

			if (cv.getThrowableMessage() != null) {
				final HTML sp = new HTML("&nbsp;&nbsp;");
				sp.addStyleName("params-validation");
				hp.add(sp);
				final HTML pb;
				hp.add(pb = new HTML("(more)"));
				pb.addStyleName("actionLink");
				pb.addStyleName("params-validation");
				pb.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(final ClickEvent event) {
						popup();
					}
				});
			}

			fp.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(final ClickEvent event) {
					focus();
				}
			});

			fp.add(hp);
			initWidget(fp);
		}

		public void popup() {
			final VerticalPanel vp = new VerticalPanel();
			final HorizontalPanel hp = new HorizontalPanel();
			hp.add(getImageForSeverity(cv.getSeverity()));
			final Label label = new Label(cv.getMessage());
			label.setWordWrap(true);
			hp.add(label);
			vp.add(hp);
			if (cv.getThrowableMessage() != null) {
				final TextArea ta = new TextArea();
				ta.setText(cv.getThrowableMessage());
				ta.setEnabled(false);
				ta.setSize("400px", "300px");
			}

			ErrorDialog.show(cv, this);
		}

		public void focus() {
			Object o = byValidation.get(cv);
			if (o instanceof Validatable) {
				final Validatable v = (Validatable) o;
				v.focus();
			} else if (o instanceof Focusable) {
				final Focusable w = (Focusable) o;
				w.setFocus(true);
			}
		}

	}

	private static final class ValidationComparator implements Comparator<ClientValidation>, Serializable {
		private static final long serialVersionUID = 20101221L;

		@Override
		public int compare(final ClientValidation o1, final ClientValidation o2) {
			return o2.getSeverity() - o1.getSeverity(); // reverse order of integer severity (ie: errors first).
		}
	}

}
