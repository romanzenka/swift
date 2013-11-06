package edu.mayo.mprc.mascot;

import edu.mayo.mprc.dbcurator.model.persistence.CurationDao;
import edu.mayo.mprc.dbcurator.model.persistence.MockCurationDao;
import edu.mayo.mprc.swift.params2.MassUnit;
import edu.mayo.mprc.swift.params2.MockParamsDao;
import edu.mayo.mprc.swift.params2.ParamsDao;
import edu.mayo.mprc.swift.params2.Tolerance;
import edu.mayo.mprc.swift.params2.mapping.*;
import edu.mayo.mprc.unimod.*;
import edu.mayo.mprc.utilities.ResourceUtilities;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.Reader;
import java.io.StringWriter;

public final class TestMascotMappings {
	private ParamsInfo abstractParamsInfo;
	private MascotMappingFactory mappingFactory;
	private Mappings mapping;
	private Unimod unimod;

	@BeforeClass
	public void startup() {
		abstractParamsInfo = getAbstractParamsInfo();
		mappingFactory = new MascotMappingFactory();
		mappingFactory.setParamsInfo(abstractParamsInfo);
		mapping = mappingFactory.createMapping();
		unimod = abstractParamsInfo.getUnimod();
	}

	@Test
	public void shouldSupportPhosphoST() {
		final MappingContext context = new PhosphoStContext(abstractParamsInfo);

		final ModSpecificity phosphoS = unimod.getSpecificitiesByMascotName("Phospho (S)").get(0);
		final ModSet set = new ModSet();
		set.add(phosphoS);
		mapping.setFixedMods(context, set);

		final ModSpecificity phosphoT = unimod.getSpecificitiesByMascotName("Phospho (T)").get(0);
		set.add(phosphoT);
		mapping.setFixedMods(context, set);

		final String output = mappingsToString(mapping);

		Assert.assertTrue(output.contains("MODS=Phospho (ST)\n"), "The mods do not match");
	}

	@Test
	public void shouldSupportDeamidatedNTerm() {
		final MappingContext context = new PhosphoStContext(abstractParamsInfo);

		final ModSpecificity deamidated = unimod.getSpecificitiesByMascotName("Deamidated (Protein N-term F)").get(0);
		final ModSet modSet = new ModSet();
		modSet.add(deamidated);
		mapping.setFixedMods(context, modSet);

		final String output = mappingsToString(mapping);

		Assert.assertTrue(output.contains("MODS=Deamidated (Protein N-term F)\n"), "The mods do not match");
	}

	@Test
	public void shouldSupportFragmentPpm() {
		final MappingContext context = new PpmContext(abstractParamsInfo);
		mapping.setFragmentTolerance(context, new Tolerance(10.0, MassUnit.Ppm));
		Assert.assertEquals(mapping.getNativeParam("ITOL"), "0.01");
		Assert.assertEquals(mapping.getNativeParam("ITOLU"), "Da");
	}

	public static ParamsInfo getAbstractParamsInfo() {
		final CurationDao curationDao = new MockCurationDao();
		final UnimodDao unimodDao = new MockUnimodDao();
		final ParamsDao paramsDao = new MockParamsDao();
		return new ParamsInfoImpl(curationDao, unimodDao, paramsDao);
	}

	private static String mappingsToString(final Mappings mapping) {
		final StringWriter writer = new StringWriter(1000);
		mapping.write(getMascotParamReader(), writer);
		return writer.toString();
	}

	private static Reader getMascotParamReader() {
		return ResourceUtilities.getReader("classpath:edu/mayo/mprc/swift/params/Orbitrap_Sprot_Latest_CarbC_OxM/mascot.params", TestMascotMappings.class);
	}

	private static final class PhosphoStContext extends TestMappingContextBase {
		private PhosphoStContext(final ParamsInfo abstractParamsInfo) {
			super(abstractParamsInfo);
		}

		@Override
		public void reportWarning(final String message) {
			Assert.assertTrue(message.matches("Mascot will search additional site \\([ST]\\) for modification Phospho \\([ST]\\)"), "Unexpected warning");
		}
	}

	private static final class PpmContext extends TestMappingContextBase {
		PpmContext(final ParamsInfo paramsInfo) {
			super(paramsInfo);
		}

		@Override
		public void reportWarning(final String message) {
			Assert.assertTrue(message.matches("Mascot does not support 'ppm' fragment tolerances; using \\d+\\.\\d* Da instead."));
		}
	}
}
