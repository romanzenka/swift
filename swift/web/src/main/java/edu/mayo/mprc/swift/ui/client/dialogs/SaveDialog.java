package edu.mayo.mprc.swift.ui.client.dialogs;

import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import edu.mayo.mprc.swift.ui.client.rpc.ClientParamSet;
import edu.mayo.mprc.swift.ui.client.rpc.ClientUser;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValidation;
import edu.mayo.mprc.swift.ui.client.service.ServiceAsync;

/**
 * Prompt user to save the param set.
 */
public final class SaveDialog extends FrameDialog {
	public interface Callback {
		void saveCompleted(ClientParamSet paramSet);
	}

	private ServiceAsync service;
	private Callback cb;
	private ClientParamSet paramSet;
	private TextBox newName;
	private HorizontalPanel newNameValidation;
	private TextBox userNameTextBox;
	private ClientUser user;

	public SaveDialog(final ClientParamSet paramSet, final ServiceAsync service, final ClientUser user,
	                  final Callback cb) {
		super("Save", true, true);

		this.service = service;
		this.cb = cb;
		this.paramSet = paramSet;
		this.user = user;

		Label l;
		final Grid g = new Grid(3, 3);
		l = new Label("Save:");
		g.setWidget(0, 0, l);
		l.setStyleName("italic");
		g.setWidget(0, 1, new Label(paramSet.getName()));

		l = new Label("As:");
		g.setWidget(1, 0, l);
		l.setStyleName("italic");
		newName = new TextBox();
		g.setWidget(1, 1, newName);
		newName.addKeyUpHandler(new KeyUpHandler() {
			@Override
			public void onKeyUp(final KeyUpEvent event) {
				setValidStatus();
			}
		});

		l = new Label("Owner:");
		g.setWidget(2, 0, l);
		l.setStyleName("italic");

		userNameTextBox = new TextBox();
		g.setWidget(2, 1, userNameTextBox);
		userNameTextBox.setText(user.getName());
		userNameTextBox.setEnabled(false);

		newNameValidation = new HorizontalPanel();
		newNameValidation.addStyleName("invisible");
		newNameValidation.add(ValidationPanel.getImageForSeverity(ClientValidation.SEVERITY_WARNING));
		newNameValidation.add(new Label("That name is already in use"));
		g.setWidget(1, 2, newNameValidation);

		setContent(g);
		center();
		setValidStatus();
		show();
	}

	private boolean setValidStatus() {
		boolean enabled = true;
		if ("".equals(newName.getText())) {
			// is it actually necessary to show a valdation here;
			enabled = false;
		}

		enableOkButton(enabled);
		return enabled;
	}

	@Override
	protected void cancel() {
		hide();
	}

	@Override
	protected void okay() {
		if (!setValidStatus()) {
			return; // TODO need better validation.
		}

		service.save(
				paramSet,
				newName.getText(),
				user.getEmail(),
				user.getInitials(),
				true,
				new AsyncCallback<ClientParamSet>() {
					@Override
					public void onFailure(final Throwable throwable) {
						hide();
						ErrorDialog.handleGlobalError(throwable);
					}

					@Override
					public void onSuccess(final ClientParamSet o) {

						if (o != null) {
							cb.saveCompleted(o);
							hide();
						} else {
							setValidStatus();
						}
					}
				}
		);
	}
}
