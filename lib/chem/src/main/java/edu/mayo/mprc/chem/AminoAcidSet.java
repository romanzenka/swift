package edu.mayo.mprc.chem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import edu.mayo.mprc.MprcException;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
		aminoAcidBuilder
				.add(new AminoAcid('G', "Gly", "C2H3ON", 57.021464, 57.0519))
				.add(new AminoAcid('A', "Ala", "C3H5ON", 71.037114, 71.0788))
				.add(new AminoAcid('S', "Ser", "C3H5O2N", 87.032028, 87.0782))
				.add(new AminoAcid('P', "Pro", "C5H7ON", 97.052764, 97.1167))
				.add(new AminoAcid('V', "Val", "C5H9ON", 99.068414, 99.1326))
				.add(new AminoAcid('T', "Thr", "C4H7O2N", 101.047678, 101.1051))
				.add(new AminoAcid('C', "Cys", "C3H5ONS", 103.009184, 103.1388))
				.add(new AminoAcid('L', "Leu", "C6H11ON", 113.084064, 113.1594))
				.add(new AminoAcid('I', "Ile", "C6H11ON", 113.084064, 113.1594))
				.add(new AminoAcid('N', "Asn", "C4H6O2N2", 114.042927, 114.1038))
				.add(new AminoAcid('D', "Asp", "C4H5O3N", 115.026943, 115.0886))
				.add(new AminoAcid('K', "Lys", "C6H12ON2", 128.094963, 128.1741))
				.add(new AminoAcid('Q', "Gln", "C5H8O2N2", 128.058577, 128.1307))
				.add(new AminoAcid('E', "Glu", "C5H7O3N", 129.042593, 129.1155))
				.add(new AminoAcid('M', "Met", "C5H9ONS", 131.040485, 131.1926))
				.add(new AminoAcid('H', "His", "C6H7ON3", 137.058912, 137.1411))
				.add(new AminoAcid('F', "Phe", "C9H9ON", 147.068414, 147.1766))
				.add(new AminoAcid('R', "Arg", "C6H12ON4", 156.101111, 156.1875))
				.add(new AminoAcid('Y', "Tyr", "C9H9O2N", 163.063328, 163.1760))
				.add(new AminoAcid('W', "Trp", "C11H10ON2", 186.079313, 186.2132))
				.add(new AminoAcid('B', "Asx", "C4H?O?N?", 0.0, 0.0))
				.add(new AminoAcid('Z', "Glx", "C5H?N?O?", 0.0, 0.0))
				.add(new AminoAcid('X', "Xaa", "?", 0.0, 0.0));

		final ImmutableList<AminoAcid> aminoAcidList = aminoAcidBuilder.build();

		data = Maps.uniqueIndex(aminoAcidList, AminoAcid.GET_CODE);

		monoisotopicMassByCode = new double[26];
		Arrays.fill(monoisotopicMassByCode, 0.0);
		for (final AminoAcid acid : data.values()) {
			final int index = codeToIndex(acid.getCode());
			monoisotopicMassByCode[index] = acid.getMonoisotopicMass();
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
