package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.swift.ui.client.rpc.files.FileInfo;
import edu.mayo.mprc.swift.ui.client.service.Service;
import edu.mayo.mprc.swift.ui.client.service.ServiceAsync;

/**
 * A dialog box that contains a file tree.
 *
 * @author: Roman Zenka
 */
public final class FileTreeDialog extends DialogBox implements ClickHandler, ValueChangeHandler<Boolean> {
	public static final String FILE_ORDER_COOKIE = "fo";
	public static final String FILE_ORDER_NAME = "name";
	public static final String FILE_ORDER_DATE = "date";

	private final Button okButton;
	private final Button cancelButton;
	private final RadioButton sortByName;
	private final RadioButton sortByDate;
	private SelectedFilesListener selectedFilesListener;

	public FileTreeDialog(final int width, final int height) {
		super(false);

		final DockPanel caption = new DockPanel();
		caption.add(new Label("Select files and folders"), DockPanel.CENTER);
		caption.addStyleName("file-dialog-caption");

		cancelButton = new Button("Cancel");
		caption.add(cancelButton, DockPanel.EAST);
		cancelButton.addClickHandler(this);

		okButton = new Button("Ok");
		caption.add(okButton, DockPanel.EAST);
		okButton.addClickHandler(this);

		String fileOrder = Cookies.getCookie(FILE_ORDER_COOKIE);
		if (fileOrder == null || fileOrder.trim().isEmpty()) {
			fileOrder = FILE_ORDER_NAME;
		}

		sortByDate = new RadioButton("sort");
		sortByDate.setText("By date");
		caption.add(sortByDate, DockPanel.EAST);
		sortByDate.addValueChangeHandler(this);

		sortByName = new RadioButton("sort");
		sortByName.setText("By name");
		caption.add(sortByName, DockPanel.EAST);
		sortByName.addValueChangeHandler(this);

		sortByName.setValue(FILE_ORDER_NAME.equals(fileOrder));
		sortByDate.setValue(FILE_ORDER_DATE.equals(fileOrder));

		final ScrollPanel panel = new ScrollPanel();
		panel.setPixelSize(width, height);

		final Grid fileListTable = new Grid(1, 1);
		fileListTable.addStyleName("maintable");
		DOM.setElementAttribute(fileListTable.getElement(), "id", "maintable");

		DOM.setElementAttribute(fileListTable.getRowFormatter().getElement(0), "class", "shrink");

		final Element fileList = DOM.createDiv();
		DOM.setElementAttribute(fileList, "id", "filelist");
		DOM.setElementAttribute(fileList, "class", "filelist");

		final Element fileListCell = fileListTable.getCellFormatter().getElement(0, 0);
		DOM.setInnerHTML(fileListCell, null);

		DOM.setElementAttribute(fileListCell, "id", "filelistcell");
		DOM.setElementAttribute(fileListCell, "valign", "top");
		fileListTable.getCellFormatter().setStyleName(0, 0, "filelistcell");

		DOM.insertChild(fileListCell, fileList, 0);

		panel.add(fileListTable);

		final DockPanel contents = new DockPanel();
		contents.add(panel, DockPanel.CENTER);
		contents.add(caption, DockPanel.NORTH);
		setWidget(contents);

	}

	@Override
	public void show() {
		super.show();
		showFileDialogBox("filelist", "", getOrder());
	}

	public SelectedFilesListener getSelectedFilesListener() {
		return selectedFilesListener;
	}

	public void setSelectedFilesListener(final SelectedFilesListener selectedFilesListener) {
		this.selectedFilesListener = selectedFilesListener;
	}

	@Override
	public void onClick(final ClickEvent event) {
		final Widget sender = (Widget) event.getSource();
		if (sender.equals(okButton)) {
			final String selectedFilesHairBall = getSelectedFiles();

			final String[] eachSelectedFile = selectedFilesHairBall.split("\\n");
			final ServiceAsync fileFinderService = (ServiceAsync) GWT.create(Service.class);
			final ServiceDefTarget endpoint = (ServiceDefTarget) fileFinderService;
			endpoint.setServiceEntryPoint(GWT.getModuleBaseURL() + "Service");

			fileFinderService.findFiles(eachSelectedFile, new AsyncCallback<FileInfo[]>() {

				@Override
				public void onFailure(final Throwable throwable) {
					//TODO: implement me
				}

				@Override
				public void onSuccess(final FileInfo[] o) {
					selectedFilesListener.selectedFiles(o);
				}
			});

			hide();
		} else if (sender.equals(cancelButton)) {
			hide();
		}
	}

	/**
	 * Calling this method will load the old Swift 1.0 FileChooser.  When the dialog is closed then a List of String of the
	 * paths to the selected file will be returned.
	 *
	 * @return the List<String> denoting the paths to the selected files/folders.
	 */
	public static native void showFileDialogBox(String iframe, String basePath, String listOrder)/*-{
        $wnd.initDialog(iframe, null, "Load", basePath, listOrder);

    }-*/;

	/**
	 * @return
	 */
	public static native String getSelectedFiles()/*-{
        var retFiles = "";

        var selectedFiles = $wnd.getSelectedFilesAndFolders();
        for (var i = 0; i < selectedFiles.length; i++) {
            retFiles += selectedFiles[i] + "\n";
        }

        return retFiles;
    }-*/;

	public String getOrder() {
		if (Boolean.TRUE.equals(sortByName.getValue())) {
			return "name";
		}
		return "date";
	}

	@Override
	public void onValueChange(final ValueChangeEvent<Boolean> event) {
		Cookies.setCookie(FILE_ORDER_COOKIE, getOrder());
		showFileDialogBox("filelist", "", getOrder());
	}
}
