package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A combo box for selecting category to put QuaMeter results into.
 *
 * @author Roman Zenka
 */
public final class QuameterCategorySelector extends HorizontalPanel {
	/* A currently selected quameter category */
	public static final String QUAMETER_CATEGORY = "quameter.category";
	/**
	 * UI configuration key for listing all available categories
	 */
	public static final String UI_QUAMETER_CATEGORIES = "swift.quameter.categories";
	private ListBox category;
	private final SearchMetadata searchMetadata;
	private final UiConfiguration uiConfiguration;

	public QuameterCategorySelector(final UiConfiguration uiConfiguration, final SearchMetadata searchMetadata) {
		this.searchMetadata = searchMetadata;
		this.uiConfiguration = uiConfiguration;
		category = new ListBox(false);

		final String categoryString = getCategoryString(uiConfiguration);
		final LinkedHashMap<String, String> categories = QuameterConfigurationUtils.parseCategories(categoryString);
		for (final Map.Entry<String, String> entry : categories.entrySet()) {
			category.addItem(entry.getKey(), entry.getValue());
		}

		final String searchCategory = getSearchCategory();
		// If we happened to be null, we overwrite the value to the default
		setSearchCategory(searchCategory);
		selectByValue(searchCategory);

		category.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(final ChangeEvent event) {
				setSearchCategory(category.getValue(category.getSelectedIndex()));
			}
		});
		this.add(new Label("QuaMeter:"));
		this.add(category);
	}

	/**
	 * If value is null, default value is selected.
	 */
	private void selectByValue(final String value) {
		final String val;
		if (value == null) {
			val = QuameterConfigurationUtils.getDefaultCategory(getCategoryString(uiConfiguration));
			if (val == null) {
				category.setSelectedIndex(0);
				return;
			}
		} else {
			val = value;
		}
		for (int i = 0; i < category.getItemCount(); i++) {
			if (val.equals(category.getValue(i))) {
				category.setSelectedIndex(i);
				break;
			}
		}
	}

	private void setSearchCategory(final String category) {
		searchMetadata.setSearchMetadata(QUAMETER_CATEGORY, category);
	}

	private String getSearchCategory() {
		final String result = searchMetadata.getSearchMetadata(QUAMETER_CATEGORY);
		if (result == null) {
			return QuameterConfigurationUtils.getDefaultCategory(getCategoryString(uiConfiguration));
		}
		return result;
	}

	public static String getCategoryString(final UiConfiguration configuration) {
		return configuration.getConfigurationSetting(UI_QUAMETER_CATEGORIES);
	}
}
