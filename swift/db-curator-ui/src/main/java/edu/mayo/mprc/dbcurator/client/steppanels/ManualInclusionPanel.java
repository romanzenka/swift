package edu.mayo.mprc.dbcurator.client.steppanels;

import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.common.client.ExceptionUtilities;

/**
 * @author Eric Winter
 */
public final class ManualInclusionPanel extends AbstractStepPanel {

	private ManualInclusionStepStub containedStep;

	private TextBox txtHeader = new TextBox();
	private TextArea txtSequence = new TextArea();

	private static final String WELCOME_SEQUENCE = "Enter a sequence or a header and sequence.";
	private static final String WELCOME_HEADER = "Enter a FASTA header here, no need for right arrow";
	public static final String TITLE = "Paste Individual Sequence";

	public ManualInclusionPanel() {
		final VerticalPanel panel = new VerticalPanel();
		panel.setWidth("100%");
		panel.add(new Label("FASTA Header:"));
		final Panel tempPanel = new HorizontalPanel();
		tempPanel.setWidth("100%");
		final Label arrow = new Label(">");
		tempPanel.add(arrow);
		txtHeader.setWidth("100%");
		txtHeader.setText(WELCOME_HEADER);
		txtHeader.addFocusListener(new FocusListener() {
			@Override
			public void onFocus(final Widget widget) {
				txtHeader.selectAll();
			}

			@Override
			public void onLostFocus(final Widget widget) {
				txtHeader.setText(stripHeaderChar(txtHeader.getText()));
			}
		});
		tempPanel.add(txtHeader);
		panel.add(tempPanel);
		panel.add(new Label("Sequence: "));
		txtSequence.setWidth("100%");
		txtSequence.setText(WELCOME_SEQUENCE);
		txtSequence.addFocusListener(new FocusListener() {
			@Override
			public void onFocus(final Widget widget) {
				txtSequence.selectAll();
			}

			@Override
			public void onLostFocus(final Widget widget) {
				if (!txtSequence.getText().equals(WELCOME_SEQUENCE)) {
					txtSequence.setText(cleanSequence(txtSequence.getText()));
				}
			}
		});
		txtSequence.setVisibleLines(5);
		panel.add(txtSequence);
		panel.setSpacing(5);
		setTitle(TITLE);
		initWidget(panel);
	}

	/**
	 * returns the step that this mainPanel will represent.  This is used to get generic step information such as the
	 * completion count, list of messages, etc.
	 *
	 * @return the step that this mainPanel represents
	 */
	@Override
	public CurationStepStub getContainedStep() {
		containedStep.header = ">" + txtHeader.getText();
		containedStep.sequence = cleanSequence(txtSequence.getText());
		return containedStep;
	}

	/**
	 * Set the step associated with this StepPanel.
	 *
	 * @param step the step you want this mainPanel to represent
	 * @throws ClassCastException if the step passed in wasn't the type that the Panel can represent
	 */
	@Override
	public void setContainedStep(final CurationStepStub step) throws ClassCastException {
		if (!(step instanceof ManualInclusionStepStub)) {
			ExceptionUtilities.throwCastException(step, ManualInclusionStepStub.class);
			return;
		}
		containedStep = (ManualInclusionStepStub) step;
		update();
	}

	@Override
	public String getStyle() {
		return "shell-header-manualinclusion";
	}

	@Override
	public void update() {
		txtHeader.setText(stripHeaderChar(containedStep.header));
		txtSequence.setText(containedStep.sequence);
	}

	/**
	 * takes the header character (right arrow) out of the header if it exists.
	 *
	 * @param toStrip the string to try to strip the right arrow out of
	 * @return the string minus a leading header character if it had existed
	 */
	public String stripHeaderChar(final String toStrip) {
		if (toStrip == null || toStrip.isEmpty()) {
			return "";
		}
		return toStrip.replaceAll("^[>]*[\\s]*", "");

	}

	/**
	 * strips header (if included) and whitespace out of the sequence
	 *
	 * @param toClean the string you want to have changed
	 * @return the sequence with header and white space removed
	 */
	public String cleanSequence(String toClean) {
		if (toClean == null || toClean.isEmpty()) {
			return toClean;
		}

		//if this includes a header then strip out between the header filter and th
		if (toClean.charAt(0) == '>') {
			final int startChar = 1;
			int endChar = toClean.indexOf('\n', 1);
			if (endChar < 0) {
				endChar = startChar;
			}
			txtHeader.setText(stripHeaderChar(toClean.substring(startChar, endChar)));

			toClean = toClean.substring(endChar + 1);
		}

		//now take out any whitespace
		toClean = toClean.replaceAll("\\s", "");


		return toClean;
	}

	@Override
	public String getImageURL() {
		return "images/step-icon-add.png";
	}

}
