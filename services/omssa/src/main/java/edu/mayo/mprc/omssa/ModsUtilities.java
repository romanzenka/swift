package edu.mayo.mprc.omssa;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import edu.mayo.mprc.unimod.ModSpecificity;

import java.util.HashMap;
import java.util.Map;


/**
 * this class contains hashes that can be moved to configuration. It also contains some modification manipulation/conversion code
 */
final class ModsUtilities {
	private BiMap<String/*modaa*/, String/*0*/> modTypeLookup;
	private Map<String/*119*/, String/*usermod1*/> modValueLookup;

	public static final double MOD_MASS_TOL = 0.001;

	/**
	 * Null Constructor
	 */
	ModsUtilities() {
		modTypeLookup = new ImmutableBiMap.Builder<String, String>()
				.put("modaa", "0")
				.put("modn", "1")
				.put("modnaa", "2")
				.put("modc", "3")
				.put("modcaa", "4")
				.put("modnp", "5")
				.put("modnpaa", "6")
				.put("modcp", "7")
				.put("modcpaa", "8")
				.put("modmax", "9")
				.build();

		modValueLookup = new HashMap<String, String>();
		int userModsSoFar = 0;
		for (int i = 119; i <= 128; i++) {
			userModsSoFar++;
			modValueLookup.put(String.valueOf(i), "usermod" + userModsSoFar);
		}
		for (int i = 142; i <= 161; i++) {
			userModsSoFar++;
			modValueLookup.put(String.valueOf(i), "usermod" + userModsSoFar);
		}
	}

	public BiMap<String, String> getModTypeLookup() {
		return modTypeLookup;
	}

	/**
	 * This method will find the mod type as specified in the OMSSA documentation
	 * <p/>
	 * modaa	-  at particular amino acids
	 * modn	-  at the N terminus of a protein
	 * modnaa	-  at the N terminus of a protein at particular amino acids
	 * modc	-  at the C terminus of a protein
	 * modcaa	-  at the C terminus of a protein at particular amino acids
	 * modnp	-  at the N terminus of a peptide
	 * modnpaa	-  at the N terminus of a peptide at particular amino acids
	 * modcp	-  at the C terminus of a peptide
	 * modcpaa	-  at the C terminus of a peptide at particular amino acids
	 */
	public static String findModType(final ModSpecificity spec) {
		if (spec.isPositionAnywhere()) {
			return "modaa";
		}

		final StringBuilder sb = new StringBuilder("mod");

		if (spec.isPositionNTerminus()) {
			sb.append("n");
		} else if (spec.isPositionCTerminus()) {
			sb.append("c");
		}

		// P - peptide only
		if (!spec.isPositionProteinSpecific()) {
			sb.append("p");
		}

		if (spec.isSiteSpecificAminoAcid()) {
			sb.append("aa");
		}

		return sb.toString();
	}

	public Map<String, String> getModValueLookup() {
		return modValueLookup;
	}
}
