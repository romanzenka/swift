package edu.mayo.mprc.swift.ui.server;

/**
 * @author Roman Zenka
 */
public final class SearchInput {
	private String title;
	private String userEmail;
	private String outputFolderName;
	private int paramSetId;
	private String[] inputFilePaths;
	private String[] biologicalSamples;
	private String[] categoryNames;
	private String[] experiments;
	private String[] enabledEngineCodes;
	private boolean peptideReport;
	private boolean fromScratch;
	private boolean lowPriority;
	private boolean publicMgfFiles;
	private boolean publicSearchFiles;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public String getOutputFolderName() {
		return outputFolderName;
	}

	public void setOutputFolderName(String outputFolderName) {
		this.outputFolderName = outputFolderName;
	}

	public int getParamSetId() {
		return paramSetId;
	}

	public void setParamSetId(int paramSetId) {
		this.paramSetId = paramSetId;
	}

	public String[] getInputFilePaths() {
		return inputFilePaths;
	}

	public void setInputFilePaths(String[] inputFilePaths) {
		this.inputFilePaths = inputFilePaths;
	}

	public String[] getBiologicalSamples() {
		return biologicalSamples;
	}

	public void setBiologicalSamples(String[] biologicalSamples) {
		this.biologicalSamples = biologicalSamples;
	}

	public String[] getCategoryNames() {
		return categoryNames;
	}

	public void setCategoryNames(String[] categoryNames) {
		this.categoryNames = categoryNames;
	}

	public String[] getExperiments() {
		return experiments;
	}

	public void setExperiments(String[] experiments) {
		this.experiments = experiments;
	}

	public String[] getEnabledEngineCodes() {
		return enabledEngineCodes;
	}

	public void setEnabledEngineCodes(String[] enabledEngineCodes) {
		this.enabledEngineCodes = enabledEngineCodes;
	}

	public boolean isPeptideReport() {
		return peptideReport;
	}

	public void setPeptideReport(boolean peptideReport) {
		this.peptideReport = peptideReport;
	}

	public boolean isFromScratch() {
		return fromScratch;
	}

	public void setFromScratch(boolean fromScratch) {
		this.fromScratch = fromScratch;
	}

	public boolean isLowPriority() {
		return lowPriority;
	}

	public void setLowPriority(boolean lowPriority) {
		this.lowPriority = lowPriority;
	}

	public boolean isPublicMgfFiles() {
		return publicMgfFiles;
	}

	public void setPublicMgfFiles(boolean publicMgfFiles) {
		this.publicMgfFiles = publicMgfFiles;
	}

	public boolean isPublicSearchFiles() {
		return publicSearchFiles;
	}

	public void setPublicSearchFiles(boolean publicSearchFiles) {
		this.publicSearchFiles = publicSearchFiles;
	}
}
