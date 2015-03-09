import com.gdevelop.gwt.syncrpc.SyncProxy;
import com.google.gwt.user.client.rpc.InvocationException;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.common.client.GWTServiceException;
import edu.mayo.mprc.swift.configuration.client.model.ConfigurationService;
import edu.mayo.mprc.utilities.*;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Roman Zenka
 */
@Test(sequential = true)
public final class SwiftTest {
	private static final Logger LOGGER = Logger.getLogger(SwiftTest.class);

	public static final long CONFIG_TIMEOUT = (long) (120 * 1000);
	private static final Pattern URL_PATTERN = Pattern.compile(".*Please point your web client to (.*)");
	private String url;
	private Object swiftEvent = new Object();
	private boolean shouldEnd;
	private String serverFail;

	private File directory;

	@BeforeClass
	public void init() {
		final Properties properties = getPropertiesFromClasspath("classpath:test.properties");
		directory = new File(properties.getProperty("home"));
	}

	@Test
	public void shouldRunHelp() {
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

	@Test(enabled = false)
	public void shouldRunSge() throws IOException {
		final File sgePacket = TestingUtilities.getTempFileFromResource("/sge_packet.xml", true, null);
		try {
			final ProcessBuilder builder = new ProcessBuilder()
					.directory(directory)
					.command("java",
							// "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=15001",
							"-Dlog4j.configuration=file://" + new File(directory, "conf/log4j.properties").getAbsolutePath(),
							"-cp", new File(directory, "lib").getAbsolutePath() + "/*",
							"edu.mayo.mprc.swift.Swift",
							"--sge", sgePacket.getAbsolutePath());
			final ProcessCaller caller = new ProcessCaller(builder);
			caller.runAndCheck("swift", 0);
			System.out.print(caller.getOutputLog());
		} finally {
			FileUtilities.cleanupTempFile(sgePacket);
		}
	}

	/**
	 * This should start swift in configuration mode.
	 */
	@Test(enabled = false)
	public void shouldConfigure() {
		final ProcessBuilder builder = new ProcessBuilder()
				.directory(directory)
				.command("java",
						// "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=15001",
						"-Dlog4j.configuration=file://" + new File(directory, "conf/log4j.properties").getAbsolutePath(),
						"-cp", new File(directory, "lib").getAbsolutePath() + "/*",
						"edu.mayo.mprc.swift.Swift");
		final ProcessCaller caller = new ProcessCaller(builder);
		caller.setKillTimeout(CONFIG_TIMEOUT);
		caller.setOutputMonitor(new MyLogMonitor(caller));
		caller.setErrorMonitor(new LogMonitor() {
			@Override
			public void line(final String line) {
				LOGGER.error("Swift> " + line);
			}
		});
		caller.runInBackground();
		while (true) {
			synchronized (swiftEvent) {
				try {
					swiftEvent.wait(1000);
					if (caller.getExitValue() != -1) {
						shouldEnd = true;
					}
					if (shouldEnd) {
						break;
					}
					if (url != null) {
						// We started Swift up
						LOGGER.info(url);
						ConfigurationService service = (ConfigurationService)
								SyncProxy.newProxyInstance(ConfigurationService.class, url + "/configuration/", "ConfigurationService");
						shouldEnd = true;
						try {
							service.loadConfiguration();
							service.saveConfiguration();
							try {
								service.terminateProgram();
								caller.kill();
								shouldEnd = true;
							} catch (InvocationException ignore) {
								// SWALLOWED: Terminating the application is likely to cause RPC exception to be thrown
							}
						} catch (GWTServiceException e) {
							// SWALLOWED: Terminating the application is likely to cause RPC exception to be thrown
							break;
						}
					}
				} catch (InterruptedException ignore) {
					// SWALLOWED: ignore interrupts
				}
			}
		}
		caller.waitFor();
		if (serverFail != null) {
			Assert.fail("Server failed: " + serverFail);
		}

		System.out.print(caller.getOutputLog());
	}

	private Properties getPropertiesFromClasspath(final String path) {
		final Properties properties = new Properties();
		final InputStream stream = ResourceUtilities.getStream(path, SwiftTest.class);
		try {
			properties.load(stream);
		} catch (IOException e) {
			throw new MprcException("Failed loading test property file", e);
		} finally {
			FileUtilities.closeQuietly(stream);
		}
		return properties;
	}

	private class MyLogMonitor implements LogMonitor {
		private final ProcessCaller caller;

		public MyLogMonitor(ProcessCaller caller) {
			this.caller = caller;
		}

		@Override
		public void line(String line) {
			LOGGER.debug("Swift> " + line);
			Matcher matcher = URL_PATTERN.matcher(line);
			if (matcher.matches()) {
				synchronized (swiftEvent) {
					url = matcher.group(1);
					swiftEvent.notifyAll();
				}
			} else if (line.contains("Swift web server could not be launched.") || line.contains("Swift configuration is not valid")) {
				LOGGER.error("The server failed to run: " + line);
				synchronized (swiftEvent) {
					shouldEnd = true;
					serverFail = line;
					swiftEvent.notifyAll();
				}
				caller.kill();
			} else if (caller.getExitValue() != -1) {
				LOGGER.error("The server finished running");
				synchronized (swiftEvent) {
					shouldEnd = true;
					swiftEvent.notifyAll();
				}
			}
		}
	}
}
