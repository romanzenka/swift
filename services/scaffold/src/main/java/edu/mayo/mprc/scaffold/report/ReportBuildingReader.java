package edu.mayo.mprc.scaffold.report;

import com.google.common.collect.Lists;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldReportReader;

import java.util.*;

/**
 * @author Roman Zenka
 */
final class ReportBuildingReader extends ScaffoldReportReader {
	static final String[] PEPTIDE_COLUMNS = {
			BIOLOGICAL_SAMPLE_NAME,
			PROTEIN_NAME,
			PROTEIN_ACCESSION_NUMBERS,
			PROTEIN_MOLECULAR_WEIGHT_DA,
			PROTEIN_ID_PROBABILITY,
			NUMBER_OF_UNIQUE_PEPTIDES,
			PERCENTAGE_SEQUENCE_COVERAGE,
			PEPTIDE_SEQUENCE,
			FIXED_MODIFICATIONS,
			VARIABLE_MODIFICATIONS
	};
	static final String PEPTIDE_GROUP_BY = ScaffoldReportReader.EXPERIMENT_NAME;

	static final String[] PROTEIN_COLUMNS = {
			BIOLOGICAL_SAMPLE_NAME,
			PROTEIN_NAME,
			PROTEIN_ACCESSION_NUMBERS,
			PROTEIN_MOLECULAR_WEIGHT_DA,
			PROTEIN_ID_PROBABILITY,
			NUMBER_OF_UNIQUE_PEPTIDES,
			PERCENTAGE_SEQUENCE_COVERAGE
	};
	static final String PROTEIN_GROUP_BY = ScaffoldReportReader.EXPERIMENT_NAME;

	private Map<String, Integer> columnMap;
	private List<String> columnNames;
	private int[] peptideColumns;
	private int[] proteinColumns;

	private int peptideGroupBy;
	private int proteinGroupBy;

	private static final Comparator<String[]> PEPTIDE_COMPARATOR = new LineComparator(
			PEPTIDE_COLUMNS,
			new String[]{BIOLOGICAL_SAMPLE_NAME /* group by */, NUMBER_OF_UNIQUE_PEPTIDES, PROTEIN_NAME, PEPTIDE_SEQUENCE, FIXED_MODIFICATIONS, VARIABLE_MODIFICATIONS},
			new String[]{"a", "di", "a", "a", "a", "a"});
	private static final Comparator<String[]> PROTEIN_COMPARATOR = new LineComparator(
			PROTEIN_COLUMNS,
			new String[]{BIOLOGICAL_SAMPLE_NAME /* group by */, NUMBER_OF_UNIQUE_PEPTIDES, PROTEIN_NAME},
			new String[]{"a", "di", "a"});

	private TreeSet<String[]> peptides;
	private TreeSet<String[]> proteins;

	@Override
	public boolean processMetadata(final String key, final String value) {
		return true;
	}

	@Override
	public boolean processHeader(final String line) {
		columnMap = buildColumnMap(line);
		columnNames = Lists.newArrayList(getColumnNames(line));
		initializeCurrentLine(columnMap);

		peptideColumns = translateColumnNames(PEPTIDE_COLUMNS);
		proteinColumns = translateColumnNames(PROTEIN_COLUMNS);

		peptideGroupBy = getColumn(columnMap, PEPTIDE_GROUP_BY);
		proteinGroupBy = getColumn(columnMap, PROTEIN_GROUP_BY);

		peptides = new TreeSet<String[]>(PEPTIDE_COMPARATOR);
		proteins = new TreeSet<String[]>(PROTEIN_COMPARATOR);

		return true;
	}

	private int[] translateColumnNames(final String[] columnNames) {
		final int[] result = new int[columnNames.length];
		for (int i = 0; i < columnNames.length; i++) {
			result[i] = getColumn(columnMap, columnNames[i]);
		}
		return result;
	}

	@Override
	public boolean processRow(final String line) {
		fillCurrentLine(line);

		final String[] peptideRow = fillArrayWithData(peptideColumns);
		final String[] proteinRow = fillArrayWithData(proteinColumns);

		peptides.add(peptideRow);
		proteins.add(proteinRow);

		return true;
	}

	private String[] fillArrayWithData(final int[] columns) {
		final int length = columns.length;
		final String[] row = new String[length];
		for (int i = 0; i < length; i++) {
			row[i] = currentLine[columns[i]];
		}
		return row;
	}

	private String getReport(final Collection<String[]> rows, final int[] columnPositions, final int groupByIndex, final boolean includeHeader) {
		final StringBuilder result = new StringBuilder(rows.size() * 50);
		final int length = columnPositions.length;
		if (includeHeader) {
			for (int i = 0; i < length; i++) {
				if (i > 0) {
					result.append('\t');
				}
				result.append(columnNames.get(columnPositions[i]));
			}
			result.append('\n');
		}

		if (rows.size() > 0) {
			String prevGroupBy = rows.iterator().next()[groupByIndex];
			for (final String[] row : rows) {
				final String groupBy = row[groupByIndex];
				if (!groupBy.equals(prevGroupBy)) {
					result.append('\n'); // Newline to separate the groups
				}
				prevGroupBy = groupBy;

				for (int i = 0; i < row.length; i++) {
					if (i > 0) {
						result.append('\t');
					}
					result.append(row[i]);
				}
				result.append('\n');
			}
		}
		return result.toString();
	}

	public String getProteinReport(final boolean includeHeader) {
		return getReport(proteins, proteinColumns, proteinGroupBy, includeHeader);
	}

	public String getPeptideReport(final boolean includeHeader) {
		return getReport(peptides, peptideColumns, peptideGroupBy, includeHeader);
	}
}
