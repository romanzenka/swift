package edu.mayo.mprc.comet;

import com.google.common.base.Joiner;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.chem.AminoAcid;
import edu.mayo.mprc.chem.AminoAcidSet;
import edu.mayo.mprc.swift.params2.*;
import edu.mayo.mprc.swift.params2.mapping.Mappings;
import edu.mayo.mprc.swift.params2.mapping.MockParamsInfo;
import edu.mayo.mprc.swift.params2.mapping.TestMappingContextBase;
import edu.mayo.mprc.unimod.*;
import edu.mayo.mprc.utilities.ResourceUtilities;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Roman Zenka
 */
public final class TestCometMappings {
	private CometMappingFactory factory = new CometMappingFactory();
	private Mappings mappings;
	private CometContext context;

	@BeforeMethod
	public void setup() {
		mappings = factory.createMapping();
		context = new CometContext();
	}


	private void assertParam(String param, String value) {
		Assert.assertEquals(mappings.getNativeParam(param), value, "Value for " + param + " does not match");
	}

	@Test
	public void shouldSupportPeptideTolerance() {
		mappings.read(mappings.baseSettings());

		// 0.3 Da
		Tolerance da = new Tolerance(0.3, MassUnit.Da);
		mappings.setPeptideTolerance(context, da);

		assertParam("peptide_mass_tolerance", "0.3");
		assertParam("peptide_mass_units", "0");

		// 12 ppm
		Tolerance ppm = new Tolerance(12, MassUnit.Ppm);
		mappings.setPeptideTolerance(context, ppm);

		assertParam("peptide_mass_tolerance", "12.0");
		assertParam("peptide_mass_units", "2");
	}

	@Test
	public void shouldSupportFragmentTolerance() {
		context.setExpectWarnings(new String[]{"does not support ppm, using"});

		mappings.read(mappings.baseSettings());

		Tolerance ppm = new Tolerance(10, MassUnit.Ppm);
		mappings.setFragmentTolerance(context, ppm);

		assertParam("fragment_bin_tol", "0.01"); // 10 ppm at 1000
		assertParam("fragment_bin_offset", "0.0"); // Offset == 0 for anything under 0.8 dalton, otherwise 0.4

		Tolerance da = new Tolerance(1, MassUnit.Da);
		mappings.setFragmentTolerance(context, da);

		assertParam("fragment_bin_tol", "1.0"); // 1 dalton bin
		assertParam("fragment_bin_offset", "0.4"); // Offset is 0.4 (for no particular reason)
	}

	@Test
	public void shouldSupportFixedMods() {
		mappings.read(mappings.baseSettings());

		ModSet fixedMods = new ModSet();
		mappings.setFixedMods(context, fixedMods);

		assertParam("add_C_cysteine", "0.0");

		// Carbamidomethylation of cysteine
		// Carbamidomethyl		57.021464	57.0513	Carboxyamidomethylation	H(3) C(2) N O	*	Nterm	false	Artefact	true	3
		fixedMods.add(getCarbC());

		mappings.setFixedMods(context, fixedMods);
		assertParam("add_C_cysteine", "57.021464");

		// Back to 0
		fixedMods = new ModSet();
		mappings.setFixedMods(context, fixedMods);
		assertParam("add_C_cysteine", "0.0");

		// Protein N-term mod
		SpecificityBuilder b;
		Mod mod;
		b = new SpecificityBuilder(null, Terminus.Nterm, true, false, "", 0);
		mod = new Mod("Acetyl", "Acetylation", 2, 42.010565, 42.0367, "H(2) C(2) O", null, b);
		fixedMods = new ModSet();
		fixedMods.add(b.build(mod));
		mappings.setFixedMods(context, fixedMods);
		assertParam("add_Nterm_protein", "42.010565");

		// Peptide C-term mod
		b = new SpecificityBuilder(null, Terminus.Cterm, false, false, "", 0);
		mod = new Mod("Acetyl", "Acetylation", 2, 42.010565, 42.0367, "H(2) C(2) O", null, b);
		fixedMods = new ModSet();
		fixedMods.add(b.build(mod));
		mappings.setFixedMods(context, fixedMods);
		assertParam("add_Cterm_peptide", "42.010565");

		// Protein N-term mod + specific site - G - should not be supported
		context.setExpectWarnings(new String[]{"Comet does not support modification with position 'Nterm' and site 'G', dropping Acetyl (Protein N-term G)"});
		b = new SpecificityBuilder(AminoAcidSet.DEFAULT.getForSingleLetterCode("G"), Terminus.Nterm, true, false, "", 0);
		mod = new Mod("Acetyl", "Acetylation", 2, 42.010565, 42.0367, "H(2) C(2) O", null, b);
		fixedMods = new ModSet();
		fixedMods.add(b.build(mod));
		mappings.setFixedMods(context, fixedMods);
		context.failIfNoWarnings();


		// Stacked mods
		context.setExpectWarnings(null);
		b = new SpecificityBuilder(AminoAcidSet.DEFAULT.getForSingleLetterCode("G"), Terminus.Anywhere, true, false, "", 0);
		mod = new Mod("Acetyl", "Acetylation", 2, 42.010565, 42.0367, "H(2) C(2) O", null, b);

		SpecificityBuilder b2 = new SpecificityBuilder(AminoAcidSet.DEFAULT.getForSingleLetterCode("G"), Terminus.Anywhere, true, false, "", 0);
		Mod mod2 = new Mod("Special mod", "Specialization", 2, 10.0, 42.0367, "H(10)", null, b2);

		fixedMods = new ModSet();
		fixedMods.add(b.build(mod));
		fixedMods.add(b2.build(mod2));

		mappings.setFixedMods(context, fixedMods);
		assertParam("add_G_glycine", "52.010565");
	}

	private ModSpecificity getCarbC() {
		SpecificityBuilder b = new SpecificityBuilder(AminoAcidSet.DEFAULT.getForSingleLetterCode("C"), Terminus.Anywhere, false, false, "", 0);
		Mod mod = new Mod("Carbamidomethyl", "Iodoacetamide derivative", 1, 57.021464, 57.0513, "H(3) C(2) N O", null, b);
		return b.build(mod);
	}

	@Test
	public void shouldSupportVariableMods() {
		mappings.read(mappings.baseSettings());

		ModSet variableMods = new ModSet();
		mappings.setVariableMods(context, variableMods);

		assertParam("variable_mod1", "0.0 X 0 3");
		assertParam("variable_mod2", "0.0 X 0 3");
		assertParam("variable_mod3", "0.0 X 0 3");
		assertParam("variable_mod4", "0.0 X 0 3");
		assertParam("variable_mod5", "0.0 X 0 3");
		assertParam("variable_mod6", "0.0 X 0 3");
		assertParam("max_variable_mods_in_peptide", "5");


		variableMods.add(getCarbC());

		assertParam("variable_mod1", "57.021464 C 0 3");


		Assert.fail("Implement me");
	}

	@Test
	public void shouldSupportInstrument() {
		mappings.read(mappings.baseSettings());

		Instrument instrument = Instrument.ORBITRAP;
		mappings.setInstrument(context, instrument);

		Assert.fail("Implement me");
	}

	@Test
	public void shouldSupportMinTerminiCleavages() {
		mappings.read(mappings.baseSettings());

		mappings.setMinTerminiCleavages(context, 1);

		Assert.fail("Implement me");
	}

	@Test
	public void shouldSupportMissedCleavages() {
		mappings.read(mappings.baseSettings());

		mappings.setMissedCleavages(context, 3);

		Assert.fail("Implement me");
	}

	@Test
	public void shouldSupportProtease() {
		mappings.read(mappings.baseSettings());

		Protease protease = Protease.getTrypsinAllowP();
		mappings.setProtease(context, protease);

		Assert.fail("Implement me");
	}

	@Test
	public void shouldSupportSequenceDatabase() {
		mappings.read(mappings.baseSettings());

		String databaseName = "hello";
		mappings.setSequenceDatabase(context, databaseName);

		Assert.fail("Implement me");
	}

	/**
	 * Fail if anything unusual happens.
	 */
	private static final class CometContext extends TestMappingContextBase {

		private String[] expectWarnings;
		private boolean warningsFound;

		/**
		 * Create basic context with mocked parameter info.
		 */
		CometContext() {
			super(new MockParamsInfo());
		}

		public void setExpectWarnings(String[] expectWarnings) {

			this.expectWarnings = expectWarnings;
			warningsFound = false;
		}

		public void failIfNoWarnings() {
			if (!warningsFound) {
				throw new MprcException("The expected warnings were not given: [" + Joiner.on("], [").join(expectWarnings) + "]");
			}
		}

		@Override
		public void reportError(final String message, final Throwable t, ParamName paramName) {
			Assert.fail(message, t);
		}

		@Override
		public void reportWarning(final String message, ParamName paramName) {
			if (expectWarnings != null) {

				for (final String warning : expectWarnings) {
					if (message.contains(warning)) {
						warningsFound = true;
						return;
					}
				}
				Assert.fail("Warning message does not contain [" + Joiner.on(", ").join(expectWarnings) + "]: " + message);
			} else {
				Assert.fail(message);
			}
		}
	}


}
