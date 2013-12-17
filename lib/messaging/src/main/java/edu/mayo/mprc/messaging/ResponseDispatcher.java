package edu.mayo.mprc.messaging;

import edu.mayo.mprc.MprcException;
import org.apache.log4j.Logger;

import javax.jms.*;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A Java process would ideally have a single temporary queue to handle all the responses.
 * Idea being that response processing is very fast, and opening a separate queue for each request
 * incurs a lot of overhead for the broker.
 * <p/>
 * This class is able to:
 * - provide a destination where to send responses
 * - register a listener to process responses matching a unique correlation id
 *
 * @author Roman Zenka
 */
public final class ResponseDispatcher {
	private static final Logger LOGGER = Logger.getLogger(ResponseDispatcher.class);

	private final Session session;

	private final String queueName;
	/**
	 * A temporary queue created on the sending end that will receive responses to requests sent from this service.
	 */
	private final Queue responseQueue;
	/**
	 * Map from correlation ID (request ID) to the response listener. Has to be synchronized, as an entry removal occurs
	 * asynchronously when message arrives, which could collide with entry adding.
	 */
	private final Map<String, ResponseListener> responseMap = Collections.synchronizedMap(new HashMap<String, ResponseListener>());
	/**
	 * An id for requests that allows responses to be matched with the original sender.
	 */
	private final AtomicLong uniqueId = new AtomicLong(System.currentTimeMillis());

	/**
	 * The very last response to a request is marked with this boolean property set to true.
	 */
	public static final String LAST_RESPONSE = "is_last";

	private final MessageConsumer queueConsumer;

	/**
	 * @param connection Connection to the broker.
	 * @param daemonName Unique name of the daemon we are running (will be used for the response queue)
	 */
	public ResponseDispatcher(final Connection connection, final String daemonName) {
		try {
			session = connection.createSession(/*transacted?*/false, /*acknowledgment*/Session.CLIENT_ACKNOWLEDGE);
			queueName = "responses-" + daemonName;
			responseQueue = session.createQueue(queueName);
			queueConsumer = session.createConsumer(responseQueue);
			queueConsumer.setMessageListener(new ResponseQueueMessageListener());

		} catch (JMSException e) {
			throw new MprcException("Could not create response queue", e);
		}
	}

	public Destination getResponseDestination() {
		return responseQueue;
	}

	public String getResponseQueueName() {
		return queueName;
	}

	/**
	 * Given a listener, registers it for particular correlation id, which is generated and returned.
	 *
	 * @param listener Listener to register.
	 * @return Correlation ID for messages that should be processed by this listener.
	 */
	public String registerMessageListener(final ResponseListener listener) {
		final String correlationId = String.valueOf(uniqueId.incrementAndGet());
		responseMap.put(correlationId, listener);
		return correlationId;
	}

	public void close() {
		try {
			queueConsumer.close();
			session.close();
			if (!responseMap.isEmpty()) {
				LOGGER.warn("The response dispatcher map of listeners is not empty - not all communication ended properly.");
				responseMap.clear();
			}
		} catch (JMSException e) {
			throw new MprcException(e);
		}
	}

	private class ResponseQueueMessageListener implements MessageListener {
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
				isLast = objectMessage.getBooleanProperty(LAST_RESPONSE);
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
