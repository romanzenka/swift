package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import edu.mayo.mprc.swift.ui.client.rpc.ClientSpectrumQa;
import edu.mayo.mprc.swift.ui.client.rpc.SpectrumQaParamFileInfo;

import java.util.List;

public final class SpectrumQaSetupPanel extends HorizontalPanel {

	private final CheckBox spectrumQaEnabled = new CheckBox("Include msmsEval analysis. Param file: ");
	private ListBox propertyFile = new ListBox(false/*multiselect*/);
	private final HelpPopupButton helpPopupButton = new HelpPopupButton("<div width='300px' class='help-panel'>" +
			"<h2>Spectral analysis</h2>" +
			"<p>msmsEval is a tool that: <ul>" +
			"<li>calculates a discriminant score D for each spectrum</li>" +
			"<li> determines probabilty P(+|D) that the spectrum is identifiable</li>" +
			"</ul>" +
			"</p>" +
			"<p>We use msmsEval to provide additional information about the quality of the spectra. The spectra of high quality that were not identified " +
			"by Swift are candidates for more detailed future analysis, e.g. using de-novo search engines.</p>" +
			"<p>The msmsEval tool uses a parameter file that specifies how to calculate the score D. " +
			"We have experimented with the Orbitrap parameter set and suggest using just that one until we create more sets for other instruments." +
			"</p>" +
			"<p>For more information about msmsEval, see <a href=\"http://delphi.mayo.edu/wiki/trac.cgi/wiki/msmsEval\">our wiki page</a>. " +
			"Feel free to add your own notes to the wiki.</p>" +
			"</div>");

	public SpectrumQaSetupPanel(final List<SpectrumQaParamFileInfo> paramFileInfos) {
		setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		spectrumQaEnabled.setValue(true);

		for (final SpectrumQaParamFileInfo paramFileInfo : paramFileInfos) {
			propertyFile.addItem(paramFileInfo.getDescription(), paramFileInfo.getPath());
		}
		propertyFile.setSelectedIndex(0);

		spectrumQaEnabled.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				updateEnabledControls();
			}
		});

		updateEnabledControls();

		add(spectrumQaEnabled);
		add(propertyFile);
		add(helpPopupButton);
	}

	private void updateEnabledControls() {
		propertyFile.setEnabled(Boolean.TRUE.equals(spectrumQaEnabled.getValue()));
	}

	public ClientSpectrumQa getParameters() {
		if (!Boolean.TRUE.equals(spectrumQaEnabled.getValue())) {
			return new ClientSpectrumQa();
		} else {
			return new ClientSpectrumQa(propertyFile.getValue(propertyFile.getSelectedIndex()));
		}
	}

	public void setParameters(final ClientSpectrumQa spectrumQa) {
		spectrumQaEnabled.setValue(spectrumQa.isEnabled());
		selectParamFilePath(spectrumQa.getParamFilePath());
	}

	/**
	 * Select the item corresponding to given parameter file path.
	 * If no item corresponds, the first item is selected.
	 *
	 * @param paramFilePath Path to select.
	 */
	private void selectParamFilePath(final String paramFilePath) {
		int indexToSelect = 0;
		for (int itemIndex = 0; itemIndex < propertyFile.getItemCount(); itemIndex++) {
			if (propertyFile.getValue(itemIndex).equals(paramFilePath)) {
				indexToSelect = itemIndex;
				break;
			}
		}
		propertyFile.setSelectedIndex(indexToSelect);
	}
}

