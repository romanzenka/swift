package edu.mayo.mprc.dbcurator.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.dbcurator.client.steppanels.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * An application that will request and display a curation as well as allow editing, saving, and running that curation
 */
public final class CurationEditor extends Composite {
	private static final int WINDOW_MARGIN = 50;

	/**
	 * A timer that should be used to cause requests to be made to the server.  We can change how often these updates are made
	 * this timer will just make a call to the syncCuration method
	 */
	private final Timer updateTimer = new Timer() {

		@Override
		public void run() {
			if (!curationIsRunning) {
				cancel();
			} else {
				syncCuration();
			}
		}
	};

	private Panel controlPanel;

	private boolean curationIsRunning = false;
	private final MessageManager messageManager = new MessageManager();
	private CurationStub curation = new CurationStub();
	private EditorCloseCallback closeCallback = null;
	private Integer initialCurationID = null;

	//widgets and other UI related things...
	private final Label lblID = new Label();
	private final Label lblShortNameError = new Label("");
	private Panel commandPanel;
	private final Anchor cmdRun = new Anchor("Run");
	private Anchor cmdView = new Anchor("View");

	private final TextArea textareaLog = new TextArea();
	private final TextBox txtShortName = new TextBox();
	private final TextBox txtTitle = new TextBox();
	private final TextArea txtNotes = new TextArea();
	private final TextBox txtDecoy = new TextBox();
	private final TextBox txtEmail = new TextBox();
	private final HTML htmlStepSelectionTitle = new HTML();
	/**
	 * holds the steps that are already part of the displayed curation
	 */
	private StepPanelContainer stepContainer;
	private final Label lblResultFilePath = new Label("Resulting File (click to copy)");
	private final CheckBox chkShowLog = new CheckBox();
	private final ListBox lstStepChoice = new ListBox();
	private final HorizontalPanel appPanel = new HorizontalPanel();
	private Panel stepOperationPanel = new VerticalPanel();
	private Panel stepInfoPanel = new VerticalPanel();
	private HandlerRegistration closeHandler = null;

	private final Map<String, String> userEmailInitialPairs;
	private final String currentUserEmail;

	private final CommonDataRequesterAsync commonDataRequester;

	/**
	 * this is an alternatie constructor that will load a given curation and call a callback when the user clicks
	 * the close button.  This is used for "embedded mode" meaning that another GWT application can call and be notified
	 * when certain events happen.
	 *
	 * @param curationToDisplay the id of the curation you wanted loaded
	 * @param closeCallback     a method we will call when the user clicks close.  As part of the callback will be the id of the currently open curation.
	 */
	public CurationEditor(final Integer curationToDisplay, final String currentUserEmail, final Map<String, String> userEmailInitialPairs, final EditorCloseCallback closeCallback) {
		commonDataRequester = GWT.create(CommonDataRequester.class);
		final ServiceDefTarget endpoint = (ServiceDefTarget) commonDataRequester;
		endpoint.setServiceEntryPoint(GWT.getModuleBaseURL() + "CommonDataRequester");

		this.closeCallback = closeCallback;
		initialCurationID = curationToDisplay;

		this.userEmailInitialPairs = userEmailInitialPairs;
		this.currentUserEmail = currentUserEmail;

		init(false);
		retreiveCuration(curationToDisplay);
	}

	/**
	 * retrieves which curation should be displayed from the server.
	 */
	private void retreiveCuration(final Integer requestedCurationID) {
		messageManager.clearMessages();

		if (requestedCurationID == null) {
			commonDataRequester.lookForCuration(new RetreivalCallback());
		} else {
			commonDataRequester.getCurationByID(requestedCurationID, new RetreivalCallback());
		}

	}

	private void init(final boolean standalone) {
		initWidget(appPanel);
		loadPanel();
		Window.enableScrolling(!standalone);
		setupCloseHandlers();
	}

	private void setupCloseHandlers() {

		closeHandler = Window.addWindowClosingHandler(new Window.ClosingHandler() {
			@Override
			public void onWindowClosing(final Window.ClosingEvent event) {
				//if the curation has not been run then make sure they are given a chance to abort.
				if (curation.hasBeenRun()) {
					event.setMessage(null);
				} else {
					event.setMessage("In order to use this database its curation must be run.\n" +
							"To do so, press cancel below and then click run.");
				}
			}
		});

	}

	/**
	 * call this method to copy the displayed curation for all except for the id and short name to allow further editing
	 */
	private void copyCuration() {
		messageManager.clearMessages();
		commonDataRequester.copyCurationStub(curation, new RetreivalCallback(true));
	}

	private void showPopupMessage(final String message) {
		final MessagePopup popup = new MessagePopup(message, getAbsoluteLeft() + 100, getAbsoluteTop() + 100);
		popup.show(0);
	}

	/**
	 * set the Curation that is associated with this editor
	 *
	 * @param curation the curation we want to work with
	 */
	public void setCuration(final CurationStub curation, final boolean isCopy) {
		if (curation == null) {
			showPopupMessage("No curation found, please try again");
			return;
		}

		this.curation = curation;

		if (this.curation.getErrorMessages() != null && !this.curation.getErrorMessages().isEmpty()) {
			for (Iterator i = this.curation.getErrorMessages().iterator(); i.hasNext(); ) {
				messageManager.addMessage((String) i.next());
			}
		} else {
			messageManager.clearMessages();
		}

		if (isCopy) {
			this.curation.setOwnerEmail(currentUserEmail);
		}

		loadPanel();
		auditEditState();
	}

	/**
	 * Starts a new curation. When creating a new curation, the info fields are retaining their original values,
	 * in case the user started editing them and then decided to press "new".
	 */
	public void startNewCuration() {
		curation = new CurationStub();
		curation.setShortName(txtShortName.getText());
		curation.setTitle(txtTitle.getText());
		curation.setNotes(txtNotes.getText());
		curation.setDecoyRegex(txtDecoy.getText());
		curation.setOwnerEmail(currentUserEmail);
		messageManager.clearMessages();
		loadPanel();
		auditEditState();
	}

	/**
	 * call this method when you want to loadPanel the panel.  This will initialize all of the sub widgets and loadPanel
	 * the contents of the currently contained CurationStub.
	 */
	private void loadPanel() {
		appPanel.setSpacing(10);

		//remove the old panels that were already in place since we have substantial updates
		appPanel.remove(stepOperationPanel);
		appPanel.remove(stepInfoPanel);

		//create new panels
		stepOperationPanel = getStepSelectionPanel();
		stepInfoPanel = getRightPanel();

		//add the panels to the main panel
		appPanel.add(stepOperationPanel);
		appPanel.add(stepInfoPanel);

		appPanel.setStyleName("curation-editor-border");

		refreshPanel();
	}

	/**
	 * call this method if you want to externally set the size of the curator.  This is needed to size some of the internal widgets appropriately.
	 *
	 * @param width
	 * @param height
	 */
	@Override
	public void setPixelSize(final int width, final int height) {
		super.setPixelSize(width, height);

		final int stepContainerHeight = height - CurationEditor.WINDOW_MARGIN;
		final int stepContainerWidth = width - CurationEditor.WINDOW_MARGIN - controlPanel.getOffsetWidth();

		if (stepContainer != null) {
			stepContainer.setPixelSize(stepContainerHeight, stepContainerWidth);
		}
	}

	/**
	 * gets the panel that will contain the step selection, log, and everything else on the left side
	 */
	private Panel getStepSelectionPanel() {
		controlPanel = new VerticalPanel();

		final HTML title = new HTML("<h1 id=\"appTitle\">FASTA Database Curation Tool</h1>");
		controlPanel.add(title);

		lblShortNameError.setText("Short name ok");
		lblShortNameError.setStyleName("warning-label");
		lblShortNameError.addStyleName("warning-label-ok");
		controlPanel.add(lblShortNameError);

		final Grid propGrid = new Grid(5, 2);
		propGrid.setStyleName("db-curator-properties");
		controlPanel.add(propGrid);

		final HTML shortName = new HTML("Short name<sup><font color=\"red\">*</font></sup>:");

		shortName.setTitle("A name for submitting to search engines");

		txtShortName.setMaxLength(CurationValidation.SHORTNAME_MAX_LENGTH);
		txtShortName.addKeyUpHandler(new KeyUpHandler() {
			@Override
			public void onKeyUp(final KeyUpEvent event) {
				validateShortName(txtShortName.getText());
			}
		});
		propGrid.setWidget(0, 0, shortName);
		propGrid.setWidget(0, 1, txtShortName);

		final Label lblDescription = new Label("Description: ");
		lblDescription.setTitle("A short description for this database to help you find it later");
		txtTitle.setVisibleLength(60);
		propGrid.setWidget(1, 0, lblDescription);
		propGrid.setWidget(1, 1, txtTitle);

		final Label lblNote = new Label("Notes: ");
		lblNote.setTitle("Notes about this database");

		txtNotes.setVisibleLines(3);
		txtNotes.setCharacterWidth(43);
		propGrid.setWidget(2, 0, lblNote);
		propGrid.setWidget(2, 1, txtNotes);

		final Label lblDecoy = new Label("Decoy prefix: ");
		lblDecoy.setTitle("A string describing which accession numbers in the database correspond to decoys");
		txtDecoy.setVisibleLength(20);
		propGrid.setWidget(3, 0, lblDecoy);
		propGrid.setWidget(3, 1, txtDecoy);

		final Label lblEmail = new Label("User Initials: ");
		lblEmail.setTitle("User that created the database");
		txtEmail.setVisibleLength(3);

		if (userEmailInitialPairs != null) {
			txtEmail.setEnabled(false);
		} else {
			txtEmail.setEnabled(true);
		}

		propGrid.setWidget(4, 0, lblEmail);
		propGrid.setWidget(4, 1, txtEmail);

		lblResultFilePath.setStyleName("resultfilepath");
		lblResultFilePath.addMouseDownHandler(new MouseDownHandler() {
			@Override
			public void onMouseDown(final MouseDownEvent event) {
				final PopupPanel pop = new PopupPanel(/*autoHide*/true);
				pop.setPopupPosition(lblResultFilePath.getAbsoluteLeft(), lblResultFilePath.getAbsoluteTop());
				final TextBox txtPath = new TextBox();
				txtPath.setText(lblResultFilePath.getTitle());
				pop.add(txtPath);
				pop.show();
			}
		});
		controlPanel.add(lblResultFilePath);


		controlPanel.add(htmlStepSelectionTitle);

		final StringBuilder sb = new StringBuilder(100);
		sb.append("Drag the tops of steps to rearrange.<br>");
		sb.append("Click 'New' to create a new Curation.<br>");
		sb.append("Click 'Copy' to copy current Curation.<br>");
		sb.append("Click 'Run' to execute.<br>");
		sb.append("Click 'Close' to return.<br><br>");

		controlPanel.add(new HTML(sb.toString()));

		initStepChoiceList();
		controlPanel.add(lstStepChoice);

		commandPanel = createCommandPanel();
		controlPanel.add(commandPanel);

		chkShowLog.setText("Show Log:");
		chkShowLog.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				textareaLog.setVisible(Boolean.TRUE.equals(chkShowLog.getValue()));
			}
		});
		controlPanel.add(chkShowLog);

		textareaLog.setStyleName("error-log");
		textareaLog.setEnabled(false);
		textareaLog.setVisibleLines(10);
		messageManager.setTextArea(textareaLog);

		controlPanel.add(textareaLog);

		return controlPanel;
	}

	private boolean validateShortName(final String toValidate) {

		final String spacesMessage = "Must not contain anything but a-z A-Z 0-9 : _ . - ( ) (no spaces)";
		final String lengthMessage = "Must be between " + CurationValidation.SHORTNAME_MIN_LENGTH + " and " + CurationValidation.SHORTNAME_MAX_LENGTH + " characters";

		final String errorMessage = CurationValidation.validateShortNameLegalCharacters(toValidate);
		if (errorMessage != null) {
			lblShortNameError.setText(errorMessage);
			lblShortNameError.removeStyleName("warning-label-ok");
			return false;
		} else {
			lblShortNameError.addStyleName("warning-label-ok");
			commonDataRequester.isShortnameUnique(txtShortName.getText(), new AsyncCallback<Boolean>() {
				@Override
				public void onFailure(final Throwable throwable) {
					Window.alert("We could not determine that uniquness of the entered shortname:\n" + throwable.getMessage());
				}

				@Override
				public void onSuccess(final Boolean isUnique) {
					if (!isUnique) {
						lblShortNameError.setText("Non-unique shortname");
						lblShortNameError.removeStyleName("warning-label-ok");
					} else {
						lblShortNameError.addStyleName("warning-label-ok");
					}
				}
			});
			return true;
		}

	}

	/**
	 * creates the list box that users will choose steps to add to the curation
	 *
	 * @return the step choice list
	 */
	private void initStepChoiceList() {
		//if we have already setup the don't do it again
		if (lstStepChoice.getItemCount() != 0) {
			return;
		}

		lstStepChoice.setVisibleItemCount(10);
		lstStepChoice.setStyleName("stepchooser");

		lstStepChoice.addItem(NewDatabaseInclusionPanel.TITLE);
		lstStepChoice.addItem(HeaderFilterStepPanel.TITLE);
		lstStepChoice.addItem(SequenceManipulationPanel.TITLE);
		lstStepChoice.addItem(ManualInclusionPanel.TITLE);
		lstStepChoice.addItem(DatabaseUploadPanel.TITLE);
		lstStepChoice.addItem(HeaderTransformPanel.TITLE);

		lstStepChoice.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(final ChangeEvent event) {
				final int selectedIndex = lstStepChoice.getSelectedIndex();

				switch (selectedIndex) {
					case 0:
						stepContainer.add(new NewDatabaseInclusionStub());
						break;
					case 1:
						stepContainer.add(new HeaderFilterStepStub());
						break;
					case 2:
						stepContainer.add(new SequenceManipulationStepStub());
						break;
					case 3:
						stepContainer.add(new ManualInclusionStepStub());
						break;
					case 4:
						stepContainer.add(new DatabaseUploadStepStub());
						break;
					case 5:
						stepContainer.add(new HeaderTransformStub());
						break;
					default:
						//do nothing
				}

				//set to none selected (actually using this as a button container...)
				lstStepChoice.setSelectedIndex(-1);
			}
		});

	}

	/**
	 * builds the panel that will be used to displaying the the step information such as the owner email and title etc
	 * as well as the StepContainer.
	 *
	 * @return the panel that should be used as a right panel
	 */
	private VerticalPanel getRightPanel() {
		final VerticalPanel toLoad = new VerticalPanel();

		//add the step container created with the steps that it should contain
		stepContainer = new StepPanelContainer(curation.getSteps());

		toLoad.add(stepContainer);
		toLoad.setSpacing(2);

		return toLoad;
	}

	private void auditEditState() {
		if (stepContainer != null) {
			stepContainer.setModificationEnabled(true);
		}
		txtShortName.setEnabled(true);
		txtTitle.setEnabled(true);
		txtNotes.setEnabled(true);
		lstStepChoice.setVisible(!(curation.hasBeenRun()));
		htmlStepSelectionTitle.setHTML("<b>Select a step to add.</b>");
		cmdView.setVisible(curation.hasBeenRun());
		cmdRun.setVisible(!curation.hasBeenRun());
	}

	/**
	 * creates all of the command buttons for dealing with a curation that will appear at the bottom of the screen
	 */
	private Panel createCommandPanel() {
		final HorizontalPanel cmdPanel = new HorizontalPanel();

		final Panel newPanel = new VerticalPanel();

		final Anchor cmdCreateNew = new Anchor("New");
		cmdCreateNew.setTitle("To create a new empty curation for you editing.");
		cmdCreateNew.setStyleName("command-link");
		cmdCreateNew.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				startNewCuration();
			}
		});
		newPanel.add(cmdCreateNew);

		final Anchor cmdCopyCurrent = new Anchor("Copy");
		cmdCopyCurrent.setTitle("To make a copy of the currently displayed curation for you own editing.");
		cmdCopyCurrent.setStyleName("command-link");
		cmdCopyCurrent.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				copyCuration();
			}
		});
		newPanel.add(cmdCopyCurrent);

		cmdPanel.add(newPanel);

		cmdRun.setStyleName("command-link");
		cmdRun.setTitle("To run the Curation and generate a FASTA file.");
		cmdRun.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				//call the server and perform a validation of the curation.  When the validation is complete then this CurationEditor
				//will be notified and it will need to tell the StepPanelContainer and it will forward the message to the steps that they
				//should check for any error messages.
				runCuration();
			}
		});

		final Panel runPanel = new VerticalPanel();

		runPanel.add(cmdRun);

		cmdPanel.add(runPanel);

		final Panel closePanel = new VerticalPanel();

		cmdView.setTitle("To view the contents of the resulting file.  If not yet run then this option will not function.");
		cmdView.setStyleName("command-link");
		cmdView.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				if (curation.getPathToResult() == null || curation.getPathToResult().isEmpty()) {
					showPopupMessage("There is not file associated with the curation please run the curation first.");
					return;
				}
				final ServerFileReader reader = new ServerFileReader();
				reader.setPopupPosition(getAbsoluteLeft() + 10, getAbsoluteTop() + 10);
				reader.setPixelSize(getWidget().getOffsetWidth() - 20, getWidget().getOffsetHeight() - 20);
				reader.setFileToDisplay(curation.getPathToResult());
				reader.setGrepPattern(">");
				reader.show();
				reader.updateContent(0);
			}
		});
		closePanel.add(cmdView);

		final Anchor cmdClose = new Anchor("Close");
		cmdClose.setTitle("Leave the Curation Editor and return the the main page selecting the current curation.");
		cmdClose.setStyleName("command-link");
		cmdClose.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				if (closeCallback != null && (curation.hasBeenRun() || Window.confirm("In order to use a database a curation must be run.\nClick 'OK' to close anyway."))) {
					closeHandler.removeHandler();
					closeCallback.editorClosed((curation.hasBeenRun() ? curation.getId() : initialCurationID));
				}
			}
		});
		closePanel.add(cmdClose);

		cmdPanel.add(closePanel);

		final DockPanel commandContainer = new DockPanel();
		final Panel spacer = new AbsolutePanel();
		commandContainer.add(spacer, DockPanel.WEST);
		commandContainer.add(cmdPanel, DockPanel.EAST);

		return commandContainer;
	}

	/**
	 * runs the currently loaded curation as a test meaning that no files or other residuals will be retained.  Only test
	 * runs are allowed from the Editor.  The Browser will be used for actually running a curation.  I want to avoid the
	 * ability to run a curation when the use just wants to test it since running will prohibit any future changes.
	 */
	public void runCuration() {
		if (!curationIsRunning) {
			if (validateShortName(txtShortName.getText())) {
				// If short name did not validate, the validateShortName method will indicate that
				// We do nothing if it does not validate
				if (curation.hasBeenRun()) {
					if (!Window.confirm("This curation has already been run.  Please confirm this action to run again.")) {
						return;
					} else {
						curation.setPathToResult("");
					}
				}

				messageManager.clearMessages();

				messageManager.addMessage("Curation run has started");
				curationIsRunning = true;
				auditEditState();
				updateTimer.scheduleRepeating(2500);

				updateCurationFromForm();

				commonDataRequester.runCuration(curation, new AsyncCallback<CurationStub>() {

					@Override
					public void onFailure(final Throwable throwable) {
						curationIsRunning = false;
						showPopupMessage(throwable.getMessage());
					}

					@Override
					public void onSuccess(final CurationStub o) {
						curationIsRunning = false;
						setCuration(o, false);
						retreiveCuration(curation.getId());
					}
				});
			}
		}
	}

	/**
	 * perform an update of the curation by going to the form and requesting the updated state of the form
	 */
	private void updateCurationFromForm() {
		curation.setShortName(txtShortName.getText());
		curation.setTitle(txtTitle.getText());
		curation.setNotes(txtNotes.getText());
		curation.setDecoyRegex(txtDecoy.getText());
		curation.setOwnerEmail(currentUserEmail);
		curation.setSteps(stepContainer.getContainedSteps());
	}

	/**
	 * this method will refresh the panel with the current state of the curation that we are displaying
	 */
	private void refreshPanel() {
		//loadPanel all of the widgets with the values of curation
		if (curation.getId() == null) {
			lblID.setText("not saved");
		} else {
			lblID.setText(curation.getId().toString());
		}
		txtShortName.setText(curation.getShortName());
		txtTitle.setText(curation.getTitle());
		txtNotes.setText(curation.getNotes());
		txtDecoy.setText(curation.getDecoyRegex());

		if (userEmailInitialPairs != null) {
			if (userEmailInitialPairs.containsKey(curation.getOwnerEmail())) {
				txtEmail.setText(userEmailInitialPairs.get(curation.getOwnerEmail()));
			} else {
				txtEmail.setText("");
			}
		}

		textareaLog.setVisible(Boolean.TRUE.equals(chkShowLog.getValue()));

		if (curation.getPathToResult() == null || curation.getPathToResult().isEmpty()) {
			lblResultFilePath.setTitle("Not yet run");
		} else {
			lblResultFilePath.setTitle(curation.getPathToResult());
		}

		stepContainer.refresh(curation.getSteps());
	}

	/**
	 * updates the status of the curation to reflect any changes on the server side as well as update
	 * the server side to reflect
	 */
	private void syncCuration() {
		updateCurationFromForm();
		commandPanel.setVisible(false);
		commonDataRequester.performUpdate(curation, new AsyncCallback<CurationStub>() {
			@Override
			public void onFailure(final Throwable throwable) {
				showPopupMessage("Failed to sync with the server");
				commandPanel.setVisible(true);
			}

			@Override
			public void onSuccess(final CurationStub result) {
				curation = result;

				for (final String errorMessage : curation.getErrorMessages()) {
					messageManager.addMessage(errorMessage);
					//stop performing updates if there was an error
					//_updateTimer.cancel();
				}
				refreshPanel();
				if (!curationIsRunning) {
					commandPanel.setVisible(true);
				}
			}
		});
	}

// -------------------------- INNER CLASSES --------------------------

	/**
	 * A callback to use when trying to retreive a curation
	 */
	private final class RetreivalCallback implements AsyncCallback<CurationStub> {

		//True if this retrieved curation is being copied.
		private boolean isCopy;
// ------------------------ INTERFACE METHODS ------------------------

// --------------------- Interface AsyncCallback ---------------------

		private RetreivalCallback() {
		}

		private RetreivalCallback(final boolean isCopy) {
			this.isCopy = isCopy;
		}

		/**
		 * this will for now just popup a message indicating failure.  In the future we may want to perform a redirect
		 * back to the browser.
		 * <p/>
		 * {@inheritDoc}
		 */
		@Override
		public void onFailure(final Throwable throwable) {
			showPopupMessage("Error retreiving curation from server: " + throwable.getMessage());
			final String msg = throwable.getMessage();
			if (msg.contains("shortname")) {
				lblShortNameError.setText(msg);
				lblShortNameError.removeStyleName("warning-label-ok");
			}
		}

		/**
		 * This will wait for a CurationStub to come back from the server and then tall the CurationEditor to display the
		 * returned curation.  Then it will hide the waiting box.
		 * <p/>
		 * {@inheritDoc}
		 */
		@Override
		public void onSuccess(final CurationStub result) {
			setCuration(result, isCopy);
		}
	}

	/**
	 * an inner class that will manage error messages for logging to a given TextArea.
	 */
	private class MessageManager {
// ------------------------------ FIELDS ------------------------------

		/**
		 * a TextArea for logging error information to
		 */
		private TextArea toLogTo;

		private final List<String> messages = new ArrayList<String>();

// -------------------------- OTHER METHODS --------------------------

		/**
		 * Add a message to this message manager these errors will be displayed until removed
		 *
		 * @param message the message you want to add to this manager
		 */
		public void addMessage(final String message) {
			if (messages.contains(message)) {
				return;
			}
			messages.add(message);

			writeMessages();
		}

		public void clearMessages() {
			messages.clear();
		}


		/**
		 * remove a message (must be an equivalent string)
		 *
		 * @param message
		 */
		public void removeError(final String message) {
			messages.remove(message);

			if (messages.isEmpty()) {
				toLogTo.removeStyleName("textboxes-error");
			}

			writeMessages();
		}

		/**
		 * set the text area that should be logged to
		 *
		 * @param toLogTo the TextArea we want to write the messages out to
		 */
		public void setTextArea(final TextArea toLogTo) {
			this.toLogTo = toLogTo;
			if (!messages.isEmpty()) {
				toLogTo.addStyleName("textboxes-error");
				writeMessages();
			}
		}

		/**
		 * write the messages to the text box after clearing it first
		 */
		private void writeMessages() {
			final StringBuilder builder = new StringBuilder();
			for (final String message : messages) {
				builder.append(message);
				builder.append("\n");
				if (message.contains("shortname")) {
					lblShortNameError.setText(message);
					lblShortNameError.removeStyleName("warning-label-ok");
				}
			}
			toLogTo.setText(builder.toString());
		}
	}
}
