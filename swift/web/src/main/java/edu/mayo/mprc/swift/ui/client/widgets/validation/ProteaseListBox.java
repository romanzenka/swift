package edu.mayo.mprc.swift.ui.client.widgets.validation;

import edu.mayo.mprc.common.client.ExceptionUtilities;
import edu.mayo.mprc.swift.ui.client.rpc.ClientProtease;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValue;

import java.util.Collections;
import java.util.List;

/**
 * Display ClientProteases.
 */
public final class ProteaseListBox extends ValidatableListBox {
	private static final List<ClientValue> EMPTY_VALUE = Collections.emptyList();

	public ProteaseListBox(final String param) {
		super(param, false);
	}

	@Override
	public ClientValue bundle(final List<? extends ClientValue> selected) {
		return null;// not used.
	}

	@Override
	public String getStringValue(final ClientValue value) {
		if (value == null) {
			return "";
		}
		if (!(value instanceof ClientProtease)) {
			ExceptionUtilities.throwCastException(value, ClientProtease.class);
			return null;
		}
		return ((ClientProtease) value).getName();
	}

	@Override
	public List<? extends ClientValue> unbundle(final ClientValue value) {
		return EMPTY_VALUE;  //To change body of implemented methods use File | Settings | File Templates.
	}

	/**
	 * If this control needs allowed values, then this method should return a string that will be
	 * passed to the params mapping (this string could be ""); if no allowed values are required,
	 * then this method should return null;
	 * <p/>
	 * For now, the list of proteases never changes. Therefore we fetch allowed values only once, at startup.
	 *
	 * @return a string that's passed to the mapping's getAllowedValues() method for this param, or null.
	 */
	@Override
	public boolean needsAllowedValues() {
		return !(allowedValues != null && !allowedValues.isEmpty());
	}
}
