package edu.mayo.mprc.sequest;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * this class will handle all of the conversion from a sequest.params file to a makedb.params file
 */
@Component("sequestToMakeDbConverter")
public final class SequestToMakeDBConverter {
	private static final Logger LOGGER = Logger.getLogger(SequestToMakeDBConverter.class);

	private StringBuilder sb = new StringBuilder();
	private Pattern regexp = null;
	private Map<String, List<Mapper>> mappers = new HashMap<String, List<Mapper>>();
	private List<Mapper> mappersInOrder = new ArrayList<Mapper>();
	private Map<Integer, String> groupMap = new HashMap<Integer, String>();

	public SequestToMakeDBConverter() {
		// NOTE: the Mappers are specified in the order their corresponding keys should appear in the output makedb.params
		add(new DirectMapper("first_database_name", "database_name"))
				.add(new ConstantMapper("sort_directory", "."))
				.add(new ConstantMapper("sort_program", "sort.exe"))
				.add(new DirectMapper("enzyme_info"))
				.add(new ConstantMapper("protein_or_nucleotide_dbase", "0"))
				.add(new ConstantMapper("nucleotide_reading_frames", "0"))
				.add(new ConstantMapper("use_mono/avg_masses", "1"))
				.add(new Mapper("digest_mass_range") {
					private final Pattern p = Pattern.compile("(\\S+)\\s+(\\S+)");

					@Override
					public Map<String, String> grab(Map<String, String> values, final String inputKey, final String inputValue) {
						final Matcher m = p.matcher(inputValue);
						if (!m.matches()) {
							throw new MprcException("Can't understand digest_mass_range '" + inputValue + "'");
						}
						values = put(values, "min_peptide_mass", m.group(1));
						return put(values, "max_peptide_mass", m.group(2));
					}

					@Override
					public void output(final Map<String, String> values, final StringBuilder newPic) {
						for (final Map.Entry<String, String> entry : values.entrySet()) {
							add(entry.getKey(), entry.getValue(), newPic);
						}
					}
				})
				.add(new ConstantMapper("min_peptide_size", "4"))
				.add(new ConstantMapper("max_peptide_size", "100"))
				.add(new DirectMapper("max_num_internal_cleavage_sites"))
				.add(new DirectMapper("max_num_differential_per_peptide"))
				.add(new ConstantMapper("restrict_amino_acid", ""))
				.add(new DirectMapper("diff_search_options"))
				.add(new DirectMapper("term_diff_search_options"))

				.add(new DirectMapper("add_.*(?:(?<!protein))"))

						// ignore these keys when they appear in sequest.params...
				.add(new NullMapper("add_.*protein"))
				.add(new NullMapper("second_database_name"))
				.add(new NullMapper("peptide_mass_tolerance"))
				.add(new NullMapper("peptide_mass_units"))
				.add(new NullMapper("ion_series"))
				.add(new NullMapper("fragment_ion_tolerance"))
				.add(new NullMapper("num_output_lines"))
				.add(new NullMapper("num_results"))
				.add(new NullMapper("num_description_lines"))
				.add(new NullMapper("show_fragment_ions"))
				.add(new NullMapper("print_duplicate_references"))
				.add(new NullMapper("mass_type_parent"))
				.add(new NullMapper("mass_type_fragment"))
				.add(new NullMapper("normalize_xcorr"))
				.add(new NullMapper("remove_precursor_peak"))
				.add(new NullMapper("ion_cutoff_percentage"))
				.add(new NullMapper("protein_mass_filter"))
				.add(new NullMapper("match_peak_count"))
				.add(new NullMapper("max_num_internal_cleavage_sites"))
				.add(new NullMapper("protein_mass_filter"))
				.add(new NullMapper("match_peak_count"))
				.add(new NullMapper("match_peak_allowed_error"))
				.add(new NullMapper("match_peak_tolerance"))
				.add(new NullMapper("partial_sequence"))
				.add(new NullMapper("sequence_header_filter"))
				.add(new NullMapper("nucleotide_reading_frame"));
	}

	private abstract static class Mapper {
		Mapper(final String regexp) {
			this.regexp = regexp;
		}

		private String regexp;

		public String getRegexp() {
			return regexp;
		}

		/**
		 * Given an input key and value, populate the values hash that will be passed to output.  One hash will
		 * be created for each Mapper; this same hash will be passed to all invocations of grab for a given
		 * mapper for a given maked.params file.
		 * <p/>
		 * This method will be invoked each time a key in the sequest.params file matches the regular expression
		 * returned yy {@link @getRegexp()}, in the order the keys appear in sequest.params.
		 *
		 * @return values
		 */
		public abstract Map<String, String> grab(Map<String, String> values, String inputKey, String inputValue);

		/**
		 * Given the values hash built by {@link #grab(java.util.Map, String, String)}, output the values into the new ParamInstanceCollection.
		 * This method will be called once for each Mapper, in the order that the Mappers are added to the SequestToMakeDBConverter.
		 */
		public abstract void output(Map<String, String> values, StringBuilder newPic);

		protected void add(final String key, final String value, final StringBuilder pic) {
			pic.append(key.trim()).append(" = ").append(value.trim()).append("\n");
		}

		protected Map<String, String> put(Map<String, String> values, final String key, final String value) {
			if (values == null) {
				values = new LinkedHashMap<String, String>();
			}
			values.put(key, value);
			return values;
		}
	}

	/**
	 * Silently ignores the input Param matching regexp.
	 */
	private static final class NullMapper extends Mapper {
		NullMapper(final String regexp) {
			super(regexp);
		}

		@Override
		public Map<String, String> grab(final Map<String, String> values, final String inputKey, final String inputValue) {
			return values;
		}

		@Override
		public void output(final Map<String, String> values, final StringBuilder newPic) { /* nothing */}
	}

	/**
	 * Adds a Param with a constant value.
	 */
	private static final class ConstantMapper extends Mapper {
		private String outputKey;
		private String value;

		ConstantMapper(final String outputKey, final String value) {
			super(null);
			this.outputKey = outputKey;
			this.value = value;
		}

		@Override
		public Map<String, String> grab(final Map<String, String> values, final String inputKey, final String inputValue) {
			// never called because has no regex
			return values;
		}

		@Override
		public void output(final Map<String, String> values, final StringBuilder newPic) {
			add(outputKey, value, newPic);
		}

	}

	/* Maps the save value from inputKey to outputKey.*/

	private static final class DirectMapper extends Mapper {
		DirectMapper(final String sameKeyRegexp) {
			super(sameKeyRegexp);
			outputKey = null;
		}

		DirectMapper(final String inputKeyRegexp, final String newKey) {
			super(inputKeyRegexp);
			outputKey = newKey;
		}

		private String outputKey;

		@Override
		public Map<String, String> grab(final Map<String, String> values, final String inputKey, final String inputValue) {
			return put(values, outputKey == null ? inputKey : outputKey, inputValue);
		}

		@Override
		public void output(final Map<String, String> values, final StringBuilder newPic) {
			for (final Map.Entry<String, String> e : values.entrySet()) {
				add(e.getKey(), e.getValue(), newPic);
			}
		}
	}

	public SequestToMakeDBConverter add(final Mapper m) {
		final String re = m.getRegexp();
		if (re != null && !re.isEmpty()) {
			if (re.matches("\\((?!\\?)")) {
				throw new MprcException("Can't handle regexps with capturing groups yet");
			}
			if (!mappers.containsKey(re)) {
				if (sb.length() != 0) {
					sb.append("|");
				}
				sb.append("(").append(re).append(")");
				groupMap.put(groupMap.size() + 1, re);
				final List<Mapper> l = new ArrayList<Mapper>();
				l.add(m);
				mappers.put(re, l);
				regexp = null;
			} else {
				mappers.get(re).add(m);
			}
		}
		mappersInOrder.add(m);
		return this;
	}

	private void map(final SequestMappings oldPic, final StringBuilder newPic) {
		final Set<String> matched = new HashSet<String>();
		final Map<Mapper, Map<String, String>> valuesHashes = new HashMap<Mapper, Map<String, String>>();
		if (regexp == null) {
			if (sb.length() == 0) {
				throw new MprcException("No Mapper regexps found");
			}
			regexp = Pattern.compile(sb.toString());
			LOGGER.info(regexp.pattern());
		}
		boolean unmatched = false;
		for (final Map.Entry<String, String> p : oldPic.getNativeParams().entrySet()) {
			final Matcher m = regexp.matcher(p.getKey());
			if (m.matches()) {
				int i;
				for (i = 1; i < m.groupCount(); ++i) {
					if (m.group(i) != null) {
						break;
					}
				}
				if (i == m.groupCount() + 1) {
					throw new MprcException("No group!?");
				}
				final String key = groupMap.get(i);
				matched.add(key);
				final List<Mapper> mm = mappers.get(key);
				for (final Mapper mapper : mm) {
					valuesHashes.put(mapper, mapper.grab(valuesHashes.get(mapper), p.getKey(), p.getValue()));
				}
			} else {
				LOGGER.error("No sequest -> makedb mapping for " + p.getKey());
				unmatched = true;
			}
		}

		for (final Map.Entry<String, List<Mapper>> k : mappers.entrySet()) {
			if (!matched.contains(k.getKey())) {
				boolean missedInterestingMapper = false;
				for (final Mapper m : k.getValue()) {
					if (!(m instanceof NullMapper)) {
						missedInterestingMapper = true;
					}
				}
				if (missedInterestingMapper) {
					LOGGER.error("Sequest->Makedb Mapper " + k + " didn't match");
					unmatched = true;
				}
			}
		}
		if (unmatched) {
			throw new MprcException("Sequest -> Makedb mapping is incorrect");
		}

		for (final Mapper mapper : mappersInOrder) {
			mapper.output(valuesHashes.get(mapper), newPic);
		}
	}

	/**
	 * creates a Sequest PIC to a MakeDB param file
	 *
	 * @param sequestParams
	 * @return MakeDB param file contents
	 */
	public StringBuilder convertSequestToMakeDB(final SequestMappings sequestParams, final File fastaFile) {
		SequestMappings pic = null;
		try {
			pic = (SequestMappings) sequestParams.clone();
		} catch (CloneNotSupportedException e) {
			throw new MprcException(e);
		}
		pic.setNativeParam("first_database_name", fastaFile.getAbsolutePath());
		final StringBuilder builder = new StringBuilder();
		builder.append("[MAKEDB]\n");
		map(pic, builder);
		return builder;
	}

	public StringBuilder convertSequestParamsFileIntoMakeDBPIC(final File sequestParamsFile, final File fastaFile, final SequestMappingFactory factory) throws IOException {
		FileReader reader = null;
		try {
			reader = new FileReader(sequestParamsFile);
			final SequestMappings sequestPIC = (SequestMappings) factory.createMapping();
			sequestPIC.read(reader);
			return convertSequestToMakeDB(sequestPIC, fastaFile);
		} finally {
			FileUtilities.closeQuietly(reader);
		}
	}

	public void writeMakedbParams(final String fileContents, final File makeDbDest) throws IOException {
		FileUtilities.writeStringToFile(makeDbDest, fileContents, false);
	}
}
