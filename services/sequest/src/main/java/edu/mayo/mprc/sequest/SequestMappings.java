package edu.mayo.mprc.sequest;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.params2.*;
import edu.mayo.mprc.swift.params2.mapping.MappingContext;
import edu.mayo.mprc.swift.params2.mapping.Mappings;
import edu.mayo.mprc.unimod.ModSet;
import edu.mayo.mprc.unimod.ModSpecificity;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ResourceUtilities;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SequestMappings implements Mappings, Cloneable {

	/**
	 * Sequest tends to slow down considerably as fragment tolernace goes down.
	 * This value is a reasonable compromise between speed and precision.
	 */
	private static final double MIN_FRAGMENT_TOLERANCE = 0.1;
	private static final double PPM_TO_DALTON = 1000.0;

	private static final String PEP_TOL_UNIT = "peptide_mass_units";
	private static final String PEP_TOL_VALUE = "peptide_mass_tolerance";
	private static final String FRAG_TOL_VALUE = "fragment_ion_tolerance";
	/**
	 * Variable modifications parameter.
	 */
	static final String VAR_MODS = "diff_search_options";

	/**
	 * C and N-terminus modifications parameter.
	 */
	static final String VAR_MODS_TERMINUS = "term_diff_search_options";
	private static final String DATABASE = "first_database_name";
	private static final String ENZYME = "enzyme_info";
	private static final String MISSED_CLEAVAGES = "max_num_internal_cleavage_sites";
	private static final String ION_SERIES = "ion_series";

	private static final Pattern FIXED = Pattern.compile("^add_([A-Z]|Nterm|Cterm)_(.*)");

	private static final Pattern SEQUEST_HEADER = Pattern.compile("\\[((?:SEQUEST)|(?:MAKEDB))\\]");

	private static final Pattern WHITESPACE = Pattern.compile("^\\s*$");
	private static final Pattern COMMENT = Pattern.compile("^\\s*(;.*)$");
	private static final Pattern EQUALS = Pattern.compile("^.*\\=.*$");
	private static final Pattern PARSE_LINE = Pattern.compile("^\\s*([^\\s=]+)\\s*=\\s*([^;]*)(\\s*;.*)?$");

	private static final String[] FIXED_MODS = new String[]{
			"add_Cterm_peptide",
			"add_Cterm_protein",
			"add_Nterm_peptide",
			"add_Nterm_protein",
			"add_G_Glycine",
			"add_A_Alanine",
			"add_S_Serine",
			"add_P_Proline",
			"add_V_Valine",
			"add_T_Threonine",
			"add_C_Cysteine",
			"add_L_Leucine",
			"add_I_Isoleucine",
			"add_X_LorI",
			"add_N_Asparagine",
			"add_O_Ornithine",
			"add_B_avg_NandD",
			"add_D_Aspartic_Acid",
			"add_Q_Glutamine",
			"add_K_Lysine",
			"add_Z_avg_QandE",
			"add_E_Glutamic_Acid",
			"add_M_Methionine",
			"add_H_Histidine",
			"add_F_Phenylalanine",
			"add_R_Arginine",
			"add_Y_Tyrosine",
			"add_W_Tryptophan",
			"add_J_user_amino_acid",
			"add_U_user_amino_acid"
	};

	/**
	 * It is important to use linked hash map because the {@link SequestToMakeDBConverter} depends
	 * on ordering of the native params.
	 * <p/>
	 * Also, just because of the converter, we store all native parameters, not just those that map to abstract params.
	 */
	private LinkedHashMap<String, String> nativeParams = new LinkedHashMap<String, String>();

	public SequestMappings() {
	}

	@Override
	public Reader baseSettings() {
		return ResourceUtilities.getReader("classpath:edu/mayo/mprc/swift/params/base.sequest.params", getClass());
	}

	/**
	 * TODO: This method is retained for right now because Sequest to MakeDB mapper actually loads the mappings from a file
	 * on the filesystem. Ideally, it would operate directly on the search parameter object instead of loading a previous saved code.
	 *
	 * @param isr Reader for the params file
	 */
	@Override
	public void read(final Reader isr) {
		final BufferedReader reader = new BufferedReader(isr);
		try {
			final String header = reader.readLine();
			final Matcher m = SEQUEST_HEADER.matcher(header);
			if ((header == null) || (header.isEmpty()) || (!m.lookingAt())) {
				throw new MprcException("Not a sequest params file");
			}
			while (true) {
				final String it = reader.readLine();
				if (it == null) {
					break;
				}

				if (WHITESPACE.matcher(it).matches() || COMMENT.matcher(it).matches()) {
					// Comment, ignore
					continue;
				}

				if (EQUALS.matcher(it).matches()) {
					// basically, we want to match: a keyword followed by equals followed by an optional value
					// followed by optional whitespace and comment.
					final Matcher matcher = PARSE_LINE.matcher(it);
					if (!matcher.matches()) {
						throw new MprcException("Can't understand '" + it + "'");
					}
					final String id = matcher.group(1);
					String value = matcher.group(2);
					if (value == null) {
						value = "";
					}

					// We store absolutely all parameters, because makedb depends on it
					nativeParams.put(id, value);
				} else {
					throw new MprcException("Can't understand '" + it + "'");
				}
			}
		} catch (Exception t) {
			throw new MprcException("Failure reading sequest parameter collection", t);
		} finally {
			FileUtilities.closeQuietly(reader);
		}
	}

	@Override
	public void write(final Reader oldParams, final Writer out) {
		BufferedReader reader = null;
		PrintWriter pw = null;
		try {
			reader = new BufferedReader(oldParams);
			pw = new PrintWriter(out);
			while (true) {
				final String it = reader.readLine();
				if (it == null) {
					break;
				}

				if (WHITESPACE.matcher(it).matches() || COMMENT.matcher(it).matches()) {
					pw.println(it);
				} else if (EQUALS.matcher(it).matches()) {
					// basically, we want to match: a keyword followed by equals followed by an optional value
					// followed by optional whitespace and comment.
					final Matcher matcher = PARSE_LINE.matcher(it);
					if (!matcher.matches()) {
						throw new MprcException("Can't understand '" + it + "'");
					}
					final String id = matcher.group(1);
					if (nativeParams.keySet().contains(id)) {
						// Replace the value
						if (matcher.group(2) != null) {
							// We have the value defined
							pw.print(it.substring(0, matcher.start(2)));
							pw.print(nativeParams.get(id));
							pw.println(it.substring(matcher.end(2)));
						} else {
							// The value is missing
							pw.print(it.trim());
							pw.println(nativeParams.get(id));
						}
					} else {
						pw.println(it);
					}
				} else {
					pw.println(it);
				}
			}
		} catch (Exception t) {
			throw new MprcException("Failure reading sequest parameter collection", t);
		} finally {
			FileUtilities.closeQuietly(reader);
			FileUtilities.closeQuietly(pw);
		}
	}

	/**
	 * @return The complete map of native parameters.
	 */
	public Map<String, String> getNativeParams() {
		return Collections.unmodifiableMap(nativeParams);
	}

	/**
	 * Returns value of a given native parameter.
	 *
	 * @param name Name of the parameter.
	 * @return Value of the native parameter.
	 */
	@Override
	public String getNativeParam(final String name) {
		return nativeParams.get(name).trim();
	}

	/**
	 * Allows the sequest to makedb converter to overwrite the native parameter values directly.
	 *
	 * @param name  Name of the native parameter.
	 * @param value Value of the native parameter.
	 */
	@Override
	public void setNativeParam(final String name, final String value) {
		nativeParams.put(name, value);
	}

	@Override
	public void setPeptideTolerance(final MappingContext context, final Tolerance peptideTolerance) {
		setNativeParam(PEP_TOL_VALUE, String.valueOf(peptideTolerance.getValue()));
		if (MassUnit.Da.equals(peptideTolerance.getUnit())) {
			setNativeParam(PEP_TOL_UNIT, "0");
			// MMU = 1 but we never use MMU
		} else if (MassUnit.Ppm.equals(peptideTolerance.getUnit())) {
			setNativeParam(PEP_TOL_UNIT, "2");
		}
	}

	@Override
	public void setFragmentTolerance(final MappingContext context, final Tolerance fragmentTolerance) {
		final Tolerance newTolerance;
		if (fragmentTolerance.getUnit() == MassUnit.Ppm) {
			final double value = fragmentTolerance.getValue() / PPM_TO_DALTON;
			newTolerance = new Tolerance(value, MassUnit.Da);
			context.reportWarning("Sequest does not support '" + fragmentTolerance.getUnit() + "' fragment tolerances; using " + newTolerance.getValue() + " " + newTolerance.getUnit().getCode() + " instead.", null);
			setNativeParam(FRAG_TOL_VALUE, String.valueOf(newTolerance.getValue()));
		} else {
			newTolerance = fragmentTolerance;
		}
		final Tolerance clampedTolerance;
		if (newTolerance.getValue() < MIN_FRAGMENT_TOLERANCE) {
			clampedTolerance = new Tolerance(MIN_FRAGMENT_TOLERANCE, MassUnit.Da);
			context.reportWarning("Sequest runs too slow with fragment tolerance below " + MIN_FRAGMENT_TOLERANCE + " Da, using " + MIN_FRAGMENT_TOLERANCE + " Da instead.", null);
		} else {
			clampedTolerance = newTolerance;
		}
		setNativeParam(FRAG_TOL_VALUE, String.valueOf(clampedTolerance.getValue()));
	}

	@Override
	public void setVariableMods(final MappingContext context, final ModSet variableMods) {
		final StringBuilder sb = new StringBuilder();

		final List<ModSpecificity> set = new ArrayList<ModSpecificity>(variableMods.getModifications());
		Collections.sort(set);

		int i = 0;

		final StringBuilder skippedMods = new StringBuilder(20);
		final StringBuilder skippedNterm = new StringBuilder(20);
		final StringBuilder skippedCterm = new StringBuilder(20);
		boolean proteinSpecificMod = false;
		Double cterm = null;
		Double nterm = null;

		for (final ModSpecificity ms : set) {
			final String title = ms.toString();
			final double mass = ms.getModification().getMassMono();

			if (notSupportedMod(ms)) {
				context.reportWarning("Sequest does not support variable modification with specific site '" +
						ms.getSite() + "' limited to " + ms.getTerm() + ", skipping " + title, null);

			} else if (ms.isPositionNTerminus()) {
				if (nterm != null) {
					appendCommaSeparated(skippedNterm, title);
				} else {
					nterm = mass;
					if (ms.isPositionProteinSpecific()) {
						proteinSpecificMod = true;
					}
				}
			} else if (ms.isPositionCTerminus()) {
				if (cterm != null) {
					appendCommaSeparated(skippedCterm, title);
				} else {
					cterm = mass;
					if (ms.isPositionProteinSpecific()) {
						proteinSpecificMod = true;
					}
				}
			} else {
				i++;
				if (i > 6) {
					appendCommaSeparated(skippedMods, title);
				} else {
					if (sb.length() != 0) {
						sb.append(" ");
					}
					sb.append(mass).append(' ').append(ms.getSite());
				}
			}
		}
		while (i < 6) {
			sb.append(sb.length() == 0 ? "" : " ").append("0.0000 X");
			i++;
		}

		setNativeParam(VAR_MODS, sb.toString());
		setNativeParam(VAR_MODS_TERMINUS, (cterm == null ? "0.0" : cterm) + " " + (nterm == null ? "0.0" : nterm));
		if (skippedMods.length() != 0) {
			context.reportWarning("Sequest supports up to 6 variable modifications, skipping " + skippedMods, null);
		}

		if (skippedNterm.length() != 0) {
			context.reportWarning("Sequest does not support multiple variable modifications at N-terminal, skipping " + skippedNterm, null);
		}

		if (skippedCterm.length() != 0) {
			context.reportWarning("Sequest does not support multiple variable modifications at C-terminal, skipping " + skippedCterm, null);
		}

		if (proteinSpecificMod) {
			context.reportWarning("Sequest does not support variable modifications specific only to protein terminus. These mods will be used for peptide terminii as well.", null);
		}

	}

	/**
	 * @param builder Builder to append next string to.
	 * @param text    String to append. Strings are separated by {@code ", "}
	 */
	private void appendCommaSeparated(final StringBuilder builder, final String text) {
		if (builder.length() > 0) {
			builder.append(", ");
		}
		builder.append(text);
	}

	/**
	 * Sequest does not support modification with specific amino acid at the protein terminus (we can do only
	 * amino acid anywhere, or anything at the terminus).
	 *
	 * @param ms Mod specificity to check
	 * @return true if Sequest supports given mod.
	 */
	private boolean notSupportedMod(final ModSpecificity ms) {
		// we can't support specific amino acids at N or C terminus of peptide or protein.
		return ms.isSiteSpecificAminoAcid() && (!ms.isPositionAnywhere() || ms.isProteinOnly());
	}

	@Override
	public void setFixedMods(final MappingContext context, final ModSet fixedMods) {
		// The key is in form [AA|Cterm|Nterm]_[protein|peptide]
		// We sum all the fixed mod contributions
		final Map<String, Double> masses = new HashMap<String, Double>();
		for (final ModSpecificity ms : fixedMods.getModifications()) {
			final String title = ms.toString();
			final String key;
			if (ms.isPositionAnywhere() && ms.isSiteSpecificAminoAcid()) {
				key = String.valueOf(ms.getSite()); // The key is single letter corresponding to the amino acid
			} else if (ms.isPositionCTerminus()) {
				key = "Cterm_" + (ms.isProteinOnly() ? "protein" : "peptide");
			} else if (ms.isPositionNTerminus()) {
				key = "Nterm_" + (ms.isProteinOnly() ? "protein" : "peptide");
			} else {
				context.reportWarning("Sequest does not support modification with position '" +
						ms.getTerm() + "' and site '" + ms.getSite() + "', dropping " + title, null);
				return;
			}

			final double mass = ms.getModification().getMassMono();
			Double d = 0.0;
			if (masses.containsKey(key)) {
				d = masses.get(key);
			}
			masses.put(key, d + mass);
		}

		for (final String param : FIXED_MODS) {
			final Matcher matcher = FIXED.matcher(param);
			if (matcher.matches()) {
				final String ssite = matcher.group(1);
				final String pos = matcher.group(2);
				final String site;
				if (pos.startsWith("protein") || pos.startsWith("peptide")) {
					site = ssite + "_" + pos;
				} else {
					site = ssite;
				}
				if (masses.containsKey(site)) {
					setNativeParam(matcher.group(), String.valueOf(masses.get(site)));
				} else {
					setNativeParam(matcher.group(), "0.0");
				}
			}
		}
	}

	@Override
	public void setSequenceDatabase(final MappingContext context, final String shortDatabaseName) {
		setNativeParam(DATABASE, "${DB:" + shortDatabaseName + "}");
	}

	@Override
	public void setProtease(final MappingContext context, final Protease protease) {

		final String name = protease.getName().replaceAll("\\s", "_");
		String firstDigit = "1"; // 0 - non-specific, 1-specific (?)
		String secondDigit = "1"; // Forward (cut C-terminus from the residue) or reverse (cut N-terminus)
		String rn = protease.getRn();
		String rnminus1 = protease.getRnminus1();

		// how do we recognize sense == 0 (ie inverted match)?
		if ("Non-Specific".equals(protease.getName())) {
			firstDigit = "0";
			secondDigit = "0";
		} else if (rnminus1.isEmpty()) {
			// if there's no rnminus1, then we assume this is an inverted match
			secondDigit = "0";
			final String swap = rn;
			rn = rnminus1;
			rnminus1 = swap;
		} else if (rn.startsWith("!")) {
			rn = rn.substring(1);
		} else if (!rn.isEmpty()) { // RN.length = 0 is fine, allowed
			// can't deal with this enzyme
			throw new MprcException("Enzyme " + protease.getName() + " cannot be used by Sequest");
		}

		setNativeParam(ENZYME,
				name + " " + firstDigit + " " + secondDigit + " " +
						(rnminus1.equals("") ? "-" : rnminus1) + " " +
						(rn.equals("") ? "-" : rn));
	}

	@Override
	public void setMinTerminiCleavages(MappingContext context, Integer minTerminiCleavages) {
		if (minTerminiCleavages == 1) {
			context.reportWarning("Sequest semitryptic not supported yet", null);
		}
	}

	@Override
	public void setMissedCleavages(final MappingContext context, final Integer missedCleavages) {
		String value = String.valueOf(missedCleavages);
		if (missedCleavages > 12) {
			value = "12";
			context.reportWarning("Sequest doesn't support > 12 missed cleavages", null);
		}
		setNativeParam(MISSED_CLEAVAGES, value);
	}

	// The series matches the pattern.
	private static final String[] INSTRUMENT_SERIES = "a      b      y      a           b           c           d           v           w           x           y           z".split("\\s+");

	@Override
	public void setInstrument(final MappingContext context, final Instrument it) {
		final Map<String, IonSeries> hasseries = new HashMap<String, IonSeries>();
		final StringBuilder sb = new StringBuilder();

		final HashSet<String> seriesset = new HashSet<String>();
		seriesset.addAll(Arrays.asList(INSTRUMENT_SERIES));
		for (final IonSeries is : it.getSeries()) {
			if (seriesset.contains(is.getName())) {
				hasseries.put(is.getName(), is);
			} else {
				context.reportWarning("Sequest doesn't support ion series " + is.getName(), null);
			}
		}

		final HashSet<String> first = new HashSet<String>();
		for (final String ss : INSTRUMENT_SERIES) {
			if (sb.length() != 0) {
				sb.append(" ");
			}
			if (hasseries.containsKey(ss)) {
				if (("a".equals(ss) || "b".equals(ss) || "y".equals(ss)) && !first.contains(ss)) {
					sb.append("1");
					first.add(ss);
				} else {
					sb.append("1.0");
				}
			} else {
				if (("a".equals(ss) || "b".equals(ss) || "y".equals(ss)) && !first.contains(ss)) {
					sb.append("0");
					first.add(ss);
				} else {
					sb.append("0.0");
				}
			}
		}

		setNativeParam(ION_SERIES, sb.toString());
	}

	@Override
	public void checkValidity(MappingContext context) {
	}


	@Override
	public Object clone() throws CloneNotSupportedException {
		final SequestMappings mappings = (SequestMappings) super.clone();
		mappings.nativeParams = new LinkedHashMap<String, String>(nativeParams.size());
		mappings.nativeParams.putAll(nativeParams);
		return mappings;
	}
}
