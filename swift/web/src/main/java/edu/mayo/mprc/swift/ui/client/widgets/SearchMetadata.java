package edu.mayo.mprc.swift.ui.client.widgets;

/**
 * An object that provides access to string based key-value metadata for search.
 *
 * @author Roman Zenka
 */
public interface SearchMetadata {
	/**
	 * Set metadata entry. When value is null, it means the entry gets deleted.
	 *
	 * @param key   Key to set. The convention is to use all lowercase and dots, e.g.
	 *              swift.quameter.group.members
	 * @param value Value for the key. Complex values need to be serialized to a human-editable form. Try to avoid complex values,
	 *              if absolutely needed, use JSON representation.
	 */
	void setSearchMetadata(final String key, final String value);

	/**
	 * @param key Metadata key.
	 * @return Value for the key or null if no such key is defined.
	 */
	String getSearchMetadata(final String key);
}
