package edu.mayo.mprc.swift.ui.client.rpc;

import edu.mayo.mprc.MprcException;

import java.io.Serializable;

/**
 * A description of a particular configuration of a search engine - knows the type of the engine
 * (code) and the specific version.
 *
 * @author Roman Zenka
 */
public final class ClientSearchEngineConfig implements Serializable {
	private static final long serialVersionUID = 5331471105916548098L;

	private String code;
	private String version;

	public ClientSearchEngineConfig() {
	}

	public ClientSearchEngineConfig(final String code, final String version) {
		setCode(code);
		setVersion(version);
	}

	public String getCode() {
		return code;
	}

	public void setCode(final String code) {
		if (code == null) {
			throw new MprcException("Engine code must not be null");
		}
		this.code = code;
	}

	public String getVersion() {
		if (code == null) {
			throw new MprcException("Engine version must not be null");
		}
		return version;
	}

	public void setVersion(final String version) {
		this.version = version;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (!(o instanceof ClientSearchEngineConfig)) return false;

		final ClientSearchEngineConfig that = (ClientSearchEngineConfig) o;

		if (!code.equals(that.code)) return false;
		if (!version.equals(that.version)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = code.hashCode();
		result = 31 * result + version.hashCode();
		return result;
	}
}
