package edu.mayo.mprc.messaging;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.MessageBroker;
import org.apache.log4j.Logger;
import org.testng.annotations.AfterClass;

public abstract class MessagingTestBase {
	private static final Logger LOGGER = Logger.getLogger(MessagingTestBase.class);
	private static final String TEST_QUEUE_NAME = "test_queue";
	protected MessageBroker broker;
	protected Service service;
	protected ServiceFactory serviceFactory;

	/**
	 * Null Constructor
	 */
	public MessagingTestBase() {
	}

	protected synchronized void startBroker() {
		LOGGER.debug(broker != null ? "JMS Broker already started ---------" : "JMS Starting broker ------------");
		if (broker != null) {
			return;
		}
		// Start a local, vm-only broker with no port.
		broker = new MessageBroker();
		broker.setBrokerUrl("vm://broker");


		serviceFactory = new ServiceFactory();
		serviceFactory.setConnectionPool(new ActiveMQConnectionPool());
		serviceFactory.setDaemonName("test-messaging-daemon");

		try {
			service = serviceFactory.createService(TEST_QUEUE_NAME);
		} catch (Exception t) {
			throw new MprcException(t);
		}
	}

	@AfterClass
	public void stopBroker() {
		if (serviceFactory != null) {
			serviceFactory.getConnectionPool().close();
			serviceFactory = null;
		}
		if (broker != null) {
			LOGGER.debug("Stopping JMS broker");
			broker.stop();
			broker = null;
			LOGGER.debug("JMS Broker stopped -------------");
		}
	}
}
