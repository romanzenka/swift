package edu.mayo.mprc.messaging;

import edu.mayo.mprc.MprcException;
import org.apache.log4j.Logger;

import javax.jms.*;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A JMS queue that allows request-response communication.
 * The responses are transferred using temporary queue, as described here:
 * http://activemq.apache.org/how-should-i-implement-request-response-with-jms.html
 * <p/>
 * Never share the service among multiple threads - it is single-threaded only due to usage of Session.
 */
final class SimpleQueueService implements Service {
	private static final Logger LOGGER = Logger.getLogger(SimpleQueueService.class);

	private final Connection connection;
	/**
	 * Each thread using the SimpleQueueService uses a separate session.
	 * A session for sending messages is separate from the session for receiving messages.
	 */
	private final ThreadLocal<Session> sendingSession = new ThreadLocal<Session>();
	private final ThreadLocal<Session> receivingSession = new ThreadLocal<Session>();
	/**
	 * This is where the requests get sent to. A destination supports concurrent use.
	 */
	private final Destination requestDestination;
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

	/**
	 * A temporary queue created on the sending end that will receive responses to requests sent from this service.
	 */
	private final TemporaryQueue responseQueue;
	/**
	 * Map from correlation ID (request ID) to the response listener. Has to be synchronized, as an entry removal occurs
	 * asynchronously when message arrives, which could collide with entry adding.
	 */
	private final Map<String, ResponseListener> responseMap = Collections.synchronizedMap(new HashMap<String, ResponseListener>());
	/**
	 * An id for requests that allows responses to be matched with the original sender.
	 */
	private final AtomicInteger uniqueId = new AtomicInteger(0);

	/**
	 * The very last response to a request is marked with this boolean property set to true.
	 */
	public static final String LAST_RESPONSE = "is_last";

	/**
	 * Establishes a link of given name on a given broker.
	 * Each link consists of two JMS queues - one for sending request and a temporary response queue
	 * for each of the requests.
	 *
	 * @param name Name of the queue.
	 */
	SimpleQueueService(final Connection connection, final String name) {
		this.queueName = name;

		try {
			this.connection = connection;

			responseQueue = sendingSession().createTemporaryQueue();

			requestDestination = sendingSession().createQueue(queueName);

			final MessageConsumer tempQueueConsumer = sendingSession().createConsumer(responseQueue);
			tempQueueConsumer.setMessageListener(new TempQueueMessageListener());

			LOGGER.info("Connected to JMS broker: " + connection.getClientID() + " queue: " + queueName);
		} catch (JMSException e) {
			throw new MprcException("Queue could not be created", e);
		}
	}

	public String getName() {
		return queueName;
	}

	public void sendRequest(final Serializable request, final int priority, final ResponseListener listener) {
		try {
			final ObjectMessage objectMessage = sendingSession().createObjectMessage(request);

			if (null != listener) {
				// User wants response to the message.

				// Register the new listener on the temporary queue and remember its correlation ID
				final String correlationId = String.valueOf(uniqueId.incrementAndGet());
				responseMap.put(correlationId, listener);

				// Replies go our temporary queue
				objectMessage.setJMSReplyTo(responseQueue);
				// Correlation ID matches the responses with the response listener
				objectMessage.setJMSCorrelationID(correlationId);
				final int extraPriority = request instanceof PrioritizedData ? ((PrioritizedData) request).getPriority() : 0;
				objectMessage.setJMSPriority(priority + extraPriority);
			}
			LOGGER.debug("Sending message to [" + queueName + "] with content [" + objectMessage.toString() + "] id: [" + objectMessage.getJMSMessageID() + "]");
			messageProducer().send(requestDestination, objectMessage);
		} catch (JMSException e) {
			throw new MprcException("Could not send message", e);
		}
	}

	private synchronized MessageProducer messageProducer() throws JMSException {
		if (null == producer.get()) {
			producer.set(sendingSession().createProducer(null));
		}
		return producer.get();
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
				responseMessage.setBooleanProperty(SimpleQueueService.LAST_RESPONSE, isLast);
				responseMessage.setJMSCorrelationID(originalMessage.getJMSCorrelationID());
				messageProducer().send(originalMessage.getJMSReplyTo(), responseMessage);
				LOGGER.debug("Message sent: " + responseMessage.getJMSMessageID() + " timestamp: " + responseMessage.getJMSTimestamp());
			}

		} catch (JMSException e) {
			throw new MprcException(e);
		}
	}

	public Request receiveRequest(final long timeout) {
		try {
			final Message message = messageConsumer().receive(timeout);
			if (message != null) {
				LOGGER.debug("Request received from queue [" + queueName + "], contents [" + message.toString() + "]");
				return wrapReceivedMessage(message);
			} else {
				return null;
			}
		} catch (JMSException e) {
			throw new MprcException("Could not receive message", e);
		}
	}

	public synchronized void stopReceiving() {
		if (null != consumer.get()) {
			try {
				consumer.get().close();
			} catch (JMSException e) {
				throw new MprcException(e);
			} finally {
				consumer.set(null);
			}
		}
	}

	private synchronized MessageConsumer messageConsumer() throws JMSException {
		if (null == consumer.get()) {
			consumer.set(receivingSession().createConsumer(requestDestination));
		}
		return consumer.get();
	}

	private Session receivingSession() {
		return setupSession(this.receivingSession);
	}

	private Session sendingSession() {
		return setupSession(this.sendingSession);
	}

	private Session setupSession(ThreadLocal<Session> sessionHolder) {
		if (sessionHolder.get() == null) {
			try {
				final Session value = connection.createSession(/*transacted?*/false, /*acknowledgment*/Session.CLIENT_ACKNOWLEDGE);
				sessionHolder.set(value);
			} catch (JMSException e) {
				throw new MprcException("Could not open JMS session", e);
			}
		}
		return sessionHolder.get();
	}

	private class TempQueueMessageListener implements MessageListener {
		@Override
		public void onMessage(final Message message) {
			try {
				processMessage(message);
			} finally {
				acknowledgeMessage(message);
			}
		}

		/**
		 * Must never throw an exception.
		 */
		private void processMessage(final Message message) {
			boolean isLast = true;
			ResponseListener listener = null;
			try {
				final ObjectMessage objectMessage = (ObjectMessage) message;
				final Serializable messageData = objectMessage.getObject();
				final String listenerId = objectMessage.getJMSCorrelationID();
				listener = responseMap.get(listenerId);
				isLast = objectMessage.getBooleanProperty(SimpleQueueService.LAST_RESPONSE);
				if (listener == null) {
					LOGGER.error("No registered listener for response");
				} else {
					if (isLast) {
						responseMap.remove(listenerId);
					}
					listener.responseReceived(messageData, isLast);
				}
			} catch (Exception t) {
				// SWALLOWED: This method cannot throw exceptions, but it can pass them as a received object.
				if (null != listener) {
					try {
						listener.responseReceived(t, isLast);
					} catch (Exception e) {
						// SWALLOWED
						LOGGER.warn("The response listener failed", e);
					}
				} else {
					LOGGER.error("No registered listener for response, cannot report error", t);
				}
			}
		}

		private void acknowledgeMessage(final Message message) {
			if (message == null) {
				return;
			}
			try {
				message.acknowledge();
			} catch (JMSException e) {
				//SWALLOWED
				try {
					LOGGER.error("Failed to acknowledge message received. Message destination: " + message.getJMSDestination(), e);
				} catch (JMSException ignore) {
					//SWALLOWED
				}
			}
		}
	}
}
