package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.ui.*;

/**
 * Lets the user choose a search type.
 * Fires an on change event when it gets modified by the user.
 */
public final class SearchTypeList extends ListBox implements SourcesChangeEvents, ClickListener, ChangeListener {

	/**
	 * Table of offered search types.
	 */
	private SearchTypeEntry[] searchTypeEntries = new SearchTypeEntry[]{
			new SearchTypeEntry(SearchType.OneToOne, "Separate experiment for each input", "one-to-one"),
			new SearchTypeEntry(SearchType.ManyToOne, "One experiment, all inputs are one biological sample", "many-to-one"),
			new SearchTypeEntry(SearchType.ManyToSamples, "One experiment, each input is a separate biological sample", "many-to-samples"),
			new SearchTypeEntry(SearchType.Custom, "Custom", "custom"),
	};
	private static final int DEFAULT_ENTRY = 2; /* ManyToSamples */

	/**
	 * Listeners interested in hearing about the change in selection.
	 */
	private ChangeListenerCollection listeners = new ChangeListenerCollection();
	private static final String SEARCH_TYPE_COOKIE = "search-type";

	public SearchTypeList() {
		for (int i = 0; i < searchTypeEntries.length; i++) {
			this.addItem(searchTypeEntries[i].getLabel(), String.valueOf(searchTypeEntries[i].getType().getType()));
			searchTypeEntries[i].index = i;
		}

		final String searchTypeCookie = Cookies.getCookie(SEARCH_TYPE_COOKIE);
		SearchTypeEntry selectedSearchType = searchTypeEntries[DEFAULT_ENTRY];
		if (searchTypeCookie != null) {
			for (final SearchTypeEntry searchTypeEntry : searchTypeEntries) {
				if (searchTypeEntry.getCookie().equalsIgnoreCase(searchTypeCookie)) {
					selectedSearchType = searchTypeEntry;
					break;
				}
			}
		}

		this.setSelectedIndex(selectedSearchType.getIndex());
		addClickListener(this);
		addChangeListener(this);
	}

	/**
	 * Given a file setup, guess which search type it corresponds to.
	 */
	public static SearchType getSearchTypeFromSetup(
			final String searchTitle,
			final String fileNameWithoutExtension,
			final String experimentName,
			final String biologicalSampleName) {
		if (experimentName.equals(searchTitle)) {
			// Likely there is one .SFD file for the entire experiment
			if (biologicalSampleName.equals(fileNameWithoutExtension)) {
				return SearchType.ManyToSamples;
			} else if (biologicalSampleName.equals(searchTitle)) {
				return SearchType.ManyToOne;
			} else {
				return SearchType.Custom;
			}
		} else {
			if (experimentName.equals(fileNameWithoutExtension) &&
					biologicalSampleName.endsWith(fileNameWithoutExtension)) {
				return SearchType.OneToOne;
			} else {
				return SearchType.Custom;
			}
		}
	}

	public SearchType getSelectedSearchType() {
		return SearchType.fromType(Integer.parseInt(this.getValue(this.getSelectedIndex())));
	}

	public void setSelectedSearchType(final SearchType type, final boolean storeUserPreference) {
		for (int i = 0; i < this.getItemCount(); i++) {
			if (this.getValue(i).equals(String.valueOf(type.getType()))) {
				this.setSelectedIndex(i);
				if (storeUserPreference) {
					storeSelectionInCookie();
				}
				return;
			}
		}
		throw new RuntimeException("Search type " + type.getType() + " not found amoung items");
	}

	private void storeSelectionInCookie() {
		if (this.getSelectedIndex() >= 0 && this.getSelectedIndex() < searchTypeEntries.length) {
			Cookies.setCookie(SEARCH_TYPE_COOKIE, searchTypeEntries[this.getSelectedIndex()].getCookie(), ParamSetSelectionController.getCookieExpirationDate(), null, "/", false);
		}
	}

	public void fireSelectionChanged() {
		listeners.fireChange(this);
		storeSelectionInCookie();
	}

	public void addSelectionChangeListener(final ChangeListener changeListener) {
		listeners.add(changeListener);
	}

	public void removeSelectionChangeListener(final ChangeListener changeListener) {
		listeners.remove(changeListener);
	}

	@Override
	public void onClick(final Widget widget) {
		fireSelectionChanged();
	}

	@Override
	public void onChange(final Widget widget) {
		fireSelectionChanged();
	}

	private static final class SearchTypeEntry {
		private SearchType type;
		private String label;
		private String cookie;
		private int index;

		private SearchTypeEntry(final SearchType type, final String label, final String cookie) {
			this.type = type;
			this.label = label;
			this.cookie = cookie;
		}

		public SearchType getType() {
			return type;
		}

		public String getLabel() {
			return label;
		}

		public String getCookie() {
			return cookie;
		}

		public int getIndex() {
			return index;
		}
	}
}
