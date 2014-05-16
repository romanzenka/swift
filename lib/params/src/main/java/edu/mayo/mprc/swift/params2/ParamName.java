package edu.mayo.mprc.swift.params2;

import edu.mayo.mprc.MprcException;

/**
 * Enum of all supported parameter names that can be stored in {@link SearchEngineParameters}.
 * This is useful if we want to iterate over them, or refer to them by their name.
 */
public enum ParamName {
	PeptideTolerance("tolerance.peptide", "Peptide Parent Ion Mass Tolerance", "A candidate theoretical peptide must differ in mass from the experimental, parent mass by less than this tolerance."),
	FragmentTolerance("tolerance.fragment", "Fragment Ion Mass Tolerance", "Fragment Ion Mass Tolerance"),
	MinTerminiCleavages("sequence.min_termini_cleavages", "Minimum Termini Cleavages", "The minimum amount of termini cleaved by a protease"),
	MissedCleavages("sequence.missed_cleavages", "Allowed Missed Cleavages", "The max number of missed cleavages to allow."),
	Database("sequence.database", "Amino Acid Sequence Database", "Amino Acid Sequence Database"),
	Enzyme("sequence.enzyme", "Endoprotease", "Endoprotease"),
	VariableMods("modifications.variable", "Variable Modifications", "Variable Modifications"),
	FixedMods("modifications.fixed", "Fixed Modifications", "Fixed Modifications"),
	Instrument("instrument", "Instrument", "Instrument"),
	ExtractMsnSettings("extractMsnSettings", "Extract_msn Settings", "How to obtain the MS2 spectrum list to send to the engine"),
	ScaffoldSettings("scaffoldSettings", "Scaffold Settings", "How to filter the output of the search engine"),
	EnabledEngines("enabledEngines", "Enabled Engines", "Which engines we should use for the search");

	private final String id;
	private final String name;
	private final String desc;

	ParamName(final String id, final String name, final String desc) {
		this.id = id;
		this.name = name;
		this.desc = desc;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDesc() {
		return desc;
	}

	public static ParamName getById(final String id) {
		for (final ParamName param : ParamName.values()) {
			if (param.getId().equals(id)) {
				return param;
			}
		}
		throw new MprcException("Unsupported parameter id " + id);
	}
}
