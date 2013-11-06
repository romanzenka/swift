package edu.mayo.mprc.dbcurator.model.curationsteps;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * @author Eric Winter
 */
public final class HeaderTransformTest {

	private List<TransformTest> tests = new ArrayList<TransformTest>();

	private void generateTests() {
		//this is similar to what we want to do with SProt databases
		tests.add(new TransformTest(
				/*toTransform*/ ">Q5JXB2|UE2NL_HUMAN Putative ubiquitin-conjugating enzyme E2 N-like - Homo sapiens (Human)",
				/*class pattern*/ ">([^|]+)\\|(.*)",
				/*sub pattern*/ ">$2 ($1)",
				/*expected result*/ ">UE2NL_HUMAN Putative ubiquitin-conjugating enzyme E2 N-like - Homo sapiens (Human) (Q5JXB2)"
		));

		//what we should do with IPI databases
		tests.add(new TransformTest(
				">IPI:IPI00000001.2|SWISS-PROT:O95793-1|TREMBL:Q59F99|ENSEMBL:ENSP00000360922|REFSEQ:NP_059347|H-INV:HIT000329496|VEGA:OTTHUMP00000031233 Tax_Id=9606 Gene_Symbol=STAU1 Isoform Long of Double-stranded RNA-binding protein Staufen homolog 1",
				">IPI:([^. |]+)[^ ]* (?:Tax_Id=[^ ]+ )?(?:Gene_Symbol=[^ ]+ )?(.*)",
				">$1 $2",
				">IPI00000001 Isoform Long of Double-stranded RNA-binding protein Staufen homolog 1"
		));

	}

	@Test
	public void testTransformFunction1() {
		generateTests();

		int testNumber = 0;
		for (final TransformTest test : tests) {
			try {
				final HeaderTransformStep toTest = new HeaderTransformStep();
				toTest.setMatchPattern(test.matchPattern);
				toTest.setSubstitutionPattern(test.subPattern);
				final String result = toTest.transformString(test.toTransform);
				Assert.assertEquals(result, test.expectedResult, "Actual result doesn't match expected");

			} catch (PatternSyntaxException e) {
				Assert.fail("Test should not have thrown an exception.  Test Number = " + testNumber, e);
			}
			++testNumber;
		}
	}

	private class TransformTest {
		public final String toTransform;
		public final String matchPattern;
		public final String subPattern;
		public final String expectedResult;

		TransformTest(final String toTransform, final String matchPattern, final String subPattern, final String expectedResult) {
			this.toTransform = toTransform;
			this.matchPattern = matchPattern;
			this.subPattern = subPattern;
			this.expectedResult = expectedResult;
		}
	}
}
