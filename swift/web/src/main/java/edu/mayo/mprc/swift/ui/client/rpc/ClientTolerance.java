package edu.mayo.mprc.swift.ui.client.rpc;

import edu.mayo.mprc.swift.params2.Tolerance;

/**
 * Proxy for {@link Tolerance}.
 * <p/>
 * Yeah, so GWT's regex support sucks, so we'll do all parsing server side.
 */
public final class ClientTolerance implements ClientValue {
	private static final long serialVersionUID = 20101221L;
	private String value;

	public ClientTolerance() {
		value = "UNSET";
	}

	public ClientTolerance(final String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(final String value) {
		this.value = value;
	}
}
