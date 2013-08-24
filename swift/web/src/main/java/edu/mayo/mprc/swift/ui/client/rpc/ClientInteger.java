package edu.mayo.mprc.swift.ui.client.rpc;

public final class ClientInteger implements ClientValue {
	private static final long serialVersionUID = 20101221L;
	private Integer value;

	public ClientInteger() {
	}

	public ClientInteger(final Integer value) {
		this.value = value;
	}

	public ClientInteger(final int value) {
		this.value = value;
	}

	public ClientInteger(final String value) {
		this(Integer.parseInt(value));
	}

	public Integer getValue() {
		return value;
	}

	public void setValue(final Integer value) {
		this.value = value;
	}

	public String toString() {
		if (value == null) {
			return "(null)";
		}
		return String.valueOf(value);
	}
}
