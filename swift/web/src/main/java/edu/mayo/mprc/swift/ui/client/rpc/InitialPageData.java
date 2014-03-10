package edu.mayo.mprc.swift.ui.client.rpc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * @author Roman Zenka
 */
public final class InitialPageData implements Serializable {
	private static final long serialVersionUID = 6769766487310788649L;

	private HashMap<String, String> uiConfiguration;
	private ClientUser[] users;
	private ClientLoadedSearch loadedSearch;
	private ClientParamSetList paramSetList;
	private HashMap<String, List<ClientValue>> allowedValues;
	private boolean databaseUndeployerEnabled;
	private List<ClientSearchEngine> searchEngines;
	private List<SpectrumQaParamFileInfo> spectrumQaParamFileInfo;
	private boolean scaffoldReportEnabled;
	private boolean extractMsnEnabled;
	private boolean msConvertEnabled;

	public InitialPageData() {
	}

	public InitialPageData(final HashMap<String, String> uiConfiguration,
	                       final ClientUser[] users,
	                       final ClientLoadedSearch loadedSearch,
	                       final ClientParamSetList paramSetList,
	                       final HashMap<String, List<ClientValue>> allowedValues,
	                       final boolean databaseUndeployerEnabled,
	                       final List<ClientSearchEngine> searchEngines,
	                       final List<SpectrumQaParamFileInfo> spectrumQaParamFileInfo,
	                       final boolean scaffoldReportEnabled,
	                       final boolean extractMsnEnabled,
	                       final boolean msConvertEnabled) {
		this.uiConfiguration = uiConfiguration;
		this.users = new ClientUser[users.length];
		System.arraycopy(users, 0, this.users, 0, users.length);
		this.loadedSearch = loadedSearch;
		this.paramSetList = paramSetList;
		this.allowedValues = allowedValues;
		this.databaseUndeployerEnabled = databaseUndeployerEnabled;
		this.searchEngines = searchEngines;
		this.spectrumQaParamFileInfo = spectrumQaParamFileInfo;
		this.scaffoldReportEnabled = scaffoldReportEnabled;
		this.extractMsnEnabled = extractMsnEnabled;
		this.msConvertEnabled = msConvertEnabled;
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

	public boolean isExtractMsnEnabled() {
		return extractMsnEnabled;
	}

	public boolean isMsConvertEnabled() {
		return msConvertEnabled;
	}

	public HashMap<String, String> getUiConfiguration() {
		return uiConfiguration;
	}
}
