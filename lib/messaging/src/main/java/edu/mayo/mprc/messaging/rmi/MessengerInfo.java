package edu.mayo.mprc.messaging.rmi;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * Class holder of Messenger object information and bound to registry information.
 *
 * @deprecated This would not work in firewalled environment.
 */
public final class MessengerInfo implements Serializable {
	private static final long serialVersionUID = 20090324L;
	private String host;
	private int port;
	private String messengerRemoteName;

	public MessengerInfo(final String host, final int port, final String messengerRemoteName) {
		this.host = host;
		this.port = port;
		this.messengerRemoteName = messengerRemoteName;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public InetSocketAddress getRegistryInfo() {
		return new InetSocketAddress(getHost(), getPort());
	}

	public String getMessengerRemoteName() {
		return messengerRemoteName;
	}
}
