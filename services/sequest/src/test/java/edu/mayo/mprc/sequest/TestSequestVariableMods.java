package edu.mayo.mprc.sequest;

import edu.mayo.mprc.swift.params2.ParamName;
import edu.mayo.mprc.swift.params2.mapping.MockParamsInfo;
import edu.mayo.mprc.swift.params2.mapping.TestMappingContextBase;
import edu.mayo.mprc.unimod.ModSet;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Check that sequest is mapping mods properly.
 *
 * @author Roman Zenka
 */
public final class TestSequestVariableMods {
	private static final String ACETYL_MASS = "42.010565";
	private static final String AMIDATED_MASS = "-0.984016";
	private SequestMappings sequestMappings;
	private SequestContext context;
	private ModSet variableMods;

	/**
	 * Setup the environment for the test.
	 */
	@BeforeClass
	public void setup() {
		final SequestMappingFactory mappingFactory = new SequestMappingFactory();
		sequestMappings = (SequestMappings) mappingFactory.createMapping();
		context = new SequestContext();
	}

	/**
	 * Reset the mapping context and mappings for the next test.
	 */
	@BeforeMethod
	public void reset() {
		context.setExpectedWarnings();
		resetMappings();
		variableMods = new ModSet();
	}

	/**
	 * Take three simple mods, set them to Sequest, check everything is ok.
	 */
	@Test
	public void shouldMapSimpleCase() {
		Assert.assertEquals(getVariableMods(), "15.99492 M 57.02146 C 0.000000 X 0.000000 T 0.000000 Y 0.000000 X");

		addMods(variableMods, "Diisopropylphosphate(S)", "GluGlu(E)", "Oxidation(P)");

		sequestMappings.setVariableMods(context, variableMods);

		Assert.assertEquals(getVariableMods(), "164.060231 S 258.085186 E 15.994915 P 0.0000 X 0.0000 X 0.0000 X");
	}

	/**
	 * Take seven phosphorylation mods, see that we get a warning + only six get mapped.
	 */
	@Test
	public void skipSeventhMod() {
		addMods(variableMods, "Phospho(C)", "Phospho(D)", "Phospho(H)", "Phospho(R)", "Phospho(S)", "Phospho(T)", "Phospho(Y)");

		// Last mod (lexicographically) will get skipped
		context.setExpectedWarnings("Sequest supports up to 6 variable modifications, skipping Phospho (Y)");
		sequestMappings.setVariableMods(context, variableMods);

		Assert.assertEquals(getVariableMods(), "79.966331 C 79.966331 D 79.966331 H 79.966331 R 79.966331 S 79.966331 T");
	}

	/**
	 * Take eight mods, make sure two last get skipped
	 */
	@Test
	public void skipTwoLastMods() {
		addMods(variableMods, "Oxidation(M)", "Phospho(C)", "Phospho(D)", "Phospho(H)", "Phospho(R)", "Phospho(S)", "Phospho(T)", "Phospho(Y)");

		// Last mod (lexicographically) will get skipped
		context.setExpectedWarnings("Sequest supports up to 6 variable modifications, skipping Phospho (T), Phospho (Y)");
		sequestMappings.setVariableMods(context, variableMods);

		Assert.assertEquals(getVariableMods(), "15.994915 M 79.966331 C 79.966331 D 79.966331 H 79.966331 R 79.966331 S");
	}

	/**
	 * Try C and N-terminal mods for both protein and peptides
	 */
	@Test
	public void proteinPeptideTerminalMods() {
		addMods(variableMods,
				"Acetyl (Protein N-term)",
				"Amidated (Protein C-term)",
				"Cation:Na (C-term)",
				"Deamidated (Protein N-term F)",
				"Formyl (N-term)");

		context.setExpectedWarnings(
				"Sequest does not support variable modification with specific site 'F' limited to Nterm, skipping Deamidated (Protein N-term F)",
				"Sequest does not support multiple variable modifications at N-terminal, skipping Formyl (N-term)",
				"Sequest does not support multiple variable modifications at C-terminal, skipping Cation:Na (C-term)",
				"Sequest does not support variable modifications specific only to protein terminus. These mods will be used for peptide terminii as well."
		);
		sequestMappings.setVariableMods(context, variableMods);

		// These must not influence the variable mods
		Assert.assertEquals(getVariableMods(), "0.0000 X 0.0000 X 0.0000 X 0.0000 X 0.0000 X 0.0000 X");
		Assert.assertEquals(getProteinTermVarMods(), AMIDATED_MASS + " " + ACETYL_MASS); // Amidated on C, acetyl on N
	}

	private void resetMappings() {
		sequestMappings.read(sequestMappings.baseSettings());
	}

	/**
	 * @param modSet List of mods to add modifications to.
	 * @param mods   Mascot names of mods to add in "mod_name(residue)" format.
	 */
	public void addMods(final ModSet modSet, final String... mods) {
		for (final String mod : mods) {
			modSet.addAll(context.getAbstractParamsInfo().getUnimod().getSpecificitiesByMascotName(mod));
		}
	}

	private String getVariableMods() {
		return sequestMappings.getNativeParam(SequestMappings.VAR_MODS);
	}

	private String getProteinTermVarMods() {
		return sequestMappings.getNativeParam(SequestMappings.VAR_MODS_TERMINUS);
	}


	private static final class SequestContext extends TestMappingContextBase {
		private String[] expectedWarnings;
		int index;

		public void setExpectedWarnings(final String... expectedWarnings) {
			this.expectedWarnings = expectedWarnings;
			index = 0;
		}

		/**
		 * Create basic context with mocked parameter info.
		 */
		SequestContext() {
			super(new MockParamsInfo());
		}

		@Override
		public void reportWarning(final String message, ParamName paramName) {
			Assert.assertEquals(message, expectedWarnings[index], "Got unexpected warning");
			index++;
		}
	}
}
