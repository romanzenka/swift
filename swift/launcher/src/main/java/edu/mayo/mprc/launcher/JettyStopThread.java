package edu.mayo.mprc.launcher;

import edu.mayo.mprc.utilities.MonitorUtilities;
import org.apache.log4j.Logger;
import org.mortbay.jetty.Server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class JettyStopThread extends Thread {

	private static final Logger LOGGER = Logger.getLogger(JettyStopThread.class);

	private final Server server;
	private final ServerSocket socket;

	public JettyStopThread(Server server) {
		this.server = server;
		setDaemon(true);
		setName("StopMonitor");
		try {
			socket = new ServerSocket(0, 1, InetAddress.getByName(MonitorUtilities.LOCALHOST));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	int getPort() {
		return socket.getLocalPort();
	}

	@Override
	public void run() {
		LOGGER.debug("running jetty 'stop' thread");
		Socket accept;
		try {
			accept = socket.accept();
			BufferedReader reader = new BufferedReader(new InputStreamReader(accept.getInputStream()));
			reader.readLine();
			LOGGER.info("stopping jetty embedded server");
			server.stop();
			accept.close();
			socket.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}