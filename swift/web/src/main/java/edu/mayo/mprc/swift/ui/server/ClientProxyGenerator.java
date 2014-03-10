package edu.mayo.mprc.swift.ui.server;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.*;
import edu.mayo.mprc.swift.params2.*;
import edu.mayo.mprc.swift.params2.mapping.ParamsInfo;
import edu.mayo.mprc.swift.params2.mapping.ParamsValidations;
import edu.mayo.mprc.swift.params2.mapping.Validation;
import edu.mayo.mprc.swift.params2.mapping.ValidationList;
import edu.mayo.mprc.swift.ui.client.rpc.*;
import edu.mayo.mprc.unimod.ModSet;
import edu.mayo.mprc.unimod.ModSpecificity;
import edu.mayo.mprc.unimod.Unimod;
import edu.mayo.mprc.unimod.UnimodDao;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.workspace.User;
import edu.mayo.mprc.workspace.WorkspaceDao;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Creates instances of classes under {@link edu.mayo.mprc.swift.ui.client.rpc}
 * that wrap classes in {@link edu.mayo.mprc.swift.params2}.
 * <p/>
 * This royally sucks.
 */
public final class ClientProxyGenerator {
	private UnimodDao unimodDao;
	private WorkspaceDao workspaceDao;
	private Unimod cachedUnimod;
	private File browseRoot;

	public ClientProxyGenerator(final UnimodDao unimodDao, final WorkspaceDao workspaceDao, final SwiftDao swiftDao, final File browseRoot) {
		this.unimodDao = unimodDao;
		this.workspaceDao = workspaceDao;
		this.browseRoot = browseRoot;
	}

	/**
	 * @return a list of client parameter sets (both the saved ones and the temporary ones).
	 */
	public static ClientParamSetList getClientParamSetList(final List<SavedSearchEngineParameters> savedSets, final List<ClientParamSet> temporarySets) {
		final List<ClientParamSet> list = new ArrayList<ClientParamSet>(savedSets.size() + temporarySets.size());
		for (final SavedSearchEngineParameters params : savedSets) {
			list.add(new ClientParamSet(params.getId(), params.getName(), params.getUser().getUserName(), params.getUser().getInitials()));
		}
		for (final ClientParamSet temporarySet : temporarySets) {
			list.add(temporarySet);
		}
		return new ClientParamSetList(list);
	}

	public ClientParamSetValues convertValues(final SearchEngineParameters ps, final ParamsValidations validations) {
		final List<ClientParam> values = new ArrayList<ClientParam>(ParamName.values().length);
		for (final ParamName paramName : ParamName.values()) {
			values.add(new ClientParam(paramName.getId(), convert(ps.getValue(paramName)), convertTo(validations.getValidationFor(paramName))));
		}
		return new ClientParamSetValues(values);
	}

	public static final class ConversionException extends MprcException {
		private static final long serialVersionUID = 20071220L;

		public ConversionException(final String message) {
			super(message);
		}

		public ConversionException(final Throwable throwable) {
			super(throwable);
		}

		public ConversionException() {
		}
	}

	public ClientParamsValidations convertTo(final ParamsValidations validations) {
		final Map<String, ClientValidationList> map = new HashMap<String, ClientValidationList>();
		for (final Map.Entry<ParamName, ValidationList> entry : validations.getValidationMap().entrySet()) {
			map.put(entry.getKey().getId(), convertTo(entry.getValue()));
		}
		return new ClientParamsValidations(map);
	}

	public ClientUser convertTo(final User user) {
		return new ClientUser(user.getUserName(), user.getFirstName() + " " + user.getLastName(), user.getInitials(), user.isParameterEditorEnabled(), user.isOutputPathChangeEnabled());
	}

	public ClientValidation convertTo(final Validation v) {
		final ClientValidation cv = new ClientValidation();
		cv.setMessage(v.getMessage());
		cv.setSeverity(v.getSeverity().getRank());
		cv.setParamId(v.getParam().getId());
		if (v.getValue() != null) {
			cv.setValue(convert(v.getValue()));
		}
		if (v.getThrowable() != null) {
			final Throwable t = v.getThrowable();
			cv.setThrowableMessage(t.getMessage());
			cv.setThrowableStackTrace(MprcException.getDetailedMessage(t));
		}
		return cv;
	}

	public ClientValidationList convertTo(final ValidationList l) {
		final ClientValidationList list = new ClientValidationList(l.size());
		for (final Validation v : l) {
			list.add(convertTo(v));
		}
		return list;
	}

	List<ClientValue> getAllowedValues(final ParamName p, final ParamsInfo paramsInfo) {
		List<ClientValue> allowedValues = null;
		final Iterable<?> vals = paramsInfo.getAllowedValues(p);
		if (vals != null) {
			allowedValues = new ArrayList<ClientValue>();
			for (final Object oo : vals) {
				allowedValues.add(convert(oo));
			}
		}
		return allowedValues;
	}

	public ClientTolerance convertTo(final Tolerance du) {
		return new ClientTolerance(du.getValue() + " " + du.getUnit().getCode());
	}

	public Tolerance convertFrom(final ClientTolerance du) {
		return new Tolerance(du.getValue());
	}

	public ClientSequenceDatabase convertTo(final Curation val) {
		return new ClientSequenceDatabase(val.getId(), val.getTitle(), val.getShortName(), val.getOwnerEmail());
	}

	public ClientInstrument convertTo(final Instrument val) {
		return new ClientInstrument(val.getName());
	}

	public Curation convertFrom(final ClientSequenceDatabase val, final Iterable<Curation> allowedValues) {
		for (final Curation db : allowedValues) {
			if (val.getShortName().equals(db.getShortName())) {
				return db;
			}
		}
		throw new MprcException("No such database " + val.getShortName());
	}

	public Instrument convertFrom(final ClientInstrument val, final Iterable<Instrument> allowedValues) {
		for (final Instrument inst : allowedValues) {
			if (val.getName().equals(inst.getName())) {
				return inst;
			}
		}
		throw new MprcException("No such instrument " + val.getName());
	}

	public ModSet convertFrom(final ClientModSpecificitySet val) {
		final ModSet modSpecSet = new ModSet();
		final List<ClientModSpecificity> modSpecs = val.getModSpecificities();

		for (final ClientModSpecificity modSpec : modSpecs) {
			if (cachedUnimod == null) {
				cachedUnimod = unimodDao.load();
			}
			final List<ModSpecificity> modspec = cachedUnimod.getSpecificitiesByMascotName(modSpec.getName());
			modSpecSet.addAll(modspec);
		}
		return modSpecSet;
	}

	public SearchEngineConfig convertFrom(final ClientSearchEngineConfig config) {
		return new SearchEngineConfig(config.getCode(), config.getVersion());
	}

	public EnabledEngines convertFrom(final Iterable<ClientSearchEngineConfig> configs) {
		final EnabledEngines enabledEngines = new EnabledEngines();
		for (final ClientSearchEngineConfig engineInstance : configs) {
			if (engineInstance != null) {
				enabledEngines.add(convertFrom(engineInstance));
			}
		}
		return enabledEngines;
	}

	public FileSearch convertFrom(final ClientFileSearch entry) {
		return new FileSearch(
				new File(browseRoot, entry.getPath()),
				entry.getBiologicalSample().trim(),
				entry.getCategoryName().trim(),
				entry.getExperiment().trim(),
				convertFrom(entry.getEnabledEngines()),
				null);
	}

	/**
	 * All files the user can enter in the web interface are relative to browse root.
	 *
	 * @param file File to convert to relative path.
	 * @return Path to the file, relative to browse root. The path uses forward slashes, irregardless of the OS.
	 */
	private String pathRelativeToBrowseRoot(final File file) {
		final String relativePath = FileUtilities.getRelativePath(browseRoot.getAbsolutePath(), file.getAbsolutePath());
		if (!"/".equals(File.separator)) {
			return relativePath.replaceAll(Pattern.quote(File.separator), "/");
		}
		return relativePath;
	}

	public ClientSearchEngineConfig convertTo(final SearchEngineConfig config) {
		return new ClientSearchEngineConfig(config.getCode(), config.getVersion());
	}

	public ArrayList<ClientSearchEngineConfig> convertTo(final EnabledEngines enabledEngines) {
		final ArrayList<ClientSearchEngineConfig> searchEngineConfigs = new ArrayList<ClientSearchEngineConfig>(enabledEngines.getEngineConfigs().size());
		for (final SearchEngineConfig config : enabledEngines.getEngineConfigs()) {
			searchEngineConfigs.add(convertTo(config));
		}
		return searchEngineConfigs;
	}

	public ClientSwiftSearchDefinition convertTo(final SwiftSearchDefinition definition, final ClientParamSetResolver resolver) {
		final String relativeOutputFolderPath = pathRelativeToBrowseRoot(definition.getOutputFolder());

		final List<FileSearch> files = definition.getInputFiles();
		final List<ClientFileSearch> clientInputFiles = new ArrayList<ClientFileSearch>(files.size());

		for (final FileSearch fileSearch : files) {
			final File inputFile = fileSearch.getInputFile();
			Long inputFileSize = null;
			if (inputFile.exists() && inputFile.isFile()) {
				inputFileSize = inputFile.length();
			} else if (FileSearchBean.isAgilentDir(inputFile)) {
				inputFileSize = FileUtilities.sizeOfDirectory(inputFile);
			} else {
				inputFileSize = -1L;
			}

			final ClientFileSearch search = new ClientFileSearch(
					pathRelativeToBrowseRoot(inputFile),
					fileSearch.getBiologicalSample(),
					fixedCategoryName(fileSearch),
					fileSearch.getExperiment(),
					convertTo(fileSearch.getEnabledEngines()),
					inputFileSize);

			clientInputFiles.add(search);
		}

		return new ClientSwiftSearchDefinition(
				definition.getTitle(),
				convertTo(definition.getUser()),
				relativeOutputFolderPath,
				resolver.resolve(definition.getSearchParameters(), definition.getUser()),
				clientInputFiles,
				convertTo(definition.getQa()),
				convertTo(definition.getPeptideReport()),
				Boolean.TRUE.equals(definition.getPublicMgfFiles()),
				Boolean.TRUE.equals(definition.getPublicMzxmlFiles()),
				Boolean.TRUE.equals(definition.getPublicSearchFiles()),
				new HashMap<String, String>(definition.getMetadata()),
				0
		);
	}

	private String fixedCategoryName(final FileSearch fileSearch) {
		return fileSearch.getCategoryName() == null ? "none" : fileSearch.getCategoryName();
	}

	private static ClientPeptideReport convertTo(final PeptideReport peptideReport) {
		return new ClientPeptideReport(peptideReport != null);
	}

	private static ClientExtractMsnSettings convertTo(final ExtractMsnSettings extractMsnSettings) {
		return new ClientExtractMsnSettings(extractMsnSettings.getCommandLineSwitches(), extractMsnSettings.getCommand());
	}

	private ClientScaffoldSettings convertTo(final ScaffoldSettings scaffoldSettings) {
		return new ClientScaffoldSettings(
				scaffoldSettings.getProteinProbability(),
				scaffoldSettings.getPeptideProbability(),
				scaffoldSettings.getMinimumPeptideCount(),
				scaffoldSettings.getMinimumNonTrypticTerminii(),
				convertTo(scaffoldSettings.getStarredProteins()),
				scaffoldSettings.isSaveOnlyIdentifiedSpectra(),
				scaffoldSettings.isSaveNoSpectra(),
				scaffoldSettings.isConnectToNCBI(),
				scaffoldSettings.isAnnotateWithGOA(),
				scaffoldSettings.isUseIndependentSampleGrouping(),
				scaffoldSettings.isUseFamilyProteinGrouping()
		);
	}

	private ClientStarredProteins convertTo(final StarredProteins starredProteins) {
		if (starredProteins == null) {
			return null;
		}
		return new ClientStarredProteins(
				starredProteins.getStarred(),
				starredProteins.getDelimiter(),
				starredProteins.isRegularExpression());
	}

	private ClientSpectrumQa convertTo(final SpectrumQa qa) {
		if (qa == null || qa.getParamFilePath() == null) {
			return new ClientSpectrumQa();
		}
		return new ClientSpectrumQa(qa.getParamFilePath());
	}

	public ExtractMsnSettings convertFrom(final ClientExtractMsnSettings extractMsnSettings) {
		if (extractMsnSettings == null) {
			return ExtractMsnSettings.DEFAULT;
		}
		return new ExtractMsnSettings(extractMsnSettings.getCommandLineSwitches(), extractMsnSettings.getCommand());
	}

	public ScaffoldSettings convertFrom(final ClientScaffoldSettings scaffoldSettings) {
		if (scaffoldSettings == null) {
			return ScaffoldSettings.DEFAULT;
		}
		return new ScaffoldSettings(
				scaffoldSettings.getProteinProbability(),
				scaffoldSettings.getPeptideProbability(),
				scaffoldSettings.getMinimumPeptideCount(),
				scaffoldSettings.getMinimumNonTrypticTerminii(),
				convertFrom(scaffoldSettings.getStarredProteins()),
				scaffoldSettings.isSaveOnlyIdentifiedSpectra(),
				scaffoldSettings.isSaveNoSpectra(),
				scaffoldSettings.isConnectToNCBI(),
				scaffoldSettings.isAnnotateWithGOA(),
				scaffoldSettings.isUseIndependentSampleGrouping(),
				scaffoldSettings.isUseFamilyProteinGrouping()
		);
	}

	private StarredProteins convertFrom(final ClientStarredProteins starredProteins) {
		if (starredProteins == null) {
			return null;
		}
		return new StarredProteins(
				starredProteins.getStarred(),
				starredProteins.getDelimiter(),
				starredProteins.isRegularExpression());
	}

	public SwiftSearchDefinition convertFrom(final ClientSwiftSearchDefinition definition, final SearchEngineParameters parameters) {
		final List<ClientFileSearch> fileTable = definition.getInputFiles();
		final List<FileSearch> fileEntries = new ArrayList<FileSearch>(fileTable.size());
		for (final ClientFileSearch entry : fileTable) {
			fileEntries.add(convertFrom(entry));
		}

		return new SwiftSearchDefinition(
				definition.getSearchTitle().trim(),
				workspaceDao.getUserByEmail(definition.getUser().getEmail()),
				new File(browseRoot, definition.getOutputFolder()),
				convertFrom(definition.getSpectrumQa()),
				convertFrom(definition.getPeptideReport()),
				parameters,
				fileEntries,
				definition.isPublicMgfFiles(),
				definition.isPublicMzxmlFiles(),
				definition.isPublicSearchFiles()
		);
	}

	public PeptideReport convertFrom(final ClientPeptideReport clientPeptideReport) {
		return clientPeptideReport.isScaffoldPeptideReportEnabled() ? new PeptideReport() : null;
	}

	public SpectrumQa convertFrom(final ClientSpectrumQa clientSpectrumQa) {
		return clientSpectrumQa.isEnabled() ? new SpectrumQa(clientSpectrumQa.getParamFilePath(), SpectrumQa.DEFAULT_ENGINE) : null;
	}

	public ClientModSpecificitySet convertTo(final ModSet val) {
		final List<ClientModSpecificity> vals = new ArrayList<ClientModSpecificity>();
		for (final ModSpecificity modspec : val.getModifications()) {
			vals.add(convertTo(modspec));
		}
		return new ClientModSpecificitySet(vals);
	}

	public ClientModSpecificity convertTo(final ModSpecificity modspec) {
		final ClientModSpecificity cmod = new ClientModSpecificity();
		cmod.setName(modspec.toString());
		cmod.setClassification(modspec.getClassification());
		cmod.setTerm(modspec.getTerm().toString());
		cmod.setSite(modspec.getSite());
		cmod.setRecordID(modspec.getModification().getRecordID());
		cmod.setComposition(modspec.getModification().getComposition());
		cmod.setMonoisotopic(modspec.getModification().getMassMono());
		cmod.setProteinOnly(modspec.isProteinOnly());
		cmod.setHidden(modspec.getHidden());
		if (modspec.getModification().getAltNames() != null) {
			final int len = modspec.getModification().getAltNames().size();
			final List<String> list = new ArrayList<String>(len);
			list.addAll(modspec.getModification().getAltNames());
			cmod.setAltNames(list);
		}
		cmod.setComments(modspec.getComments());

		return cmod;
	}

	public Protease convertFrom(final ClientProtease val,
	                            final Iterable<Protease> allowedValues) {
		for (final Protease p : allowedValues) {
			if (val.getName().equals(p.getName())) {
				return p;
			}
		}
		throw new ConversionException("Can't seem to find Protease " + val.getName());
	}

	public ClientProtease convertTo(final Protease val) {
		return new ClientProtease(val.getName());
	}

	public ClientValue convert(final Object val) {
		if (val instanceof Tolerance) {
			return convertTo((Tolerance) val);
		} else if (val instanceof Validation) {
			return convertTo((Validation) val);
		} else if (val instanceof ValidationList) {
			return convertTo((ValidationList) val);
		} else if (val instanceof Curation) {
			return convertTo((Curation) val);
		} else if (val instanceof Instrument) {
			return convertTo((Instrument) val);
		} else if (val instanceof ModSpecificity) {
			return convertTo((ModSpecificity) val);
		} else if (val instanceof ModSet) {
			return convertTo((ModSet) val);
		} else if (val instanceof Protease) {
			return convertTo((Protease) val);
		} else if (val instanceof ExtractMsnSettings) {
			return convertTo((ExtractMsnSettings) val);
		} else if (val instanceof ScaffoldSettings) {
			return convertTo((ScaffoldSettings) val);
		} else if (val instanceof Integer) {
			return new ClientInteger((Integer) val);
		} else {
			throw new MprcException("Can't convert " + val.getClass().getName() + " to client proxy.");
		}
	}

	public Object convert(final ClientValue val, final Object allowedValues) {
		if (val instanceof ClientInteger) {
			return ((ClientInteger) val).getValue();
		} else if (val instanceof ClientTolerance) {
			return convertFrom((ClientTolerance) val);
		} else if (val instanceof ClientSequenceDatabase) {
			return convertFrom((ClientSequenceDatabase) val,
					(Iterable<Curation>) allowedValues);
		} else if (val instanceof ClientInstrument) {
			return convertFrom((ClientInstrument) val,
					(Iterable<Instrument>) allowedValues);
		} else if (val instanceof ClientModSpecificitySet) {
			return convertFrom((ClientModSpecificitySet) val
			);
		} else if (val instanceof ClientProtease) {
			return convertFrom((ClientProtease) val,
					(Iterable<Protease>) allowedValues);
		} else if (val instanceof ClientExtractMsnSettings) {
			return convertFrom((ClientExtractMsnSettings) val);
		} else if (val instanceof ClientScaffoldSettings) {
			return convertFrom((ClientScaffoldSettings) val);
		} else if (val instanceof ClientProtease) {
			return convertFrom((ClientProtease) val,
					(Iterable<Protease>) allowedValues);

		} else {
			throw new MprcException("Can't convert " + val.getClass().getName() + " from client proxy");
		}
	}
}
