package edu.mayo.mprc.swift.ui.client.rpc;

import java.io.Serializable;

/**
 * Current setup for msmsEval.
 */
public final class ClientSpectrumQa implements Serializable {
	private static final long serialVersionUID = 20111119L;
	private boolean enabled;
	private String paramFilePath;

	/**
	 * Null Constructor for serialization.
	 */
	public ClientSpectrumQa() {
		enabled = false;
		paramFilePath = null;
	}

	public ClientSpectrumQa(final String paramFilePath) {
		enabled = paramFilePath != null;
		this.paramFilePath = paramFilePath;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public String getParamFilePath() {
		return paramFilePath;
	}
}
