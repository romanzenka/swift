package edu.mayo.mprc.messaging;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import edu.mayo.mprc.MprcException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * For debugging purposes.
 * <p/>
 * A thread safe class that can store every message being sent out, as well as all the messages
 * that were returned in response.
 * <p/>
 * Once a "last" message gets delivered, the whole batch of messages is purged - the transaction completed successfully.
 * <p/>
 * This allows tracking of which messages are in flight and also which messages are being delivered after the last one.
 *
 * @author Roman Zenka
 */
public final class MessagingTracker {
	private class MessageExchange {
		private Message message;
		private Serializable payload;
		private int sent;
		private int received;

		private MessageExchange(final boolean sent) {
			if (sent) {
				this.sent = 1;
			} else {
				received = 1;
			}
		}

		public MessageExchange(final Message message, final boolean sent) {
			this(sent);
			this.message = message;
		}

		public MessageExchange(final Serializable payload, final boolean sent) {
			this(sent);
			this.payload = payload;
		}

		public Message getMessage() {
			return message;
		}

		public boolean isSent() {
			return sent > 0;
		}

		public void addSent() {
			this.sent++;
		}

		public boolean isReceived() {
			return received > 0;
		}

		public void addReceived() {
			this.received++;
		}

		public boolean isLast() {
			try {
				if (message == null) {
					return false;
				}
				return message.propertyExists(ResponseDispatcher.LAST_RESPONSE) && message.getBooleanProperty(ResponseDispatcher.LAST_RESPONSE);
			} catch (JMSException e) {
				throw new MprcException(e);
			}
		}

		@Override
		public String toString() {
			try {
				final String sendReceive = isSent() && !isReceived() ? ">>> " : isReceived() && !isSent() ? "<!< " : "    ";
				if (message instanceof ObjectMessage) {
					final ObjectMessage objMessage = (ObjectMessage) message;
					return sendReceive + objectToString(objMessage.getObject());
				} else {
					return sendReceive + "[special] " + (message != null ? message.toString() : payloadToString());
				}
			} catch (JMSException e) {
				throw new MprcException("Could not convert message to string", e);
			}
		}

		private String payloadToString() {
			return payload != null ? payload.toString() : "[null]";
		}

		private String objectToString(final Serializable object) {
			if (object == null) {
				return "[null]";
			}
			return object.toString();
		}

		public boolean isProblem() {
			return sent != received || sent > 1 || received > 1;
		}
	}

	final ListMultimap<String, MessageExchange> messages = LinkedListMultimap.create(100);

	public void sendMessage(final Message message) {
		recordMessage(message, true);
	}

	public void receiveMessage(final Message message) {
		recordMessage(message, false);
	}

	public void serializeMessage(Serializable message, String key) {
		if (key == null) {
			// Messages with no correlation IDs get skipped
			return;
		}
		synchronized (messages) {
			messages.put(key, new MessageExchange(message, true));
		}
	}

	private void recordMessage(final Message message, final boolean send) {
		try {
			final String key = message.getJMSCorrelationID();
			if (key == null) {
				// Messages with no correlation IDs get skipped
				return;
			}
			synchronized (messages) {
				boolean found = false;
				for (final MessageExchange exchanges : messages.get(key)) {
					if (exchanges.getMessage() != null && exchanges.getMessage().getJMSMessageID().equals(message.getJMSMessageID())) {
						if (send) {
							exchanges.addSent();
						} else {
							exchanges.addReceived();
						}
						found = true;
						break;
					}
				}
				if (!found) {
					messages.put(key, new MessageExchange(message, send));
				}
			}
			toString();
		} catch (JMSException e) {
			throw new MprcException("Error tracking messages", e);
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(1000);
		final StringBuilder messageBuilder = new StringBuilder(1000);
		synchronized (messages) {
			for (final Map.Entry<String, Collection<MessageExchange>> entry : messages.asMap().entrySet()) {
				boolean wasProblem = false;
				messageBuilder.setLength(0);
				messageBuilder.append(entry.getKey()).append(":\n");
				boolean wasClosed = false;
				for (final MessageExchange message : entry.getValue()) {
					if (wasClosed) {
						wasProblem = true;
					}
					messageBuilder.append('\t').append(message.toString()).append('\n');
					if (message.isLast()) {
						messageBuilder.append("\t-----------\n");
						wasClosed = true;
					}
					if (message.isProblem()) {
						wasProblem = true;
					}
				}

				if (wasProblem) {
					builder.append(messageBuilder);
				}
			}
		}
		return builder.toString();
	}
}
