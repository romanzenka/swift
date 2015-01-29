package edu.mayo.mprc.utilities;

import edu.mayo.mprc.MprcException;
import org.apache.log4j.Logger;

import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Utility functions for monitoring current execution - determine JVM PID, hostname, etc.
 */
public final class MonitorUtilities {
	private static final Logger LOGGER = Logger.getLogger(MonitorUtilities.class);

	public static final String LOCALHOST = "127.0.0.1";
	private static final Pattern PID_MATCHER = Pattern.compile("(\\d+)@.*");

	private MonitorUtilities() {
	}

	/**
	 * Return the process id of this java virtual machine.  This
	 * should only be used in an advisory context (error/log messages, etc), because obtaining
	 * this depends on JVM vendor specific functionality.
	 *
	 * @return a process id for the current JVM.
	 */
	public static int getPid() {
		// WARNING: this is vendor specific... oh well...

		final String pidstr = ManagementFactory.getRuntimeMXBean().getName();
		if (pidstr == null || pidstr.isEmpty()) {
			return -1;
		}
		final Matcher m = PID_MATCHER.matcher(pidstr);
		if (!m.lookingAt()) {
			return -1;
		}
		try {
			return Integer.parseInt(m.group(1));
		} catch (NumberFormatException e) {
			throw new MprcException("Programmer error, the regular expression " + PID_MATCHER.pattern() + " matched a non-numeric string in group #1", e);
		}
	}

	/**
	 * Return a string identifiying this host/user/JVM.
	 */
	public static String getHostInformation() {
		final String hostname = getShortHostname();

		// find the user name
		final String userName = System.getProperty("user.name");
		// and add these to the message
		return userName + "@" + hostname + " " + (getPid() != -1 ? "(" + getPid() + ")" : "");
	}

	/**
	 * @return Hostname of this machine (without the fulld domain info)
	 */
	public static String getShortHostname() {
		return getFullHostname().replaceAll("\\..*", "");
	}

	/**
	 * @return Full hostname of this machine.
	 */
	public static String getFullHostname() {
		// find the hostname
		String hostname = "unknown";
		try {
			final InetAddress host = InetAddress.getLocalHost();
			hostname = host.getHostName();
		} catch (Exception ignore) {
			// SWALLOWED
		}
		return hostname;
	}

	public static void sendStopSignal(int port) {
		try {
			Socket s = new Socket(InetAddress.getByName(LOCALHOST), port);
			OutputStream out = s.getOutputStream();
			out.write(("\r\n").getBytes());
			out.flush();
			s.close();
		} catch (Exception e /* SWALLOWED - it is not a tragedy to fail to stop jetty. Do not litter error log with unrelated exceptions */) {
			LOGGER.warn("Failed to stop Jetty", e);
		}
	}
}
