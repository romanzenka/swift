package edu.mayo.mprc.swift.ui.client.rpc;

public final class ClientString implements ClientValue {
	private static final long serialVersionUID = 20101221L;
	private String value;

	public ClientString() {
	}

	public ClientString(final String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}

	public String toString() {
		if (value == null) {
			return "(null)";
		}
		return String.valueOf(value);
	}
}
