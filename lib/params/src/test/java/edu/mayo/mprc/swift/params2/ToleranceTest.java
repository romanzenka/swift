package edu.mayo.mprc.swift.params2;

import edu.mayo.mprc.MprcException;
import org.testng.Assert;
import org.testng.annotations.Test;

public final class ToleranceTest {

	public void test(final String text, final double expectedValue, final MassUnit expectedUnit, final String result) {
		final Tolerance t = new Tolerance(text);
		Assert.assertEquals(t.getValue(), expectedValue, "Value does not match");
		Assert.assertEquals(t.getUnit(), expectedUnit, "Unit does not match");
		Assert.assertEquals(t.toString(), result, "Conversion back to string does not match");
	}

	@Test
	public void shouldParseDaltons() {
		test("1 Da", 1.0, MassUnit.Da, "1 Da");
		test("   0.0   Da ", 0.0, MassUnit.Da, "0 Da");
		test("1E3 Da", 1000.0, MassUnit.Da, "1000 Da");
		test("1.5Da", 1.5, MassUnit.Da, "1.5 Da");
		test(".5 da", 0.5, MassUnit.Da, "0.5 Da");
		test("+2.5 Da", 2.5, MassUnit.Da, "2.5 Da");
		test("0.0000001 Da", 0.0000001, MassUnit.Da, "0.0000001 Da");
		test("1 Dalton", 1, MassUnit.Da, "1 Da");
		test("1 Daltons", 1, MassUnit.Da, "1 Da");
	}

	@Test
	public void shouldParsePpm() {
		test("1ppm", 1.0, MassUnit.Ppm, "1 ppm");
		test("   0.0PPM ", 0.0, MassUnit.Ppm, "0 ppm");
		test("-1.2E3PPM", -1200.0, MassUnit.Ppm, "-1200 ppm");
		test("1.5ppm", 1.5, MassUnit.Ppm, "1.5 ppm");
		test(".5  ppm", 0.5, MassUnit.Ppm, "0.5 ppm");
		test("+2.5  ppm", 2.5, MassUnit.Ppm, "2.5 ppm");
		test("-2.5  ppm", -2.5, MassUnit.Ppm, "-2.5 ppm");
	}

	@Test(expectedExceptions = MprcException.class)
	public void shouldFail() {
		test("1.2 Kg", 1.2, MassUnit.Ppm, "1.2 Kg");
	}
}
