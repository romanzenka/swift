package edu.mayo.mprc.dbcurator.client.steppanels;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.dbcurator.client.curatorstubs.CurationStepStub;


/**
 * A mainPanel that manages the features that are common to all StepPanels these features include
 * - move up button
 * - move down button
 * - removal button
 * - ability to drag-and-drop for movement
 * - title bar (include a step type icon implemented with CSS)
 * - sequence count of result
 */
class StepPanelShell extends Composite {
	/**
	 * the panel that this shell is based on
	 */
	private DockPanel panel = new DockPanel();

	/**
	 * the step that this mainPanel will provide the window dressing for
	 */
	private final AbstractStepPanel containedStepPanel;

	/**
	 * the mainPanel that contains this shell and is used to order the steps
	 */
	private final StepPanelContainer container;

	/**
	 * the title, this will depend on the contained step
	 */
	private Label title = new Label("");

	/**
	 * the place to put the progress ('%' suffix) or the number of sequences once complete ('#' prefix) or not run ("---")
	 */
	private Label completionCount = new Label("-");

	/**
	 * A text area to put any error messages that were given in the step
	 */
	private TextArea txtErrorReport = new TextArea();

	private Label lblStepNumber = new Label("");


	/**
	 * constructor that sets up the drag-and-drop functionality of these Panels
	 *
	 * @param toContain the StepPanel that we will wrap around
	 * @param container the panel that contains this shell
	 */
	public StepPanelShell(final AbstractStepPanel toContain, final StepPanelContainer container) {
		this.containedStepPanel = toContain;

		this.container = container;

		this.panel.add(initializeHeader(toContain), DockPanel.NORTH);
		this.panel.add(initializeFooter(), DockPanel.SOUTH);

		toContain.addStyleName("stepshell-containedstep");
		this.panel.add(toContain, DockPanel.CENTER);

		this.panel.addStyleName("stepshell-panel");

		initWidget(this.panel);

		//tell the parent classes that we are interested in mouse events
		sinkEvents(Event.MOUSEEVENTS);
	}

	/**
	 * update this shell assuming there hasn't been a change in step
	 */
	public void update() {
		this.update(this.containedStepPanel.getContainedStep());
	}

	/**  */
	private boolean minimized = false;
	private Panel collapsePanel = new AbsolutePanel();
	private Image collapseButton;
	private Image expandButton;

	public void toggleCollapse() {
		minimized = !minimized;
		if (minimized) {
			panel.remove(containedStepPanel);
			collapsePanel.remove(collapseButton);
			collapsePanel.add(expandButton);
		} else {
			panel.add(containedStepPanel, DockPanel.CENTER);
			collapsePanel.remove(expandButton);
			collapsePanel.add(collapseButton);
		}
	}

	/**
	 * Creates the header for this shell.  The header contains such things as the step number, title, and step movement buttons
	 *
	 * @param toContain step that this shell contains
	 * @return the header that should be used
	 */
	private Panel initializeHeader(final AbstractStepPanel toContain) {
		final FlowPanel header = new FlowPanel();

		header.setStyleName("stepshell-header-panel");

		this.lblStepNumber.setStyleName("stepshell-stepnumber");
		header.add(this.lblStepNumber);

		expandButton = new Image("images/rightarrow.png");
		expandButton.addClickListener(new ClickListener() {
			@Override
			public void onClick(final Widget sender) {
				toggleCollapse();
			}
		});

		collapseButton = new Image("images/downarrow.png");
		collapseButton.addClickListener(new ClickListener() {
			@Override
			public void onClick(final Widget sender) {
				toggleCollapse();
			}
		});

		collapsePanel.add(collapseButton);
		collapsePanel.addStyleName("stepshell-header-collapserpanel");
		header.add(collapsePanel);

		this.title.setText(toContain.getTitle());
		this.title.setStyleName("shell-header-title");

		final Image removalButton = new Image("images/delete.png");
		removalButton.setStyleName("stepshell-header-stepremover");
//		removalButton.setOnStyle("stepshell-header-stepremover");
//		removalButton.setOffStyle("stepshell-header-stepremover");
		removalButton.addClickListener(new ClickListener() {
			@Override
			public void onClick(final Widget sender) {
				if (StepPanelShell.this.containedStepPanel.isEditable()) {
					container.remove(StepPanelShell.this);
					container.refresh();
				}
			}
		});
		header.add(removalButton);

		final Image image = new Image(this.containedStepPanel.getImageURL());
		image.addStyleName("shell-header-stepimage");
		header.add(image);

		header.add(this.title);
		return header;
	}

	/**
	 * setup the footer panel which will contain all of the progress information
	 *
	 * @return the footer panel
	 */
	private Panel initializeFooter() {
		final FlowPanel panel = new FlowPanel();

		panel.setStyleName("stepshell-footer");
		this.txtErrorReport.setVisibleLines(1);
		this.txtErrorReport.setStyleName("step-error-report");
		panel.add(txtErrorReport);
		completionCount.addStyleName("stepshell-completioncount");
		panel.add(completionCount);

		return panel;
	}


	/**
	 * get the mainPanel that this shell encompasses
	 *
	 * @return the mainPanel this shell encompasses
	 */
	public AbstractStepPanel getContainedStepPanel() {
		this.containedStepPanel.getContainedStep();
		return this.containedStepPanel;
	}

	/**
	 * method to call when we want to load this shell
	 */
	@Override
	protected void onLoad() {
		super.onLoad();
	}

	/**
	 * tells this shell that it should be updated hence instructing the contained StepPanel to also refresh itself
	 *
	 * @param stub the stub we want to update the panel with.
	 */
	public void update(final CurationStepStub stub) {
		this.containedStepPanel.setContainedStep(stub);

		this.lblStepNumber.setText(String.valueOf(this.container.getWidgetIndex(this) + 1));

		//print any error messages out into the error message box
		if (this.containedStepPanel.getContainedStep().getErrorMessages() != null
				&& !this.containedStepPanel.getContainedStep().getErrorMessages().isEmpty()) {
			final StringBuilder builder = new StringBuilder();
			for (final Object o : this.containedStepPanel.getContainedStep().getErrorMessages()) {
				builder.append((String) o);
				builder.append("\n");
			}

			//if the step has failed we want to change our style to indicate a failed step
			this.txtErrorReport.setText(builder.toString());
			panel.addStyleName("stepshell-inerror");
			this.containedStepPanel.addStyleName("stepshell-containedstep-inerror");
		}

		//if the step has been completed then we want to change our style to indicate that we have completed
		if (this.containedStepPanel.getContainedStep() != null
				&& this.getContainedStepPanel().getContainedStep().getCompletionCount() != null) {
			this.containedStepPanel.addStyleName("stepshell-containedstep-complete");
			this.panel.addStyleName("stepshell-complete");
			this.txtErrorReport.setText("Step completed successfully");
			this.title.removeStyleName("stepshell-title-inprogress");
			final Integer completionCount = this.getContainedStepPanel().getContainedStep().getCompletionCount();

			this.completionCount.setText((completionCount == null ? "???" : commaFormatNumber(completionCount)));
		} else if (this.containedStepPanel.getContainedStep().getProgress() != null) {
			this.completionCount.setText(this.containedStepPanel.getContainedStep().getProgress().toString() + "%");
			this.txtErrorReport.setText("Step is running");

			this.title.addStyleName("stepshell-title-inprogress");
		} else {
			this.completionCount.setText("---");
		}
	}

	/**
	 * format the number to include the commas.  This is a little verbose because of the 1.4.2 limitation of GWT and
	 * no supporting the format methods on string ("printf").
	 *
	 * @param toFormat the number you want to but commas into
	 * @return the number with commas inserted
	 */
	private static String commaFormatNumber(final Integer toFormat) {
		final int length = toFormat.toString().length();
		String numberAsString = toFormat.toString();
		int currentPlacer = length - 3;

		while (currentPlacer > 0) {
			numberAsString = numberAsString.substring(0, currentPlacer) + "," + numberAsString.substring(currentPlacer);
			currentPlacer -= 3;
		}

		return numberAsString;
	}

	/**
	 * gets the title bar that is used by this shell in order to use it as a handle for drag-and-drop
	 *
	 * @return the title widget (Label)
	 */
	Label getStepTitle() {
		return this.title;
	}

	public String toString() {
		return "StepPanelShell{" +
				"title=" + title +
				'}';
	}
}
