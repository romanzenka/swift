package edu.mayo.mprc.swift.ui.client.rpc;

/**
 * A search to be loaded into the client.
 * When loading a search, new temporary parameter sets can be created.
 * If that is the case, the parameter sets are passed along.
 * If the parameter is null, the parameter sets stay unchanged.
 * TODO: This should ideally transfer only the delta, not all param sets.
 */
public final class ClientLoadedSearch implements ClientValue {
	private static final long serialVersionUID = 20101221L;
	private int searchId;
	private ClientSwiftSearchDefinition definition;
	private ClientParamSetList clientParamSetList;

	public ClientLoadedSearch() {
	}

	public ClientLoadedSearch(final int searchId, final ClientSwiftSearchDefinition definition, final ClientParamSetList clientParamSetList) {
		this.searchId =searchId;
		this.definition = definition;
		this.clientParamSetList = clientParamSetList;
	}

	public int getSearchId() {
		return searchId;
	}

	public ClientSwiftSearchDefinition getDefinition() {
		return definition;
	}

	public ClientParamSetList getClientParamSetList() {
		return clientParamSetList;
	}
}
