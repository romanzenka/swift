package edu.mayo.mprc.swift.ui.client.widgets.validation;


import edu.mayo.mprc.swift.ui.client.rpc.ClientSequenceDatabase;
import edu.mayo.mprc.swift.ui.client.rpc.ClientUser;
import edu.mayo.mprc.swift.ui.client.rpc.ClientValue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Display a list of {@link ClientSequenceDatabase} objects.
 */
public final class DatabaseListBox extends ValidatableListBox {
	private static final List<ClientValue> EMPTY_VALUES = Collections.emptyList();

	//User map. Used to map e-mail to user id.
	private Map<String/*email*/, ClientUser> userInfo;

	public DatabaseListBox(final String param, final Map<String, ClientUser> userInfo) {
		super(param, false);

		this.userInfo = userInfo;
	}

	@Override
	public String getStringValue(final ClientValue value) {
		if (value == null) {
			return "";
		}
		if (!(value instanceof ClientSequenceDatabase)) {
			throw new RuntimeException("Expected a ClientSequenceDatabase");
		}
		final ClientSequenceDatabase csd = (ClientSequenceDatabase) value;
		return csd.getShortName() + " - " + csd.getDisplayName() + (userInfo != null && csd.getOwnerEmail() != null && !csd.getOwnerEmail().isEmpty()
				&& userInfo.get(csd.getOwnerEmail()) != null ? " (" + userInfo.get(csd.getOwnerEmail()).getInitials() + ")" : "");
	}

	@Override
	public void setValue(final ClientValue value) {
		try {
			super.setValue(value);
		} catch (Exception ignore) {
			// SWALLOWED: Ignore the error for now
		}
	}

	public void select(final int databaseId) {
		for (final ClientValue allowedValue : allowedValues) {
			final ClientSequenceDatabase csd = (ClientSequenceDatabase) allowedValue;
			if (csd.getId() == databaseId) {
				setValue(csd, true);
				return;
			}

		}
		// If we cannot find the database - we will not change the database
		// throw new RuntimeException("Can't find database with id " + databaseId);
	}

	@Override
	public ClientValue bundle(final List<? extends ClientValue> selected) {
		return null;//unused
	}

	@Override
	public List<? extends ClientValue> unbundle(final ClientValue value) {
		return EMPTY_VALUES; // unused
	}

	@Override
	public boolean needsAllowedValues() {
		return true;
	}
}
