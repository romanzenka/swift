package edu.mayo.mprc.messaging;

import java.io.Serializable;

/**
 * Identifies a request that was transferred out of band, without using JMS.
 *
 * @author Roman Zenka
 */
public final class SerializedRequest implements Serializable {
	private static final long serialVersionUID = 3082080221081174799L;

	private Serializable message;
	private String jmsCorrelationId;
	private String responseQueueName;

	public SerializedRequest(final String responseQueueName, final Serializable message, final String jmsCorrelationId) {
		this.responseQueueName = responseQueueName;
		this.message = message;
		this.jmsCorrelationId = jmsCorrelationId;
	}

	public Serializable getMessage() {
		return message;
	}

	public String getResponseQueueName() {
		return responseQueueName;
	}

	public String getJmsCorrelationId() {
		return jmsCorrelationId;
	}
}
