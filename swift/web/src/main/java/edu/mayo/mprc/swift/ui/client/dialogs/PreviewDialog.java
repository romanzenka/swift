package edu.mayo.mprc.swift.ui.client.dialogs;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabPanel;
import edu.mayo.mprc.swift.ui.client.rpc.ClientParamFile;
import edu.mayo.mprc.swift.ui.client.rpc.ClientParamSet;
import edu.mayo.mprc.swift.ui.client.service.ServiceAsync;

/**
 * Show a preview of the generated params files.
 */
public final class PreviewDialog extends FrameDialog {

	public PreviewDialog(final ClientParamSet paramSet, final ServiceAsync service) {
		super("Parameter file preview", /*ok button*/true, /*cancel button*/false, /*closed by clicking outside*/true, false);

		final Label label = new Label("Please wait..."); // TODO replace with Roman's indeterminate progress bar?

		setContent(label);

		service.getFiles(paramSet, new AsyncCallback<ClientParamFile[]>() {
			@Override
			public void onFailure(final Throwable throwable) {
				hide();
				ErrorDialog.handleGlobalError(throwable);
			}

			@Override
			public void onSuccess(final ClientParamFile[] files) {

				final TabPanel panel = new TabPanel();
				panel.setWidth((int) (Window.getClientWidth() * 0.8) + "px");
				panel.setHeight((int) (Window.getClientHeight() * 0.8) + "px");
				for (final ClientParamFile file : files) {
					String text = file.getText();
					if (text == null) {
						text = "(null)";
					}
					text = text.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
					final HTML html = new HTML("<div class=\"param-preview\" style=\"width: " + Window.getClientWidth() * 0.8 + "px; height: " + Window.getClientHeight() * 0.8 + "px\"><pre>" + text + "</pre></div>");
					//ScrollPanel spanel = new ScrollPanel(html);
					panel.add(html, file.getName());
				}
				setContent(panel);
				panel.selectTab(0);
				center();
				show();
			}
		});
		center();
		show();
	}

	@Override
	protected void cancel() {
		hide();
	}

	@Override
	protected void okay() {
		hide();
	}
}
