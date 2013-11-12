package edu.mayo.mprc.swift;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

@Test(sequential = true)
public final class ConversionTest {
	private static final char nulChar = (char) 0;
	private static final String NUL_STRING = "test:a" + nulChar + 'b' + nulChar + 'c' + nulChar + 'd' + nulChar + nulChar + '\r' + '\n' + nulChar + '1' + '2' + '3';

	@Test(enabled = true, groups = {"fast", "unit"})
	public void testNullRemoval() {
		final String noNulls = NUL_STRING.replaceAll("\\x00", "");
		Assert.assertEquals(noNulls, "test:abcd" + '\r' + '\n' + "123", "The nulls were not removed properly");
	}

	@Test(enabled = true, groups = {"fast", "unit"})
	public void testReader() throws IOException {
		final StringReader stringReader = new StringReader(NUL_STRING);
		final BufferedReader reader = new BufferedReader(stringReader);
		final String line1 = reader.readLine().replaceAll("\\x00", "");
		Assert.assertEquals(line1, "test:abcd", "First line read through reader does not match");
		final String line2 = reader.readLine().replaceAll("\\x00", "");
		Assert.assertEquals(line2, "123", "Second line read through reader does not match");
	}

	@Test(enabled = true, groups = {"fast", "unit"})
	public void testReplaceBackslashes() {
		Assert.assertEquals("/a/b/c/d".replaceAll("\\/", "\\\\\\\\"), "\\\\a\\\\b\\\\c\\\\d", "/ --> \\\\ conversion failed");
	}
}
