package edu.mayo.mprc.swift.ui.client.rpc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * @author Roman Zenka
 */
public final class InitialPageData implements Serializable {
	private static final long serialVersionUID = 6769766487310788649L;

	private ClientUser[] users;
	private ClientLoadedSearch loadedSearch;
	private String userMessage;
	private ClientParamSetList paramSetList;
	private HashMap<String, List<ClientValue>> allowedValues;
	private boolean databaseUndeployerEnabled;
	private List<ClientSearchEngine> searchEngines;
	private List<SpectrumQaParamFileInfo> spectrumQaParamFileInfo;
	private boolean scaffoldReportEnabled;

	public InitialPageData() {
	}

	public InitialPageData(ClientUser[] users,
	                       ClientLoadedSearch loadedSearch,
	                       String userMessage,
	                       ClientParamSetList paramSetList,
	                       HashMap<String, List<ClientValue>> allowedValues,
	                       boolean databaseUndeployerEnabled,
	                       List<ClientSearchEngine> searchEngines,
	                       List<SpectrumQaParamFileInfo> spectrumQaParamFileInfo,
	                       boolean scaffoldReportEnabled) {
		this.users = new ClientUser[users.length];
		System.arraycopy(users, 0, this.users, 0, users.length);
		this.loadedSearch = loadedSearch;
		this.userMessage = userMessage;
		this.paramSetList = paramSetList;
		this.allowedValues = allowedValues;
		this.databaseUndeployerEnabled = databaseUndeployerEnabled;
		this.searchEngines = searchEngines;
		this.spectrumQaParamFileInfo = spectrumQaParamFileInfo;
		this.scaffoldReportEnabled = scaffoldReportEnabled;
	}

	public List<ClientSearchEngine> getSearchEngines() {
		return searchEngines;
	}

	public ClientUser[] listUsers() {
		return users;
	}

	public ClientLoadedSearch loadedSearch() {
		return loadedSearch;
	}

	public String getUserMessage() {
		return userMessage;
	}

	public ClientParamSetList getParamSetList() {
		return paramSetList;
	}

	public HashMap<String, List<ClientValue>> getAllowedValues() {
		return allowedValues;
	}

	public boolean isDatabaseUndeployerEnabled() {
		return databaseUndeployerEnabled;
	}

	public List<SpectrumQaParamFileInfo> getSpectrumQaParamFileInfo() {
		return spectrumQaParamFileInfo;
	}

	public boolean isScaffoldReportEnabled() {
		return scaffoldReportEnabled;
	}
}
