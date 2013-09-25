package edu.mayo.mprc.utilities;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;

public final class StringUtilitiesTest {

	@Test
	public void shouldAppendTabBeforeLines() {
		Assert.assertEquals(StringUtilities.appendTabBeforeLines("a\nb"), "\ta\n\tb");
	}

	@Test
	public void testRepeat() {
		Assert.assertEquals(StringUtilities.repeat('x', 5), "xxxxx");
		Assert.assertEquals(StringUtilities.repeat('x', 0), "");
		Assert.assertEquals(StringUtilities.repeat('x', -10), "");
	}

	@Test
	public void testEscapeHtml() {
		Assert.assertEquals(StringUtilities.escapeHtml("<hello & world>''\"\""), "&lt;hello &amp; world&gt;''&quot;&quot;");
	}

	@Test
	public void testFirstToUpper() {
		Assert.assertEquals(StringUtilities.firstToUpper("hello WORLD"), "Hello WORLD");
		Assert.assertEquals(StringUtilities.firstToUpper(" "), " ");
		Assert.assertEquals(StringUtilities.firstToUpper(""), "");
	}

	@Test
	public void testFirstToUpperRestToLower() {
		Assert.assertEquals(StringUtilities.firstToUpperRestToLower("hello WORLD"), "Hello world");
		Assert.assertEquals(StringUtilities.firstToUpperRestToLower(" "), " ");
		Assert.assertEquals(StringUtilities.firstToUpperRestToLower(""), "");
	}

	@Test
	public void testStartsWithIgnoreCase() {
		Assert.assertTrue(StringUtilities.startsWithIgnoreCase("Hello World", "HELL"));
		Assert.assertTrue(StringUtilities.startsWithIgnoreCase("HELLo World", "hell"));
		Assert.assertTrue(StringUtilities.startsWithIgnoreCase("123", "123"));
		Assert.assertTrue(StringUtilities.startsWithIgnoreCase("abcde", ""));
		Assert.assertTrue(StringUtilities.startsWithIgnoreCase("", ""));
		Assert.assertFalse(StringUtilities.startsWithIgnoreCase("hell", "HELLo World"));
	}

	@Test
	public void testEndsWithIgnoreCase() {
		Assert.assertTrue(StringUtilities.endsWithIgnoreCase("Hello World", "RLD"));
		Assert.assertTrue(StringUtilities.endsWithIgnoreCase("HELLo World", "rld"));
		Assert.assertTrue(StringUtilities.endsWithIgnoreCase("123", "123"));
		Assert.assertTrue(StringUtilities.endsWithIgnoreCase("", ""));
		Assert.assertFalse(StringUtilities.endsWithIgnoreCase("rld", "HELLo World"));
		Assert.assertTrue(StringUtilities.endsWithIgnoreCase("aaa", ""));
	}

	@Test
	public void testContainsIgnoreCase() {
		Assert.assertTrue(StringUtilities.containsIgnoreCase("blaBLAbla", "ABL"));
		Assert.assertFalse(StringUtilities.containsIgnoreCase("blaBLAbla", "BAL"));
		Assert.assertTrue(StringUtilities.containsIgnoreCase("blaBLAbla", ""));
	}

	@Test
	public void shouldEscapeUnicode() {
		final String test = "_&a \u1234 b \u4321 c \ubbaa \u0040 \u007f \u0080 \u001f \n";
		final String result = StringUtilities.toUnicodeEscapeString(test);
		Assert.assertEquals(result, "_&a \\u1234 b \\u4321 c \\ubbaa @ \u007f \\u0080 \\u001f \\u000a");
	}

	@Test
	public void shouldTestEmpty() {
		Assert.assertTrue(StringUtilities.stringEmpty(null));
		Assert.assertTrue(StringUtilities.stringEmpty(""));
		Assert.assertTrue(StringUtilities.stringEmpty("      "));
		Assert.assertTrue(StringUtilities.stringEmpty(" \t \t     "));

		Assert.assertFalse(StringUtilities.stringEmpty("a"));
		Assert.assertFalse(StringUtilities.stringEmpty("hello"));
	}

	@Test
	public void shouldSplitTypical() {
		final ArrayList<String> list = new ArrayList<String>(10);
		StringUtilities.split("a\tb\t\tc\tdef\t", '\t', list);
		Assert.assertEquals(list.toArray(), new String[]{"a", "b", "", "c", "def", ""});
	}

	@Test
	public void shouldSplitEmpty() {
		final ArrayList<String> list = new ArrayList<String>(10);
		StringUtilities.split("", '\t', list);
		Assert.assertEquals(list.toArray(), new String[]{""});
	}

	@Test
	public void shouldSplitOnlyDelim() {
		final ArrayList<String> list = new ArrayList<String>(10);
		StringUtilities.split("\t\t\t", '\t', list);
		Assert.assertEquals(list.toArray(), new String[]{"", "", "", ""});
	}

	@Test
	public void shouldConvertToHex() {
		Assert.assertEquals(StringUtilities.toHex(0xa), 'a');
		Assert.assertEquals(StringUtilities.toHex(-1), 'f');
		Assert.assertEquals(StringUtilities.toHex(0xcafebabe), 'e');

		Assert.assertEquals(StringUtilities.toHex(new byte[]{(byte) 0x01, (byte) 0xca, (byte) 0x02, (byte) 0xba, (byte) 0x34}, ""), "01ca02ba34");
		Assert.assertEquals(StringUtilities.toHex(new byte[]{(byte) 0x01, (byte) 0xca, (byte) 0x02, (byte) 0xba, (byte) 0x34}, "-"), "01-ca-02-ba-34");
		Assert.assertEquals(StringUtilities.toHex(new byte[]{}, ":"), "");
	}

	@Test
	public void shouldFindPrefixes() {
		Assert.assertEquals(StringUtilities.longestPrefix(Arrays.asList("hello", "hero")), "he");
		Assert.assertEquals(StringUtilities.longestPrefix(new ArrayList<String>(0)), "");
		Assert.assertEquals(StringUtilities.longestPrefix(Arrays.asList("hello")), "hello");
		Assert.assertEquals(StringUtilities.longestPrefix(Arrays.asList("hello", "hello")), "hello");
		Assert.assertEquals(StringUtilities.longestPrefix(Arrays.asList("", "hello", "hell", "her", "he", "h")), "");
	}

	@Test
	public void shouldFormatMessages() {
		Assert.assertEquals(MessageFormat.format("{0,number,#}", 123456), "123456");
	}
}
