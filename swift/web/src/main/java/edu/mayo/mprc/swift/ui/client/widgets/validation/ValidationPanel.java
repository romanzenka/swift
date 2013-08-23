package edu.mayo.mprc.swift.ui.client.widgets.validation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.swift.ui.client.dialogs.ErrorDialog;
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
	public interface SeverityImageBundle extends ImageBundle {
		@Resource("edu/mayo/mprc/swift/ui/public/images/20pxblank.png")
		AbstractImagePrototype none();

		@Resource("edu/mayo/mprc/swift/ui/public/images/info.png")
		AbstractImagePrototype info();

		@Resource("edu/mayo/mprc/swift/ui/public/images/warning.png")
		AbstractImagePrototype warning();

		@Resource("edu/mayo/mprc/swift/ui/public/images/error.png")
		AbstractImagePrototype error();
	}

	private static SeverityImageBundle bundle;

	public static synchronized Image getImageForSeverity(final int severity) {
		if (bundle == null) {
			bundle = (SeverityImageBundle) GWT.create(SeverityImageBundle.class);
		}
		switch (severity) {
			case ClientValidation.SEVERITY_NONE:
				return bundle.none().createImage();
			case ClientValidation.SEVERITY_INFO:
				return bundle.info().createImage();
			case ClientValidation.SEVERITY_WARNING:
				return bundle.warning().createImage();
			case ClientValidation.SEVERITY_ERROR:
				return bundle.error().createImage();
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
	private Map<ClientValidation, Validatable> byValidation = new HashMap<ClientValidation, Validatable>();
	private Map<Validatable, List<ClientValidation>> byValidatable = new HashMap<Validatable, List<ClientValidation>>();
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


	public void addValidation(final ClientValidation cv, final Validatable v) {
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

	public void removeValidationsFor(final Validatable v) {
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
					ValidationPanel.this.reflow();
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

		public ValidationWidget(final ClientValidation cv) {
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
				pb.addClickListener(new ClickListener() {
					@Override
					public void onClick(final Widget sender) {
						popup();
					}
				});
			}

			fp.addClickListener(new ClickListener() {
				@Override
				public void onClick(final Widget sender) {
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
			final Validatable v = (Validatable) byValidation.get(cv);
			v.focus();
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
