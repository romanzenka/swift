package edu.mayo.mprc.daemon.monitor;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class MonitorUtilities {

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
		if (pidstr == null || pidstr.length() == 0) {
			return -1;
		}
		final Matcher m = PID_MATCHER.matcher(pidstr);
		if (!m.lookingAt()) {
			return -1;
		}
		try {
			return Integer.parseInt(m.group(1));
		} catch (Exception e) {
			// SWALLOWED: won't happen...
		}
		return -1;
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
		final InetAddress host;
		try {
			host = InetAddress.getLocalHost();
			hostname = host.getHostName();
		} catch (Exception ignore) {
			// SWALLOWED
		}
		return hostname;
	}

}
