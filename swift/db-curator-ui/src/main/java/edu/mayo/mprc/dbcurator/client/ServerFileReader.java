package edu.mayo.mprc.dbcurator.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.dbcurator.client.services.CommonDataRequester;
import edu.mayo.mprc.dbcurator.client.services.CommonDataRequesterAsync;

/**
 * This will be a widget that can display a file from the server.  It will do this in a manner analagous to
 * the unix 'less' command.  Only a single page will be retreived from the server at a time.  The users will have
 * page up and down commands as well as "Home" and "End" commands which will go the beggining and end of
 * the file, respectively.
 * <p/>
 * Once a line has been retreived from the server it will be retained up to a given threshold of size allowing
 * scrolling without server hits.
 * <p/>
 * This widget will also supply Grep like capabilities allowing the user to search for lines containing a given
 * search expression.
 */
public final class ServerFileReader extends PopupPanel implements ClickListener {

	private static final int DEFAULT_LINES_PER_PAGE = 50;

	private String fileDisplayed;

	private Hyperlink cmdNextPage;
	private Hyperlink cmdPrevPage;
	private Hyperlink cmdFirstPage;

	private TextBox txtGrepExpression;
	private Hyperlink cmdApplyGrep;

	private DockPanel mainPanel;
	private TextArea contentRenderer;

	private int startingLine = 0;
	private int linesPerPage = DEFAULT_LINES_PER_PAGE;

	private MessagePopup waitPopup;

	private boolean commandsEnabled = true;

	private Timer resultTime = new Timer() {
		@Override
		public void run() {
			ServerFileReader.this.getInterrimResults();
		}
	};

	public ServerFileReader() {
		this.init();
	}

	protected void init() {
		this.mainPanel = new DockPanel();

		mainPanel.setStyleName("serverfilereader");
		mainPanel.add(getCommandPanel(), DockPanel.NORTH);

		this.contentRenderer = new TextArea();
		this.contentRenderer.setSize("100%", "100%");

		contentRenderer.setReadOnly(true);
		contentRenderer.setStyleName("serverfilereader_text");

		//wrap not official HTML standard I think but is supported by IE and FireFox at least
		DOM.setElementAttribute(contentRenderer.getElement(), "wrap", "off");

		mainPanel.add(contentRenderer, DockPanel.CENTER);
		this.add(mainPanel);
	}

	public ServerFileReader setFileToDisplay(final String path) {
		this.fileDisplayed = path;
		return this;
	}

	public ServerFileReader setLinesPerPage(final int linesPerPage) {
		if (this.linesPerPage != linesPerPage) {
			this.linesPerPage = linesPerPage;
		}
		return this;
	}

	public void setGrepPattern(final String pattern) {
		this.txtGrepExpression.setText(pattern);
	}

	@Override
	public void setSize(final String w, final String h) {
		super.setSize(w, h);
		this.mainPanel.setSize(w, h);
		contentRenderer.setSize(w, h);
	}

	@Override
	public void setPixelSize(final int w, final int h) {
		super.setPixelSize(w, h);
		this.mainPanel.setPixelSize(w, h);
		contentRenderer.setPixelSize(w, h - 25);
	}

	protected Panel getCommandPanel() {
		final Panel commandPanel = new HorizontalPanel();
		commandPanel.setStyleName("serverfilereader_commands");

		cmdNextPage = new Hyperlink("Next Page", "Next Page");
		cmdNextPage.addClickListener(this);
		cmdNextPage.setStyleName("serverfilereader_commandbuttons");
		commandPanel.add(cmdNextPage);

		cmdPrevPage = new Hyperlink("Previous Page", "Previous Page");
		cmdPrevPage.addClickListener(this);
		cmdPrevPage.setStyleName("serverfilereader_commandbuttons");
		commandPanel.add(cmdPrevPage);

		cmdFirstPage = new Hyperlink("First Page", "First Page");
		cmdFirstPage.addClickListener(this);
		cmdFirstPage.setStyleName("serverfilereader_commandbuttons");
		commandPanel.add(cmdFirstPage);

		txtGrepExpression = new TextBox();
		commandPanel.add(txtGrepExpression);
		cmdApplyGrep = new Hyperlink("Filter", "Filter");

		cmdApplyGrep.addClickListener(this);
		cmdApplyGrep.setStyleName("serverfilereader_commandbuttons");
		commandPanel.add(cmdApplyGrep);

		final Hyperlink cmdClose = new Hyperlink("Close", "Close");
		cmdClose.setStyleName("serverfilereader_commandbuttons");
		cmdClose.addClickListener(new ClickListener() {
			@Override
			public void onClick(final Widget widget) {
				if (waitPopup != null && waitPopup.isVisible()) {
					waitPopup.hide();
				}
				ServerFileReader.this.cancelRequest(); //if there is any requesting being processed then just cancel it.
				ServerFileReader.this.hide();
			}
		});

		commandPanel.add(cmdClose);

		return commandPanel;
	}


	protected void setCommandsEnabled(final boolean enabled) {
		this.commandsEnabled = enabled;
	}

	public static final int PAGE_OVERLAP = 5;

	@Override
	public void onClick(final Widget widget) {
		if (widget.equals(cmdApplyGrep) && ((Hyperlink) widget).getText().equals("Stop")) {
			cancelRequest();
		} else if (!commandsEnabled) {
			new MessagePopup("Currently retreiving file contents.  Please wait.  Depending on filter criteria this may take a while.", this.getAbsoluteLeft() + 50, this.getAbsoluteTop() + 50).show();
		} else {
			if (widget.equals(cmdNextPage)) {
				this.startingLine = this.startingLine + this.linesPerPage - PAGE_OVERLAP;
			} else if (widget.equals(cmdPrevPage)) {
				this.startingLine = this.startingLine - this.linesPerPage + PAGE_OVERLAP;
			} else if (widget.equals(cmdFirstPage)) {
				this.startingLine = 0;
			} else if (widget.equals(cmdApplyGrep)) {
				this.startingLine = 0; //change to the first line for less bafflement.
			}
			updateContent(this.startingLine);
		}
	}

	protected void showMessage(final String message) {
		new MessagePopup(message, this.getAbsoluteLeft() + 50, this.getAbsoluteTop() + 50).show();
	}

	public void updateContent(final int startingLine) {
		this.startingLine = startingLine;

		waitPopup = new MessagePopup("Retreiving file data.  Please wait.", this.getAbsoluteLeft() + 100, this.getAbsoluteTop() + 50);
		waitPopup.show();
		this.setCommandsEnabled(false);

		this.txtGrepExpression.setText(txtGrepExpression.getText().trim());
		cmdApplyGrep.setText("Stop");
		//todo retreive content
		getService().getLines(this.fileDisplayed, this.startingLine, this.linesPerPage, txtGrepExpression.getText(), new AsyncCallback<String[]>() {
			@Override
			public void onFailure(final Throwable throwable) {
				setCommandsEnabled(true);
				resultTime.cancel();
				showMessage("Could not get contents from server.  " + throwable.getMessage());
			}

			@Override
			public void onSuccess(final String[] o) {
				setCommandsEnabled(true);
				if (waitPopup != null) {
					waitPopup.hide();
				}
				resultTime.cancel();
				getInterrimResults();
				cmdApplyGrep.setText("Filter");
			}
		});

		resultTime.scheduleRepeating(2500);
	}

	public void cancelRequest() {

		//todo retreive content
		getService().setCancelMessage(true, new AsyncCallback<Void>() {

			@Override
			public void onFailure(final Throwable throwable) {
				showMessage("Could not cancel for some reason. \n" + throwable.getMessage());
			}

			@Override
			public void onSuccess(final Void aVoid) {
				cmdApplyGrep.setText("Filter");
			}
		});
	}

	public void getInterrimResults() {
		getService().getResults(new ContentRetreivalCallback());
	}

	private CommonDataRequesterAsync getService() {
		final CommonDataRequesterAsync service = (CommonDataRequesterAsync) GWT.create(CommonDataRequester.class);
		final ServiceDefTarget endpoint = (ServiceDefTarget) service;
		endpoint.setServiceEntryPoint(GWT.getModuleBaseURL() + "CommonDataRequester");
		return service;
	}

	private class ContentRetreivalCallback implements AsyncCallback<String[]> {
		@Override
		public void onFailure(final Throwable throwable) {
			setCommandsEnabled(true);
			showMessage("Could not get contents from server.  " + throwable.getMessage());
		}

		@Override
		public void onSuccess(final String[] currentLines) {
			if (currentLines.length > 0 && waitPopup != null && waitPopup.isVisible()) {
				waitPopup.hide();
			}
			final StringBuilder content = new StringBuilder();
			for (final String currentLine : currentLines) {
				if (currentLine == null) {
					continue;
				}
				if (contentRenderer.getCharacterWidth() < currentLine.length() + 2) {
					contentRenderer.setCharacterWidth(currentLine.length() + 2);
				}
				content.append(currentLine).append("\n");
			}
			ServerFileReader.this.contentRenderer.setText(content.toString());

		}
	}
}
