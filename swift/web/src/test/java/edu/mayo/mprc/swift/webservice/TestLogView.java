package edu.mayo.mprc.swift.webservice;

import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

import static org.mockito.Mockito.mock;

/**
 * @author Roman Zenka
 */
public final class TestLogView {
	private File logFile;
	private MyOutStream outStream;

	@BeforeTest
	public void setup() throws IOException {
		logFile = TestingUtilities.getTempFileFromResource(this.getClass(), "/edu/mayo/mprc/swift/webservice/test.log", true, null);
		outStream = new MyOutStream();
	}

	@AfterTest
	public void teardown() throws IOException {
		FileUtilities.cleanupTempFile(logFile);
	}

	@Test
	public void shouldLogFile() throws IOException {
		final LogView logView = new LogView();
		logView.printLogFile(mock(HttpServletResponse.class), outStream, logFile);
		final String s = outStream.getOutput().toString();
		Assert.assertTrue(s.contains("&lt;3"), "<3 must have gotten escaped");
		Assert.assertTrue(s.contains("prg style=\"width: 0px\""), "Must have the first line at 0");
		Assert.assertTrue(s.contains("prg style=\"width: 1000px\""), "Must have the last line at 1000");
	}

	public static class MyOutStream extends ServletOutputStream {
		private StringBuilder output = new StringBuilder(1000);

		public void write(final int b) throws IOException {
			output.append((char) b);
		}

		public StringBuilder getOutput() {
			return output;
		}
	}

}
