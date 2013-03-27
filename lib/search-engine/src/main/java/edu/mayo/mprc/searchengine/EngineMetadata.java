package edu.mayo.mprc.searchengine;

import edu.mayo.mprc.swift.params2.mapping.MappingFactory;

/**
 * @author Roman Zenka
 */
public final class EngineMetadata {
	private final String code;
	private final String resultExtension;
	private final String friendlyName;
	private final boolean onByDefault;
	private final String outputDirName;
	private final MappingFactory mappingFactory;
	private final boolean aggregator;

	private final String[] workerTypes;
	private final String[] cacheTypes;
	private final String[] deployerTypes;
	private final int order;

	/**
	 * @param code            Search engine code. All uppercase, one word, simple. Will be stored in the database, Swift uses it to determine the type of the engine.
	 * @param resultExtension The file extension produced by this engine. Will be used to create the name of the output file.
	 * @param friendlyName    Name to display to the user.
	 * @param onByDefault     Whether this engine should be enabled by default in the ui.
	 * @param outputDirName   Name of the folder to put search engine outputs into.
	 * @param mappingFactory  A factory that can create {@link edu.mayo.mprc.swift.params2.mapping.Mappings} to map input parameters to the search engine parameters.
	 * @param workerTypes     An array of types (registered with {@link edu.mayo.mprc.config.ResourceFactory} that can serve as workers for this engine.
	 * @param cacheTypes      An array of types (registered with {@link edu.mayo.mprc.config.ResourceFactory} that can serve to cache results for this engine.
	 * @param deployerTypes   An array of types (registered with {@link edu.mayo.mprc.config.ResourceFactory} that can serve to prepare the environment for the searches to run optimally.
	 * @param order           Order in which is the search engine presented to the user (less will be first)
	 * @param aggregator      True if this engine aggregates output of other engines (e.g. Scaffold, IDPicker)
	 */
	public EngineMetadata(
			final String code, final String resultExtension,
			final String friendlyName, final boolean onByDefault, final String outputDirName,
			final MappingFactory mappingFactory,
			final String[] workerTypes,
			final String[] cacheTypes,
			final String[] deployerTypes,
			final int order,
			final boolean aggregator) {
		this.code = code;
		this.resultExtension = resultExtension;
		this.friendlyName = friendlyName;
		this.onByDefault = onByDefault;
		this.outputDirName = outputDirName;
		this.mappingFactory = mappingFactory;
		this.workerTypes = workerTypes;
		this.cacheTypes = cacheTypes;
		this.deployerTypes = deployerTypes;
		this.order = order;
		this.aggregator = aggregator;
	}

	public String getCode() {
		return code;
	}

	public String getResultExtension() {
		return resultExtension;
	}

	public String getFriendlyName() {
		return friendlyName;
	}

	public boolean isOnByDefault() {
		return onByDefault;
	}

	public String getOutputDirName() {
		return outputDirName;
	}

	public MappingFactory getMappingFactory() {
		return mappingFactory;
	}

	public String[] getWorkerTypes() {
		return workerTypes;
	}

	public String[] getCacheTypes() {
		return cacheTypes;
	}

	public String[] getDeployerTypes() {
		return deployerTypes;
	}

	public int getOrder() {
		return order;
	}

	public boolean isAggregator() {
		return aggregator;
	}
}
