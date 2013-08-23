package edu.mayo.mprc.swift.ui.client.dialogs;

import com.google.gwt.user.client.ui.*;

/**
 * A floating dialog with a frame, and a close box.
 */
public abstract class FrameDialog extends DialogBox {
	private SimplePanel contentPanel;
	private Button okayButton;
	private Button cancelButton;
	private Label dialogTitleLabel;

	protected FrameDialog(final String title, final boolean enableOk, final boolean enableCancel, final boolean clickOutsideCancels, final boolean modal) {
		// Closed by clicking outside only if we do not provide closing buttons
		super(clickOutsideCancels, modal);

		final VerticalPanel vp = new VerticalPanel();
		vp.setStyleName("frameDialog");
		final DockPanel dp = new DockPanel();
		dialogTitleLabel = new Label(title);
		dp.add(dialogTitleLabel, DockPanel.CENTER);
		// TODO: close box.
		vp.add(dp);
		contentPanel = new SimplePanel();
		vp.add(contentPanel);
		dp.setStyleName("frameDialogHeader");

		final FlowPanel buttonPanel = new FlowPanel();
		buttonPanel.addStyleName("ok-cancel-button-panel");

		if (enableOk) {
			okayButton = new Button("OK");
			buttonPanel.add(okayButton);
			okayButton.addClickListener(new ClickListener() {
				@Override
				public void onClick(final Widget widget) {
					okay();
				}
			});
		}

		if (enableCancel) {
			cancelButton = new Button("Cancel");
			buttonPanel.add(cancelButton);
			cancelButton.addClickListener(new ClickListener() {
				@Override
				public void onClick(final Widget widget) {
					cancel();
				}
			});
		}

		if (enableOk || enableCancel) {
			vp.add(buttonPanel);
		}

		setWidget(vp);
	}

	protected FrameDialog(final boolean enableOk, final boolean enableCancel, final boolean clickOutsideCancels) {
		this("", enableOk, enableCancel, clickOutsideCancels, false);
	}

	protected FrameDialog(final String title, final boolean enableOk, final boolean enableCancel) {
		this(title, enableOk, enableCancel, !enableOk && !enableCancel, false);
	}

	protected FrameDialog(final boolean enableOk, final boolean enableCancel) {
		this("", enableOk, enableCancel, !enableOk && !enableCancel, false);
	}

	public String getDialogTitle() {
		return dialogTitleLabel.getText();
	}

	public void setDialogTitle(final String title) {
		dialogTitleLabel.setText(title);
	}

	public void setContent(final Widget contentPanel) {
		this.contentPanel.setWidget(contentPanel);
	}

	protected void enableOkButton(final boolean enabled) {
		okayButton.setEnabled(enabled);
	}

	protected void enableCancelButton(final boolean enabled) {
		cancelButton.setEnabled(enabled);
	}


	public void setOkListener(final ClickListener listener) {
		this.okayButton.addClickListener(listener);
	}

	protected abstract void okay();

	protected abstract void cancel();
}
