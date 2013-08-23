package edu.mayo.mprc.swift.configuration.client.view;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.mayo.mprc.swift.configuration.client.ConfigurationService;
import edu.mayo.mprc.swift.configuration.client.FixTagActionListener;
import edu.mayo.mprc.swift.configuration.client.model.ResourceModel;
import edu.mayo.mprc.swift.configuration.client.model.UiChanges;
import edu.mayo.mprc.swift.configuration.client.model.UiChangesReplayer;
import edu.mayo.mprc.swift.configuration.client.validation.local.RequiredFieldValidator;
import edu.mayo.mprc.swift.configuration.client.validation.local.Validator;

import java.util.ArrayList;
import java.util.List;

/**
 * The validator validates a change locally.
 * If it passes, it sends the change to the server.
 * This is not obvious.
 */
class MultiValidator implements Validator {
	private final ResourceModel model;
	private final String propertyName;
	private final List<Validator> validators = new ArrayList<Validator>(2);
	private boolean onDemand;
	private ValidationPanel validationPanel;

	MultiValidator(final ResourceModel model, final String propertyName) {
		this.model = model;
		this.propertyName = propertyName;
	}

	public void addValidator(final Validator v) {
		validators.add(v);
	}

	public void addOnDemandValidator() {
		onDemand = true;
	}

	@Override
	public String validate(final String value) {
		// The first synchronous validator to fail stops further validations
		// - it is already broken, why bother doing expensive validation?
		if (runLocalValidation(value, validationPanel)) {
			return "";
		}

		sendPropertyChange(value, false);
		return "";
	}

	private void sendPropertyChange(final String value, final boolean onDemand) {
		ConfigurationService.App.getInstance().propertyChanged(model.getId(), propertyName, value, onDemand, new AsyncCallback<UiChangesReplayer>() {
			@Override
			public void onFailure(final Throwable throwable) {
				validationPanel.addMessage(throwable.getMessage());
			}

			@Override
			public void onSuccess(final UiChangesReplayer uiChangesReplayer) {
				validationPanel.empty();
				final MyUiChanges uiChanges = new MyUiChanges(value);
				uiChangesReplayer.replay(uiChanges);
				if (!uiChanges.isError()) {
					validationPanel.showSuccess();
				}
				model.setProperty(propertyName, value);
			}
		});
	}

	/**
	 * Run local validation (e.g. integer validation, things that do not need to go to the server).
	 *
	 * @param value           Value to validate.
	 * @param validationPanel Panel to display the result of validation in.
	 * @return True if error. (action is needed). False - all ok.
	 */
	private boolean runLocalValidation(final String value, final ValidationPanel validationPanel) {
		validationPanel.clear();
		validationPanel.setVisible(false);
		for (final Validator v : validators) {
			final String result = v.validate(value);
			if (result != null) {
				validationPanel.addMessage(result, null);
				return true;
			}
		}
		return false;
	}

	public boolean hasOnDemandValidation() {
		return onDemand;
	}

	public void runOnDemandValidation(final String value, final ValidationPanel validationPanel) {
		validationPanel.empty();
		validationPanel.validationStarted();
		if (runLocalValidation(value, validationPanel)) {
			return;
		}
		sendPropertyChange(value, true);
	}

	public boolean isRequiredField() {
		for (final Validator v : validators) {
			if (v instanceof RequiredFieldValidator) {
				return true;
			}
		}
		return false;
	}

	public void setValidationPanel(final ValidationPanel validationPanel) {
		this.validationPanel = validationPanel;
	}

	public ValidationPanel getValidationPanel() {
		return validationPanel;
	}

	private class MyUiChanges implements UiChanges {
		private static final long serialVersionUID = 5427348584970584338L;
		private boolean error;
		private final String value;

		public MyUiChanges(final String value) {
			this.value = value;
			error = false;
		}

		@Override
		public void setProperty(final String resourceId, final String propertyName, final String newValue) {
			//TODO: implement me
		}

		public void setPropertyDescription(final String resourceId, final String propertyName, final String description) {
			//TODO: implement me
		}

		@Override
		public void displayPropertyError(final String resourceId, final String propertyName, final String error) {
			//TODO: Implement properly (allow user to display error on a different property than the validated one)
			if (error == null) {
				validationPanel.clear();
			} else {
				this.error = true;
				validationPanel.addMessage(error, new FixTagActionListener() {
					@Override
					public void onFix(final String action, final ValidationPanel validationPanel) {
						ConfigurationService.App.getInstance().fix(resourceId, propertyName, action, new AsyncCallback<Void>() {
							@Override
							public void onFailure(final Throwable throwable) {
								validationPanel.addMessage(throwable.getMessage());
							}

							@Override
							public void onSuccess(final Void aVoid) {
								validationPanel.clear();
								sendPropertyChange(value, false);
							}
						});
					}
				});
			}
		}

		public boolean isError() {
			return error;
		}
	}
}
