package edu.mayo.mprc.mascot;

import com.google.common.collect.ImmutableMap;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.params2.Instrument;
import edu.mayo.mprc.swift.params2.MassUnit;
import edu.mayo.mprc.swift.params2.Protease;
import edu.mayo.mprc.swift.params2.Tolerance;
import edu.mayo.mprc.swift.params2.mapping.MappingContext;
import edu.mayo.mprc.swift.params2.mapping.Mappings;
import edu.mayo.mprc.swift.params2.mapping.ParamsInfo;
import edu.mayo.mprc.unimod.ModSet;
import edu.mayo.mprc.unimod.ModSpecificity;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ResourceUtilities;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.Writer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MascotMappings implements Mappings {
	public static final double PPM_TO_DALTON = 1000.0;
	private final Map<Protease, String> mascotNamesByEnzyme;
	private static final String PEP_TOL_VALUE = "TOL";
	private static final String PEP_TOL_UNIT = "TOLU";
	private static final String FRAG_TOL_VALUE = "ITOL";
	private static final String FRAG_TOL_UNIT = "ITOLU";
	private static final String DATABASE = "DB";
	private static final String VAR_MODS = "IT_MODS";
	private static final String FIXED_MODS = "MODS";
	private static final String ENZYME = "CLE";
	private static final String MISSED_CLEAVAGES = "PFA";
	private static final String INSTRUMENT = "INSTRUMENT";
	private static final HashSet<String> PARSED_PARAMS = new HashSet<String>(Arrays.asList(PEP_TOL_VALUE,
			PEP_TOL_UNIT,
			FRAG_TOL_VALUE,
			FRAG_TOL_UNIT,
			DATABASE,
			VAR_MODS,
			FIXED_MODS,
			ENZYME,
			MISSED_CLEAVAGES,
			INSTRUMENT));

	/**
	 * Mascot supports only limited amount of variable modifications.
	 */
	public static final int MAX_VARIABLE_MODS = 9;

	private Map<String, String> nativeParams = new HashMap<String, String>();
	private static final Pattern COMMA_SPLIT = Pattern.compile(",");

	public MascotMappings(final ParamsInfo info) {
		// params name : mascot name
		final Map<String, String> enzymeNames = new ImmutableMap.Builder<String, String>()
				.put("Trypsin (restrict P)", "Trypsin")
				.put("Arg-C", "Arg-C")
				.put("Asp-N", "Asp-N")
				.put("Asp-N_ambic", "Asp-N_ambic")
				.put("Chymotrypsin", "Chymotrypsin")
				.put("CNBr", "CNBr")
				.put("Formic_acid", "Formic_acid")
				.put("Lys-C (restrict P)", "Lys-C")
				.put("Lys-C (allow P)", "Lys-C/P")
				.put("PepsinA", "PepsinA")
				.put("Tryp-CNBr", "Tryp-CNBr")
				.put("TrypChymo", "TrypChymo")
				.put("Trypsin (allow P)", "Trypsin/P")
				.put("V8-DE", "V8-DE")
				.put("V8-E", "V8-E")
				.put("ChymoAndGluC", "ChymoAndGluC")
				.put("Non-Specific", "None")
				.put("GluC", "GluC")
				.build();

		final Map<String, Protease> allowedH = new HashMap<String, Protease>();

		for (final Protease protease : info.getEnzymeAllowedValues()) {
			allowedH.put(protease.getName(), protease);
		}
		mascotNamesByEnzyme = getNamesByEnzyme(allowedH, enzymeNames);
	}

	private static final Pattern PARAM = Pattern.compile("^[^#]+=.*");
	private static final Pattern COMMENT = Pattern.compile("^\\s*#.*");
	private static final Pattern KEY_VALUE_COMMENT = Pattern.compile("\\s*([^\\s=]+)=([^#]*)(\\s#.*)?");

	@Override
	public Reader baseSettings() {
		return ResourceUtilities.getReader("classpath:edu/mayo/mprc/swift/params/base.mascot.params", getClass());
	}

	@Override
	public void read(final Reader isr) {
		nativeParams = new HashMap<String, String>();
		LineNumberReader br = null;
		try {
			br = new LineNumberReader(isr);
			while (true) {
				final String it = br.readLine();
				if (it == null) {
					break;
				}
				if (it.isEmpty()) {
					break;
				}

				if (PARAM.matcher(it).matches()) {
					final Matcher matcher = KEY_VALUE_COMMENT.matcher(it);
					if (!matcher.matches()) {
						throw new MprcException("Can't understand '" + it + "'");
					}

					final String id = matcher.group(1);
					final String value = matcher.group(2);

					if (PARSED_PARAMS.contains(id)) {
						nativeParams.put(id, value);
					}
				} else if (COMMENT.matcher(it).matches()) {
					ignoreComment();
				} else {
					throw new MprcException("Can't understand '" + it + "'");
				}
			}
		} catch (IOException e) {
			throw new MprcException("Cannot parse mascot parameter file.", e);
		} finally {
			FileUtilities.closeQuietly(br);
		}
	}

	private void ignoreComment() {
		// Comments are ignored, do nothing
	}

	@Override
	public void write(final Reader oldParams, final Writer out) {
		Writer writer = null;
		try {
			writer = out;
			final LineNumberReader br = new LineNumberReader(oldParams);
			while (true) {
				final String it = br.readLine();
				if (it == null) {
					break;
				}
				if (it.isEmpty()) {
					writer.write(it);
					writer.write('\n');
				} else if (PARAM.matcher(it).matches()) {
					writeParam(writer, it);
				} else if (COMMENT.matcher(it).matches()) {
					writer.write(it);
					writer.write('\n');
				} else {
					throw new MprcException("Can't understand '" + it + "'");
				}
			}
		} catch (IOException e) {
			throw new MprcException("Cannot parse mascot parameter file.", e);
		} finally {
			FileUtilities.closeQuietly(writer);
		}
	}

	private void writeParam(final Writer writer, final String it) throws IOException {
		final Matcher matcher = KEY_VALUE_COMMENT.matcher(it);
		if (!matcher.matches()) {
			throw new MprcException("Can't understand '" + it + "'");
		}

		final String id = matcher.group(1);
		final String value = matcher.group(2);

		if (nativeParams.keySet().contains(id)) {
			final String newValue = nativeParams.get(id);
			if (!newValue.equals(value)) {
				writer.write(id + "=" + newValue);
				if (matcher.group(3) != null) {
					writer.write(matcher.group(3));
				}
				writer.write('\n');
			} else {
				writer.write(it);
				writer.write('\n');
			}
		} else {
			writer.write(it);
			writer.write('\n');
		}
	}

	@Override
	public void setPeptideTolerance(final MappingContext context, final Tolerance peptideTolerance) {
		mapToleranceToNative(context, peptideTolerance, PEP_TOL_VALUE, PEP_TOL_UNIT);
	}

	@Override
	public void setFragmentTolerance(final MappingContext context, final Tolerance fragmentTolerance) {
		if (fragmentTolerance.getUnit() == MassUnit.Ppm) {
			final double value = fragmentTolerance.getValue() / PPM_TO_DALTON;
			final Tolerance newTolerance = new Tolerance(value, MassUnit.Da);
			context.reportWarning("Mascot does not support '" + fragmentTolerance.getUnit() + "' fragment tolerances; using " + newTolerance.getValue() + " " + newTolerance.getUnit().getCode() + " instead.");
			mapToleranceToNative(context, newTolerance, FRAG_TOL_VALUE, FRAG_TOL_UNIT);
		} else {
			mapToleranceToNative(context, fragmentTolerance, FRAG_TOL_VALUE, FRAG_TOL_UNIT);
		}
	}

	@Override
	public void setVariableMods(final MappingContext context, final ModSet variableMods) {
		final TreeSet<String> mods = new TreeSet<String>();
		int i = 0;
		final StringBuilder droppedMods = new StringBuilder();
		for (final ModSpecificity ms : variableMods.getModifications()) {
			if (i >= MAX_VARIABLE_MODS) {
				droppedMods.append(ms.toString());
				droppedMods.append(", ");
			} else {
				warnMascotMultipleSites(context, ms, variableMods.getModifications());
				mods.add(ms.toMascotString());
				i++;
			}
		}

		if (droppedMods.length() > 0) {
			droppedMods.setLength(droppedMods.length() - 2);
			context.reportWarning("Mascot supports up to " + MAX_VARIABLE_MODS + " variable modifications; dropping " + droppedMods.toString());
		}

		setNativeMods(context, VAR_MODS, mods);
	}

	@Override
	public void setFixedMods(final MappingContext context, final ModSet fixedMods) {
		final TreeSet<String> mods = new TreeSet<String>();

		// we first loop through the mods and stuff their string reps into a hashset;
		// this eliminates duplicates wrt spec_group
		try {
			for (final ModSpecificity ms : fixedMods.getModifications()) {
				warnMascotMultipleSites(context, ms, fixedMods.getModifications());
				mods.add(ms.toMascotString());
			}
		} catch (Exception t) {
			context.reportError("Problem obtaining mascot fixed modifications", t);
		}

		setNativeMods(context, FIXED_MODS, mods);
	}

	@Override
	public String getNativeParam(final String name) {
		return nativeParams.get(name);
	}

	@Override
	public void setNativeParam(final String name, final String value) {
		nativeParams.put(name, value);
	}

	/**
	 * The short db name matches directly the db name in Mascot.
	 */
	@Override
	public void setSequenceDatabase(final MappingContext context, final String shortName) {
		setNativeParam(DATABASE, shortName);
	}

	@Override
	public void setProtease(final MappingContext context, final Protease protease) {
		final String cle;
		if (!mascotNamesByEnzyme.containsKey(protease)) {
			cle = "Trypsin/P";
			context.reportWarning("Mascot doesn't support " + (protease == null ? "null enzyme" : protease.getName()) + ", using Trypsin (allow P)");
		} else {
			cle = mascotNamesByEnzyme.get(protease);
		}

		setNativeParam(ENZYME, cle);
	}

	@Override
	public void setMissedCleavages(final MappingContext context, final Integer missedCleavages) {
		if (missedCleavages != null) {
			setNativeParam(MISSED_CLEAVAGES, String.valueOf(missedCleavages));
		}
	}

	@Override
	public void setInstrument(final MappingContext context, final Instrument instrument) {
		final String instName = instrument.getMascotName();
		setNativeParam(INSTRUMENT, instName);
	}

	private void setNativeMods(final MappingContext context, final String nativeParamName, final Set<String> mods) {
		final StringBuilder sb = new StringBuilder();

		for (final String mod : mods) {
			if (sb.length() != 0) {
				sb.append(",");
			}
			sb.append(mod);
		}

		if (context.noErrors()) {
			setNativeParam(nativeParamName, sb.toString());
		}

		if (VAR_MODS.equals(nativeParamName)) {
			checkForSameFixAndVariableMods(context);
		}
	}

	private void checkForSameFixAndVariableMods(final MappingContext context) {
		final String fixedMods = getNativeParam(FIXED_MODS);
		final String variableMods = getNativeParam(VAR_MODS);

		if (fixedMods != null && !fixedMods.isEmpty() && variableMods != null && !variableMods.isEmpty()) {
			final String[] fixedModsArr = COMMA_SPLIT.split(fixedMods);
			final String[] variableModsArr = COMMA_SPLIT.split(variableMods);

			StringBuilder repeatMods = null;

			int numRepeatMods = 0;
			for (final String fixedMod : fixedModsArr) {
				for (final String variableMod : variableModsArr) {
					if (fixedMod.trim().equals(variableMod.trim())) {
						numRepeatMods++;
						if (repeatMods == null) {
							repeatMods = new StringBuilder(fixedMod);
						} else {
							repeatMods.append(", ").append(fixedMod);
						}
					}
				}
			}

			if (repeatMods != null) {
				context.reportError((numRepeatMods == 1 ? "Modification" : "Modifications") + " " + repeatMods + " cannot be both fixed and variable", null);
			}
		}
	}

	private void warnMascotMultipleSites(final MappingContext context, final ModSpecificity ms, final Set<ModSpecificity> allSets) {
		if (ms.getSpecificityGroup() != null && ms.groupSpecificities().size() > 1) {
			final StringBuilder specificities = new StringBuilder();
			for (final ModSpecificity modSpecificity : ms.groupSpecificities()) {
				if (!allSets.contains(modSpecificity)) {
					specificities.append(modSpecificity.getSite());
				}
			}
			if (!specificities.toString().isEmpty()) {
				context.reportWarning("Mascot will search additional site (" + specificities.toString() + ") for modification " + ms.toString());
			}
		}
	}

	private void mapToleranceToNative(final MappingContext context, final Tolerance unit, final String tolName, final String tolUnitName) {
		if (!Arrays.asList("ppm", "Da", "mmu").contains(unit.getUnit().getCode())) {
			setNativeParam(tolName, "1");
			setNativeParam(tolUnitName, "Da");
			context.reportWarning("Mascot does not support '" + unit + "' tolerances; using 1 Da instead.");
		} else {
			setNativeParam(tolName, String.valueOf(unit.getValue()));
			setNativeParam(tolUnitName, unit.getUnit().getCode());
		}
	}

	private Map<String, Protease> getEnzymesByName(final Map<String, Protease> allowedHash, final Map<String, String> namesHash) {
		final Map<String, Protease> hash = new HashMap<String, Protease>();
		for (final Map.Entry<String, String> e : namesHash.entrySet()) {
			hash.put(e.getValue(), allowedHash.get(e.getKey()));
		}
		return hash;
	}

	private Map<Protease, String> getNamesByEnzyme(final Map<String, Protease> allowedHash, final Map<String, String> namesHash) {
		final Map<Protease, String> hash = new HashMap<Protease, String>();
		for (final Map.Entry<String, String> e : namesHash.entrySet()) {
			hash.put(allowedHash.get(e.getKey()), e.getValue());
		}
		return hash;
	}
}
