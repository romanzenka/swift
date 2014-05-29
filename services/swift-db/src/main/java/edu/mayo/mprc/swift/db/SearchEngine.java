package edu.mayo.mprc.swift.db;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.FactoryDescriptor;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.searchengine.EngineMetadata;
import edu.mayo.mprc.swift.params2.ParamName;
import edu.mayo.mprc.swift.params2.SearchEngineConfig;
import edu.mayo.mprc.swift.params2.SearchEngineParameters;
import edu.mayo.mprc.swift.params2.mapping.*;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Collection;

/**
 * Object representing a configured search engine.
 * <p/>
 * Can be used to run searches, deploy and undeploy databases,
 * and map search engine parameters to engine-specific format (both directions).
 * <p/>
 * Since the object is very heavy-weight, it should not be stored and transferred as is. Instead use the {@link #getCode()} method
 * and {@link #getForId}.
 */
public final class SearchEngine implements Comparable<SearchEngine> {
	private EngineMetadata engineMetadata;
	private Config config;

	private DaemonConnection searchDaemon;
	private DaemonConnection dbDeployDaemon;

	public SearchEngine() {
	}

	public SearchEngine(final EngineMetadata engineMetadata, final Config config, final DaemonConnection searchDaemon, final DaemonConnection dbDeployDaemon) {
		if (engineMetadata == null) {
			throw new MprcException("The search engine must have metadata always defined. This is a programmer error.");
		}
		this.engineMetadata = engineMetadata;
		this.config = config;
		final String version = config.getVersion();
		this.searchDaemon = searchDaemon;
		this.dbDeployDaemon = dbDeployDaemon;
		if (searchDaemon == null) {
			throw new MprcException("Configuration error: Engine " + engineMetadata.getCode() + " version " + version + " must have the worker daemon defined in order to function");
		}
	}

	/**
	 * Get an engine for given engine code and version.
	 *
	 * @param code    Code of the engine (e.g. MASCOT)
	 * @param ver     Specific version of the engine.
	 * @param engines List of all engines we know about.
	 * @return Configured engine ready to have commands sent to it.
	 */
	public static SearchEngine getForId(final String code, final String ver, final Collection<SearchEngine> engines) {
		for (final SearchEngine engine : engines) {
			if (engine.getCode().equalsIgnoreCase(code) && engine.getVersion().equalsIgnoreCase(ver)) {
				return engine;
			}
		}
		return null;
	}

	/**
	 * Validate a list of parameters against a list of search engines.
	 *
	 * @param parameters Parameters to validate.
	 * @param engines    List of engines that has to have valid parameter mappings.
	 * @return Object with a list of validations for each parameter
	 */
	public static ParamsValidations validate(final SearchEngineParameters parameters, final Collection<SearchEngine> engines, final ParamsInfo paramsInfo) {
		final ParamsValidations validations = new ParamsValidations();
		for (final SearchEngine engine : engines) {
			engine.validate(parameters, validations, paramsInfo);
		}
		return validations;
	}

	/**
	 * Return a file to store the parameters into
	 *
	 * @param folder             Folder where the file should go
	 * @param distinguishingName A name to distinguish the particular parameter set from others from the same engine
	 * @return File to save the parameters to.
	 */
	public File getParameterFile(final File folder, final String distinguishingName) {
		return new File(folder, getMappingFactory().getCanonicalParamFileName(distinguishingName));
	}

	/**
	 * No writing of parameters, only the validations object gets filled.
	 */
	public void validate(final SearchEngineParameters parameters, final ParamsValidations validations, final ParamsInfo paramsInfo) {
		if (getMappingFactory() == null) {
			// This engine does not support mapping (e.g. Scaffold).
			return;
		}

		parametersToString(parameters, validations, paramsInfo);
	}

	public String parametersToString(final SearchEngineParameters params, ParamsValidations validations, final ParamsInfo paramsInfo) {
		if (getMappingFactory() == null) {
			// This engine does not support mapping (e.g. Scaffold).
			return null;
		}

		if (validations == null) {
			validations = new ParamsValidations();
		}
		final ParamValidationsMappingContext context = new ParamValidationsMappingContext(validations, paramsInfo);

		// Initialize the mappings object
		final Mappings mapping = getMappingFactory().createMapping();
		final Reader baseSettings = mapping.baseSettings();
		mapping.read(baseSettings);
		FileUtilities.closeQuietly(baseSettings);

		// Map each parameter
		context.startMapping(ParamName.PeptideTolerance);
		mapping.setPeptideTolerance(context, params.getPeptideTolerance());

		context.startMapping(ParamName.FragmentTolerance);
		mapping.setFragmentTolerance(context, params.getFragmentTolerance());

		context.startMapping(ParamName.MinTerminiCleavages);
		mapping.setMinTerminiCleavages(context, params.getMinTerminiCleavages());

		context.startMapping(ParamName.MissedCleavages);
		mapping.setMissedCleavages(context, params.getMissedCleavages());

		context.startMapping(ParamName.Database);
		mapping.setSequenceDatabase(context, params.getDatabase().getShortName());

		context.startMapping(ParamName.Enzyme);
		mapping.setProtease(context, params.getProtease());

		context.startMapping(ParamName.FixedMods);
		mapping.setFixedMods(context, params.getFixedModifications());

		context.startMapping(ParamName.VariableMods);
		mapping.setVariableMods(context, params.getVariableModifications());

		context.startMapping(ParamName.Instrument);
		mapping.setInstrument(context, params.getInstrument());

		// Final validity check
		mapping.checkValidity(context);

		if (!context.noErrors()) {
			// Errors detected with this parameter set.
			throw new MprcException("Search engine parameters have following errors:\n" + validations.toString(ValidationSeverity.ERROR));
		}

		final StringWriter writer = new StringWriter(100);
		mapping.write(mapping.baseSettings(), writer);
		FileUtilities.closeQuietly(writer);
		return writer.toString();
	}

	public EngineMetadata getEngineMetadata() {
		return engineMetadata;
	}

	public void setEngineMetadata(final EngineMetadata engineMetadata) {
		this.engineMetadata = engineMetadata;
	}

	public String getVersion() {
		return config.getVersion();
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(final Config config) {
		this.config = config;
	}

	/**
	 * @return Engine code (e.g. MASCOT). Same as {@link edu.mayo.mprc.swift.params2.SearchEngineConfig#code}
	 */
	public String getCode() {
		if (getEngineMetadata().getCode() != null) {
			return getEngineMetadata().getCode();
		} else {
			throw new MprcException("Unknown search engine code");
		}
	}

	/**
	 * @return User-friendly name for the engine.
	 */
	public String getFriendlyName() {
		return getEngineMetadata().getFriendlyName();
	}

	/**
	 * @return File extension of the resulting files this engine produces.
	 */
	public String getResultExtension() {
		return getEngineMetadata().getResultExtension();
	}

	public DaemonConnection getSearchDaemon() {
		return searchDaemon;
	}

	public void setSearchDaemon(final DaemonConnection searchDaemon) {
		this.searchDaemon = searchDaemon;
	}

	public MappingFactory getMappingFactory() {
		return getEngineMetadata().getMappingFactory();
	}

	public DaemonConnection getDbDeployDaemon() {
		return dbDeployDaemon;
	}

	public void setDbDeployDaemon(final DaemonConnection dbDeployDaemon) {
		this.dbDeployDaemon = dbDeployDaemon;
	}

	/**
	 * @return True, if the user interface should offer this search engine to be enabled by default.
	 */
	public boolean isOnByDefault() {
		return getEngineMetadata().isOnByDefault();
	}

	public String getOutputDirName() {
		return getEngineMetadata().getOutputDirName();
	}

	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SearchEngine)) {
			return false;
		}

		final SearchEngine that = (SearchEngine) o;

		if (!getCode().equals(that.getCode())) {
			return false;
		}
		if (!getVersion().equals(that.getVersion())) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return getCode().hashCode()
				+ 31 * getVersion().hashCode();
	}

	@Override
	public int compareTo(final SearchEngine o) {
		final int comparison = getCode().compareTo(o.getCode());
		return comparison == 0 ? getVersion().compareTo(o.getVersion()) : comparison;
	}

	public SearchEngineConfig getEngineConfig() {
		return new SearchEngineConfig(getCode(), getVersion());
	}

	public boolean matchesConfig(final SearchEngineConfig config) {
		return getCode().equals(config.getCode()) && getVersion().equals(config.getVersion());
	}

	@Component("searchEngineFactory")
	public static final class Factory extends FactoryBase<Config, SearchEngine> implements FactoryDescriptor {
		EngineFactoriesList engineFactoriesList;

		@Override
		public SearchEngine create(final Config config, final DependencyResolver dependencies) {
			final EngineMetadata metadata = getEngineFactoriesList().getEngineMetadataForCode(config.getCode());
			if (metadata == null) {
				throw new MprcException("Could not find engine for code [" + config.getCode() + "]");
			}
			DaemonConnection dbDeployer = null;
			if (config.getDeployer() != null) {
				dbDeployer = (DaemonConnection) dependencies.createSingleton(config.getDeployer());
			} else {
				if (metadata.getDeployerTypes().length > 0) {
					// We are expected to have a deployer. Cannot create functional engine.
					throw new MprcException("Cannot create search engine " + config.getCode() + " - database deployer not configured!");
				}
			}
			if (config.getWorker() == null) {
				// We are expected to have a worker. Cannot create functional engine.
				throw new MprcException("Cannot create search engine " + config.getCode() + " - worker not configured!");
			}
			return new SearchEngine(
					metadata,
					config,
					(DaemonConnection) dependencies.createSingleton(config.getWorker()),
					dbDeployer);
		}

		@Override
		public String getType() {
			return "searchEngine";
		}

		@Override
		public String getUserName() {
			return "Search Engine Reference";
		}

		@Override
		public String getDescription() {
			return "A reference to fully configured search engine";
		}

		@Override
		public Class<? extends ResourceConfig> getConfigClass() {
			return Config.class;
		}

		@Override
		public ServiceUiFactory getServiceUiFactory() {
			return null;
		}

		public EngineFactoriesList getEngineFactoriesList() {
			return engineFactoriesList;
		}

		@Resource(name = "engineFactoriesList")
		public void setEngineFactoriesList(final EngineFactoriesList engineFactoriesList) {
			this.engineFactoriesList = engineFactoriesList;
		}
	}

	/**
	 * Configuration of a particular engine.
	 */
	public static final class Config implements ResourceConfig {
		private String code;
		private String version;
		private ServiceConfig worker;
		private ServiceConfig deployer;

		public Config(final String code, final String version, final ServiceConfig worker, final ServiceConfig deployer) {
			this.code = code;
			this.version = version;
			this.worker = worker;
			this.deployer = deployer;
		}

		public String getCode() {
			return code;
		}

		public void setCode(final String code) {
			this.code = code;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(final String version) {
			this.version = version;
		}

		public ServiceConfig getWorker() {
			return worker;
		}

		public void setWorker(final ServiceConfig worker) {
			this.worker = worker;
		}

		public ServiceConfig getDeployer() {
			return deployer;
		}

		public void setDeployer(final ServiceConfig deployer) {
			this.deployer = deployer;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put("code", getCode());
			writer.put("version", getVersion());
			writer.put("worker", writer.save(getWorker()));
			writer.put("deployer", writer.save(getDeployer()));
		}

		@Override
		public void load(final ConfigReader reader) {
			setCode(reader.get("code"));
			setVersion(reader.get("version"));
			setWorker((ServiceConfig) reader.getObject("worker"));
			setDeployer((ServiceConfig) reader.getObject("deployer"));
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}
}
