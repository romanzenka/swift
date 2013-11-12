package edu.mayo.mprc.utilities;

import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Roman Zenka
 */
public final class TestMonitorUtilities {
	private static final Logger LOGGER = Logger.getLogger(TestMonitorUtilities.class);

	@Test
	public void shouldGetPid() {
		int pid = MonitorUtilities.getPid();
		if (pid == -1) {
			LOGGER.warn("Could not determine PID of the running process");
			return;
		}
		Assert.assertTrue(pid > 0, "PID should be greater than zero, was " + pid);
		Assert.assertTrue(pid < 1000000, "PID should be relatively small (not over million) - otherwise something is likely wrong. Was " + pid);
	}

	@Test
	public void shouldGetShortHostname() {
		final String shortHostname = MonitorUtilities.getShortHostname();
		Assert.assertNotNull(shortHostname);
		Assert.assertTrue(shortHostname.length() > 1, "hostname should be longer than 1 character");
		final char c = shortHostname.charAt(0);
		Assert.assertTrue((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'), "hostname should start with a-zA-Z");
	}
}
