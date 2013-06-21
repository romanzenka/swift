package edu.mayo.mprc.swift.ui.server;

import org.springframework.util.MultiValueMap;

import java.util.List;

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
	private String[] enabledEngineVersions;
	private int[] paramSetIds;
	private boolean peptideReport;
	private boolean fromScratch;
	private boolean lowPriority;
	private boolean publicMgfFiles;
	private boolean publicSearchFiles;

	public SearchInput(final MultiValueMap<String, String> searchInputMap) {
		title = searchInputMap.getFirst("title");
		userEmail = searchInputMap.getFirst("userEmail");
		outputFolderName = searchInputMap.getFirst("outputFolderName");
		paramSetId = Integer.parseInt(searchInputMap.getFirst("paramSetId"));
		inputFilePaths = getStringArray(searchInputMap, "inputFilePaths");
		biologicalSamples = getStringArray(searchInputMap, "biologicalSamples");
		categoryNames = getStringArray(searchInputMap, "categoryNames");
		experiments = getStringArray(searchInputMap, "experiments");
		enabledEngineCodes = getStringArray(searchInputMap, "enabledEngineCodes");
		enabledEngineVersions = getStringArray(searchInputMap, "enabledEngineVersions");
		if (searchInputMap.get("paramSetIds") != null) {
			// If set, use the values
			paramSetIds = getIntArray(searchInputMap, "paramSetIds");
		} else {
			// If not, replicate the global search id over and over
			paramSetIds = new int[inputFilePaths.length];
			for (int i = 0; i < inputFilePaths.length; i++) {
				paramSetIds[i] = paramSetId;
			}
		}
		peptideReport = isTrue(searchInputMap, "peptideReport");
		fromScratch = isTrue(searchInputMap, "fromScratch");
		lowPriority = isTrue(searchInputMap, "lowPriority");
		publicMgfFiles = isTrue(searchInputMap, "publicMgfFiles");
		publicSearchFiles = isTrue(searchInputMap, "publicSearchFiles");
	}

	private static boolean isTrue(final MultiValueMap<String, String> searchInputMap, final String key) {
		return Boolean.parseBoolean(searchInputMap.getFirst(key));
	}

	private static String[] getStringArray(final MultiValueMap<String, String> searchInputMap, final String key) {
		final List<String> strings = searchInputMap.get(key);
		final String[] result = new String[strings.size()];
		strings.toArray(result);
		return result;
	}

	private static int[] getIntArray(final MultiValueMap<String, String> searchInputMap, final String key) {
		final List<String> strings = searchInputMap.get(key);
		final int[] result = new int[strings.size()];
		int i = 0;
		for (final String value : strings) {
			result[i] = Integer.parseInt(value);
			i++;
		}
		return result;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(final String userEmail) {
		this.userEmail = userEmail;
	}

	public String getOutputFolderName() {
		return outputFolderName;
	}

	public void setOutputFolderName(final String outputFolderName) {
		this.outputFolderName = outputFolderName;
	}

	public int getParamSetId() {
		return paramSetId;
	}

	public void setParamSetId(final int paramSetId) {
		this.paramSetId = paramSetId;
	}

	public int[] getParamSetIds() {
		return paramSetIds;
	}

	public void setParamSetIds(int[] paramSetIds) {
		this.paramSetIds = paramSetIds;
	}

	public String[] getInputFilePaths() {
		return inputFilePaths;
	}

	public void setInputFilePaths(final String[] inputFilePaths) {
		this.inputFilePaths = inputFilePaths;
	}

	public String[] getBiologicalSamples() {
		return biologicalSamples;
	}

	public void setBiologicalSamples(final String[] biologicalSamples) {
		this.biologicalSamples = biologicalSamples;
	}

	public String[] getCategoryNames() {
		return categoryNames;
	}

	public void setCategoryNames(final String[] categoryNames) {
		this.categoryNames = categoryNames;
	}

	public String[] getExperiments() {
		return experiments;
	}

	public void setExperiments(final String[] experiments) {
		this.experiments = experiments;
	}

	public String[] getEnabledEngineCodes() {
		return enabledEngineCodes;
	}

	public void setEnabledEngineCodes(final String[] enabledEngineCodes) {
		this.enabledEngineCodes = enabledEngineCodes;
	}

	public String[] getEnabledEngineVersions() {
		return enabledEngineVersions;
	}

	public void setEnabledEngineVersions(final String[] enabledEngineVersions) {
		this.enabledEngineVersions = enabledEngineVersions;
	}

	public boolean isPeptideReport() {
		return peptideReport;
	}

	public void setPeptideReport(final boolean peptideReport) {
		this.peptideReport = peptideReport;
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

	public boolean isPublicMgfFiles() {
		return publicMgfFiles;
	}

	public void setPublicMgfFiles(final boolean publicMgfFiles) {
		this.publicMgfFiles = publicMgfFiles;
	}

	public boolean isPublicSearchFiles() {
		return publicSearchFiles;
	}

	public void setPublicSearchFiles(final boolean publicSearchFiles) {
		this.publicSearchFiles = publicSearchFiles;
	}
}
