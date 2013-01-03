package edu.mayo.mprc.messaging.rmi;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @deprecated This would not work in firewalled environment.
 */
public final class SimpleOneWayMessenger implements OneWayMessenger {
	private static final long serialVersionUID = 20090324L;
	private final List<MessageListener> messageListeners;

	public SimpleOneWayMessenger() {
		messageListeners = Collections.synchronizedList(new LinkedList<MessageListener>());
	}

	public void sendMessage(final Object message) throws RemoteException {
		try {
			synchronized (messageListeners) {
				for (final MessageListener messageListener : messageListeners) {
					messageListener.messageReceived(message);
				}
			}
		} catch (Exception e) {
			throw new RemoteException("Failed to notify listeners", e);
		}
	}

	public void addMessageListener(final MessageListener messageListener) {
		synchronized (messageListeners) {
			messageListeners.add(messageListener);
		}
	}

	public void removeMessageListener(final MessageListener messageListener) {
		synchronized (messageListeners) {
			messageListeners.remove(messageListener);
		}
	}
}
