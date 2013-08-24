package edu.mayo.mprc.swift.ui.client.widgets.validation;

import com.google.gwt.user.client.ui.ListBox;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValue;

import java.util.*;

/**
 * A ListBox that can display a list of valid values fetched from the server.
 */
public abstract class ValidatableListBox extends ListBox implements Validatable {

	private Map<ClientValue, Integer> byObject = new HashMap();

	protected List<? extends ClientValue> allowedValues;
	protected String param;
	protected boolean hasNull = false;

	public ValidatableListBox(final String param, final boolean allowMultiple) {
		super(allowMultiple);
		this.param = param;
	}

	@Override
	public void focus() {
		super.setFocus(true);
	}

	/**
	 * get the selected values in the list
	 *
	 * @return the selected values bundled into a
	 */
	@Override
	public ClientValue getClientValue() {
		if (allowedValues == null) {
			return null;
		}
		if (isMultipleSelect()) {
			final List<ClientValue> items = new ArrayList<ClientValue>();
			for (int i = 0; i < getItemCount(); ++i) {
				if (isItemSelected(i)) {
					items.add(allowedValues.get(i));
				}
			}
			return bundle(items);
		} else {
			if (hasNull && getSelectedIndex() == getItemCount() - 1) {
				return null;
			}
			return allowedValues.get(getSelectedIndex());
		}
	}

	/**
	 * set the list of objects supported by the list box
	 *
	 * @param values
	 */
	@Override
	public void setAllowedValues(final List<? extends ClientValue> values) {
		if (values.equals(allowedValues)) {
			return;
		}
		clear();
		byObject.clear();
		hasNull = false;
		for (int i = 0; i < values.size(); ++i) {
			final String stringrep = getStringValue(values.get(i));
			addItem(stringrep);
			byObject.put(values.get(i), i);
		}
		allowedValues = values;
	}

	/**
	 * retrieve the list of objects represented by this list box
	 *
	 * @return array of ClientValue
	 */
	public List<? extends ClientValue> getAllValues() {
		return allowedValues;
	}

	@Override
	public void setValidationSeverity(final int validationSeverity) {
		ValidationController.setValidationSeverity(validationSeverity, this);
	}

	/**
	 * used to clear the values from list box
	 */
	public void clearValues() {
		allowedValues = null;
		clear();
	}

	/**
	 * add a value to the list, if not already in it
	 *
	 * @param value - the value(s) to add, if multi select then is bundled
	 */
	public void addValue(final ClientValue value, final Comparator<ClientValue> c) {
		if (isMultipleSelect()) {
			final ClientValue selected = getClientValue();
			addToMultiSelect(value, c);
			// now set the selected ones
			setValue(value);

			setValue(selected);
		}
		// TODO: Add non-multiple select variant
	}

	private void addToMultiSelect(final ClientValue value, final Comparator<ClientValue> c) {
		// unpack value
		final List<? extends ClientValue> toAdd = unbundle(value);
		// copy allowed values since will replace

		final HashSet<ClientValue> items = new HashSet<ClientValue>();
		if (allowedValues != null) {
			items.addAll(allowedValues);
		}
		for (final ClientValue aToAdd : toAdd) {
			if (items.contains(aToAdd)) {
				continue;
			}
			items.add(aToAdd);
		}
		final List<ClientValue> allowed = new ArrayList<ClientValue>(items.size());
		allowed.addAll(items);

		// these need to be sorted
		Collections.sort(allowed, c);

		setAllowedValues(allowed);
	}

	/**
	 * add a value to the list, if not already in it
	 *
	 * @param value - the value(s) to add, if multi select then is bundled
	 */
	public void addValueWithoutSelecting(final ClientValue value, final Comparator<ClientValue> c) {

		if (isMultipleSelect()) {
			final ClientValue selected = getClientValue();
			addToMultiSelect(value, c);

			setValue(selected);
		}
		// TODO: Add non-multiple select variant
	}


	/**
	 * remove a value from the list
	 *
	 * @param value
	 */
	public void removeValue(final ClientValue value, final Comparator<ClientValue> c) {
		if (isMultipleSelect()) {
			// unpack value
			final List<? extends ClientValue> toRemove = unbundle(value);
			// copy allowed values since will replace
			final HashSet<ClientValue> hs = new HashSet<ClientValue>();
			hs.addAll(toRemove);
			final List<ClientValue> toKeep = new ArrayList<ClientValue>();
			for (final ClientValue allowedValue : allowedValues) {
				// if not in hs keep it
				if (!hs.contains(allowedValue)) {
					toKeep.add(allowedValue);
				}
			}

			Collections.sort(toKeep, c);
			setAllowedValues(toKeep);
		}
		// TODO: Add non-multiple select variant
	}

	/**
	 * sets the value(s) as  selected
	 *
	 * @param value
	 */
	@Override
	public void setValue(final ClientValue value) {
		if (isMultipleSelect()) {
			if (value != null) {
				final List<? extends ClientValue> selected = unbundle(value);
				final HashSet<ClientValue> hs = new HashSet<ClientValue>();
				hs.addAll(selected);
				for (int i = 0; i < allowedValues.size(); ++i) {
					setItemSelected(i, hs.contains(allowedValues.get(i)));
				}
			}
		} else {
			if (value == null) {
				addItem("");
				setSelectedIndex(getItemCount() - 1);
				hasNull = true;
				return;
			} else if (hasNull) {
				removeItem(getItemCount() - 1);
				hasNull = false;
			}
			final Integer i = (Integer) byObject.get(value);
			if (i == null) {
				throw new RuntimeException(getStringValue(value)
						+ " doesn't appear in the list of allowed values for " + param);
			}
			setSelectedIndex(i);
		}
	}

	public ClientValue getSelected() {
		return allowedValues.get(getSelectedIndex());
	}

	public abstract String getStringValue(ClientValue value);

	public abstract ClientValue bundle(List<? extends ClientValue> selected);

	public abstract List<? extends ClientValue> unbundle(ClientValue value);
}
