package edu.mayo.mprc.messaging;

import com.google.common.base.Strings;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.Lifecycle;
import edu.mayo.mprc.config.RunningApplicationContext;
import edu.mayo.mprc.daemon.MessageBroker;
import org.apache.log4j.Logger;

import javax.jms.*;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Returns a service using a queue of a given name.
 * This has to be thread-safe as multiple users can start this from different threads.
 */
public final class ServiceFactory implements Lifecycle {
	private static final Logger LOGGER = Logger.getLogger(ServiceFactory.class);

	private URI brokerUri;

	private ActiveMQConnectionPool connectionPool;
	private Connection connection;
	private RunningApplicationContext context;

	public ServiceFactory() {
	}

	/**
	 * Creates a service ({@link Service})
	 * <p/>
	 * The implementation can be virtually anything. What gets created is determined
	 * by the URI format that is passed in. So far we implement only simple JMS queues.
	 *
	 * @param queueName          Name of the queue belonging to the service.
	 * @param responseDispatcher Dispatcher for responses sent to this service. Can be null for unidirectional messaging.
	 * @return Service running at the given URI.
	 * @throws MprcException Service could not be created.
	 */
	public Service createService(final String queueName, final ResponseDispatcher responseDispatcher) {
		if (!isRunning()) {
			throw new MprcException("Cannot use ServiceFactory if it is not running");
		}
		// TODO: This is hardcoded right now. Eventually would allow registering of new URI handlers.
		if (Strings.isNullOrEmpty(queueName)) {
			throw new MprcException("queue name must not be empty");
		}

		return createJmsQueue(queueName, responseDispatcher);
	}

	static UserInfo extractJmsUserinfo(final URI serviceURI) {
		return new UserInfo(serviceURI);
	}

	/**
	 * Creates a simple message queue. The queue allows one producer to send messages to one consumer.
	 * The consumer can send responses back.
	 *
	 * @param name               Name of the queue
	 * @param responseDispatcher The service that can dispatch responses for messages sent to this queue
	 * @return Service based on a simple queue that can be used for both sending and receiving of messages.
	 */
	public Service createJmsQueue(final String name, final ResponseDispatcher responseDispatcher) {
		if (!isRunning()) {
			throw new MprcException("Cannot use ServiceFactory if it is not running");
		}
		return new SimpleQueueService(this, responseDispatcher, name);
	}

	public ActiveMQConnectionPool getConnectionPool() {
		return connectionPool;
	}

	public void setConnectionPool(final ActiveMQConnectionPool connectionPool) {
		this.connectionPool = connectionPool;
	}

	public RunningApplicationContext getContext() {
		return context;
	}

	public void setContext(final RunningApplicationContext context) {
		this.context = context;
	}

	public URI getBrokerUri() {
		if (brokerUri == null && context != null) {
			MessageBroker.Config config = context.getSingletonConfig(MessageBroker.Config.class);
			if (config == null) {
				throw new MprcException("The application does not define a message broker");
			}
			try {
				brokerUri = new URI(config.getBrokerUrl());
			} catch (URISyntaxException e) {
				throw new MprcException("The broker URI is in invalid format: " + config.getBrokerUrl(), e);
			}
		}
		return brokerUri;
	}

	public void setBrokerUri(URI brokerUri) {
		this.brokerUri = brokerUri;
	}

	public SerializedRequest serializeRequest(final Serializable message, final ResponseDispatcher responseDispatcher, final ResponseListener listener) {
		return new SerializedRequest(responseDispatcher.getResponseQueueName(), message, responseDispatcher.registerMessageListener(listener));
	}

	public Request deserializeRequest(final SerializedRequest serializedRequest) {
		return new DeserializedRequest(getConnection(), serializedRequest);
	}

	public Connection getConnection() {
		synchronized (this) {
			return connection;
		}
	}

	@Override
	public boolean isRunning() {
		synchronized (this) {
			return connection != null;
		}
	}

	@Override
	public void start() {
		synchronized (this) {
			if (!isRunning()) {
				if (connection == null) {
					final UserInfo info = extractJmsUserinfo(getBrokerUri());
					connection = getConnectionPool().getConnectionToBroker(getBrokerUri(), info.getUserName(), info.getPassword());
				}
			}
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			synchronized (this) {
				try {
					connection.close();
				} catch (JMSException e) {
					// SWALLOWED
					LOGGER.warn("Could not close connection when shutting down service", e);
				}
				connection = null;
			}
			connectionPool.close();
			connectionPool = null;
		}
	}

	private static class DeserializedRequest implements Request {
		private final SerializedRequest serializedRequest;
		private final Session session;
		private final MessageProducer producer;

		DeserializedRequest(final Connection connection, final SerializedRequest serializedRequest) {
			this.serializedRequest = serializedRequest;
			try {
				session = connection.createSession(/*transacted*/false, Session.CLIENT_ACKNOWLEDGE);
				final Queue queue = session.createQueue(serializedRequest.getResponseQueueName());
				producer = session.createProducer(queue);
			} catch (JMSException e) {
				throw new MprcException("Could not create session", e);
			}
		}

		@Override
		public Serializable getMessageData() {
			return serializedRequest.getMessage();
		}

		@Override
		public void sendResponse(final Serializable response, final boolean isLast) {
			try {
				// Response was requested
				final ObjectMessage responseMessage = session.createObjectMessage(response);
				responseMessage.setBooleanProperty(ResponseDispatcher.LAST_RESPONSE, isLast);
				responseMessage.setJMSCorrelationID(serializedRequest.getJmsCorrelationId());
				producer.send(responseMessage);
				LOGGER.debug("Message sent: " + responseMessage.getJMSMessageID() + " timestamp: " + responseMessage.getJMSTimestamp());
			} catch (JMSException e) {
				throw new MprcException(e);
			}
		}

		@Override
		public void processed() {
			try {
				producer.close();
				session.close();
			} catch (Exception e) {
				throw new MprcException(e);
			}
		}
	}
}
