package edu.mayo.mprc.dbcurator.client.steppanels;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.common.client.ExceptionUtilities;
import edu.mayo.mprc.common.client.StringUtilities;

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
		form.addSubmitHandler(new FormPanel.SubmitHandler() {
			/**
			 * Whent he form is submitted do some validation on it and indicate that the file is being uploaded.  Unfortunately
			 * there is no progress indicator for this.  Maybe we can find another widget, someday.
			 */
			@Override
			public void onSubmit(final FormPanel.SubmitEvent event) {
				if (lblClientPath.getText().trim().isEmpty()) {
					lblNotification.setText("You must select a file first");
					event.cancel();
				} else if (containedStep.getCompletionCount() != null) {
					lblNotification.setText("You cannot change the file of an already run curation please copy the curation first");
					event.cancel();
				} else {
					lblNotification.setText("File is being uploaded");
				}
			}
		});
		form.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
			@Override
			public void onSubmitComplete(final FormPanel.SubmitCompleteEvent event) {
				final String results = event.getResults();
				if (results.contains("<Error>")) {
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
			private String removeTags(final String toDeTag) {
				String result = toDeTag;
				if ("<pre>".equals(StringUtilities.toLowerCase(result).substring(0, "<pre>".length()))) {
					result = result.substring("<pre>".length());
				}
				if ("</pre>".equals(StringUtilities.toLowerCase(result).substring(0, "</pre>".length()))) {
					result = result.substring(0, result.length() - "</pre>".length());
				}
				return result;
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
		cmdStart.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
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
		containedStep.clientFilePath = lblClientPath.getText();
		containedStep.serverFilePath = lblServerPath.getText();
		return containedStep;
	}

	@Override
	public void setContainedStep(final CurationStepStub step) throws ClassCastException {
		if (!(step instanceof DatabaseUploadStepStub)) {
			ExceptionUtilities.throwCastException(step, DatabaseUploadStepStub.class);
			return;
		}
		containedStep = (DatabaseUploadStepStub) step;
		update();
	}

	@Override
	public String getStyle() {
		return "shell-header-uploadstep";
	}

	@Override
	public void update() {
		if (containedStep.clientFilePath != null) {
			lblClientPath.setText(containedStep.clientFilePath);
		} else {
			lblClientPath.setText("");
		}

		if (containedStep.serverFilePath != null && !containedStep.serverFilePath.isEmpty()) {
			lblServerPath.setText(containedStep.serverFilePath);
			lblNotification.setText("Upload Complete");
		}
	}

	@Override
	public String getImageURL() {
		return "images/fileupload.png";
	}
}
