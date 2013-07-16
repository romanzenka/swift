package edu.mayo.mprc.chem;

import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public final class AminoAcidsTest {
	private AminoAcidSet set;

	@BeforeClass
	public void setup() {
		set = AminoAcidSet.DEFAULT;
	}

	@Test
	public void testAminoAcidMasses() {
		Assert.assertEquals(89.047679, set.getMonoisotopicMass("A"), "The mass of the amino acid does not match");
		final AminoAcid gly = set.getForSingleLetterCode("G");
		final Chemical glycine = new Chemical(gly.getFormula() + " H2 O1", PeriodicTableFactory.getTestPeriodicTable());
		Assert.assertEquals("Gly", gly.getCode3());
		Assert.assertEquals(set.getMonoisotopicMass(String.valueOf(gly.getCode())), 75.032029);
		// When we calculate the mass from the equation, we get better results
		Assert.assertEquals(glycine.getMonoisotopicMass(), 75.03202840990001);

		Assert.assertEquals(set.getMonoisotopicMass("GDDITMVLILPKPEK"), 1667.916795);
		Assert.assertEquals(set.getMonoisotopicMass("GASPVTCLINDKQEMHFRYW"), 2394.124905);
	}

	/**
	 * The set of default Amino Acids should produce a correct listing.
	 */
	@Test
	public void shouldProduceReport() {
		Assert.assertNull(TestingUtilities.compareStringToResourceByLine(set.report(), "edu/mayo/mprc/chem/amino_acid_report.html"), "Amino acids do not match.");
	}
}
