package edu.mayo.mprc.messaging;

import edu.mayo.mprc.MprcException;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import java.io.Serializable;

/**
 * Request received from JMS. Knows how to deliver response. Internal implementation of generic {@link edu.mayo.mprc.messaging.Request}.
 */
class JmsRequest implements Request {
	private ObjectMessage objectMessage;
	private SimpleQueueService receivedFrom;

	/**
	 * Id that lets us correlate the response with a particular request.
	 */
	private boolean lastResponseSent;

	/**
	 * {@link edu.mayo.mprc.messaging.Request} implementation. Knows where to send the response to (combination of {@link javax.jms.Destination} and coordination ID).
	 *
	 * @param objectMessage Message object for this request.
	 */
	JmsRequest(final ObjectMessage objectMessage, final SimpleQueueService receivedFrom) {
		this.receivedFrom = receivedFrom;
		this.lastResponseSent = false;
		this.objectMessage = objectMessage;
	}

	/**
	 * Acknowledge to the JMS broker that the request has been processed.
	 */
	@Override
	public void processed() {
		try {
			objectMessage.acknowledge();
		} catch (JMSException e) {
			throw new MprcException("Error acknowledging JMS request message.", e);
		}
	}

	@Override
	public Serializable getMessageData() {
		try {
			return objectMessage.getObject();
		} catch (JMSException e) {
			throw new MprcException("Error occurred while getting the data object from message.", e);
		}
	}

	@Override
	public void sendResponse(final Serializable response, final boolean isLast) {
		assert !lastResponseSent : "Last response was already sent.";
		lastResponseSent = isLast;
		receivedFrom.sendResponse(response, objectMessage, isLast);
	}
}
