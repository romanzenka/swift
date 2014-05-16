package edu.mayo.mprc.swift.ui.client.rpc;

import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Ui version of {@link SwiftSearchDefinition}.
 */
public final class ClientSwiftSearchDefinition implements Serializable {
	private static final long serialVersionUID = 20111129L;
	private String searchTitle;
	private ClientUser user;
	/**
	 * Path to output folder. Relative to browse root.
	 */
	private String outputFolder;
	private ClientSpectrumQa clientSpectrumQa;
	private ClientPeptideReport clientPeptideReport;
	private ClientParamSet paramSet;
	private boolean publicMgfFiles;
	private boolean publicMzxmlFiles;
	private boolean publicSearchFiles;

	private boolean fromScratch; // Rerun everything from scratch, no caching

	private ArrayList<ClientSearchEngineConfig> enabledEngines;
	private List<ClientFileSearch> inputFiles;

	// Search run id in case this search was previously run
	private int previousSearchRunId;
	private boolean lowPriority;

	// Generic metadata - can be used for extending the search information
	private HashMap<String, String> metadata;

	public ClientSwiftSearchDefinition() {
	}

	/**
	 * Creates search definition object.
	 *
	 * @param searchTitle       Title of the search.
	 * @param user              User.
	 * @param outputFolder      Output folder. Relative to browse root - the folder that is root for the UI.
	 *                          Example "/instruments/search1234"
	 * @param enabledEngines    Enabled engines
	 * @param inputFiles        Table of files to be searched + information about how to search them.
	 * @param spectrumQa        Parameters for spectrum QA.
	 * @param peptideReport     Parameters for peptide report
	 * @param publicMgfFiles    True if the mgf files are to be published.
	 * @param publicMzxmlFiles  True if the mzxml files are to be published.
	 * @param publicSearchFiles True if the intermediate search results are to be published.
	 * @param metadata          Metadata associated with this search (can be used for filtering)
	 */
	public ClientSwiftSearchDefinition(final String searchTitle, final ClientUser user, final String outputFolder,
	                                   final ClientParamSet paramSet,
	                                   final ArrayList<ClientSearchEngineConfig> enabledEngines,
	                                   final List<ClientFileSearch> inputFiles,
	                                   final ClientSpectrumQa spectrumQa, final ClientPeptideReport peptideReport,
	                                   final boolean publicMgfFiles, final boolean publicMzxmlFiles, final boolean publicSearchFiles,
	                                   final HashMap<String, String> metadata,
	                                   final int previousSearchRunId) {
		this.searchTitle = searchTitle;
		this.user = user;
		this.outputFolder = outputFolder;
		this.paramSet = paramSet;
		this.enabledEngines = enabledEngines;
		this.inputFiles = inputFiles;
		clientSpectrumQa = spectrumQa;
		clientPeptideReport = peptideReport;
		this.publicMgfFiles = publicMgfFiles;
		this.publicMzxmlFiles = publicMzxmlFiles;
		this.publicSearchFiles = publicSearchFiles;
		this.metadata = metadata;
		this.previousSearchRunId = previousSearchRunId;
		fromScratch = false;
	}

	public String getSearchTitle() {
		return searchTitle;
	}

	public ClientUser getUser() {
		return user;
	}

	public String getOutputFolder() {
		return outputFolder;
	}

	public ArrayList<ClientSearchEngineConfig> getEnabledEngines() {
		return enabledEngines;
	}

	public List<ClientFileSearch> getInputFiles() {
		return inputFiles;
	}

	public ClientSpectrumQa getSpectrumQa() {
		return clientSpectrumQa;
	}

	public ClientPeptideReport getPeptideReport() {
		return clientPeptideReport;
	}

	public int getPreviousSearchRunId() {
		return previousSearchRunId;
	}

	public ClientParamSet getParamSet() {
		return paramSet;
	}

	public boolean isPublicMgfFiles() {
		return publicMgfFiles;
	}

	public boolean isPublicMzxmlFiles() {
		return publicMzxmlFiles;
	}

	public boolean isPublicSearchFiles() {
		return publicSearchFiles;
	}

	public boolean isFromScratch() {
		return fromScratch;
	}

	public void setFromScratch(final boolean fromScratch) {
		this.fromScratch = fromScratch;
	}

	public boolean isLowPriority() {
		return lowPriority;
	}

	public void setLowPriority(final boolean lowPriority) {
		this.lowPriority = lowPriority;
	}

	public HashMap<String, String> getMetadata() {
		return metadata;
	}
}

