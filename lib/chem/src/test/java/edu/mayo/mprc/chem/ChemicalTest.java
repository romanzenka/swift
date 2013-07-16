package edu.mayo.mprc.chem;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public final class ChemicalTest {

	private PeriodicTable periodicTable;

	@BeforeClass
	public void beforeTest() {
		periodicTable = PeriodicTableFactory.getTestPeriodicTable();
	}

	@Test
	public void shouldReadDefaultPeriodicTable() {
		Assert.assertNotNull(periodicTable.getElementBySymbol("E-"), "Electron not defined");
		Assert.assertNotNull(periodicTable.getElementBySymbol("H+"), "Proton not defined");
		Assert.assertNotNull(periodicTable.getElementBySymbol("Lr"), "Laurentium not defined");
		Assert.assertNull(periodicTable.getElementBySymbol("Rg"), "Roentgenium defined");

		Assert.assertEquals(periodicTable.getProtonMass(), 1.00727646, "Proton mass incorrect");
		Assert.assertEquals(periodicTable.getElectronMass(), 0.00054858, "Electron mass incorrect");

		Assert.assertEquals(periodicTable.getElementBySymbol("H").getIsotope(0).getMass() * 2 +
				periodicTable.getElementBySymbol("O").getIsotope(0).getMass(), 18.0105646863, "Monoisotopic mass of H2O not calculated correctly");

		Assert.assertEquals(periodicTable.getElementBySymbol("H").getAverageMass() * 2 +
				periodicTable.getElementBySymbol("O").getAverageMass(), 18.015286436209855, "Average mass of H2O not calculated correctly");

	}

	@Test(dependsOnMethods = {"shouldReadDefaultPeriodicTable"})
	public void shouldParseChemicals() {
		final PeriodicTable pt = PeriodicTableFactory.getTestPeriodicTable();
		final Chemical c1 = new Chemical("H2O", pt);
		Assert.assertEquals(c1.getCanonicalFormula(), "H2 O1");

		final Chemical c2 = new Chemical("Lr3Be27O-5", pt);
		Assert.assertEquals(c2.getCanonicalFormula(), "Be27 Lr3 O-5");

		final Chemical c5 = new Chemical("((H)2O)3 C3 H-1", pt);
		Assert.assertEquals(c5.getCanonicalFormula(), "C3 H5 O3");

		Assert.assertEquals(c1.getMonoisotopicMass(), pt.getElementBySymbol("H").getIsotope(0).getMass() * 2 +
				pt.getElementBySymbol("O").getIsotope(0).getMass(), "The monoisotopic mass of H2O is not calculated correctly");

		Assert.assertEquals(c1.getAverageMass(), pt.getElementBySymbol("H").getAverageMass() * 2 +
				pt.getElementBySymbol("O").getAverageMass(), "The average mass of H2O is not calculated correctly");
	}

	@Test(dependsOnMethods = {"shouldReadDefaultPeriodicTable"})
	public void shouldPerformOperationsOnChemicals() throws CloneNotSupportedException {
		final PeriodicTable pt = PeriodicTableFactory.getTestPeriodicTable();
		final Chemical c1 = new Chemical("H2O", pt);
		final Chemical c2 = new Chemical("Lr3Be27O-5", pt);
		final Chemical c5 = new Chemical("(H2O)3 C3 H-1", pt);

		final Chemical c3 = Chemical.add(c1, c2);
		Assert.assertEquals(c3.getCanonicalFormula(), "Be27 H2 Lr3 O-4");

		final Chemical c4 = Chemical.subtract(c2, c1);
		Assert.assertEquals(c4.getCanonicalFormula(), "Be27 H-2 Lr3 O-6");

		final Chemical c6 = c5.clone();
		c5.subtract(c6);
		Assert.assertEquals(c5.getCanonicalFormula(), "");

		final Chemical c7 = c6.clone();
		c6.subtract(c6);
		Assert.assertEquals(c6.getCanonicalFormula(), "");

		final Chemical c8 = c7.clone();
		c7.add(c7);
		Assert.assertEquals(c7.getCanonicalFormula(), "C6 H10 O6");

		c8.multiply(0);
		Assert.assertEquals(c8.getCanonicalFormula(), "");

		final Chemical selen = new Chemical("Se2", pt);
		Assert.assertEquals(selen.getName(), "Se2", "selen formula failure");
		Assert.assertEquals(selen.getMostAbundantMass(), 2 * 79.916520, "Selen most abundant mass calulation failure");
	}

	@Test(dependsOnMethods = {"shouldReadDefaultPeriodicTable"})
	public void shouldSupportNaming() {
		final Chemical water = new Chemical("H2O", periodicTable);
		Assert.assertEquals(water.getName(), "H2 O1", "water formula failure");
		water.setName("Water");
		Assert.assertEquals(water.getName(), "Water", "water name is not getting set");
	}

	@Test(dependsOnMethods = {"shouldReadDefaultPeriodicTable"})
	public void shouldSupportElementQueries() {
		final Chemical water = new Chemical("H2O", periodicTable);
		water.setName("Water");
		Assert.assertEquals(water.getAtomsOf(periodicTable.getElementBySymbol("O")), 1.0, "One oxygen in water");
		Assert.assertEquals(water.getAtomsOf(periodicTable.getElementBySymbol("H")), 2.0, "Two hydrogens in water");
		Assert.assertEquals(water.getAtomsOf(periodicTable.getElementBySymbol("S")), 0.0, "No sulfur in water");

		Assert.assertEquals(water.toString(), "Water (H2 O1) 18.0105646863", "Water to string dump");
	}

	@Test(dependsOnMethods = {"shouldReadDefaultPeriodicTable"})
	public void shouldFailForUnknownElements() {
		try {
			final Chemical test1 = new Chemical("(H2O)5 Qq 10", periodicTable);
			Assert.fail("The parsing of element Qq must fail");
		} catch (Exception e) {
			Assert.assertEquals(e.getMessage(), "Can't find element Qq in chemical formula:\n(H2O)5 >>>Qq 10", "The error message must explain what went wrong");
		}
		try {
			final Chemical test1 = new Chemical("(H2O)5 (Pp 10)-3", periodicTable);
			Assert.fail("The parsing of element Pp must fail");
		} catch (Exception e) {
			Assert.assertEquals(e.getMessage(), "Can't find element Pp in chemical formula:\n(H2O)5 (>>>Pp 10)-3", "The error message must explain what went wrong");
		}
		try {
			final Chemical test1 = new Chemical("(H2{O}1)5 (He 10)-3", periodicTable);
			Assert.fail("The parsing of { must fail");
		} catch (Exception e) {
			Assert.assertEquals(e.getMessage(), "Parse error, can't understand '{':\n(H2>>>{O}1)5 (He 10)-3", "The error message must explain what went wrong");
		}
	}

	@Test(dependsOnMethods = {"shouldReadDefaultPeriodicTable"})
	public void shouldParseFractions() {
		final Chemical water = new Chemical("H0.2O0.1", periodicTable);
		water.setName("1/10th Water");
		Assert.assertEquals(water.getAtomsOf(periodicTable.getElementBySymbol("O")), 0.1, "0.1 oxygen in 1/10 water");
		Assert.assertEquals(water.getAtomsOf(periodicTable.getElementBySymbol("H")), 0.2, "0.2 hydrogens in 1/10 water");
		Assert.assertEquals(water.getAtomsOf(periodicTable.getElementBySymbol("S")), 0.0, "No sulfur in 1/10 water");

		Assert.assertEquals(water.toString(), "1/10th Water (H0.2 O0.1) 1.8010564686300001", "Water to string dump");
	}

	@Test(dependsOnMethods = {"shouldReadDefaultPeriodicTable"})
	public void shouldSupportAveragine() {
		final Chemical water = new Averagine(periodicTable);
		// C4.9384 H7.7583 N1.3577 O1.4773 S0.0417
		Assert.assertEquals(water.getAtomsOf(periodicTable.getElementBySymbol("C")), 4.9384);
		Assert.assertEquals(water.getAtomsOf(periodicTable.getElementBySymbol("O")), 1.4773);
		Assert.assertEquals(water.getAtomsOf(periodicTable.getElementBySymbol("H")), 7.7583);
		Assert.assertEquals(water.getAtomsOf(periodicTable.getElementBySymbol("S")), 0.0417);

		Assert.assertEquals(water.toString(), "Averagine (C4.9384 H7.7583 N1.3577 O1.4773 S0.0417) 111.05430524240279", "Averagine to string dump failed");
	}

}
