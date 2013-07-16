package edu.mayo.mprc.messaging;

import com.google.common.base.Strings;
import edu.mayo.mprc.MprcException;
import org.apache.log4j.Logger;

import javax.jms.*;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Returns a service using a queue of a given name.
 * All services need to be queued using a single JMS connection that has to be initialized by calling
 * {@link #initialize}.
 */
public final class ServiceFactory {
	private static final Logger LOGGER = Logger.getLogger(ServiceFactory.class);

	private URI brokerUri;
	private ActiveMQConnectionPool connectionPool;
	private Connection connection;
	private ResponseDispatcher responseDispatcher;

	public ServiceFactory() {
	}

	/**
	 * Give the service factory the name of the daemon we operate within, so the response queue can
	 * be properly established.
	 *
	 * @param daemonName There should be a single response queue per daemon, it is named using the daemon's name.
	 */
	public void initialize(final String brokerUriString, final String daemonName) {
		try {
			brokerUri = new URI(brokerUriString);
		} catch (URISyntaxException e) {
			throw new MprcException("Invalid broker URI", e);
		}

		final UserInfo info = extractJmsUserinfo(brokerUri);

		connection = getConnectionPool().getConnectionToBroker(brokerUri, info.getUserName(), info.getPassword());

		if (daemonName == null) {
			throw new MprcException("The daemon name has to be set before a ServiceFactory can be used");
		}
		if (responseDispatcher == null) {
			responseDispatcher = new ResponseDispatcher(connection, daemonName);
		}
	}

	/**
	 * Creates a service ({@link Service})
	 * <p/>
	 * The implementation can be virtually anything. What gets created is determined
	 * by the URI format that is passed in. So far we implement only simple JMS queues.
	 *
	 * @param queueName Name of the queue belonging to the service.
	 * @return Service running at the given URI.
	 * @throws MprcException Service could not be created.
	 */
	public Service createService(final String queueName) {
		// TODO: This is hardcoded right now. Eventually would allow registering of new URI handlers.
		if (Strings.isNullOrEmpty(queueName)) {
			throw new MprcException("queue name must not be empty");
		}

		return createJmsQueue(queueName);
	}

	static UserInfo extractJmsUserinfo(final URI serviceURI) {
		return new UserInfo(serviceURI);
	}

	/**
	 * Creates a simple message queue. The queue allows one producer to send messages to one consumer.
	 * The consumer can send responses back.
	 *
	 * @return Service based on a simple queue that can be used for both sending and receiving of messages.
	 */
	public Service createJmsQueue(final String name) {
		return new SimpleQueueService(connection, responseDispatcher, name);
	}

	public ActiveMQConnectionPool getConnectionPool() {
		return connectionPool;
	}

	public void setConnectionPool(final ActiveMQConnectionPool connectionPool) {
		this.connectionPool = connectionPool;
	}

	public SerializedRequest serializeRequest(final Serializable message, final ResponseListener listener) {
		return new SerializedRequest(brokerUri.toString(), responseDispatcher.getResponseQueueName(), message, responseDispatcher.registerMessageListener(listener));
	}

	public Request deserializeRequest(final SerializedRequest serializedRequest) {
		return new DeserializedRequest(connection, serializedRequest);
	}

	private static class DeserializedRequest implements Request {
		private final SerializedRequest serializedRequest;
		private final Session session;
		private final MessageProducer producer;

		public DeserializedRequest(final Connection connection, final SerializedRequest serializedRequest) {
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
