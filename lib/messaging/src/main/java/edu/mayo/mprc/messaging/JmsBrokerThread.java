package edu.mayo.mprc.messaging;

import edu.mayo.mprc.MprcException;
import org.apache.activemq.broker.BrokerService;

import javax.management.ObjectName;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Handle to a locally running JMS message broker.
 * <p/>
 * There must be at least one JMS broker running anywhere in the system for the messaging to function. You either create
 * your own broker, or you know URI of an existing broker to connect to.
 */
public final class JmsBrokerThread {
	private BrokerService broker = null;
	private URI uri = null;

	/**
	 * Starts running a message broker at the current computer. You need at least one broker running anywhere in your
	 * system to be able to send messages. It does not have to be local.
	 *
	 * @param uri        Uri for the broker. This is one of the uris as described here at http://activemq.apache.org/uri-protocols.html
	 *                   You can use {@link #getUriForPort(int)} method for obtaining URI for a tcp broker running at specific
	 *                   port, or {@link #getVmUri()} for a broker that runs just within this VM.
	 * @param brokerName Unique name of the broker for JMX. If null or zero length, JMX is not enabled.
	 */
	public JmsBrokerThread(final URI uri, final String brokerName) {
		try {
			this.uri = uri;
			this.broker = new BrokerService();
			this.broker.setPersistent(false);
			this.broker.setUseJmx(true);
			if (brokerName != null && brokerName.length() != 0) {
				this.broker.setBrokerObjectName(new ObjectName(brokerName, "swift", "2.0"));
			}
			this.broker.addConnector(uri);
			this.broker.start();
		} catch (Exception e) {
			throw new MprcException("Could not start broker for uri " + uri.toString(), e);
		}
	}

	/**
	 * Returns the URI of the broker. Useful only for testing, otherwise you should always know the URI from your
	 * configuration files.
	 *
	 * @return URI of the broker.
	 */
	public URI getURI() {
		return uri;
	}

	/**
	 * Stops the execution of the broker.
	 */
	public void stopBroker() {
		try {
			broker.stop();
			broker.waitUntilStopped();
		} catch (Exception e) {
			throw new MprcException("Could not stop broker", e);
		}
	}

	public void start() {
		try {
			broker.start();
		} catch (Exception e) {
			throw new MprcException("Could not start broker " + broker.toString(), e);
		}
	}

	public static URI getUriForPort(final int port) {
		try {
			return new URI("tcp", null, InetAddress.getLocalHost().getHostName(), port, null, null, null);
		} catch (Exception t) {
			throw new MprcException(t);
		}
	}

	public static URI getVmUri() {
		try {
			return new URI("vm", null, "localhost", -1, null, "broker.persistent=false&broker.useJmx=false", null);
		} catch (URISyntaxException e) {
			throw new MprcException(e);
		}
	}

	public void deleteAllMessages() {
		try {
			broker.deleteAllMessages();
		} catch (IOException e) {
			throw new MprcException("Could not delete all messages from broker " + broker.toString(), e);
		}
	}
}