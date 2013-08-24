package edu.mayo.mprc.swift.ui.client.widgets.validation;

import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.mayo.mprc.swift.ui.client.dialogs.FrameDialog;
import edu.mayo.mprc.swift.ui.client.rpc.ClientScaffoldSettings;
import edu.mayo.mprc.swift.ui.client.rpc.ClientStarredProteins;

public final class StarredProteinsDialog extends FrameDialog {
	private ClientScaffoldSettings value;
	private ClientStarredProteins lastValue;
	private VerticalPanel panel;
	private TextArea starredProteins;

	public StarredProteinsDialog() {
		super("Starred Proteins", true, true, true, true);
		panel = new VerticalPanel();

		starredProteins = new TextArea();
		starredProteins.setVisibleLines(20);
		starredProteins.setCharacterWidth(100);
		panel.add(starredProteins);

		setContent(panel);
	}


	@Override
	protected void okay() {
		lastValue.setStarred(convertEditableToRegEx(starredProteins.getText()));
		value.setStarredProteins(lastValue);
		hide();
	}

	@Override
	protected void cancel() {
		hide();
	}

	public void setValue(final ClientScaffoldSettings value) {
		this.value = value;
		if (value.getStarredProteins() != null) {
			lastValue = new ClientStarredProteins(
					value.getStarredProteins().getStarred(),
					"\\s+",
					true);

		} else {
			if (lastValue == null) {
				lastValue = new ClientStarredProteins("\\bADH1_YEAST\\b\n\\bOVAL_CHICK\\b\n\\bBGAL_ECOLI\\b\n\\bLACB_BOVIN\\b", "\\s+", true);
			}
		}

		starredProteins.setText(convertRegexToEditable(lastValue.getStarred()));
	}

	public ClientStarredProteins getLastValue() {
		return lastValue;
	}

	private String convertEditableToRegEx(final String s) {
		final String[] chunks = s.split("\\s+");
		final StringBuilder result = new StringBuilder();
		for (final String chunk : chunks) {
			result.append("\\b")
					.append(chunk)
					.append("\\b\n");
		}
		return result.substring(0, result.length() - 1);
	}

	private String convertRegexToEditable(final String starredDescription) {
		final String[] proteinList = starredDescription.split("\\n");
		final StringBuilder replaced = new StringBuilder();
		for (final String s : proteinList) {
			String result = s.trim();
			if (result.startsWith("\\b")) {
				result = result.substring(2);
			}
			if (result.endsWith("\\b")) {
				result = result.substring(0, result.length() - 2);
			}
			replaced.append(result).append('\n');
		}
		return replaced.toString();
	}
}
