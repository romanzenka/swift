package edu.mayo.mprc.scaffold.report;

import com.google.common.collect.Lists;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldReportReader;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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
			PEPTIDE_SEQUENCE
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

	private LineComparator peptideComparator;
	private LineComparator proteinComparator;

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

		peptideComparator = new LineComparator(
				new String[]{EXPERIMENT_NAME /* group by */, PROTEIN_ACCESSION_NUMBERS, BIOLOGICAL_SAMPLE_NAME, PROTEIN_MOLECULAR_WEIGHT_DA},
				new String[]{"a", "di", "a", "a"},
				columnMap);
		proteinComparator = new LineComparator(
				new String[]{EXPERIMENT_NAME /* group by */, PROTEIN_ACCESSION_NUMBERS, BIOLOGICAL_SAMPLE_NAME},
				new String[]{"a", "di", "a"},
				columnMap);

		peptides = new TreeSet<String[]>(peptideComparator);
		proteins = new TreeSet<String[]>(proteinComparator);

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
