package edu.mayo.mprc.swift.ui.client.widgets.validation;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.dbcurator.client.CurationEditor;
import edu.mayo.mprc.dbcurator.client.EditorCloseCallback;
import edu.mayo.mprc.swift.ui.client.dialogs.ErrorDialog;
import edu.mayo.mprc.swift.ui.client.dialogs.PreviewDialog;
import edu.mayo.mprc.swift.ui.client.dialogs.SaveDialog;
import edu.mayo.mprc.swift.ui.client.dialogs.ValidationPanel;
import edu.mayo.mprc.swift.ui.client.rpc.*;
import edu.mayo.mprc.swift.ui.client.service.ServiceAsync;
import edu.mayo.mprc.swift.ui.client.widgets.Callback;
import edu.mayo.mprc.swift.ui.client.widgets.ExistingDOMPanel;
import edu.mayo.mprc.swift.ui.client.widgets.ParamSetSelectionController;
import edu.mayo.mprc.swift.ui.client.widgets.ParamsSelector;

import java.util.*;

/**
 * Allow users to edit parameter sets directly on the swift submit page.
 * <p/>
 * Note that this is no longer really a panel.  It requires a number of HTML elements
 * that it inserts itself into:
 * The first row is fixed and has ids:
 * paramsToggle, paramsSelector, globalParamsValidation
 * Subsequent rows labeled with paramRow are cloned and elements
 * filled as parameters are added:
 * paramRow: paramLabel, paramEntry, paramValidation
 */
public final class SimpleParamsEditorPanel implements SourcesChangeEvents {
	public static final String ACTION_LINK = "actionLink";
	public static final String SPACE_AFTER = "spaceAfter";

	public static final String LABEL1 = "paramLabelLeftCol";
	public static final String ENTRY1 = "paramEntryLeftCol";
	public static final String LABEL2 = "paramLabelRightCol";
	public static final String ENTRY2 = "paramEntryRightCol";
	public static final String VALIDATION = "paramValidation";
	public static final String PARAMS_LABEL = "params-label";

	public static final String MODIFICATIONS = "modifications";
	public static final String TOLERANCES = "tolerances";
	public static final String INSTRUMENT = "instrument";
	public static final String SCAFFOLD_SETTINGS = "scaffoldSettings";

	private ChangeListenerCollection listeners = new ChangeListenerCollection();

	private ParamsSelector selector;

	private ServiceAsync serviceAsync;
	private ValidationController validationController;
	private ParamSetSelectionController selectionController;
	private boolean editorEnabled = false;
	private boolean editorExpanded = false;
	private boolean editorVisible = editorExpanded && editorEnabled;
	private boolean editorErrorMessageVisible = true;
	private ModificationsLabel fixedMods;
	private ModificationsLabel varMods;

	//Current user
	private ClientUser user;

	//User map. Used to map e-mail to user id.
	private Map<String/*email*/, ClientUser> userInfo;

	private PushButton saveButton;
	private PushButton deleteButton;

	// Lists of elements constituting the editor
	private List<Element> editorElements;

	private List<PushButton> buttons = new ArrayList();
	private DatabaseListBox dlb;


	public SimpleParamsEditorPanel(final ServiceAsync serviceAsync, final InitialPageData pageData) {
		this.serviceAsync = serviceAsync;
		userInfo = new HashMap<String, ClientUser>();
		for (ClientUser clientUser : pageData.listUsers()) {
			userInfo.put(clientUser.getEmail(), clientUser);
		}
		selectionController = new ParamSetSelectionController(serviceAsync);
		validationController = new ValidationController(serviceAsync, selectionController, pageData);
		editorElements = new ArrayList();

		final HorizontalPanel hp = new HorizontalPanel();
		final RootPanel paramsSelectorPanel = RootPanel.get("paramsSelector");
		paramsSelectorPanel.add(hp);

		selector = new ParamsSelector();
		selectionController.setSelector(selector);
		selectionController.setParamSetList(pageData.getParamSetList());
		hp.add(selector);

		// save buttons //////////////////////////////////////////////////////////
		final PushButton button;
		hp.add(saveButton = new PushButton("Save..."));
		saveButton.addStyleName(ACTION_LINK);
		saveButton.addStyleName(SPACE_AFTER);
		saveButton.addClickListener(new ClickListener() {
			@Override
			public void onClick(final Widget widget) {
				save();
			}
		});
		saveButton.setVisible(editorVisible);
		buttons.add(saveButton);

		hp.add(button = new PushButton("Preview..."));
		button.addStyleName(ACTION_LINK);
		button.addStyleName(SPACE_AFTER);
		button.addClickListener(new ClickListener() {
			@Override
			public void onClick(final Widget widget) {
				preview();
			}
		});
		buttons.add(button);

		hp.add(deleteButton = new PushButton("Delete..."));
		deleteButton.addStyleName(ACTION_LINK);
		deleteButton.addStyleName(SPACE_AFTER);
		deleteButton.addClickListener(new ClickListener() {
			@Override
			public void onClick(final Widget widget) {
				delete();
			}
		});
		buttons.add(deleteButton);

		// description, initially hidden /////////////////////////////////////////////


		final HTMLPanel description;
		paramsSelectorPanel.add(description = new HTMLPanel("<I>This is a description of the ParameterSet.</I>"));
		description.setSize("500px", "50px");
		description.setStyleName("dottedBorder");
		description.setVisible(false);

		/// Existing DOM //////////////////////////////////////////////////////////////
		// Grab the existing DOM for the parameter rows.

		final ExistingDOMPanel edp = new ExistingDOMPanel("paramRow");

		/// database ///////////////////////////////////////////
		{
			final ExistingDOMPanel dbrow = new ExistingDOMPanel("paramDbRow");
			final Label label = new Label("Database:");
			label.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
			label.setStyleName(PARAMS_LABEL);
			editorElements.add(dbrow.append("database", "paramDbLabel", label, editorVisible));

			final ValidationPanel vp = new ValidationPanel(10);

			dlb = new DatabaseListBox("sequence.database", userInfo);

			final HorizontalPanel p = new HorizontalPanel();

			p.add(dlb);
			//dlb.setStyleName("spaceAfter");
			final PushButton pb = new PushButton("Add or Review Database...");
			pb.addStyleName(ACTION_LINK);
			pb.setTitle("Click here to review the selected database and potentially modify it for your own needs.");
			pb.addClickListener(new ClickListener() {
				@Override
				public void onClick(final Widget widget) {
					popupDbCurator();
				}
			});
			validationController.add(dlb, "sequence.database", vp);
			p.add(pb);

			//Add undeployer link if enabled.
			if (pageData.isDatabaseUndeployerEnabled()) {
				final PushButton du = new PushButton("Undeploy Database");
				du.addStyleName(ACTION_LINK);
				du.setTitle("Click here to undeploy database from search engines.");
				du.addClickListener(new DatabaseUndeploymentAction(serviceAsync, dlb));
				p.add(du);
			}

			editorElements.add(dbrow.append("database", "paramDbEntry", p, editorVisible));

			editorElements.add(dbrow.append("database", "paramDbValidation", vp, editorVisible));

		}

		/// enzyme /////////////////////////////////////////////

		{

			final Label label = new Label("Protease:");
			label.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
			label.setStyleName(PARAMS_LABEL);
			editorElements.add(edp.append("enzyme", LABEL1, label, editorVisible));

			final ValidationPanel vp = new ValidationPanel(10);

			final ProteaseListBox tb;
			validationController.add(tb = new ProteaseListBox("sequence.enzyme"), "sequence.enzyme", vp);

			editorElements.add(edp.append("enzyme", ENTRY1, tb, editorVisible));

			final Label label1 = new Label("Missed Cleavages:");
			label1.setStyleName(PARAMS_LABEL);
			label1.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
			final ValidatableTextBox tdb = new ValidatableTextBox("sequence.missed_cleavages") {
				@Override
				protected ClientValue getValueFromString(final String value) {
					if ((value == null) || (value.isEmpty())) {
						return null;
					}
					try {
						return new ClientInteger(value);
					} catch (NumberFormatException ignore) {
						final ClientValidationList list = new ClientValidationList();
						final ClientValidation cv = new ClientValidation("Not a number: " + value,
								"sequence.missed_cleavages", ClientValidation.SEVERITY_ERROR);
						list.add(cv);
						validationController.update("sequence.missed_cleavages", list);
						return null;
					}
				}

				@Override
				protected String setValueAsString(final ClientValue object) {
					return object.toString();
				}

				@Override
				public void setAllowedValues(final List<? extends ClientValue> values) {
					// unused.
				}

				@Override
				public boolean needsAllowedValues() {
					return false;
				}
			};
			tdb.setVisibleLength(5);
			editorElements.add(edp.append("enzyme", LABEL2, label1, editorVisible));
			editorElements.add(edp.append("enzyme", ENTRY2, tdb, editorVisible));
			validationController.add(tdb, "sequence.missed_cleavages", vp);

			editorElements.add(edp.append("enzyme", VALIDATION, vp, editorVisible));
		}

		/// modifications /////////////////////////////////////////


		{

			final ValidationPanel vp = new ValidationPanel(10);

			final Label label = new Label("Fixed Modifications:");
			label.setStyleName(PARAMS_LABEL);
			label.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
			editorElements.add(edp.append(MODIFICATIONS, LABEL1, label, editorVisible));

			// fixed mods label
			fixedMods = new ModificationsLabel(ModificationSelectionEditor.FIXED_PARAM_NAME, ModificationSelectionEditor.FIXED_PARAM_NAME);
			final ModificationSelectionEditor fixedModsEditor = new ModificationSelectionEditor(ModificationSelectionEditor.FIXED_PARAM_NAME, ModificationSelectionEditor.FIXED_MOD_TYPE);
			fixedMods.setEditor(fixedModsEditor);
			validationController.add(fixedMods, ModificationSelectionEditor.FIXED_PARAM_NAME, vp);

			editorElements.add(edp.append(MODIFICATIONS, ENTRY1, fixedMods, editorVisible));

			// variable mods label
			varMods = new ModificationsLabel(ModificationSelectionEditor.VARIABLE_PARAM_NAME, ModificationSelectionEditor.VARIABLE_PARAM_NAME);
			final ModificationSelectionEditor varModsEditor = new ModificationSelectionEditor(ModificationSelectionEditor.VARIABLE_PARAM_NAME, ModificationSelectionEditor.VARIABLE_MOD_TYPE);
			varMods.setEditor(varModsEditor);
			validationController.add(varMods, ModificationSelectionEditor.VARIABLE_PARAM_NAME, vp);

			final Label label1 = new Label("Variable Modifications:");
			label1.setStyleName(PARAMS_LABEL);
			label1.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
			editorElements.add(edp.append(MODIFICATIONS, LABEL2, label1, editorVisible));


			editorElements.add(edp.append(MODIFICATIONS, ENTRY2, varMods, editorVisible));

			editorElements.add(edp.append(MODIFICATIONS, VALIDATION, vp, editorVisible));
		}

		/// tolerances /////////////////////////////////////////


		{

			final Label label = new Label("Peptide Tolerance:");
			label.setStyleName(PARAMS_LABEL);
			label.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
			editorElements.add(edp.append(TOLERANCES, LABEL1, label, editorVisible));


			final ValidationPanel vp = new ValidationPanel(10);

			final ToleranceBox peptideTolerance;
			validationController.add(peptideTolerance = new ToleranceBox("tolerance.peptide"), "tolerance.peptide", vp);
			editorElements.add(edp.append(TOLERANCES, ENTRY1, peptideTolerance, editorVisible));

			final Label label1 = new Label("Fragment Tolerance:");
			editorElements.add(edp.append(TOLERANCES, LABEL2, label1, editorVisible));
			label1.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
			label1.setStyleName(PARAMS_LABEL);

			final ToleranceBox fragmentTolerance;
			validationController.add(fragmentTolerance = new ToleranceBox("tolerance.fragment"), "tolerance.fragment", vp);
			editorElements.add(edp.append(TOLERANCES, ENTRY2, fragmentTolerance, editorVisible));

			editorElements.add(edp.append(TOLERANCES, VALIDATION, vp, editorVisible));

		}

		/// instrument /////////////////////////////////////////
		final ValidationPanel instrumentVp;
		{
			final Label label = new Label("Instrument:");
			editorElements.add(edp.append(INSTRUMENT, LABEL1, label, editorVisible));
			label.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
			label.setStyleName(PARAMS_LABEL);
			instrumentVp = new ValidationPanel(10);
			final InstrumentListBox lb;
			validationController.add(lb = new InstrumentListBox(INSTRUMENT), INSTRUMENT, instrumentVp);
			editorElements.add(edp.append(INSTRUMENT, ENTRY1, lb, editorVisible));
			editorElements.add(edp.append(INSTRUMENT, VALIDATION, instrumentVp, editorVisible));
		}

		/// spectrum extraction params /////////////////////////////////////////
		{
			final Label label = new Label("Spectrum extraction:");
			editorElements.add(edp.append(INSTRUMENT, LABEL2, label, editorVisible));
			label.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
			label.setStyleName(PARAMS_LABEL);
			final SpectrumExtractionEditor ed = new SpectrumExtractionEditor();
			validationController.add(ed, "extractMsnSettings", instrumentVp);
			editorElements.add(edp.append(INSTRUMENT, ENTRY2, ed, editorVisible));
		}

		/// scaffold params /////////////////////////////////////////
		{
			final ExistingDOMPanel row = new ExistingDOMPanel("scaffoldRow");

			final Label label = new Label("Scaffold:");
			editorElements.add(row.append(SCAFFOLD_SETTINGS, "scaffoldLabel", label, editorVisible));
			label.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
			label.setStyleName(PARAMS_LABEL);
			final ValidationPanel vp = new ValidationPanel(10);
			final ScaffoldSettingsEditor ed = new ScaffoldSettingsEditor();
			validationController.add(ed, SCAFFOLD_SETTINGS, vp);
			editorElements.add(row.append(SCAFFOLD_SETTINGS, "scaffoldEntry", ed, editorVisible));
			editorElements.add(row.append(SCAFFOLD_SETTINGS, "scaffoldValidation", vp, editorVisible));
		}

		validationController.setEnabled(false);
		selectionController.setParamSetList(pageData.getParamSetList());
		if (pageData.loadedSearch() == null) {
			selectionController.setDefaultParameterSet();
		}

		validationController.addChangeListener(new ChangeListener() {
			@Override
			public void onChange(final Widget widget) {
				for (final PushButton button : buttons) {
					button.setEnabled(isValid());
				}
			}
		});
	}

	/**
	 * @param editorEnabled If true, the user can edit the parameters.
	 * @param user          current user
	 */
	public void setEditorEnabled(final boolean editorEnabled, final ClientUser user) {
		this.editorEnabled = editorEnabled;
		this.user = user;
		setEditorVisible(editorExpanded && this.editorEnabled, !this.editorEnabled && editorExpanded);
		setDeleteVisible(editorEnabled);
	}

	private void setDeleteVisible(final boolean editorEnabled) {
		deleteButton.setVisible(editorEnabled);
	}

	public boolean isEditorEnabled() {
		return editorEnabled;
	}

	public void setParamSetList(final ClientParamSetList newList) {
		selectionController.setParamSetList(newList);
	}

	/**
	 * @param editorExpanded When set to true, the parameter editor is displayed
	 */
	public void setEditorExpanded(final boolean editorExpanded) {
		this.editorExpanded = editorExpanded;
		setEditorVisible(this.editorExpanded && editorEnabled, !editorEnabled && this.editorExpanded);
	}

	public boolean isEditorExpanded() {
		return editorExpanded;
	}

	/**
	 * The actual function that does the grunt work of making the editor visible if it is expanded,
	 * and hiding it when it is not. Tests whether the editor was visible previously and changes visibility only
	 * if there was a change.
	 *
	 * @param editorVisible       Whether the editor should be made visible.
	 * @param errorMessageVisible Whether the editor error message should be made visible instead of the editor.
	 */
	private void setEditorVisible(final boolean editorVisible, final boolean errorMessageVisible) {
		if (this.editorVisible != editorVisible) {
			saveButton.setVisible(editorVisible && editorEnabled);
			for (final Element e : editorElements) {
				DOM.setStyleAttribute(e, "display", editorVisible ? "" : "none");
			}
			if (editorVisible) {
				// We are becoming visible. We load list of fixed/variable mods so the controls are ready when the user needs them
				if (fixedMods.getAllowedValues() == null || fixedMods.getAllowedValues().isEmpty()) {
					loadModificationAllowedValues();
				}
			}
		}
		this.editorVisible = editorVisible;

		if (editorErrorMessageVisible != errorMessageVisible) {
			DOM.setStyleAttribute(DOM.getElementById("parameterEditorDisabledMessage"), "display", errorMessageVisible ? "" : "none");
		}
		editorErrorMessageVisible = errorMessageVisible;
	}

	/**
	 * We load allowed mods values for both the fixed and variable mod control.
	 * Since both of them operate on the same list, we load the data just once and then copy it.
	 */
	private void loadModificationAllowedValues() {
		validationController.getAllowedValuesForValidatable(fixedMods, new Callback() {
			@Override
			public void done() {
				varMods.setAllowedValues(fixedMods.getAllowedValues());
			}
		});
	}

	public ClientParamSet getSelectedParamSet() {
		return selectionController.getSelectedParamSet();
	}

	public void setSelectedParamSet(final ClientParamSet paramSet) {
		selectionController.select(paramSet);
	}

	public ValidationController getValidationController() {
		return validationController;
	}

	/**
	 * Fires change events whenever the selected ParamSet changes, or when a change in validation state occurs.
	 */
	@Override
	public void addChangeListener(final ChangeListener changeListener) {
		listeners.add(changeListener);
	}

	@Override
	public void removeChangeListener(final ChangeListener changeListener) {
		listeners.remove(changeListener);
	}

	/**
	 * @return true if it's possible to call flushParamsFiles(), that is, if the current parameter
	 *         selections validate with no errors.
	 */
	public boolean isValid() {
		return validationController.isValid();
	}

	private void save() {
		new SaveDialog(selector.getSelectedParamSet(), serviceAsync, user,
				new SaveDialog.Callback() {
					@Override
					public void saveCompleted(final ClientParamSet paramSet) {
						selectionController.refresh();

					}
				});
	}

	private void preview() {
		new PreviewDialog(selector.getSelectedParamSet(), serviceAsync);
	}

	private void delete() {
		final ClientParamSet setToDelete = getSelectedParamSet();
		if (selectionController.getClientParamSets().size() <= 1) {
			Window.alert("Cannot delete all parameter sets - at least one must remain.");
		} else {
			if (Window.confirm("Do you really want to delete parameter set " + setToDelete.getName() + "?")) {
				serviceAsync.delete(
						selector.getSelectedParamSet(), new AsyncCallback<Void>() {
					@Override
					public void onFailure(final Throwable throwable) {
						ErrorDialog.handleGlobalError(throwable);
					}

					@Override
					public void onSuccess(final Void aVoid) {
						selectionController.refresh();
					}
				});
			}
		}
	}

	private void popupDbCurator() {

		final ClientSequenceDatabase csd = (ClientSequenceDatabase) dlb.getSelected();
		final Integer selected = csd.getId();

		final Map<String, String> emailInitialPairs = new TreeMap<String, String>();

		for (final Map.Entry<String, ClientUser> me : userInfo.entrySet()) {
			emailInitialPairs.put(me.getKey(), me.getValue().getInitials());
		}

		final DialogBox dialogBox = new DialogBox(false);
		final CurationEditor ce = new CurationEditor(selected, user.getEmail(), emailInitialPairs, new EditorCloseCallback() {
			@Override
			public void editorClosed(final Integer openCurationID) {
				validationController.getAllowedValues(dlb, new Callback() {
					@Override
					public void done() {
						if (openCurationID != null) {
							dlb.select(openCurationID, validationController);
						}
						dialogBox.hide();
					}
				});
			}
		});
		DOM.setElementAttribute(dialogBox.getElement(), "id", "db-curator");
		dialogBox.setStyleName("dbCuratorEmbed");
		dialogBox.setWidget(ce);
		dialogBox.setSize(Window.getClientWidth() * .8 + "px", Window.getClientHeight() * .8 + "px");
		ce.setPixelSize(Math.max((int) (Window.getClientWidth() * .8), 770), (int) (Window.getClientHeight() * .8));
//		LightBox lb = new LightBox(dialogBox);
//		try {
//			lb.show();
//		} catch (Exception ignore) {
		dialogBox.show();
//		}
		dialogBox.center();
	}
}
