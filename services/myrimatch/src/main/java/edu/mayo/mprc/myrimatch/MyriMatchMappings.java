package edu.mayo.mprc.myrimatch;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.params2.*;
import edu.mayo.mprc.swift.params2.mapping.MappingContext;
import edu.mayo.mprc.swift.params2.mapping.Mappings;
import edu.mayo.mprc.unimod.ModSet;
import edu.mayo.mprc.unimod.ModSpecificity;
import edu.mayo.mprc.unimod.Terminus;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ResourceUtilities;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class MyriMatchMappings implements Mappings {

	public static final String STATIC_MODS = "StaticMods";
	public static final String DYNAMIC_MODS = "DynamicMods";
	public static final String CLEAVAGE_RULES = "CleavageRules";
	public static final String FRAGMENTATION_RULE = "FragmentationRule";
	public static final String NUM_MIN_TERMINI_CLEAVAGES = "MinTerminiCleavages";
	public static final String NUM_MAX_MISSED_CLEAVAGES = "MaxMissedCleavages";
	public static final String PRECURSOR_MZ_TOLERANCE = "PrecursorMzTolerance";
	public static final String FRAGMENT_MZ_TOLERANCE = "FragmentMzTolerance";
	public static final String PRECURSOR_MZ_TOLERANCE_RULE = "PrecursorMzToleranceRule";


	private Map<String, String> nativeParams;
	private static final String VARIABLE_MODS_MARKERS = "*^@%~&?!:;|+-/";
	private static final Pattern INTEGER = Pattern.compile("^[0-9]+$");

	public MyriMatchMappings() {
		nativeParams = initNativeParams();
	}

	@Override
	public Reader baseSettings() {
		return ResourceUtilities.getReader("classpath:edu/mayo/mprc/myrimatch/base.myrimatch.cfg", this.getClass());
	}

	@Override
	public void read(final Reader isr) {
		nativeParams = initNativeParams();
		final BufferedReader bufferedReader = new BufferedReader(isr);
		try {
			while (true) {
				final String s = bufferedReader.readLine();
				if (s == null) {
					break;
				}
				final String row = stripComment(s);
				final int equalSignPos = row.indexOf('=');
				if (equalSignPos >= 0) {
					final String key = row.substring(0, equalSignPos).trim();
					final String value = row.substring(equalSignPos + 1).trim();
					if (nativeParams.containsKey(key)) {
						nativeParams.put(key, value);
					}
				}
			}
		} catch (Exception e) {
			throw new MprcException("Cannot load base MyriMatch configuration", e);
		} finally {
			FileUtilities.closeQuietly(bufferedReader);
		}
	}

	/**
	 * Access for testing purposes only.
	 *
	 * @return Map of all native parameters.
	 */
	Map<String, String> getNativeParams() {
		return ImmutableMap.copyOf(nativeParams);
	}

	/**
	 * A comment starts with the character #
	 *
	 * @param s String to remove comment from
	 * @return String without the comment part
	 */
	static String stripComment(final String s) {
		final int pos = s.indexOf('#');
		return pos >= 0 ? s.substring(0, pos) : s;
	}

	private Map<String, String> initNativeParams() {
		final Map<String, String> map = new HashMap<String, String>();
		map.put(STATIC_MODS, null);
		map.put(DYNAMIC_MODS, null);
		map.put(CLEAVAGE_RULES, null);
		map.put(NUM_MIN_TERMINI_CLEAVAGES, null);
		map.put(NUM_MAX_MISSED_CLEAVAGES, null);
		map.put(PRECURSOR_MZ_TOLERANCE, null);
		map.put(FRAGMENT_MZ_TOLERANCE, null);
		map.put(FRAGMENTATION_RULE, null);
		return map;
	}

	@Override
	public void write(final Reader oldParams, final Writer out) {
		final BufferedReader bufferedReader = new BufferedReader(oldParams);
		final StringBuilder result = new StringBuilder(100);
		try {
			while (true) {
				boolean processed = false;

				final String line = bufferedReader.readLine();
				if (null == line) {
					break;
				}

				final int commentPos = line.indexOf('#');
				final int lastPart;
				if (commentPos < 0) {
					lastPart = line.length();
				} else {
					lastPart = commentPos;
				}

				final int equalsSign = line.indexOf('=');
				if (equalsSign < lastPart && equalsSign >= 0) {
					final String key = line.substring(0, equalsSign).trim();
					final String oldValue = line.substring(equalsSign + 1, lastPart).trim();
					final String value = nativeParams.get(key);
					// We escape strings
					final String newValue = value == null || INTEGER.matcher(value).matches() ? value : '"' + value + '"';

					if (null != newValue && !oldValue.equals(newValue)) {
						processed = true;
						result.setLength(0);
						result
								.append(key)
								.append(" = ")
								.append(newValue)
								.append(line.substring(lastPart))
								.append('\n');
						out.write(result.toString());
					}
				}

				if (!processed) {
					out.write(line);
					out.write('\n');
				}
			}
		} catch (Exception e) {
			throw new MprcException("Cannot load base MyriMatch configuration", e);
		} finally {
			FileUtilities.closeQuietly(bufferedReader);
		}
	}

	@Override
	public void setPeptideTolerance(final MappingContext context, final Tolerance peptideTolerance) {
		nativeParams.put(PRECURSOR_MZ_TOLERANCE, String.valueOf(peptideTolerance.getValue()) + massUnitToMyriMatch(peptideTolerance));
	}

	private String massUnitToMyriMatch(final Tolerance peptideTolerance) {
		if (MassUnit.Da == peptideTolerance.getUnit()) {
			return "daltons";
		}
		if (MassUnit.Ppm == peptideTolerance.getUnit()) {
			return "ppm";
		}
		throw new MprcException("MyriMatch does not support mass unit " + peptideTolerance.getUnit().name() + "(" + peptideTolerance.getUnit().getDescription() + ")");
	}

	@Override
	public void setFragmentTolerance(final MappingContext context, final Tolerance fragmentTolerance) {
		final String tolerance = massUnitToMyriMatch(fragmentTolerance);
		nativeParams.put(FRAGMENT_MZ_TOLERANCE, fragmentTolerance.getValue() + "" + tolerance);
	}

	@Override
	public void setVariableMods(final MappingContext context, final ModSet variableMods) {
		nativeParams.put(DYNAMIC_MODS, variableModsToString(context, variableMods));
	}

	private String variableModsToString(final MappingContext context, final ModSet variableMods) {
		if (variableMods.getModifications().size() == 0) {
			return "";
		}

		final StringBuilder result = new StringBuilder(100);
		int index = 0;
		boolean warnedUnsupportedProteinOnly = false;

		for (final ModSpecificity specificity : variableMods.getModifications()) {
			if (!warnedUnsupportedProteinOnly && specificity.isPositionProteinSpecific()) {
				context.reportWarning("Protein terminus-only mods are not supported. Will allow modification at peptide terminus.");
				warnedUnsupportedProteinOnly = true;
			}

			if (specificity.getTerm() == Terminus.Nterm) {
				result.append("(");
			}

			result.append(specificity.getSite());

			if (specificity.getTerm() == Terminus.Cterm) {
				result.append(")");
			}

			result.append(' ')
					.append(VARIABLE_MODS_MARKERS.charAt(index))
					.append(' ')
					.append(specificity.getModification().getMassMono())
					.append(' ');

			index++;
			if (variableMods.size() >= VARIABLE_MODS_MARKERS.length()) {
				context.reportWarning("MyriMatch supports only " + VARIABLE_MODS_MARKERS.length() + " modifications.");
				break;
			}
		}
		return result.substring(0, result.length() - 1);
	}

	@Override
	public void setFixedMods(final MappingContext context, final ModSet fixedMods) {
		nativeParams.put(STATIC_MODS, fixedModsToString(fixedMods));
	}

	private String fixedModsToString(final ModSet fixedMods) {
		if (fixedMods.getModifications().size() == 0) {
			return "";
		}
		final StringBuilder result = new StringBuilder(50);
		for (final ModSpecificity specificity : fixedMods.getModifications()) {
			result
					.append(specificity.getSite())
					.append(' ')
					.append(specificity.getModification().getMassMono())
					.append(' ');
		}
		return result.substring(0, result.length() - 1);
	}

	@Override
	public void setSequenceDatabase(final MappingContext context, final String shortDatabaseName) {
		// The database is not set in the config
	}

	@Override
	/**
	 * Map enzyme to PCRE regular expression with zero-width lookbehind, lookahead assertions.
	 *
	 * <table>
	 *     <tr><th></th><th>Behind</th><th>Ahead</th></tr>
	 *     <tr><td>Positive</td><td>(&lt;<= )</td><td>(?= )</td></tr>
	 *     <tr><td>Negative</td><td>(&lt;<! )</td><td>(?! )</td></tr>
	 * </table>
	 *
	 * Example: Trypsin <pre>(?<=[KR])(?!P)</pre>
	 */
	public void setProtease(final MappingContext context, final Protease protease) {
		nativeParams.put(CLEAVAGE_RULES, enzymeToString(protease));
		if (isNonSpecificEnzyme(protease)) {
			nativeParams.put(NUM_MIN_TERMINI_CLEAVAGES, "0");
		} else {
			nativeParams.put(NUM_MIN_TERMINI_CLEAVAGES, "2");
		}
	}

	static String enzymeToString(final Protease enzyme) {
		if (isNonSpecificEnzyme(enzyme)) {
			// Non-specific enzymes in Myrimatch are done using a normal enzyme
			// with MinTerminiCleavages set to 0
			return enzymeToString(Protease.TRYPSIN_P);
		}

		final StringBuilder result = new StringBuilder(20);

		if (!"".equals(enzyme.getRnminus1())) {
			if (enzyme.getRnminus1().startsWith("!")) {
				result
						.append("(?<!")
						.append(wrapAminoAcidGroup(enzyme.getRnminus1().substring(1)))
						.append(")");
			} else {
				result
						.append("(?<=")
						.append(wrapAminoAcidGroup(enzyme.getRnminus1()))
						.append(")");
			}
		}
		if (!"".equals(enzyme.getRn())) {
			if (enzyme.getRn().startsWith("!")) {
				result
						.append("(?!")
						.append(wrapAminoAcidGroup(enzyme.getRn().substring(1)))
						.append(")");
			} else {
				result
						.append("(?=")
						.append(wrapAminoAcidGroup(enzyme.getRn()))
						.append(")");
			}
		}
		return result.toString();
	}

	private static boolean isNonSpecificEnzyme(Protease enzyme) {
		return "".equals(enzyme.getRnminus1()) && "".equals(enzyme.getRn());
	}

	private static String wrapAminoAcidGroup(final String group) {
		if (group.length() <= 1) {
			return group;
		}
		return "[" + group + "]";
	}

	@Override
	public void setMissedCleavages(final MappingContext context, final Integer missedCleavages) {
		nativeParams.put(NUM_MAX_MISSED_CLEAVAGES, String.valueOf(missedCleavages));
	}

	@Override
	public void setInstrument(final MappingContext context, final Instrument instrument) {
		// Orbis use a monoisotopic precursor
		nativeParams.put(PRECURSOR_MZ_TOLERANCE_RULE, Instrument.ORBITRAP.equals(instrument) ? "mono" : "avg");
		final String series = Joiner.on(",").join(Sets.<IonSeries>newTreeSet(instrument.getSeries()));
		nativeParams.put(FRAGMENTATION_RULE, "manual:" + series);
	}

	@Override
	public String getNativeParam(final String name) {
		return nativeParams.get(name);
	}

	@Override
	public void setNativeParam(final String name, final String value) {
		nativeParams.put(name, value);
	}
}
