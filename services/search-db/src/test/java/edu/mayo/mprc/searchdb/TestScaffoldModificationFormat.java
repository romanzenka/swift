package edu.mayo.mprc.searchdb;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.searchdb.dao.LocalizedModBag;
import edu.mayo.mprc.searchdb.dao.LocalizedModification;
import edu.mayo.mprc.unimod.IndexedModSet;
import edu.mayo.mprc.unimod.MockUnimodDao;
import edu.mayo.mprc.unimod.Unimod;
import edu.mayo.mprc.utilities.ResourceUtilities;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collection;

/**
 * Check that we can correctly parse typical mods from Scaffold reports.
 *
 * @author Roman Zenka
 */
public final class TestScaffoldModificationFormat {
	private ScaffoldModificationFormat format;
	private static final double EPSILON = 0.05;

	@BeforeClass
	public void setup() {
		final MockUnimodDao unimodDao = new MockUnimodDao();
		final IndexedModSet unimod = unimodDao.load();
		final Unimod scaffoldUnimod = new Unimod();
		scaffoldUnimod.parseUnimodXML(ResourceUtilities.getStream("classpath:edu/mayo/mprc/searchdb/scaffold_unimod.xml", getClass()));
		format = new ScaffoldModificationFormat(unimod, scaffoldUnimod);
	}

	/**
	 * No mods specified - no mods returned
	 */
	@Test
	public void shouldParseEmptyString() {
		final LocalizedModBag mods = format.parseModifications("EDEEESLNEVGYDDIGGCR", "", "");
		Assert.assertEquals(mods.size(), 0, "No mods");
	}

	/**
	 * Single mods parsing, no trouble with ordering.
	 */
	@Test
	public void shouldParseSingleMods() {
		checkSingleMod(
				format.parseModifications("EDEEESLNEVGYDDIGGCR", "c18: Carbamidomethyl (+57.02)", ""),
				"Carbamidomethyl", 57.02, 17, 'C');

		checkSingleMod(
				format.parseModifications("MFLSFPTTK", "m1: Oxidation (+15.99)", ""),
				"Oxidation", 15.99, 0, 'M');

		checkSingleMod(
				format.parseModifications("NmQDMVEDYR", "m2: Oxidation (+15.99)", ""),
				"Oxidation", 15.99, 1, 'M');

		checkSingleMod(
				format.parseModifications("IQVRLGEHNIDVLEGNEQFINAAKIITHPNFNGNTLDNDImLIKLSSPATLNSR", "m41: Oxidation (+15.99)", ""),
				"Oxidation", 15.99, 40, 'M');
	}

	/**
	 * Should notice that the residue reported does not match the sequence.
	 */
	@Test(expectedExceptions = MprcException.class)
	public void shouldCatchResidueMismatch() {
		format.parseModifications("EDEEESLNEVGYDDIGGZR", "c18: Carbamidomethyl (+57.02)", "");
	}

	/**
	 * Should notice non-existing modification, even if the mass matches
	 */
	@Test(expectedExceptions = MprcException.class)
	public void shouldCatchNonExistingMod() {
		format.parseModifications("EDEEESLNEVGYDDIGGCR", "c18: Blablamidomethyl (+57.02)", "");
	}

	/**
	 * Should notice extra garbage in the parsed string
	 */
	@Test
	public void shouldCatchGarbage() {
		try {
			format.parseModifications("EDEEESLNEVGYDDIGGCR", "c18: Carbamidomethyl (+57.02), GARBAGE!", "");
			Assert.fail("Expected exception");
		} catch (MprcException e) {
			Assert.assertTrue(e.getMessage().contains("[GARBAGE!]"), "The garbage should be properly reported");
		}

		try {
			format.parseModifications("EDEEESLNEVGYDDIGGCR", "GARBAGE2!", "");
			Assert.fail("Expected exception");
		} catch (MprcException e) {
			Assert.assertTrue(e.getMessage().contains("[GARBAGE2!]"), "The garbage should be properly reported");
		}
	}

	/**
	 * Scaffold tends to report Pyro-cmC instead of Pyro-Glu due to a bug in its unimod parser code.
	 */
	@Test
	public void shouldFixPyroCmc() {
		checkSingleMod(
				format.parseModifications("QSVEADINGLR", "n-term: Pyro-cmC (-17.03)", ""),
				"Gln->pyro-Glu", -17.03, 0, 'Q');
	}

	/**
	 * Apparently, X!Tandem considers Q and Z amino acids to be the same thing. So there can be
	 * Pyro-cmC on Z as well.
	 */
	@Test
	public void shouldResolveZPyroCmc() {
		checkSingleMod(
				format.parseModifications("zSPSSLSASVGBR", "n-term: Pyro-cmC (-17.03)", ""),
				"Gln->pyro-Glu", -17.03, 0, 'Z');
	}

	/**
	 * Scaffold tends to report Hydroxylation(interim name) instead of preferred Oxidation.
	 */
	@Test
	public void shouldFixHydroxylation() {
		checkSingleMod(
				format.parseModifications("QMSVEADINGLR", "m2: Hydroxylation (+15.99)", ""),
				"Oxidation", 15.99, 1, 'M');
	}

	/**
	 * Unknown or non-specific amino acids need to be properly parsed.
	 */
	@Test
	public void shouldFixUnknownAminoAcid() {
		checkSingleMod(
				format.parseModifications("EDEEESLNEVGYDDIGGXR", "x18: Carbamidomethyl (+57.02)", ""),
				"Carbamidomethyl", 57.02, 17, 'X');

		checkSingleMod(
				format.parseModifications("EDEEESLNEVGYDDIGGBR", "b18: Carbamidomethyl (+57.02)", ""),
				"Carbamidomethyl", 57.02, 17, 'B');

		checkSingleMod(
				format.parseModifications("EDEEESLNEVGYDDIGGZR", "z18: Carbamidomethyl (+57.02)", ""),
				"Carbamidomethyl", 57.02, 17, 'Z');

	}

	/**
	 * Check single expected mod for parse errors.
	 *
	 * @param mods         Set of modifications. There should be just one.
	 * @param name         name of the modification {@link edu.mayo.mprc.unimod.Mod#getTitle}
	 * @param expectedMass The mass should be within {@link #EPSILON} from the required
	 * @param position     0 = N-term
	 * @param residue      Residue the modification occurs on
	 */
	private void checkSingleMod(final Collection<LocalizedModification> mods, final String name, final double expectedMass, final int position, final char residue) {
		Assert.assertEquals(mods.size(), 1, "one mod expected");
		final LocalizedModification localizedModification = mods.iterator().next();
		Assert.assertEquals(localizedModification.getModSpecificity().getModification().getTitle(), name);
		assertNear(localizedModification.getModSpecificity().getModification().getMassMono(), expectedMass, "Mass should match");
		Assert.assertEquals(localizedModification.getPosition(), position, "Mod position should match");
		Assert.assertEquals(localizedModification.getResidue(), residue, "Mod residue should match");
	}


	public static void assertNear(final double d1, final double d2, final String message) {
		final double delta = d1 - d2;
		Assert.assertTrue(-EPSILON < delta && delta < EPSILON, message);
	}
}
