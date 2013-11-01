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

	private final ResponseDispatcher responseDispatcher;

	/**
	 * Name of the queue to send requests to / receive responses from.
	 */
	private final String queueName;

	/**
	 * Establishes a link of given name on a given broker.
	 * Each link consists of two JMS queues - one for sending requests, one (wrapped in ResponseDispatcher) for receiving responses.
	 *
	 * @param connection         The ActiveMQ connection to use.
	 * @param responseDispatcher Response dispatcher that can handle transfer of responses to the requests.
	 * @param name               Name of the queue.
	 */
	SimpleQueueService(final Connection connection, final ResponseDispatcher responseDispatcher, final String name) {
		queueName = name;
		this.responseDispatcher = responseDispatcher;

		try {
			this.connection = connection;

			requestDestination = sendingSession().createQueue(queueName);

			LOGGER.debug("Connected to JMS broker: " + connection.getClientID() + " queue: " + queueName);
		} catch (JMSException e) {
			throw new MprcException("Queue could not be created", e);
		}
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

			LOGGER.debug("Sending message to [" + queueName + "] with content [" + objectMessage.toString() + "] id: [" + objectMessage.getJMSMessageID() + "]");
			messageProducer().send(requestDestination, objectMessage);
		} catch (JMSException e) {
			throw new MprcException("Could not send message", e);
		}
	}

	private synchronized MessageProducer messageProducer() throws JMSException {
		if (null == producer.get()) {
			final MessageProducer producer1 = sendingSession().createProducer(null);
			producer.set(producer1);
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
				responseMessage.setBooleanProperty(ResponseDispatcher.LAST_RESPONSE, isLast);
				responseMessage.setJMSCorrelationID(originalMessage.getJMSCorrelationID());
				messageProducer().send(originalMessage.getJMSReplyTo(), responseMessage);
				LOGGER.debug("Message sent: " + responseMessage.getJMSMessageID() + " timestamp: " + responseMessage.getJMSTimestamp());
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
				LOGGER.debug("Request received from queue [" + queueName + "], contents [" + message.toString() + "]");
				return wrapReceivedMessage(message);
			} else {
				return null;
			}
		} catch (JMSException e) {
			throw new MprcException("Could not receive message", e);
		}
	}

	@Override
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
		return setupSession(receivingSession);
	}

	private Session sendingSession() {
		return setupSession(sendingSession);
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

}
