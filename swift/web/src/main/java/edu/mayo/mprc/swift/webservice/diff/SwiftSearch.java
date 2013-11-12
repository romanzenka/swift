package edu.mayo.mprc.swift.webservice.diff;

import com.google.common.base.Objects;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.dbmapping.SearchEngineConfig;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.swift.params2.SearchEngineParameters;
import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.unimod.ModSpecificity;

import java.io.File;
import java.util.Date;
import java.util.Set;

/**
 * A short representation of {@link SearchRun}.
 *
 * @author Roman Zenka
 */
@XStreamAlias("swiftSearch")
public final class SwiftSearch {
	private final int id;
	private final String title;
	private final Date startTimestamp;
	private final String[] engines;
	private final String curationName;
	private final String enzymeName;
	private final int missedCleavages;
	private final String[] fixedModifications;
	private final String[] variableModifications;
	private final String peptideTolerance;
	private final String fragmentTolerance;
	private final String instrument;
	private final String raw2mgfConvertor;
	private final String scaffoldProteinProbability;
	private final String scaffoldPeptideProbability;
	private final String scaffoldMinimumPeptideCount;
	private final String scaffoldMinimumNonTrypticTerminii;
	private final boolean useIndependentSampleGrouping;
	private final boolean useFamilyProteinGrouping;
	private final String qaUrl;

	public SwiftSearch(final SearchRun run, final SwiftSearchDefinition searchDefinition, final WebUi webUi) {
		id = run.getId();
		title = run.getTitle();
		if (!Objects.equal(searchDefinition.getId(), run.getSwiftSearch())) {
			throw new MprcException("Search must match the definition");
		}
		startTimestamp = run.getStartTimestamp();
		final SearchEngineParameters params = searchDefinition.getSearchParameters();
		engines = enginesToString(searchDefinition.getInputFiles().iterator().next().getEnabledEngines().getEngineConfigs());
		curationName = params.getDatabase().getShortName();
		enzymeName = params.getProtease().getName();
		missedCleavages = params.getMissedCleavages();
		fixedModifications = modsToString(params.getFixedModifications().getModifications());
		variableModifications = modsToString(params.getVariableModifications().getModifications());
		peptideTolerance = params.getPeptideTolerance().toString();
		fragmentTolerance = params.getFragmentTolerance().toString();
		instrument = params.getInstrument().getName();
		raw2mgfConvertor = params.getExtractMsnSettings().getCommand() + " " + params.getExtractMsnSettings().getCommandLineSwitches();
		scaffoldProteinProbability = 100.0d * params.getScaffoldSettings().getProteinProbability() + "%";
		scaffoldPeptideProbability = 100.0d * params.getScaffoldSettings().getPeptideProbability() + "%";
		scaffoldMinimumPeptideCount = String.valueOf(params.getScaffoldSettings().getMinimumPeptideCount());
		scaffoldMinimumNonTrypticTerminii = String.valueOf(params.getScaffoldSettings().getMinimumNonTrypticTerminii());
		if (searchDefinition.getOutputFolder() != null && searchDefinition.getOutputFolder().isDirectory()) {
			qaUrl = webUi.fileToUserLink(new File(searchDefinition.getOutputFolder(), "qa/index.html"));
		} else {
			qaUrl = null;
		}
		useFamilyProteinGrouping = params.getScaffoldSettings().isUseFamilyProteinGrouping();
		useIndependentSampleGrouping = params.getScaffoldSettings().isUseIndependentSampleGrouping();
	}

	private String[] enginesToString(final Set<SearchEngineConfig> engineConfigs) {
		final String[] result = new String[engineConfigs.size()];
		int i = 0;
		for (final SearchEngineConfig config : engineConfigs) {
			result[i] = config.getCode() + " " + config.getVersion();
			i++;
		}
		return result;
	}

	private String[] modsToString(final Set<ModSpecificity> modifications) {
		final String[] result = new String[modifications.size()];
		int i = 0;
		for (final ModSpecificity mod : modifications) {
			result[i] = mod.toString();
			i++;
		}
		return result;
	}

	public int getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String[] getEngines() {
		return engines;
	}

	public Date getStartTimestamp() {
		return startTimestamp;
	}

	public String getCurationName() {
		return curationName;
	}

	public String getEnzymeName() {
		return enzymeName;
	}

	public int getMissedCleavages() {
		return missedCleavages;
	}

	public String[] getFixedModifications() {
		return fixedModifications;
	}

	public String[] getVariableModifications() {
		return variableModifications;
	}

	public String getPeptideTolerance() {
		return peptideTolerance;
	}

	public String getFragmentTolerance() {
		return fragmentTolerance;
	}

	public String getInstrument() {
		return instrument;
	}

	public String getRaw2mgfConvertor() {
		return raw2mgfConvertor;
	}

	public String getScaffoldProteinProbability() {
		return scaffoldProteinProbability;
	}

	public String getScaffoldPeptideProbability() {
		return scaffoldPeptideProbability;
	}

	public String getScaffoldMinimumPeptideCount() {
		return scaffoldMinimumPeptideCount;
	}

	public String getScaffoldMinimumNonTrypticTerminii() {
		return scaffoldMinimumNonTrypticTerminii;
	}

	public String getQaUrl() {
		return qaUrl;
	}

	public boolean isUseIndependentSampleGrouping() {
		return useIndependentSampleGrouping;
	}

	public boolean isUseFamilyProteinGrouping() {
		return useFamilyProteinGrouping;
	}
}
