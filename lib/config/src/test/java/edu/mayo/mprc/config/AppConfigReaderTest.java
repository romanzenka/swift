package edu.mayo.mprc.config;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Roman Zenka
 */
public final class AppConfigReaderTest {
	@Test
	public void testUnescapeLine() throws Exception {
		Assert.assertEquals(AppConfigReader.unescapeLine(null), null);
		Assert.assertEquals(AppConfigReader.unescapeLine("   "), "");
		Assert.assertEquals(AppConfigReader.unescapeLine("hello world"), "hello world");
		Assert.assertEquals(AppConfigReader.unescapeLine("    hello world    "), "hello world");
		Assert.assertEquals(AppConfigReader.unescapeLine("hello\\nworld"), "hello\nworld");
		Assert.assertEquals(AppConfigReader.unescapeLine("hello\\rworld"), "hello\rworld");
		Assert.assertEquals(AppConfigReader.unescapeLine("hello\\\\world"), "hello\\world");
		Assert.assertEquals(AppConfigReader.unescapeLine("hello\\\\\\nworld"), "hello\\\nworld");
		Assert.assertEquals(AppConfigReader.unescapeLine(" hello world    #  Comment"), "hello world");
		Assert.assertEquals(AppConfigReader.unescapeLine(" hello world \\# Comment \\## Comment2"), "hello world # Comment #");
	}
}
