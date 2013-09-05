package edu.mayo.mprc.swift;

import edu.mayo.mprc.utilities.FileUtilities;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * Test whether Swift can run with particular command line options.
 *
 * @author Roman Zenka
 */
public final class TestSwiftStartup {
	/**
	 * Running Swift with no arguments yields an error.
	 */
	@Test
	public void shouldFailEmpty() {
		Assert.assertEquals(Swift.runSwift(), ExitCode.Error);
	}

	/**
	 * Swift with --help will produce help and end ok.
	 */
	@Test
	public void shouldProvideHelp() {
		Assert.assertEquals(Swift.runSwift("--help"), ExitCode.Ok);
	}

	/**
	 * Swift with --sge without the actual install config should complain and terminate.
	 */
	@Test
	public void shouldRunSge() throws IOException {
		File file = File.createTempFile("broken-sge", "xml");
		try {
			FileUtilities.ensureFileExists(file);
			Assert.assertEquals(Swift.runSwift("--sge", file.getAbsolutePath()), ExitCode.Error);
		} finally {
			FileUtilities.cleanupTempFile(file);
		}
	}

}
