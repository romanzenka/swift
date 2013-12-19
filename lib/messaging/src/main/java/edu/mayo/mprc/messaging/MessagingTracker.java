package edu.mayo.mprc.messaging;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import edu.mayo.mprc.MprcException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
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
		private int sent;
		private int received;

		public MessageExchange(final Message message, final boolean sent) {
			this.message = message;
			if (sent) {
				this.sent = 1;
			} else {
				received = 1;
			}
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

		@Override
		public String toString() {
			try {
				final String sendReceive = isSent() && !isReceived() ? ">>> " : isReceived() && !isSent() ? "<!< " : "    ";
				if (message instanceof ObjectMessage) {
					final ObjectMessage objMessage = (ObjectMessage) message;
					return sendReceive + (objMessage.getObject() == null ? "[null]" : objMessage.getObject().toString());
				} else {
					return sendReceive + " non-object message " + message.toString();
				}
			} catch (JMSException e) {
				throw new MprcException("Could not convert message to string", e);
			}
		}

	}

	final ListMultimap<String, MessageExchange> messages = LinkedListMultimap.create(100);

	public void sendMessage(final Message message) {
		recordMessage(message, true);
	}

	public void receiveMessage(final Message message) {
		recordMessage(message, false);
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
					if (exchanges.getMessage().getJMSMessageID().equals(message.getJMSMessageID())) {
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
				if (message.propertyExists(ResponseDispatcher.LAST_RESPONSE)) {
					if (message.getBooleanProperty(ResponseDispatcher.LAST_RESPONSE)) {
						messages.removeAll(key);
					}
				}
			}
		} catch (JMSException e) {
			throw new MprcException("Error tracking messages", e);
		}
	}

	public String dump() {
		final StringBuilder builder = new StringBuilder(1000);
		synchronized (messages) {
			for (final Map.Entry<String, Collection<MessageExchange>> entry : messages.asMap().entrySet()) {
				builder.append(entry.getKey()).append(":\n");
				for (final MessageExchange message : entry.getValue()) {
					builder.append('\t').append(message.toString()).append('\n');
				}
			}
		}
		return builder.toString();
	}
}
