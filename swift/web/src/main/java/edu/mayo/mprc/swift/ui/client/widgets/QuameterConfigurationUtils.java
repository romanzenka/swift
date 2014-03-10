package edu.mayo.mprc.swift.ui.client.widgets;

import java.util.LinkedHashMap;

/**
 * @author Roman Zenka
 */
public final class QuameterConfigurationUtils {

	public static final String DEFAULT_CATEGORIES = "no category";

	/**
	 * We are parsing a string like animal,-dog,--chihuahua*,-cat,--siamese
	 * into a list of strings to put into ListBox and values. The strings
	 * are used as keys and use tabs/whatever system to show hierarchy.
	 * The values are the values themselves, with no dashes at the beginning.
	 * A trailing asterisk denotes a default value and is parsed by a different function.
	 *
	 * @param categoryString Category string to parse.
	 * @return Map with the parsed categories.
	 */
	public static LinkedHashMap<String, String> parseCategories(final String categoryString) {
		final LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		final String[] categories = splitCategoryString(categoryString);
		for (final String cat : categories) {
			final String category = cat.trim();
			final int i = getNumDashes(category);
			final int isDefault = isDefault(category) ? 1 : 0;
			final String value = category.substring(i, category.length() - isDefault);
			final String key = repeat(i, " - ") + value;
			result.put(key, value);
		}

		return result;
	}

	private static String[] splitCategoryString(final String categoryString) {
		final String catStr = (categoryString == null ? DEFAULT_CATEGORIES : categoryString);
		return catStr.split(",");
	}

	static int getNumDashes(final String category) {
		int i = 0;
		while (i < category.length() && (int) category.charAt(i) == (int) '-') {
			i++;
		}
		return i;
	}

	static boolean isDefault(final String category) {
		return category.endsWith("*");
	}

	private static String repeat(final int i, final CharSequence text) {
		return new String(new char[i]).replace("\0", text);
	}

	public static String getDefaultCategory(final String categoryString) {
		String firstCategory = null;
		final String[] categories = splitCategoryString(categoryString);
		for (final String cat : categories) {
			final String category = cat.trim();
			if (isDefault(category)) {
				return category.substring(getNumDashes(category), category.length() - 1);
			}
			if (firstCategory == null) {
				firstCategory = category.substring(getNumDashes(category));
			}
		}
		return firstCategory;
	}
}
