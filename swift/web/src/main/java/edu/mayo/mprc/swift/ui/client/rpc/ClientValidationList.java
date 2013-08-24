package edu.mayo.mprc.swift.ui.client.rpc;

import edu.mayo.mprc.swift.params2.mapping.ValidationList;

import java.util.ArrayList;

/**
 * Client side proxy for {@link ValidationList}.
 */
public final class ClientValidationList extends ArrayList<ClientValidation> implements ClientValue {
	private static final long serialVersionUID = 20101221L;

	public ClientValidationList(final int initialCapacity) {
		super(initialCapacity);
	}

	public ClientValidationList() {
	}

	public ClientValidation getLast() {
		return get(size() - 1);
	}

	public ClientValue getValue() {
		for (final ClientValidation v : this) {
			if (v.getValue() != null) {
				return v.getValue();
			}
		}
		return null;
	}

	public int getWorstSeverity() {
		return getWorstSeverityRec();
	}

	private int getWorstSeverityRec() {
		int currentSeverity = -1;
		for (final ClientValidation v : this) {
			if (currentSeverity == -1 || v.getSeverity() > currentSeverity) {
				currentSeverity = v.getSeverity();
			}
		}
		return currentSeverity;
	}
}
