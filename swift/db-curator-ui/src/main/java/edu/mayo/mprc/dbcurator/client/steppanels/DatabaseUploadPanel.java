package edu.mayo.mprc.dbcurator.client.steppanels;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.common.client.ExceptionUtilities;
import edu.mayo.mprc.common.client.StringUtilities;
import edu.mayo.mprc.dbcurator.client.curatorstubs.CurationStepStub;
import edu.mayo.mprc.dbcurator.client.curatorstubs.DatabaseUploadStepStub;

import java.util.Date;

/**
 * @author Eric Winter
 */
public final class DatabaseUploadPanel extends AbstractStepPanel {
	public static final String TITLE = "Upload a Database file";

	private DatabaseUploadStepStub containedStep;

	private Label lblClientPath = new Label();
	private Label lblServerPath = new Label();
	private FileUpload uploadWidget = new FileUpload();

	private Label lblNotification = new Label();

	private VerticalPanel mainPanel = new VerticalPanel();

	/**
	 * creates and initizalizes the panel to contain:
	 * - a file selection widget
	 * - the selected file on the client machine
	 * - a button allowing the user to initilize the upload
	 * - the path to the file on the server after the upload
	 */
	public DatabaseUploadPanel() {

		final FormPanel form = new FormPanel();
		initWidget(form);

		//set form to contain the panel that we will load with widgets
		form.setWidget(mainPanel);

		//initilize the action that will be done ont he form when the "Upload" button is pressed
		form.setAction(GWT.getModuleBaseURL() + (GWT.getModuleBaseURL().endsWith("/") ? "" : "/") + "FileUploadServlet");
		form.setEncoding(FormPanel.ENCODING_MULTIPART);
		form.setMethod(FormPanel.METHOD_POST);
		form.addFormHandler(new FormHandler() {
			/**
			 * Whent he form is submitted do some validation on it and indicate that the file is being uploaded.  Unfortunately
			 * there is no progress indicator for this.  Maybe we can find another widget, someday.
			 * @param event
			 */
			@Override
			public void onSubmit(final FormSubmitEvent event) {
				if (lblClientPath.getText().trim().isEmpty()) {
					lblNotification.setText("You must select a file first");
					event.setCancelled(true);
				} else if (containedStep.getCompletionCount() != null) {
					lblNotification.setText("You cannot change the file of an already run curation please copy the curation first");
					event.setCancelled(true);
				} else {
					lblNotification.setText("File is being uploaded");
				}
			}

			/**
			 * when the submission is complete look for errors and if there is an error then display the error else
			 * display the location of the new file on the server
			 * @param event contains the result (from Response.Writer) which contains ther error messages or the path to the file
			 */
			@Override
			public void onSubmitComplete(final FormSubmitCompleteEvent event) {
				final String results = event.getResults();
				if (results.indexOf("<Error>") != -1) {
					containedStep.serverFilePath = null;
					lblNotification.setText(removeTags(results));
				} else {
					containedStep.serverFilePath = removeTags(results);
				}
				update();

			}

			/**
			 * removes the "pre" and tags that come back on the result because of the HTTP nature of the Response.Writer.
			 *
			 * HACK: We should investigate why are these appended (only for Firefox, BTW) and fix the servlet instead.
			 * @param toDeTag the string you want to remove the tags from
			 * @return the string without the <pre> tags
			 */
			private String removeTags(String toDeTag) {
				if (StringUtilities.toLowerCase(toDeTag).startsWith("<pre>")) {
					toDeTag = toDeTag.substring("<pre>".length());
				}
				if (StringUtilities.toLowerCase(toDeTag).endsWith("</pre>")) {
					toDeTag = toDeTag.substring(0, toDeTag.length() - "</pre>".length());
				}
				return toDeTag;
			}
		});

		//populate the main panel
		mainPanel.addStyleName("fileupload-mainPanel");

		uploadWidget.setName("fileUpload_" + new Date().toString());
		uploadWidget.setWidth("100%");
		uploadWidget.setStyleName("fileupload");
		uploadWidget.setName("uploadFormElement");
		mainPanel.add(uploadWidget);

		lblClientPath.setWidth("100%");
		lblClientPath.addStyleName("uploadpanel-clientpath");
		mainPanel.add(lblClientPath);

		final Button cmdStart = new Button("Upload");
		cmdStart.addClickListener(new ClickListener() {
			@Override
			public void onClick(final Widget widget) {
				containedStep.clientFilePath = uploadWidget.getFilename();
				lblClientPath.setText(uploadWidget.getFilename());
				form.submit();
			}
		});


		lblServerPath.addStyleName("uploadpanel-lblServerPath");
		mainPanel.add(cmdStart);

		lblServerPath.setWidth("100%");
		mainPanel.add(lblServerPath);

		mainPanel.add(lblNotification);

		super.setTitle(TITLE);
	}


	@Override
	public CurationStepStub getContainedStep() {
		this.containedStep.clientFilePath = this.lblClientPath.getText();
		this.containedStep.serverFilePath = this.lblServerPath.getText();
		return containedStep;
	}

	@Override
	public void setContainedStep(final CurationStepStub step) throws ClassCastException {
		if (!(step instanceof DatabaseUploadStepStub)) {
			ExceptionUtilities.throwCastException(step, DatabaseUploadStepStub.class);
			return;
		}
		this.containedStep = (DatabaseUploadStepStub) step;
		update();
	}

	@Override
	public String getStyle() {
		return "shell-header-uploadstep";
	}

	@Override
	public void update() {
		if (this.containedStep.clientFilePath != null) {
			this.lblClientPath.setText(this.containedStep.clientFilePath);
		} else {
			this.lblClientPath.setText("");
		}

		if (containedStep.serverFilePath != null && !containedStep.serverFilePath.isEmpty()) {
			this.lblServerPath.setText(this.containedStep.serverFilePath);
			lblNotification.setText("Upload Complete");
		}
	}

	@Override
	public String getImageURL() {
		return "images/fileupload.png";
	}
}
