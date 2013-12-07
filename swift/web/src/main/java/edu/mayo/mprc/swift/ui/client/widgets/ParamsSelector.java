package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.user.client.ui.ListBox;
import edu.mayo.mprc.swift.ui.client.rpc.ClientParamSet;

import java.util.List;

/**
 * A widget that allows selection amongst ClientParamSets
 */
public final class ParamsSelector extends ListBox {

	private List<ClientParamSet> sets;  // just a reference to the list obtained from the controller.
	private ClientParamSet selected;

	public ParamsSelector() {
	}

	public ClientParamSet getSelectedParamSet() {
		if (!sets.isEmpty()) {
			return sets.get(getSelectedIndex());
		}
		return null;
	}

	public void select(final int i) {
		selected = sets.get(i);
		setSelectedIndex(i);
	}

	/**
	 * Repopulate the list of available parameter sets.
	 *
	 * @param paramSets    New list of parameter sets.
	 * @param defaultSetId id of a parameter set to be selected by default if there was nothing selected previously
	 */
	public void update(final List<ClientParamSet> paramSets, final int defaultSetId) {
		int selectedItem = update(paramSets);
		selectIfNothingSelected(defaultSetId, selectedItem);
	}

	/**
	 * Repopulate the list of available parameter sets.
	 *
	 * @param paramSets New list of parameter sets.
	 * @return Imdex of the currently selected item or -1 if none available.
	 */
	public int update(final List<ClientParamSet> paramSets) {
		clear();
		sets = paramSets;

		if (sets.isEmpty()) {
			addItem("(No parameter sets available)");
			setEnabled(false);
			return -1;
		} else {
			setEnabled(true);
			return addItems();
			// no need to update, as we're already selected.
		}
	}

	private void selectIfNothingSelected(int defaultSetId, int selectedIndex) {
		if (!sets.isEmpty()) {
			if (selectedIndex == -1) {
				// couldn't find the selected paramSet or none was yet selected, select the default.
				selectedIndex = setIdToSelectedIndex(defaultSetId);
			}
			selected = sets.get(selectedIndex);
			setSelectedIndex(selectedIndex);
		}
	}

	/**
	 * Adds items to the list, returns the index of the item that matches
	 * the one currently stored as {@link #selected}
	 */
	private int addItems() {
		int selectedIndex = -1;
		for (int i = 0; i < sets.size(); ++i) {
			addItem(sets.get(i).getName() +
					(sets.get(i).getInitials() != null ?
							(" (" + sets.get(i).getInitials() + ")") : ""));
			if (selected != null && selected.equals(sets.get(i))) {
				selectedIndex = i;
			}
		}
		return selectedIndex;
	}

	private int setIdToSelectedIndex(int defaultSetId) {
		int defaultValue = 0;
		for (int i = 0; i < sets.size(); i++) {
			if (sets.get(i).getId() == defaultSetId) {
				defaultValue = i;
				break;
			}
		}
		return defaultValue;
	}
}
