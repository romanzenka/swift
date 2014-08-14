package edu.mayo.mprc.comet;

import edu.mayo.mprc.MprcException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;

/**
 * @author Roman Zenka
 */
public final class TestCometWorkPacket {
	@Test
	public void shouldSupportSqt() {
		final CometWorkPacket workPacket = createPacket("test.sqt");
		Assert.assertEquals(workPacket.getSearchParams(), "# Test comet\n"
				+ "output_sqtfile = 1                     # 0=no, 1=yes  write sqt file\n"
				+ "output_txtfile = 0                     # 0=no, 1=yes  write tab-delimited txt file\n"
				+ "output_pepxmlfile = 0                  # 0=no, 1=yes  write pep.xml file\n");
	}

	@Test
	public void shouldSupportPepXml() {
		final CometWorkPacket workPacket = createPacket("test.pep.xml");
		Assert.assertEquals(workPacket.getSearchParams(), "# Test comet\n"
				+ "output_sqtfile = 0                     # 0=no, 1=yes  write sqt file\n"
				+ "output_txtfile = 0                     # 0=no, 1=yes  write tab-delimited txt file\n"
				+ "output_pepxmlfile = 1                  # 0=no, 1=yes  write pep.xml file\n");
	}

	@Test(expectedExceptions = MprcException.class)
	public void shouldFailOtherwise() {
		createPacket("test.unknown_extension");
	}

	private CometWorkPacket createPacket(final String outputFileName) {
		return new CometWorkPacket(new File("input.mgf"),
				"# Test comet\n"
						+ "output_sqtfile = 0                     # 0=no, 1=yes  write sqt file\n"
						+ "output_txtfile = 0                     # 0=no, 1=yes  write tab-delimited txt file\n"
						+ "output_pepxmlfile = 0                  # 0=no, 1=yes  write pep.xml file\n",
				new File(outputFileName), new File("database.fasta"), false, false);
	}
}
