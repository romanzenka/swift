package edu.mayo.mprc.scaffold;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Roman Zenka
 */
public final class TestScaffoldWorker {
	/**
	 * The .scafml for spectrum export should be properly produced.
	 */
	@Test
	public void testSpectrumExport() {
		final File input = new File("/scaffold.sfd");
		final File output = new File("/spectrum.spectra.txt");
		final ScaffoldSpectrumExportWorkPacket work = new ScaffoldSpectrumExportWorkPacket(false, input, output);
		final String result = ScaffoldWorker.getScafmlSpectrumExport(work);
		Assert.assertEquals(result, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				"<Scaffold version=\"1.5\">\n" +
				"  <Experiment load=\"/scaffold.sfd\" name=\"scaffold\">\n" +
				"    <DisplayThresholds id=\"thresh\" minimumNTT=\"1\" minimumPeptideCount=\"1\" name=\"Some Thresholds\" peptideProbability=\"0.8\" proteinProbability=\"0.8\" useCharge=\"true,true,true\" useMergedPeptideProbability=\"true\"/>\n" +
				"    <Export path=\"/spectrum.spectra.txt\" thresholds=\"thresh\" type=\"spectrum\"/>\n" +
				"  </Experiment>\n" +
				"</Scaffold>\n");
	}
}
