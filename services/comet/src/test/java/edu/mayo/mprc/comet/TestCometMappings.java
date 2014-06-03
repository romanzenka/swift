package edu.mayo.mprc.comet;

import com.google.common.base.Joiner;
import edu.mayo.mprc.swift.params2.*;
import edu.mayo.mprc.swift.params2.mapping.Mappings;
import edu.mayo.mprc.swift.params2.mapping.MockParamsInfo;
import edu.mayo.mprc.swift.params2.mapping.TestMappingContextBase;
import edu.mayo.mprc.unimod.ModSet;
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


	@Test
	public void shouldSupportPeptideTolerance() {
		mappings.read(mappings.baseSettings());

		// 0.3 Da
		Tolerance da = new Tolerance(0.3, MassUnit.Da);
		mappings.setPeptideTolerance(context, da);

		Assert.assertEquals(mappings.getNativeParam("peptide_mass_tolerance"), "0.3");
		Assert.assertEquals(mappings.getNativeParam("peptide_mass_units"), "0" /* 0-amu */);

		// 12 ppm
		Tolerance ppm = new Tolerance(12, MassUnit.Ppm);
		mappings.setPeptideTolerance(context, ppm);

		Assert.assertEquals(mappings.getNativeParam("peptide_mass_tolerance"), "12");
		Assert.assertEquals(mappings.getNativeParam("peptide_mass_units"), "2" /* 2-ppm */);
	}

	@Test
	public void shouldSupportFragmentTolerance() {
		mappings.read(mappings.baseSettings());

		Tolerance fragmentTolerance = new Tolerance(10, MassUnit.Ppm);
		mappings.setFragmentTolerance(context, fragmentTolerance);
	}


	@Test
	public void shouldSupportFixedMods() {
		mappings.read(mappings.baseSettings());

		ModSet fixedMods = new ModSet();
		mappings.setFixedMods(context, fixedMods);
	}

	@Test
	public void shouldSupportVariableMods() {
		mappings.read(mappings.baseSettings());

		ModSet variableMods = new ModSet();
		mappings.setVariableMods(context, variableMods);
	}

	@Test
	public void shouldSupportInstrument() {
		mappings.read(mappings.baseSettings());

		Instrument instrument = Instrument.ORBITRAP;
		mappings.setInstrument(context, instrument);
	}

	@Test
	public void shouldSupportMinTerminiCleavages() {
		mappings.read(mappings.baseSettings());

		mappings.setMinTerminiCleavages(context, 1);
	}

	@Test
	public void shouldSupportMissedCleavages() {
		mappings.read(mappings.baseSettings());

		mappings.setMissedCleavages(context, 3);
	}

	@Test
	public void shouldSupportProtease() {
		mappings.read(mappings.baseSettings());

		Protease protease = Protease.getTrypsinAllowP();
		mappings.setProtease(context, protease);
	}

	@Test
	public void shouldSupportSequenceDatabase() {
		mappings.read(mappings.baseSettings());

		String databaseName = "hello";
		mappings.setSequenceDatabase(context, databaseName);
	}

	/**
	 * Fail if anything unusual happens.
	 */
	private static final class CometContext extends TestMappingContextBase {

		private String[] expectWarnings;

		/**
		 * Create basic context with mocked parameter info.
		 */
		CometContext() {
			super(new MockParamsInfo());
		}

		public void setExpectWarnings(String[] expectWarnings) {
			this.expectWarnings = expectWarnings;
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
