package edu.mayo.mprc.dbcurator.client.steppanels;

import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.common.client.ExceptionUtilities;
import edu.mayo.mprc.dbcurator.client.curatorstubs.CurationStepStub;
import edu.mayo.mprc.dbcurator.client.curatorstubs.HeaderFilterStepStub;

import java.util.Date;

/**
 * A mainPanel that will hold all of the information necessary for
 *
 * @author Eric Winter
 */
public final class HeaderFilterStepPanel extends AbstractStepPanel {
	private HeaderFilterStepStub containedStep;

	private TextBox criteria = new TextBox();
	private RadioButton radioModeSimple;
	private RadioButton radioModeRegEx;

	private RadioButton radioLogicalAny;
	private RadioButton radioLogicalAll;
	private RadioButton radioLogicalNone;

	private VerticalPanel mainPanel = new VerticalPanel();
	public static final String TITLE = "Filter Sequences by Header Content";

	/**
	 * CTOR
	 * Initializes the widget and sets the components to default values
	 */
	public HeaderFilterStepPanel() {

		final long radioGroup = new Date().getTime();
		final String modeGroup = String.valueOf(radioGroup);
		final String logicalGroup = String.valueOf(radioGroup + 1);

		radioModeSimple = new RadioButton(modeGroup, "Simple Text");
		radioModeRegEx = new RadioButton(modeGroup, "Regular Expression");

		radioLogicalAny = new RadioButton(logicalGroup, "Or");
		radioLogicalAny.setTitle("For headers with any of the terms in them.");
		radioLogicalAll = new RadioButton(logicalGroup, "And");
		radioLogicalAll.setTitle("For headers with all of the terms in them.");
		radioLogicalNone = new RadioButton(logicalGroup, "None");
		radioLogicalNone.setTitle("For headers that do not contain any of the terms.");

		initWidget(mainPanel);
		mainPanel.add(new Label("Enter filter criteria: "));
		mainPanel.setSpacing(5);

		criteria.setWidth("300px");
		mainPanel.add(criteria);

		final HorizontalPanel textModePanel = new HorizontalPanel();
		textModePanel.setSpacing(5);
		textModePanel.add(new Label("Text search mode: "));
		radioModeSimple.setChecked(true);
		textModePanel.add(radioModeSimple);
		textModePanel.add(radioModeRegEx);

		final HorizontalPanel logicModePanel = new HorizontalPanel();
		logicModePanel.setSpacing(5);
		logicModePanel.add(new Label("Logical Mode: "));
		radioLogicalAny.setChecked(true);
		logicModePanel.add(radioLogicalAny);
		logicModePanel.add(radioLogicalAll);
		logicModePanel.add(radioLogicalNone);

		super.setTitle(TITLE);

		mainPanel.add(textModePanel);
		mainPanel.add(logicModePanel);

	}

	/**
	 * returns the containedStep that this mainPanel will represent.  This is used to get generic containedStep information such as the
	 * completion count, list of messages, etc.
	 *
	 * @return the containedStep that this mainPanel represents
	 */
	@Override
	public CurationStepStub getContainedStep() {

		this.containedStep.criteria = this.criteria.getText();

		//see which match mode should be used
		this.containedStep.textMode = (radioModeSimple.isChecked() ? "simple" : "regex");

		//look at which mode is checked and set appropriately
		this.containedStep.matchMode = (radioLogicalAll.isChecked() ? "all" : (radioLogicalNone.isChecked() ? "none" : "any"));

		return this.containedStep;
	}

	/**
	 * Set the containedStep associated with this StepPanel.
	 *
	 * @param step the containedStep you want this mainPanel to represent
	 * @throws ClassCastException if the containedStep passed in wasn't the type that the Panel can represent
	 */
	@Override
	public void setContainedStep(final CurationStepStub step) throws ClassCastException {
		if (!(step instanceof HeaderFilterStepStub)) {
			ExceptionUtilities.throwCastException(step, HeaderFilterStepStub.class);
			return;
		}
		this.containedStep = (HeaderFilterStepStub) step;
		this.setTitle("Filter sequences by header content");
		update();
	}

	/**
	 * gets a css style name that should be associated with this mainPanel.
	 *
	 * @return a css style to use in conjunction with this mainPanel
	 */
	@Override
	public String getStyle() {
		return "shell-header-headerfilter";
	}

	/**
	 * call this method when this mainPanel should look for updates in its contained containedStep
	 */
	@Override
	public void update() {
		criteria.setText(this.containedStep.criteria);

		final String logicalMode = this.containedStep.matchMode;
		if (logicalMode.equalsIgnoreCase("none")) {
			radioLogicalNone.setChecked(true);
		} else if (logicalMode.equalsIgnoreCase("all")) {
			radioLogicalAll.setChecked(true);
		} else {
			radioLogicalAny.setChecked(true);
		}

		if (this.containedStep.textMode.equalsIgnoreCase("regex")) {
			radioModeRegEx.setChecked(true);
		} else {
			radioModeSimple.setChecked(true);
		}
	}

	@Override
	public String getImageURL() {
		return "images/step-icon-filter.png";
	}

}
