import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.ResourceUtilities;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Roman Zenka
 */
public final class InstallTest {
	@Test
	public void shouldRunHelp() throws IOException {
		final Properties properties = getPropertiesFromClasspath("classpath:test.properties");
		final File directory = new File(properties.getProperty("home"));
		final ProcessBuilder builder = new ProcessBuilder()
				.directory(directory)
				.command("java",
						"-Dlog4j.configuration=file://" + new File(directory, "conf/log4j.properties").getAbsolutePath(),
						"-cp", new File(directory, "lib").getAbsolutePath() + "/*",
						"edu.mayo.mprc.swift.Swift",
						"--run", "help");
		final ProcessCaller caller = new ProcessCaller(builder);
		caller.runAndCheck("swift", 0);
		System.out.print(caller.getOutputLog());
	}

	private Properties getPropertiesFromClasspath(final String path) throws IOException {
		final Properties properties = new Properties();
		final InputStream stream = ResourceUtilities.getStream(path, InstallTest.class);
		try {
			properties.load(stream);
		} finally {
			FileUtilities.closeQuietly(stream);
		}
		return properties;
	}
}
