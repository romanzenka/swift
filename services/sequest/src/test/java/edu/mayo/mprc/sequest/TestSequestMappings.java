package edu.mayo.mprc.sequest;

import com.google.common.base.Joiner;
import edu.mayo.mprc.swift.params2.MassUnit;
import edu.mayo.mprc.swift.params2.Tolerance;
import edu.mayo.mprc.swift.params2.mapping.MockParamsInfo;
import edu.mayo.mprc.swift.params2.mapping.TestMappingContextBase;
import edu.mayo.mprc.unimod.ModSet;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test that Sequest mappings work correctly.
 *
 * @author Roman Zenka
 */
public final class TestSequestMappings {
	private SequestMappings sequestMappings;
	private SequestContext context;

	/**
	 * Setup the environment for the test.
	 */
	@BeforeClass
	public void setup() {
		final SequestMappingFactory mappingFactory = new SequestMappingFactory();
		sequestMappings = (SequestMappings) mappingFactory.createMapping();
		context = new SequestContext();
	}

	@Test
	public void shouldMap() {
		final ModSet modSet = new ModSet();
		addMods(modSet, "Carbamidomethyl (C)");
		sequestMappings.setFixedMods(context, modSet);
		Assert.assertEquals(sequestMappings.getNativeParam("add_C_Cysteine"), "57.021464", "Cysteine did not map");
	}

	@Test
	public void shouldClampFragment() {
		context.setExpectWarnings(new String[] { "'ppm' fragment tolerances", "slow with fragment tolerance below 0.1 Da" });
		sequestMappings.setFragmentTolerance(context, new Tolerance(20, MassUnit.Ppm));
		Assert.assertEquals(sequestMappings.getNativeParam("fragment_ion_tolerance"), "0.1", "The tolerance did not get clamped");
	}

	@Test
	public void shouldRetainFragment() {
		sequestMappings.setFragmentTolerance(context, new Tolerance(0.2, MassUnit.Da));
		Assert.assertEquals(sequestMappings.getNativeParam("fragment_ion_tolerance"), "0.2", "The tolerance got clamped");
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

	/**
	 * Fail if anything unusual happens.
	 */
	private static final class SequestContext extends TestMappingContextBase {

		private String[] expectWarnings;

		/**
		 * Create basic context with mocked parameter info.
		 */
		public SequestContext() {
			super(new MockParamsInfo());
		}

		public void setExpectWarnings(String[] expectWarnings) {
			this.expectWarnings = expectWarnings;
		}

		@Override
		public void reportError(final String message, final Throwable t) {
			Assert.fail(message, t);
		}

		@Override
		public void reportWarning(final String message) {
			if (expectWarnings != null) {

				for(final String warning : expectWarnings) {
					if(message.contains(warning)) {
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
