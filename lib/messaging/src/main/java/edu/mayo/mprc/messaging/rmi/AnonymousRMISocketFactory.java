package edu.mayo.mprc.messaging.rmi;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMISocketFactory;

/**
 * @deprecated This would not work in firewalled environment.
 */
class AnonymousRMISocketFactory extends RMISocketFactory {

	private int lastUsedPort;

	public AnonymousRMISocketFactory() {
		super();
	}

	public Socket createSocket(final String host, final int port) throws IOException {
		final Socket socket = new Socket(host, port);
		lastUsedPort = socket.getLocalPort();
		return socket;
	}

	public ServerSocket createServerSocket(final int port) throws IOException {
		final ServerSocket serverSocket = new ServerSocket(port);
		lastUsedPort = serverSocket.getLocalPort();
		return serverSocket;
	}

	public int getLastUsedPort() {
		return lastUsedPort;
	}
}
