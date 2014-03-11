package edu.mayo.mprc.swift.ui.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.swift.ui.client.dialogs.ErrorDialog;
import edu.mayo.mprc.swift.ui.client.dialogs.ValidationPanel;
import edu.mayo.mprc.swift.ui.client.rpc.*;
import edu.mayo.mprc.swift.ui.client.rpc.files.FileInfo;
import edu.mayo.mprc.swift.ui.client.widgets.*;
import edu.mayo.mprc.swift.ui.client.widgets.validation.ParamsEditorApp;
import edu.mayo.mprc.swift.ui.client.widgets.validation.SimpleParamsEditorPanel;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * * Entry point classes define {@code onModuleLoad()}.
 */
public final class SwiftApp implements EntryPoint, HidesPageContentsWhileLoading, SearchMetadata, UiConfiguration {

	private static final String SELECT_USER_STRING = "<Select User>";
	private static final String TITLE_ALLOWED = "^[a-zA-Z0-9-+._()\\[\\]{}=# ]*$";
	public static final String USER_MESSAGE = "user.message";

	private ListBox users = new ListBox();
	private TextBox title = new TextBox();
	private boolean userEditedTitle = false;
	private HTML messageDisplay = new HTML("");

	private FileTable files;

	private TextBox output;
	private ValidationPanel outputValidationPanel;
	private PushButton runButton;
	private PushButton addFileButton;
	private SimpleParamsEditorPanel paramsEditor;
	private ToggleButton editorToggle;
	private SpectrumQaSetupPanel spectrumQaSetupPanel;
	private ReportSetupPanel reportSetupPanel;
	private AdditionalSettingsPanel additionalSettingsPanel;
	private EnabledEnginesPanel enabledEnginesPanel;
	/**
	 * Metadata associated with the search
	 */
	private HashMap<String, String> searchMetadata = new HashMap<String, String>();

	/**
	 * User interface configuration
	 */
	private HashMap<String, String> uiConfiguration = new HashMap<String, String>();

	private Map<String, ClientUser> userInfo = new HashMap<String, ClientUser>();

	/**
	 * This will get incremented with every RPC call made on the page load, and decremented when the load
	 * finishes/fails. When the counter drops back to zero, the page contents will become visible by
	 * removing the class="hidden" from #hidden-until-loaded div.
	 */
	private int hiddenWhileLoadingCounter = 0;

	private String outputPath;
	private boolean outputPathChangeEnabled = true;

	// The output path control is showing a special value - not really a path, more of an informative string
	private boolean outputPathSpecial = false;
	// The user changed the output path, do not update it automatically
	private boolean outputPathUserSpecified = false;
	// The querystring parameter specifying what search definition to load initially
	private static final String LOAD_SEARCH_DEFINITION_ID = "load";
	private SwiftApp.TitleChangeListener titleChangeListener;
	private int previousSearchRunId;

	/**
	 * This is the entry point method. Creates all the elements and hooks up some
	 * event listeners.
	 */
	@Override
	public void onModuleLoad() {
		try {
			DOM.setStyleAttribute(DOM.getElementById("shown_while_loading"), "display", "block");
			DOM.setStyleAttribute(DOM.getElementById("hidden_while_loading"), "display", "none");
			hidePageContentsWhileLoading();

			final String searchDefinitionId = getQueryString(LOAD_SEARCH_DEFINITION_ID);
			initTitleEditor();
			initEditorToggle();
			initAddFilesButton();
			initOutputLocation();
			initRunButton();
			initPage(searchDefinitionId == null ? null : Integer.parseInt(searchDefinitionId));
		} catch (Exception t) {
			throw new RuntimeException(t);
		} finally {
			loadCompleted();
			showPageContentsAfterLoad();
		}
	}

	@Override
	public synchronized void hidePageContentsWhileLoading() {
		hiddenWhileLoadingCounter++;
	}

	@Override
	public synchronized void showPageContents() {
		DOM.setStyleAttribute(DOM.getElementById("shown_while_loading"), "display", "none");
		DOM.setStyleAttribute(DOM.getElementById("hidden_while_loading"), "display", "block");
	}

	@Override
	public synchronized void showPageContentsAfterLoad() {
		hiddenWhileLoadingCounter--;
		if (hiddenWhileLoadingCounter == 0) {
			showPageContents();
		}
	}

	private void loadCompleted() {
		// The load completed, enable buttons
		runButton.setEnabled(true);
		addFileButton.setEnabled(true);
	}

	private void initPage(final Integer previousSearchId) {
		hidePageContentsWhileLoading();
		ServiceConnection.instance().getInitialPageData(previousSearchId, new AsyncCallback<InitialPageData>() {
			@Override
			public void onFailure(final Throwable caught) {
				// SWALLOWED: Fail
				finalizeFileTableInit();
				finalizeSpectrumQa(null);
				// InitReport fail
				DOM.setStyleAttribute(DOM.getElementById("reportingRow"), "display", "none");
				showPageContentsAfterLoad();
			}

			@Override
			public void onSuccess(final InitialPageData result) {
				previousSearchRunId = result.loadedSearch() == null ? -1 : result.loadedSearch().getSearchId();
				initUiConfiguration(result.getUiConfiguration());
				initSearchMetadata(result.loadedSearch() == null ? new HashMap<String, String>() : result.loadedSearch().getDefinition().getMetadata());
				initParamsEditor(result);
				finalizeFileTableInit();
				finalizeSpectrumQa(result.getSpectrumQaParamFileInfo());
				finalizeInitReport(result.isScaffoldReportEnabled());
				initEnabledEngines(result.getSearchEngines());
				initAdditionalSettings(result.getSearchEngines());
				initUserList(result.listUsers());
				showPageContentsAfterLoad();
				initMessage(getConfigurationSetting(USER_MESSAGE));
				loadPreviousSearch(result.loadedSearch());
			}
		});
	}

	private void initUiConfiguration(final HashMap<String, String> uiConfiguration) {
		this.uiConfiguration = uiConfiguration;
	}

	private void finalizeFileTableInit() {
		files = new FileTable();
		final RootPanel filePanel = RootPanel.get("fileTable");
		filePanel.add(files);
		connectOutputLocationAndFileTable();
		connectTitleAndFileTable();
	}

	private void initAdditionalSettings(final List<ClientSearchEngine> engines) {
		additionalSettingsPanel = new AdditionalSettingsPanel(engines);
		final RootPanel panel = RootPanel.get("publicResults");
		panel.add(additionalSettingsPanel);
	}

	private void initEnabledEngines(final List<ClientSearchEngine> enabledEngines) {
		enabledEnginesPanel = new EnabledEnginesPanel(enabledEngines, this, this);
		final RootPanel panel = RootPanel.get("enginesPanel");
		if (panel != null) {
			panel.add(enabledEnginesPanel);
		}
	}

	private void finalizeSpectrumQa(final List<SpectrumQaParamFileInfo> list) {
		if (list == null || list.isEmpty()) {
			DOM.setStyleAttribute(DOM.getElementById("spectrumQaRow"), "display", "none");
		} else {
			DOM.setStyleAttribute(DOM.getElementById("spectrumQaRow"), "display", "");
			spectrumQaSetupPanel = new SpectrumQaSetupPanel(list);
			final RootPanel rootSpectrumQaPanel = RootPanel.get("spectrumQa");
			rootSpectrumQaPanel.add(spectrumQaSetupPanel);
		}
		showPageContentsAfterLoad();
	}

	private void initTitleEditor() {
		final RootPanel titlePanel = RootPanel.get("title");
		title.setVisibleLength(30);
		titlePanel.add(title);
		title.addKeyUpHandler(new KeyUpHandler() {
			@Override
			public void onKeyUp(final KeyUpEvent event) {
				userEditedTitle = true;
				updateOutputLocation();
			}
		});
		titleChangeListener = new TitleChangeListener();
		title.addChangeHandler(titleChangeListener);
	}

	private void finalizeInitReport(final Boolean reportEnabled) {
		if (reportEnabled) {
			DOM.setStyleAttribute(DOM.getElementById("reportingRow"), "display", "");
			reportSetupPanel = new ReportSetupPanel();
			final RootPanel rootPanel = RootPanel.get("report");
			rootPanel.add(reportSetupPanel);
		} else {
			DOM.setStyleAttribute(DOM.getElementById("reportingRow"), "display", "none");
		}
	}

	private void initParamsEditor(final InitialPageData pageData) {
		ParamsEditorApp.onModuleLoad(pageData);
		paramsEditor = ParamsEditorApp.getPanel();
	}

	private void initEditorToggle() {
		final RootPanel togglePanel = RootPanel.get("paramsToggleButton");
		editorToggle = new ToggleButton(new Image("images/triright.png"), new Image("images/tridown.png"));
		editorToggle.setDown(false);
		editorToggle.addStyleName("toggle-button");
		togglePanel.add(editorToggle);
		editorToggle.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				paramsEditor.setEditorExpanded(editorToggle.isDown());
			}
		});
	}

	private void userChanged() {
		if (users == null) {
			return;
		}
		ClientUser user = null;
		if (users.getSelectedIndex() > 0) {
			final String email = users.getValue(users.getSelectedIndex());
			if (userInfo.containsKey(email)) {
				user =
						userInfo.get(email);
			}

			Cookies.setCookie("email", users.getValue(users.getSelectedIndex()), ParamSetSelectionController.getCookieExpirationDate(), null, "/", false);
		}

		// Figure out whether this user can edit parameters or not
		final boolean editorEnabled = user != null && user.isParameterEditorEnabled();
		if (paramsEditor != null) {
			paramsEditor.setEditorEnabled(editorEnabled, user);
		}
		setOutputPathChangeEnabled(user != null && user.isOutputPathChangeEnabled());
	}

	private void setOutputPathChangeEnabled(final boolean enabled) {
		if (output != null && enabled != outputPathChangeEnabled) {
			outputPathChangeEnabled = enabled;
			if (!outputPathChangeEnabled) {
				outputPathUserSpecified = false;
			}
			output.setReadOnly(!enabled);
			// Update the output location, when changes are disabled, the field gets filled automatically
			updateOutputLocation();
		}
	}

	private static String wrapDisplayMessage(final String message) {
		return "<div class=\"user-message\">" + message + "</div>";
	}

	private void initMessage(final String message) {
		final RootPanel messagePanel = RootPanel.get("messagePlaceholder");
		messageDisplay.setVisible(false);
		messagePanel.add(messageDisplay);
		displayMessage(message);
	}

	/**
	 * returns specific query-string parameter
	 *
	 * @param name Query string parameter name
	 * @return Value of the parameter
	 */
	private static String getQueryString(final String name) {
		return Window.Location.getParameter(name);
	}

	/**
	 * Check query string to see if the user wants to load previous search data into the forms.
	 */
	private void loadPreviousSearch(final ClientLoadedSearch result) {
		if (result == null) {
			return;
		}
		final ClientSwiftSearchDefinition definition = result.getDefinition();
		displayMessage("Loaded previous search " + definition.getSearchTitle());

		if (result.getClientParamSetList() != null) {
			// We transfered a new parameter set list together with search definition
			// Update the parameter editor.
			paramsEditor.setParamSetList(result.getClientParamSetList());
		}

		paramsEditor.setSelectedParamSet(result.getDefinition().getParamSet());

		// Determine search type
		SearchType searchType = null;
		ClientFileSearch firstSearch = null;
		for (final ClientFileSearch clientFileSearch : definition.getInputFiles()) {
			if (firstSearch == null) {
				firstSearch = clientFileSearch;
			}
			final String fileNamefileNameWithoutExtension = FilePathWidget.getFileNameWithoutExtension(clientFileSearch.getPath());
			final SearchType newSearchType = SearchTypeList.getSearchTypeFromSetup(
					definition.getSearchTitle(),
					fileNamefileNameWithoutExtension,
					clientFileSearch.getExperiment(),
					clientFileSearch.getBiologicalSample());
			if (searchType == null) {
				searchType = newSearchType;
			} else {
				if (!searchType.equals(newSearchType)) {
					searchType = SearchType.Custom;
					break;
				}
			}
		}

		final String title = definition.getSearchTitle();
		setTitleText(title);
		if (spectrumQaSetupPanel != null) {
			spectrumQaSetupPanel.setParameters(definition.getSpectrumQa());
		}
		if (reportSetupPanel != null) {
			reportSetupPanel.setParameters(definition.getPeptideReport());
		}
		final List<ClientFileSearch> inputFiles = definition.getInputFiles();

		files.setFiles(inputFiles, searchType);

		output.setText(definition.getOutputFolder());
		selectUser(definition.getUser().getEmail());

		additionalSettingsPanel.setDefinition(definition);
		enabledEnginesPanel.setCurrentEngines(firstSearch.getEnabledEngines());

		showPageContentsAfterLoad();
	}

	private void updateUserMessage() {
		ServiceConnection.instance().getUiConfig(new AsyncCallback<HashMap<String, String>>() {
			@Override
			public void onFailure(final Throwable throwable) {
				displayMessage("");
			}

			@Override
			public void onSuccess(final HashMap<String, String> uiConfig) {
				final String message = uiConfig.get(USER_MESSAGE);
				displayMessage(message == null ? "" : message);
			}
		});
	}

	private void displayMessage(final String message) {
		if (message != null && !message.isEmpty()) {
			messageDisplay.setHTML(wrapDisplayMessage(message));
			messageDisplay.setVisible(true);
		} else {
			messageDisplay.setVisible(false);
		}
	}

	private void initUserList(final ClientUser[] list) {
		// The user listing is downloaded by an async call.
		final RootPanel userPanel = RootPanel.get("email");
		userPanel.add(users);
		users.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(final ChangeEvent event) {
				userChanged();
			}
		});
		users.addItem(SELECT_USER_STRING, "");
		userInfo.clear();
		for (final ClientUser user : list) {
			users.addItem(user.getName(), user.getEmail());
			userInfo.put(user.getEmail(), user);
		}

		// Select the user according to the cookie stored
		final String userEmail = Cookies.getCookie("email");
		selectUser(userEmail);
	}

	private void initSearchMetadata(final HashMap<String, String> metadata) {
		this.searchMetadata = metadata;
	}

	public HashMap<String, String> getSearchMetadata() {
		return searchMetadata;
	}

	@Override
	public void setSearchMetadata(final String key, final String value) {
		searchMetadata.put(key, value);
	}

	@Override
	public String getSearchMetadata(final String key) {
		return searchMetadata.get(key);
	}

	private void selectUser(final String userEmail) {
		if (userEmail != null) {
			for (int i = 0; i < users.getItemCount(); i++) {
				if (users.getValue(i).equalsIgnoreCase(userEmail)) {
					users.setSelectedIndex(i);
					break;
				}
			}
		}
		userChanged();
	}

	private void initAddFilesButton() {
		// Add files button produces a dialog
		addFileButton = new PushButton("", new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				final int clientWidth = Window.getClientWidth();
				final int clientHeight = Window.getClientHeight();
				final int popupWidth = clientWidth * 3 / 4;
				final int popupHeight = clientHeight * 3 / 4;
				final int posX = (clientWidth - popupWidth) / 2;
				final int posY = (clientHeight - popupHeight) / 2;
				final FileTreeDialog dialog = new FileTreeDialog(popupWidth, popupHeight);
				dialog.setPopupPosition(posX, posY);
				dialog.setSelectedFilesListener(new SelectedFilesListener() {
					@Override
					public void selectedFiles(final FileInfo[] info) {
						files.addFiles(info);
					}
				});
				dialog.show();
			}
		});
		addFileButton.setEnabled(false);
		RootPanel.get("addFileButton").add(addFileButton);
	}

	private void initOutputLocation() {
		// currently, the save location is populated deterministically by the combination of
		// the users's input

		final RootPanel outputPanel = RootPanel.get("output");
		output = new TextBox();
		output.setVisibleLength(150);
		output.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(final ChangeEvent event) {
				outputPathUserSpecified = true;
				updateOutputLocation();
			}
		});
		outputPanel.add(output);

		final RootPanel outputWarning = RootPanel.get("outputWarning");
		outputValidationPanel = new ValidationPanel(2);

		outputWarning.add(outputValidationPanel);

		connectOutputLocationAndFileTable();
		// Fire userChanged to get the output location updated
		userChanged();
		updateOutputLocation();
	}

	// Called after either the output location or the file table get initialized.

	private void connectOutputLocationAndFileTable() {
		if (files != null) {
			files.addValueChangeHandler(new ValueChangeHandler<Void>() {
				@Override
				public void onValueChange(final ValueChangeEvent<Void> event) {
					updateOutputLocation();
				}
			});
		}
	}

	private void connectTitleAndFileTable() {
		if (files != null) {
			files.addValueChangeHandler(new ValueChangeHandler<Void>() {
				@Override
				public void onValueChange(final ValueChangeEvent<Void> event) {
					updateTitleFromFileTable();
				}
			});
		}
	}

	private void updateTitleFromFileTable() {
		// User edited the title field. We have no business here
		if (userEditedTitle) {
			return;
		}
		List<String> names = files.getFileNames();
		if (names.size() == 0) {
			setTitleText("");
		} else {
			Iterator<String> iterator = names.iterator();
			String longestPrefix = iterator.next();
			while (iterator.hasNext()) {
				String next = iterator.next();
				for (int i = 0; i < Math.min(longestPrefix.length(), next.length()); i++) {
					if (next.charAt(i) != longestPrefix.charAt(i)) {
						longestPrefix = longestPrefix.substring(0, i);
						break;
					}
				}
			}
			setTitleText(longestPrefix);
		}

	}

	private void initRunButton() {
		// Run Button ////////////////////////////////////////////
		runButton = new PushButton();
		RootPanel.get("runButton").add(runButton);
		runButton.setEnabled(false);
		// Make params editor disable the runWithCallback button when it is invalid
		if (paramsEditor != null) {
			paramsEditor.getValidationController().addValueChangeHandler(new ValueChangeHandler<ClientValue>() {
				@Override
				public void onValueChange(final ValueChangeEvent<ClientValue> event) {
					runButton.setEnabled(paramsEditor.isValid());
				}
			});
		}

		runButton.addClickHandler(new RunClickHandler());
	}

	/**
	 * Update the output location as appropriate.
	 */
	public void updateOutputLocation() {
		if (!outputPathChangeEnabled || !outputPathUserSpecified) {
			// The user is not able to or chose not to influence output path, it gets set automatically
			final List<ClientFileSearch> fileSearches = getFileSearches();
			if (files == null || (fileSearches == null) || (fileSearches.isEmpty())) {
				if (!outputPathChangeEnabled) {
					output.setText("<No Files Selected>");
					outputPathSpecial = true;
				} else {
					output.setText("");
				}
				outputValidationPanel.removeValidationsFor(output);
				outputPath = null;
			} else if ((getTitleText() == null) || getTitleText().equals("")) {
				if (!outputPathChangeEnabled) {
					output.setText("<No Title>");
					outputPathSpecial = true;
				} else {
					output.setText("");
				}
				outputValidationPanel.removeValidationsFor(output);
				outputPath = null;
			} else {
				outputPathSpecial = false;
				outputPath = fileSearches.get(0).getPath();
				if (!outputPath.endsWith("/")) {
					final int i = outputPath.lastIndexOf('/');
					if (i < 0) {
						outputPath = "";
					} else {
						outputPath = outputPath.substring(0, i + 1);
					}
				}
				outputPath += pathify(getTitleText());
				output.setText(outputPath);
				validateOutputPath(output.getText());
			}
		} else {
			// The user can influence output path. Keep whatever was the previous setting, unless
			// it is a special value like "<No Files Selected>" or "<No Title>" - wipe that one.
			if (outputPathSpecial) {
				outputPathSpecial = false;
			}
			validateOutputPath(output.getText());
		}

	}

	private void validateOutputPath(final String text) {
		ServiceConnection.instance().outputFolderExists(text, new AsyncCallback<Boolean>() {
			@Override
			public void onFailure(final Throwable caught) {
				outputValidationPanel.removeValidationsFor(output);
			}

			@Override
			public void onSuccess(final Boolean result) {
				outputValidationPanel.removeValidationsFor(output);
				if (Boolean.TRUE.equals(result)) {
					outputValidationPanel.addValidation(new ClientValidation("Folder exists, will be overwritten"), output);
				}
			}
		});
	}

	private List<ClientFileSearch> getFileSearches() {
		return files != null ? files.getData(enabledEnginesPanel.getEnabledEngines()) : null;
	}

	/**
	 * When the title changes, we need to fire onChange event on its listener.
	 */
	private void setTitleText(final String title) {
		this.title.setText(title);
		titleChangeListener.onChange(null);
	}

	/**
	 * @return Contents of the Title field, trimmed.
	 */
	private String getTitleText() {
		return title.getText() == null ? null : title.getText().trim();
	}

	static String pathify(final String userProvided) {
		final String s = userProvided.replaceAll("\\.\\.", "_");
		return s.replaceAll("/", "_");
	}

	//redirect the browser to the given url

	public static native void redirect(String url)/*-{
        $wnd.location = url;
    }-*/;

	@Override
	public String getConfigurationSetting(String key) {
		return uiConfiguration.get(key);
	}

	private class RunClickHandler implements ClickHandler {
		RunClickHandler() {
		}

		@Override
		public void onClick(final ClickEvent event) {
			updateUserMessage();
			updateOutputLocation();

			final String titleText = title.getText();

			if (titleText == null) {
				Window.alert("No title specified");
				title.setFocus(true);
				return;
			}

			if (!titleText.matches(TITLE_ALLOWED)) {
				Window.alert("The search title " + titleText + " is invalid. Please use only letters, numbers, - + . _ ( ) [ ] { } = # and a space.");
				title.setFocus(true);
				return;
			}

			final String effectiveOutputPath = outputPathUserSpecified ? output.getText() : outputPath;
			final String user = users.getValue(users.getSelectedIndex());

			final List<ClientFileSearch> fileSearches = getFileSearches();
			if (fileSearches == null || fileSearches.isEmpty()) {
				Window.alert("No files added to the search");
				return;
			}

			if (effectiveOutputPath == null || "".equals(effectiveOutputPath)) {
				Window.alert("The output folder not specified");
				return;
			}

			if ("".equals(user) || user.equals(SELECT_USER_STRING)) {
				Window.alert("No user selected");
				users.setFocus(true);
				return;
			}

			final ProgressDialogBox dialog = new ProgressDialogBox();
			dialog.setProgressMessage("Submitting search...");
			dialog.setRelativeSize(0.25, 0.25);
			dialog.setMinimumSize(200, 200);
			dialog.showModal();

			try {
				final List<ClientFileSearch> entries = files.getData(enabledEnginesPanel.getEnabledEngines());

				final ClientSpectrumQa clientSpectrumQa;
				if (spectrumQaSetupPanel != null) {
					clientSpectrumQa = spectrumQaSetupPanel.getParameters();
				} else {
					clientSpectrumQa = new ClientSpectrumQa();
				}
				final ClientSwiftSearchDefinition def = new ClientSwiftSearchDefinition(
						getTitleText(),
						userInfo.get(users.getValue(users.getSelectedIndex())),
						effectiveOutputPath,
						paramsEditor.getSelectedParamSet(),
						entries,
						clientSpectrumQa,
						reportSetupPanel == null ? new ClientPeptideReport(false) : reportSetupPanel.getParameters(),
						additionalSettingsPanel.isPublicMgfs(),
						additionalSettingsPanel.isPublicMzxmls(),
						additionalSettingsPanel.isPublicSearchFiles(),
						getSearchMetadata(),
						previousSearchRunId
				);
				def.setFromScratch(additionalSettingsPanel.isFromScratch());
				def.setLowPriority(additionalSettingsPanel.isLowPriority());

				ServiceConnection.instance().startSearch(def, new AsyncCallback<Void>() {
					@Override
					public void onFailure(final Throwable throwable) {
						dialog.hide();
						ErrorDialog.handleGlobalError(throwable);
					}

					@Override
					public void onSuccess(final Void o) {
						dialog.hide();
						redirect("/report/report.jsp");
					}
				});
			} catch (Exception e) {
				dialog.hide();
				ErrorDialog.handleGlobalError(e);
			}
		}
	}

	private class TitleChangeListener implements ChangeHandler {
		@Override
		public void onChange(final ChangeEvent event) {
			files.updateSearchTitle(getTitleText());
		}
	}
}
