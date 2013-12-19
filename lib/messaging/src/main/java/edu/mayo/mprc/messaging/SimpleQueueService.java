package edu.mayo.mprc.messaging;

import edu.mayo.mprc.MprcException;
import org.apache.log4j.Logger;

import javax.jms.*;
import java.io.Serializable;

/**
 * A JMS queue that allows request-response communication.
 * The responses are transferred using temporary queue, as described here:
 * http://activemq.apache.org/how-should-i-implement-request-response-with-jms.html
 * <p/>
 * Multithreaded access is resolved using threadlocal variables.
 */
final class SimpleQueueService implements Service {
	private static final Logger LOGGER = Logger.getLogger(SimpleQueueService.class);

	private final ServiceFactory serviceFactory;

	/**
	 * Each thread using the SimpleQueueService uses a separate session.
	 * A session for sending messages is separate from the session for receiving messages.
	 */
	private final ThreadLocal<Session> sendingSession = new ThreadLocal<Session>();
	private final ThreadLocal<Session> receivingSession = new ThreadLocal<Session>();

	/**
	 * Connection to the activemq server;
	 */
	private Connection connection;
	private Destination requestDestination;
	/**
	 * Cached producer for sending messages.
	 */
	private final ThreadLocal<MessageProducer> producer = new ThreadLocal<MessageProducer>();
	/**
	 * Cached consumer for receiving messages.
	 */
	private final ThreadLocal<MessageConsumer> consumer = new ThreadLocal<MessageConsumer>();

	/**
	 * Name of the queue to send requests to / receive responses from.
	 */
	private final String queueName;

	private final ResponseDispatcher responseDispatcher;

	/**
	 * Establishes a link of given name on a given broker.
	 * Each link consists of two JMS queues - one for sending requests, one (wrapped in ResponseDispatcher) for receiving responses.
	 *
	 * @param serviceFactory Service factory
	 * @param name           Name of the queue.
	 */
	SimpleQueueService(final ServiceFactory serviceFactory, final ResponseDispatcher responseDispatcher, final String name) {
		this.serviceFactory = serviceFactory;
		this.responseDispatcher = responseDispatcher;
		queueName = name;
	}

	@Override
	public String getName() {
		return queueName;
	}

	@Override
	public void sendRequest(final Serializable request, final int priority, final ResponseListener listener) {
		try {
			final ObjectMessage objectMessage = sendingSession().createObjectMessage(request);

			final int extraPriority = request instanceof PrioritizedData ? ((PrioritizedData) request).getPriority() : 0;
			objectMessage.setJMSPriority(priority + extraPriority);

			if (null != listener) {
				// Register the new listener on the temporary queue and remember its correlation ID
				final String correlationId = responseDispatcher.registerMessageListener(listener);
				// Replies go our temporary queue
				objectMessage.setJMSReplyTo(responseDispatcher.getResponseDestination());
				// Correlation ID matches the responses with the response listener
				objectMessage.setJMSCorrelationID(correlationId);
			}

			messageProducer().send(getRequestDestination(), objectMessage);
		} catch (JMSException e) {
			throw new MprcException("Could not send message", e);
		}
	}

	private MessageProducer messageProducer() throws JMSException {
		synchronized (this) {
			if (null == producer.get()) {
				final MessageProducer producer1 = sendingSession().createProducer(null);
				producer.set(producer1);
			}
			return producer.get();
		}
	}

	/**
	 * Wraps received message into an object that allows the receiver to send a response (if requested by sender).
	 *
	 * @param message Message to wrap
	 * @return Wrapped message
	 */
	private JmsRequest wrapReceivedMessage(final Message message) {
		return new JmsRequest((ObjectMessage) message, this);
	}

	/**
	 * To be used by JmsRequest for sending responses.
	 *
	 * @param response        User response.
	 * @param originalMessage Message this was response to.
	 * @param isLast          True if the message is the last one.
	 */
	void sendResponse(final Serializable response, final ObjectMessage originalMessage, final boolean isLast) {
		try {
			if (originalMessage.getJMSCorrelationID() != null) {
				// Response was requested
				final ObjectMessage responseMessage = receivingSession().createObjectMessage(response);
				responseMessage.setBooleanProperty(ResponseDispatcher.LAST_RESPONSE, isLast);
				responseMessage.setJMSCorrelationID(originalMessage.getJMSCorrelationID());
				messageProducer().send(originalMessage.getJMSReplyTo(), responseMessage);
			}

		} catch (JMSException e) {
			throw new MprcException(e);
		}
	}

	@Override
	public Request receiveRequest(final long timeout) {
		try {
			final Message message = messageConsumer().receive(timeout);
			if (message != null) {
				return wrapReceivedMessage(message);
			} else {
				return null;
			}
		} catch (JMSException e) {
			throw new MprcException("Could not receive message", e);
		}
	}

	private MessageConsumer messageConsumer() throws JMSException {
		synchronized (this) {
			if (null == consumer.get()) {
				consumer.set(receivingSession().createConsumer(getRequestDestination()));
			}
			return consumer.get();
		}
	}

	private Session receivingSession() {
		return setupSession(receivingSession);
	}

	private Session sendingSession() {
		return setupSession(sendingSession);
	}

	private Session setupSession(ThreadLocal<Session> sessionHolder) {
		if (sessionHolder.get() == null) {
			try {
				final Session value = createConnection().createSession(/*transacted?*/false, /*acknowledgment*/Session.CLIENT_ACKNOWLEDGE);
				sessionHolder.set(value);
			} catch (JMSException e) {
				throw new MprcException("Could not open JMS session", e);
			}
		}
		return sessionHolder.get();
	}

	private Connection createConnection() {
		synchronized (this) {
			if (connection == null) {
				if (!serviceFactory.isRunning()) {
					serviceFactory.start();
				}
				connection = serviceFactory.getConnection();
			}
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
				try {
					createConnection();

					setRequestDestination(sendingSession().createQueue(queueName));

					LOGGER.debug("Connected to JMS broker: " + connection.getClientID() + " queue: " + queueName);
				} catch (JMSException e) {
					throw new MprcException("Queue could not be created", e);
				}
			}
		}
	}

	@Override
	public void stop() {
		synchronized (this) {
			if (isRunning()) {
				closeSession(receivingSession);
				closeSession(sendingSession);
				if (null != consumer.get()) {
					try {
						consumer.get().close();
					} catch (JMSException e) {
						throw new MprcException(e);
					} finally {
						consumer.set(null);
					}
				}
				connection = null;
			}
		}
	}

	private static void closeSession(final ThreadLocal<Session> toClose) {
		final Session session = toClose.get();
		if (session != null) {
			try {
				session.close();
				toClose.set(null);
			} catch (JMSException e) {
				throw new MprcException("Could not close session", e);
			}
		}
	}

	/**
	 * This is where the requests get sent to. A destination supports concurrent use.
	 */
	public Destination getRequestDestination() {
		synchronized (this) {
			return requestDestination;
		}
	}

	public void setRequestDestination(Destination requestDestination) {
		synchronized (this) {
			this.requestDestination = requestDestination;
		}
	}
}
