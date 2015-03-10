package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.user.client.ui.TextBox;
import edu.mayo.mprc.swift.ui.client.rpc.ClientFileSearch;

import java.util.List;

/**
 * The output path has quite non-trivial logic, turned into its own control.
 *
 * @author Roman Zenka
 */
public final class OutputPath extends TextBox {
	private enum OutputPathState {
		NoFiles, // Show that there are no files
		NoTitle, // Show that there is no title
		LoadedFromPreviousSearch, // Show previously loaded path + title
		UserModified, // Show user-entered path
		DeterminedFromFiles, // Show maximum prefix from file table
	}

	private enum OutputPathAlteringEvent {
		TitleEmptied, // The search title is empty
		TitleSet, // The search title got set
		FileTableEmptied, // The file table is empty now
		FileTableChanged, // User added/removed files
		SearchLoaded, // Previous search got loaded, the file table is modified now
		UserChanged, // The user modified the path
		UserEmpty, // The user emptied the path
	}

	// These two store the output path state
	// state itself + the locking state that is independent
	private OutputPathState outputPathState;
	private boolean outputPathChangeEnabled = true;
	private boolean outputPathUserSpecified = false;


	private void setOutputPathChangeEnabled(final boolean enabled) {
		if (enabled != outputPathChangeEnabled) {
			outputPathChangeEnabled = enabled;
			if (!outputPathChangeEnabled) {
				outputPathUserSpecified = false;
			}
			setReadOnly(!enabled);
			// Update the output location, when changes are disabled, the field gets filled automatically
			updateOutputLocation();
		}
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
}
