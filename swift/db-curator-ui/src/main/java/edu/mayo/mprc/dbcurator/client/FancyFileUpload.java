package edu.mayo.mprc.dbcurator.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;

public final class FancyFileUpload extends Composite implements HasText, HasWordWrap, SourcesChangeEvents {

	/**
	 * State definitions
	 */
	public static final int EMPTY_STATE = 1;
	public static final int PENDING_STATE = 2;
	public static final int UPLOADING_STATE = 3;
	public static final int UPLOADED_STATE = 4;
	public static final int DELETED_STATE = 5;
	public static final int FAILED_STATE = 6;

	/**
	 * Initial State of the widget.
	 */
	private int widgetState = EMPTY_STATE;

	/**
	 * Default delay to check an empty FileUpload widget for
	 * arrival of a  filename.
	 */
	private int searchUpdateDelay = 500;

	/**
	 * Default delay for pending state, when delay over the form is submitted.
	 */
	private int pendingUpdateDelay = 5000;

	/**
	 * OK message expected from file upload servlet to indicate successful upload.
	 */
	private String returnOKMessage = "OK";

	private FormPanel uploadForm = new FormPanel();
	private VerticalPanel mainPanel = new VerticalPanel();

	/**
	 * Internal timer for checking fileupload text for a value.
	 */
	private Timer t;

	/**
	 * Internal timer for checking if pending delay is over.
	 */
	private Timer p;

	/**
	 * Widget representing file to be uploaded.
	 */
	private UploadDisplay uploadItem;

	/**
	 * FileName to be uploaded
	 */
	private String fileName = "";

	/**
	 * Class used for the display of filename to be uploaded,
	 * and handling the update of the display states.
	 *
	 * @author tacyad
	 */
	protected class UploadDisplay extends Composite {

		/**
		 * FileUpload Widget
		 */
		private FileUpload uploadFileWidget = new FileUpload();

		/**
		 * Label to display after file widget is filled with a filename
		 */
		private Label uploadFileName = new Label();

		/**
		 * Checkbox to invoke deletion functionality of the file.
		 */
		private CheckBox proceed = new CheckBox();

		/**
		 * Panel to hold the widget
		 */
		private FlowPanel mainPanel = new FlowPanel();

		/**
		 * Panel to hold pending, loading, loaded or failed state details.
		 */
		private HorizontalPanel pendingPanel = new HorizontalPanel();

		/**
		 * Constructor
		 */
		public UploadDisplay() {
			mainPanel.add(uploadFileWidget);

			pendingPanel.add(uploadFileName);
			uploadFileName.setWordWrap(true);
			pendingPanel.add(proceed);
			proceed.setChecked(true);
			// Set up a click listener on the proceed check box
			proceed.addClickListener(new ClickListener() {
				@Override
				public void onClick(final Widget sender) {
					// If clicked we need to check the status of the upload.
					if (widgetState == UPLOADED_STATE) {
						// File has previously been uploaded, so now must be deleted.
						// set the encoding type of the form to be urlencoded (alters functionality of the servlet)
						uploadForm.setEncoding(FormPanel.ENCODING_URLENCODED);
						// Set the status.
						setDeleted();
						// Call the servlet
						deleteFiles();
					} else {
						// If file has not been uploaded, just reset the widget
						reset();
						// and then start waiting again.
						t.cancel();
						p.cancel();
						startWaiting();
					}
				}
			});
			mainPanel.add(pendingPanel);
			pendingPanel.setVisible(false);
			initWidget(mainPanel);
		}

		/**
		 * Set the widget into pending mode by altering style
		 * of pending panel and displaying it.  Hide the FileUpload
		 * widget and finally set the state to Pending.
		 */
		private void setPending() {
			uploadFileName.setText(uploadFileWidget.getFilename());
			uploadFileWidget.setVisible(false);
			pendingPanel.setVisible(true);
			pendingPanel.setStyleName("fancyfileupload-pending");
			widgetState = PENDING_STATE;
		}

		/**
		 * Set the widget into Loading mode by changing the style name
		 * and updating the widget State to Uploading.
		 */
		private void setLoading() {
			pendingPanel.setStyleName("fancyfileupload-loading");
			widgetState = UPLOADING_STATE;
		}

		/**
		 * Set the widget to Loaded mode by changing the style name
		 * and updating the widget State to Loaded.
		 */
		private void setLoaded() {
			pendingPanel.setStyleName("fancyfileupload-loaded");
			widgetState = UPLOADED_STATE;
		}

		/**
		 * Set the widget to Deleted mode by changing the style name
		 * and updating the widget State to Deleted.
		 */
		private void setDeleted() {
			pendingPanel.setStyleName("fancyfileupload-deleted");
			widgetState = DELETED_STATE;
		}

		/**
		 * Set the widget to Failed mode by changing the style name
		 * and updating the widget State to Failed.
		 * Additionally, hide the pending panel and display the FileUpload
		 * widget.
		 */
		private void setFailed() {
			widgetState = FAILED_STATE;
			uploadFileName.setText("Operation Failed");
		}

		/**
		 * Reset the display
		 */
		private void reset() {
			fileName = uploadFileName.getText();
			widgetState = EMPTY_STATE;
			uploadFileName.setText("");
			uploadFileWidget.setVisible(true);
			pendingPanel.setVisible(false);
			proceed.setChecked(true);
		}
	}

	/**
	 * Perform the uploading of a file by changing state of display widget
	 * and then calling form.submit() method.
	 */
	private void uploadFiles() {
		fileName = uploadItem.uploadFileWidget.getFilename();
		if (uploadItem.proceed.isChecked()) {
			uploadItem.setLoading();
			uploadForm.submit();
		}
	}

	private void deleteFiles() {
		uploadForm.submit();
	}

	/**
	 * Put the widget into a Pending state, set the Pending delay timer
	 * to call the upload file method when ran out.
	 */
	private void pendingUpload() {
		// Fire an onChange event to anyone who is listening
		uploadItem.setPending();
		p = new Timer() {
			@Override
			public void run() {
				uploadFiles();
			}
		};
		p.schedule(pendingUpdateDelay);
	}

	/**
	 * Method to check if FileUpload Widget has a filename within it.
	 * If so, cancel the timer that was set to call this method and then
	 * call the pendingUpload() method.
	 * If not, do nothing.
	 */
	private void checkForFileName() {
		GWT.log(uploadItem.uploadFileWidget.getFilename() + " : " + fileName, null);
		if (!uploadItem.uploadFileWidget.getFilename().equals("")) {
			if (!uploadItem.uploadFileWidget.getFilename().equals(fileName)) {
				t.cancel();
				pendingUpload();
			}
		}
	}

	/**
	 * This method sets up a repeating schedule to call the checkforfilename
	 * method to see if the FileUpload widget has any text in it.
	 */
	private void startWaiting() {
		t = null;
		t = new Timer() {
			@Override
			public void run() {
				checkForFileName();
			}
		};
		t.scheduleRepeating(searchUpdateDelay);
	}

	/**
	 * Constructor.
	 *
	 * @param servletPath is the servlet that will handle the submission of this form
	 */
	public FancyFileUpload(final String servletPath) {
		// Set Form details
		// Set the action to call on submit
		uploadForm.setAction(servletPath);
		// Set the form encoding to multipart to indicate a file upload
		uploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		// Set the method to Post
		uploadForm.setMethod(FormPanel.METHOD_POST);
		uploadForm.setWidget(mainPanel);

		// Create a new upload display widget
		uploadItem = new UploadDisplay();
		// Set the name of the upload file form element
		uploadItem.uploadFileWidget.setName("uploadFormElement");
		// Add the new widget to the panel.
		mainPanel.add(uploadItem);

		// Start the waiting for a name to appear in the file upload widget.
		startWaiting();
		// Initialise the widget.
		initWidget(uploadForm);

		// Add an event handler to the form.
		uploadForm.addFormHandler(new FormHandler() {
			@Override
			public void onSubmitComplete(final FormSubmitCompleteEvent event) {
				// Fire an onChange Event
				fireChangeEvent();
				// Cancel all timers to be absolutely sure nothing is going on.
				t.cancel();
				p.cancel();
				// Ensure that the form encoding is set correctly.
				uploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
				// Check the result to see if an OK message is returned from the server.
				if (event.getResults().equals(returnOKMessage)) {
					// If yes, and this is an upload then set the widget to loaded state.
					if (!(widgetState == DELETED_STATE)) {
						uploadItem.setLoaded();
					}
				} else {
					// If no, set the widget to failed state.
					uploadItem.setFailed();
				}
			}

			@Override
			public void onSubmit(final FormSubmitEvent event) {
				//No validation in this version.
			}
		});
	}

	/**
	 * Fire a change event to anyone listening to us.
	 */
	private void fireChangeEvent() {
		if (changeListeners != null) {
			changeListeners.fireChange(this);
		}
	}

	/**
	 * Get the text from the widget - which in reality will be retrieving
	 * any
	 * value set in the Label element of the display widget.
	 */
	@Override
	public String getText() {
		return uploadItem.uploadFileName.getText();
	}

	/**
	 * Cannot set the text of a File Upload Widget, so raise an exception.
	 */
	@Override
	public void setText(final String text) {
		throw new RuntimeException("Cannot set text of a FileUpload Widget");
	}

	/**
	 * Retrieve the status of the upload widget.
	 *
	 * @return Status of upload widget.
	 */
	public int getUploadState() {
		return widgetState;
	}

	/**
	 * Set the delay for checking for a filename to appear in the
	 * FileUpload widget
	 * Might be useful if there are performance issues.
	 *
	 * @param newDelay
	 */
	public void setCheckForFileNameDelay(final int newDelay) {
		searchUpdateDelay = newDelay;
	}

	/**
	 * Set the delay value indicating how long a file will remain in
	 * pending mode
	 * prior to the upload action taking place.
	 *
	 * @param newDelay
	 */
	public void setPendingDelay(final int newDelay) {
		pendingUpdateDelay = newDelay;
	}

	/**
	 * Return the delay value set for checking a file.
	 *
	 * @return
	 */
	public int getCheckForFileNameDelay() {
		return searchUpdateDelay;
	}

	/**
	 * Return value set for pending delay.
	 *
	 * @return
	 */
	public int getPendingDelay() {
		return pendingUpdateDelay;
	}

	/**
	 * Return if the label in the display widget is wordwrapped or not.
	 */
	@Override
	public boolean getWordWrap() {
		return uploadItem.uploadFileName.getWordWrap();
	}

	/**
	 * Set the word wrap value of the label in the display widget.
	 */
	@Override
	public void setWordWrap(final boolean wrap) {
		uploadItem.uploadFileName.setWordWrap(wrap);
	}

	private ChangeListenerCollection changeListeners;

	/**
	 * Add a change listener
	 *
	 * @param listener
	 */
	@Override
	public void addChangeListener(final ChangeListener listener) {
		if (changeListeners == null) {
			changeListeners = new ChangeListenerCollection();
		}
		changeListeners.add(listener);
	}

	/**
	 * Remove a change listener
	 *
	 * @param listener
	 */
	@Override
	public void removeChangeListener(final ChangeListener listener) {
		if (changeListeners != null) {
			changeListeners.remove(listener);
		}
	}

}