package edu.mayo.mprc.messaging;

import edu.mayo.mprc.MprcException;
import org.apache.log4j.Logger;
import org.testng.annotations.AfterClass;

import java.net.URI;
import java.text.MessageFormat;

public abstract class MessagingTestBase {
	private static final Logger LOGGER = Logger.getLogger(MessagingTestBase.class);
	private static final String TEST_QUEUE_NAME = "test_queue";
	protected JmsBrokerThread broker;
	protected Service service;
	private ServiceFactory serviceFactory = new ServiceFactory();

	/**
	 * Null Constructor
	 */
	public MessagingTestBase() {
	}

	protected synchronized void startBroker() {
		serviceFactory.setConnectionPool(new ActiveMQConnectionPool());
		serviceFactory.initialize("test-messaging-daemon");
		LOGGER.debug(broker != null ? "JMS Broker already started ---------" : "JMS Starting broker ------------");
		if (broker != null) {
			return;
		}
		// Start a local, vm-only broker with no port.
		broker = new JmsBrokerThread(JmsBrokerThread.getVmUri(), null);

		try {
			service = serviceFactory.createService(
					new URI(MessageFormat.format("jms.vm://localhost?broker.persistent=false&simplequeue={0}", TEST_QUEUE_NAME)));
		} catch (Exception t) {
			throw new MprcException(t);
		}
	}

	@AfterClass
	public void stopBroker() {
		LOGGER.debug("Stopping JMS broker");
		if (broker != null) {
			broker.stopBroker();
			broker = null;
		}
		serviceFactory.getConnectionPool().close();
		LOGGER.debug("JMS Broker stopped -------------");
	}
}
