package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.swift.db.SearchEngine;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.swift.dbmapping.SearchEngineConfig;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.swift.params2.SearchEngineParameters;
import edu.mayo.mprc.swift.params2.mapping.ParamsInfo;
import edu.mayo.mprc.utilities.FileUtilities;

import java.io.File;
import java.util.*;

/**
 * A collection of all parameters anyone has given to our search engines to process.
 * <p/>
 * For each set of parameters and engine it is capable of producing a file name of file that contains
 * the search engine parameters, serialized to the disk.
 * <p/>
 * A default parameters folder is used for saving these files. The tool makes sure that if same engine
 * needs multiple different parameter sets, the file names will not collide and the files will appear in the order
 * in which the search was requested.
 * <p/>
 * TODO: currently the whole system gets all information in advance and it spews out all config files at once.
 * This needs to change to work on-demand. It would simplify the design and make it more extensible.
 *
 * @author Roman Zenka
 */
public final class SearchEngineParametersCollection {
	private static final String DEFAULT_PARAMS_FOLDER = "params";

	/**
	 * Key: {@link SearchEngineParameters} parameter set
	 * Value: A string uniquely identifying the parameter set.
	 * <p/>
	 * When there is just one parameter set, the string would be "".
	 * When there are more, the string would be '1' for the first parameter set mentioned by first input file, '2' for second and so on.
	 */
	private Map<SearchEngineParameters, String> searchEngineParametersNames;
	/**
	 * Key: search engine:{@link #getSearchEngineParametersName}
	 * Value: parameter file name
	 */
	private Map<String, File> parameterFiles;
	private final boolean qualityControlEnabled;
	private final SearchEngineList searchEngineList;
	private final ParamsInfo paramsInfo;

	public SearchEngineParametersCollection(final boolean qualityControlEnabled, final SearchEngineList searchEngineList, final ParamsInfo paramsInfo) {
		this.qualityControlEnabled = qualityControlEnabled;
		this.searchEngineList = searchEngineList;
		this.paramsInfo = paramsInfo;
	}

	/**
	 * Save parameter files to the disk.
	 */
	public void createParameterFiles(final SwiftSearchDefinition searchDefinition) {
		searchEngineParametersNames = nameSearchEngineParameters(searchDefinition.getInputFiles(), searchDefinition.getSearchParameters());

		// Obtain a set of all search engines that were requested
		// This way we only create config files that we need
		final Set<String> enabledEngines = new HashSet<String>();
		for (final FileSearch fileSearch : searchDefinition.getInputFiles()) {
			if (fileSearch != null) {
				for (final SearchEngineConfig config : fileSearch.getEnabledEngines().getEngineConfigs()) {
					enabledEngines.add(config.getCode());
				}
			}
		}

		addEnginesForQualityControl(enabledEngines);

		final File paramFolder = new File(searchDefinition.getOutputFolder(), DEFAULT_PARAMS_FOLDER);
		FileUtilities.ensureFolderExists(paramFolder);
		parameterFiles = new HashMap<String, File>();
		if (!enabledEngines.isEmpty()) {
			FileUtilities.ensureFolderExists(paramFolder);
			for (final String engineCode : enabledEngines) {
				final SearchEngine engine = searchEngineList.getSearchEngine(engineCode);
				for (final Map.Entry<SearchEngineParameters, String> parameterSet : searchEngineParametersNames.entrySet()) {
					final File file = engine.writeSearchEngineParameterFile(
							paramFolder, parameterSet.getKey(), parameterSet.getValue(), null /*We do not validate, validation should be already done*/, paramsInfo);
					addParamFile(engineCode, parameterSet.getValue(), file);
				}
			}
		}
	}

	public File getParamFile(final SearchEngine engine, final SearchEngineParameters parameters) {
		return parameterFiles.get(getParamFileHash(engine, parameters));
	}

	private String getSearchEngineParametersName(final SearchEngineParameters parameters) {
		return searchEngineParametersNames.get(parameters);
	}

	private void addParamFile(final String engineCode, final String parametersName, final File file) {
		parameterFiles.put(getParamFileHash(engineCode, parametersName), file);
	}

	private String getParamFileHash(final SearchEngine engine, final SearchEngineParameters parameters) {
		return getParamFileHash(engine.getCode(), getSearchEngineParametersName(parameters));
	}

	private static String getParamFileHash(final String engineCode, final String parametersName) {
		return engineCode + ":" + parametersName;
	}

	/**
	 * Enable extra engines that support the Quality Control (MyriMatch and IdpQonvert)
	 *
	 * @param enabledEngines List of currently enabled engines. Will append new ones.
	 */
	private void addEnginesForQualityControl(final Set<String> enabledEngines) {
		if (isQualityControlEnabled()) {
			enabledEngines.add("MYRIMATCH");
			enabledEngines.add("IDPQONVERT");
		}
	}

	/**
	 * Create a map from all used search engine parameters to short names that distinguish them.
	 */
	private static Map<SearchEngineParameters, String> nameSearchEngineParameters(final List<FileSearch> searches, final SearchEngineParameters defaultParameters) {
		final List<SearchEngineParameters> parameters = new ArrayList<SearchEngineParameters>(10);
		final Collection<SearchEngineParameters> seenParameters = new HashSet<SearchEngineParameters>(10);
		for (final FileSearch fileSearch : searches) {
			final SearchEngineParameters searchParameters = fileSearch.getSearchParametersWithDefault(defaultParameters);
			if (!seenParameters.contains(searchParameters)) {
				seenParameters.add(searchParameters);
				parameters.add(searchParameters);
			}
		}
		// Now we have a list of unique search parameters in same order as they appear in files
		final Map<SearchEngineParameters, String> resultMap = new HashMap<SearchEngineParameters, String>(parameters.size());
		if (parameters.size() == 1) {
			resultMap.put(parameters.get(0), "");
		} else {
			for (int i = 0; i < parameters.size(); i++) {
				resultMap.put(parameters.get(i), String.valueOf(i + 1));
			}
		}
		return resultMap;
	}

	private boolean isQualityControlEnabled() {
		return qualityControlEnabled;
	}
}
