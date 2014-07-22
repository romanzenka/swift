package edu.mayo.mprc.swift;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfigInfo;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;

public final class ReportUtilsTest {

	@Test
	public void shouldConvertTokens() {
		final DaemonConfigInfo info = new DaemonConfigInfo("my daemon", "/mnt/raid1", "/tmp/log");
		final DaemonConfigInfo dbInfo = new DaemonConfigInfo("database daemon", "/mnt/raid1", "/tmp/log");
		final DatabaseFileTokenFactory tokenFactory = new DatabaseFileTokenFactory(info);
		tokenFactory.setDatabaseDaemonConfigInfo(dbInfo);
		Assert.assertEquals(
				ReportUtils.replaceTokensWithHyperlinks("hello world", new File("/mnt/raid1/browsing"), "file:///rome/mprc", tokenFactory),
				"hello world", "Simple strings must remain unchanged");

		final File sharedFile = new File("/mnt/raid1/browsing/test.txt");
		Assert.assertEquals(
				ReportUtils.replaceTokensWithHyperlinks(
						"hello <file>shared:/browsing/test.txt</file> world",
						/*browse root*/new File("/mnt/raid1/browsing"),
						/*browse web root*/"file:///rome/mprc", tokenFactory),
				"hello <a class=\"path\" href=\"file:///rome/mprc/test.txt\" title=\"" + sharedFile.getAbsolutePath() +
						"\">test.txt</a> world", "Tags bit replaced correctly");

	}

	@Test
	public void shouldReplaceNewlines() {
		Assert.assertEquals(ReportUtils.newlineToBr("hello world"), "hello world", "No change");
		Assert.assertEquals(ReportUtils.newlineToBr("hello\r\nworld\r\n\r\ntest"), "hello<br/>world<br/><br/>test", "Windows style");
		Assert.assertEquals(ReportUtils.newlineToBr("hello\nworld\n\ntest"), "hello<br/>world<br/><br/>test", "Unix style");
		Assert.assertEquals(ReportUtils.newlineToBr("hello world\n"), "hello world<br/>", "Unix style last newline");
		Assert.assertEquals(ReportUtils.newlineToBr("hello\r\nworld\r\n\ntest"), "hello<br/>world<br/><br/>test", "Mixed style");
	}

	@DataProvider(name = "dates")
	public Object[][] dateTable() {
		return new Object[][]{
				{"2010-10-23", "10-23-2010 00:00:00"},
				{"1960-2-28", "02-28-1960 00:00:00"},
				{"1960-2-29", "02-29-1960 00:00:00"},
				{"1960/02/29", "Cannot parse start date. Expected yyyy-MM-dd format. - Invalid format: \"1960/02/29\" is malformed at \"/02/29\""},
				{"1978-13-10", "Cannot parse start date. Expected yyyy-MM-dd format. - Cannot parse \"1978-13-10\": Value 13 for monthOfYear must be in the range [1,12]"},
				{"2020-03-15T10:20:30", "03-15-2020 10:20:30"},
				{"2020-03-15 10:20:30", "Cannot parse start date. Expected yyyy-MM-dd format. - Invalid format: \"2020-03-15 10:20:30\" is malformed at \" 10:20:30\""},
		};
	}

	@Test(dataProvider = "dates")
	public void shouldParseDates(final String input, final String output) {
		try {
			final DateTime date = ReportUtils.parseDate(input, "start");
			Assert.assertEquals(date.toString("MM-dd-yyyy HH:mm:ss"), output);
		} catch (Exception e) {
			Assert.assertEquals(MprcException.getDetailedMessage(e), output, "Exception message does not match");
		}
	}
}
