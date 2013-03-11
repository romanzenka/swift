package edu.mayo.mprc.chem;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ResourceUtilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * Class represents the whole set of defined amino acids.
 * <p/>
 * Source for default monoisotopic masses:
 * Protein Calculator 2.0.2.0614, Thermo Scientific
 * <p/>
 * Source for default average masses:
 * http://education.expasy.org/student_projects/isotopident/htdocs/aa-list.html
 */
public final class AminoAcidSet {

	private final double[] monoisotopicMassByCode;
	private final Map<Character, AminoAcid> data;

	private static final double MONOISOTOPIC_WATER_MASS = 18.010565;
	public static final AminoAcidSet DEFAULT = new AminoAcidSet();
	private static final int AA_REPORT_SIZE = 5000;

	public AminoAcidSet() {
		final ImmutableList.Builder<AminoAcid> aminoAcidBuilder = new ImmutableList.Builder<AminoAcid>();
		aminoAcidBuilder.addAll(loadDefaultAminoAcids());

		final ImmutableList<AminoAcid> aminoAcidList = aminoAcidBuilder.build();

		data = Maps.uniqueIndex(aminoAcidList, AminoAcid.GET_CODE);

		monoisotopicMassByCode = new double[26];
		Arrays.fill(monoisotopicMassByCode, 0.0);
		for (final AminoAcid acid : data.values()) {
			final int index = codeToIndex(acid.getCode());
			monoisotopicMassByCode[index] = acid.getMonoisotopicMass();
		}
	}

	private List<AminoAcid> loadDefaultAminoAcids() {
		final List<AminoAcid> result = new ArrayList<AminoAcid>(26);
		final BufferedReader reader = new BufferedReader(ResourceUtilities.getReader("classpath:edu/mayo/mprc/chem/amino-acid.tsv", AminoAcidSet.class));
		try {
			final Splitter splitter = Splitter.on("\t").trimResults();
			while (true) {
				final String line = reader.readLine();
				if (line == null) {
					break;
				}
				if (line.trim().isEmpty()) {
					continue;
				}
				final Iterator<String> iterator = splitter.split(line).iterator();

				final String letterString = iterator.next();
				if (letterString == null || letterString.length() != 1) {
					throw new MprcException("Wrong AA code " + letterString);
				}
				final char code = letterString.charAt(0);

				final String code3 = iterator.next();
				if (code3 == null || code3.length() != 3) {
					throw new MprcException("Wrong three code amino acid code [" + code3 + "]");
				}

				final String formula = iterator.next();
				if (formula == null) {
					throw new MprcException("Unspecified amino acid formula [" + formula + "]");
				}

				final String monoMassString = iterator.next();
				final double monoisotopicMass;
				try {
					monoisotopicMass = Double.parseDouble(monoMassString);
				} catch (NumberFormatException e) {
					throw new MprcException("Cannot parse monoisotopic mass [" + monoMassString + "]", e);
				}

				final String averageMassString = iterator.next();
				final double averageMass;
				try {
					averageMass = Double.parseDouble(averageMassString);
				} catch (NumberFormatException e) {
					throw new MprcException("Cannot parse average mass [" + monoMassString + "]", e);
				}

				result.add(new AminoAcid(code, code3, formula, monoisotopicMass, averageMass));
			}
			return result;
		} catch (IOException e) {
			throw new MprcException("Could not read the default amino acids", e);
		} finally {
			FileUtilities.closeQuietly(reader);
		}
	}

	private int codeToIndex(final char code) {
		final int index = (int) code - (int) 'A';
		if (index < 0 || index >= 26) {
			throw new MprcException("Unsupported amino acid code " + code);
		}
		return index;
	}

	/**
	 * Return amino acid corresponding to given single letter code.
	 *
	 * @param code One letter code to look up.
	 * @return Null if such amino acid does not exist.
	 */
	public AminoAcid getForSingleLetterCode(final CharSequence code) {
		if (code == null || code.length() != 1) {
			return null;
		}
		final char oneLetterCode = code.charAt(0);
		return data.get(oneLetterCode);
	}

	/**
	 * Returns monoisotopic mass of given peptide. The mass includes the extra H and OH at the terminals.
	 */
	public double getMonoisotopicMass(final CharSequence peptideSequence) {
		double totalMass = MONOISOTOPIC_WATER_MASS;
		for (int i = 0; i < peptideSequence.length(); i++) {
			final int index = codeToIndex(peptideSequence.charAt(i));
			totalMass += monoisotopicMassByCode[index];
		}
		return totalMass;
	}

	/**
	 * @return Set of all amino acid codes.
	 */
	public Set<String> getCodes() {
		final TreeSet<String> names = new TreeSet<String>();

		for (final AminoAcid aminoAcid : data.values()) {
			names.add(String.valueOf(aminoAcid.getCode()));
		}

		return names;
	}

	/**
	 * @return An HTML table listing the amino acids.
	 */
	public String report() {
		final StringBuilder result = new StringBuilder(AA_REPORT_SIZE);
		result.append("<table>\n<tr><th>Code</th><th>Three letter code</th><th>Monoisotopic mass</th><th>Average mass</th><th>Formula</th></tr>\n");
		for (final AminoAcid acid : data.values()) {
			result
					.append("<tr><td>")
					.append(acid.getCode()).append("</td><td>")
					.append(acid.getCode3()).append("</td><td>")
					.append(acid.getMonoisotopicMass()).append("</td><td>")
					.append(acid.getAverageMass()).append("</td><td>")
					.append(acid.getFormula()).append("</td></tr>\n");
		}
		result.append("</table>");
		return result.toString();
	}
}
