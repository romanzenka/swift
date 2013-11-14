package edu.mayo.mprc.daemon;

/**
 * @author Roman Zenka
 */
public final class DaemonUtilities {
	public static void startDaemonConnections(DaemonConnection... connections) {
		for (DaemonConnection connection : connections) {
			if (connection != null) {
				connection.start();
			}
		}
	}
}
