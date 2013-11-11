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
 */
public final class ServiceFactory implements Lifecycle {
	private static final Logger LOGGER = Logger.getLogger(ServiceFactory.class);

	private URI brokerUri;
	private String daemonName;

	private ActiveMQConnectionPool connectionPool;
	private Connection connection;
	private ResponseDispatcher responseDispatcher;
	private RunningApplicationContext context;

	public ServiceFactory() {
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
		return new SimpleQueueService(getConnection(), getResponseDispatcher(), name);
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

	public String getDaemonName() {
		if (daemonName == null && context != null) {
			daemonName = context.getDaemonConfig().getName();
		}
		return daemonName;
	}

	public void setDaemonName(String daemonName) {
		this.daemonName = daemonName;
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

	public SerializedRequest serializeRequest(final Serializable message, final ResponseListener listener) {
		return new SerializedRequest(getResponseDispatcher().getResponseQueueName(), message, getResponseDispatcher().registerMessageListener(listener));
	}

	public Request deserializeRequest(final SerializedRequest serializedRequest) {
		return new DeserializedRequest(getConnection(), serializedRequest);
	}

	public Connection getConnection() {
		return connection;
	}

	public ResponseDispatcher getResponseDispatcher() {
		if (responseDispatcher == null) {
			throw new MprcException("This service factory does not support response dispatch. This probably because it is not running within a daemon (daemonName is set to " + daemonName + ")");
		}
		return responseDispatcher;
	}

	@Override
	public boolean isRunning() {
		return connection != null;
	}

	@Override
	public void start() {
		if (connection == null) {
			final UserInfo info = extractJmsUserinfo(getBrokerUri());
			connection = getConnectionPool().getConnectionToBroker(getBrokerUri(), info.getUserName(), info.getPassword());
		}
		if (responseDispatcher == null && getDaemonName() != null) {
			responseDispatcher = new ResponseDispatcher(connection, getDaemonName());
		}
	}

	@Override
	public void stop() {
		getResponseDispatcher().close();
		connectionPool.close();
		connectionPool = null;
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
