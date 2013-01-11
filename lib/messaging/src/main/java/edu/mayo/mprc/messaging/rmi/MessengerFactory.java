package edu.mayo.mprc.messaging.rmi;

import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;

import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class provide functionality to create local and get remote BoundMessenger object.
 *
 * @deprecated This would not work in firewalled environment.
 */
public final class MessengerFactory {

	private AtomicLong messengerIdentifier = new AtomicLong();
	private RemoteObjectHandler remoteObjectHandler;

	public MessengerFactory(final RemoteObjectHandler handler) {
		this.remoteObjectHandler = handler;
	}

	public BoundMessenger<SimpleOneWayMessenger> createMessenger() throws RemoteException, UnknownHostException {

		final SimpleOneWayMessenger simpleMessenger = new SimpleOneWayMessenger();
		final String messengerName = SimpleOneWayMessenger.class.getName() + messengerIdentifier.incrementAndGet();
		remoteObjectHandler.registerRemoteObject(messengerName, simpleMessenger);

		return new BoundMessenger<SimpleOneWayMessenger>(new MessengerInfo(remoteObjectHandler.getHost(), remoteObjectHandler.getLocalPort(), messengerName), simpleMessenger, remoteObjectHandler);
	}

	public BoundMessenger<SimpleOneWayMessenger> createOneWayMessenger() throws RemoteException, UnknownHostException {

		final SimpleOneWayMessenger simpleOneWayMessenger = new SimpleOneWayMessenger();
		final String messengerName = SimpleOneWayMessenger.class.getName() + messengerIdentifier.incrementAndGet();
		remoteObjectHandler.registerRemoteObject(messengerName, simpleOneWayMessenger);

		return new BoundMessenger<SimpleOneWayMessenger>(
				new MessengerInfo(remoteObjectHandler.getHost(), remoteObjectHandler.getLocalPort(), messengerName),
				simpleOneWayMessenger,
				remoteObjectHandler);
	}

	public BoundMessenger<OneWayMessenger> getOneWayMessenger(final MessengerInfo messengerInfo) throws NotBoundException, RemoteException {

		final Remote remoteObject = remoteObjectHandler.getRemoteObject(messengerInfo.getRegistryInfo(), messengerInfo.getMessengerRemoteName());
		if (remoteObject instanceof OneWayMessenger) {
			final OneWayMessenger messenger = (OneWayMessenger) remoteObject;
			return new BoundMessenger<OneWayMessenger>(
					messengerInfo,
					messenger,
					remoteObjectHandler);
		} else {
			ExceptionUtilities.throwCastException(remoteObject, OneWayMessenger.class);
			return null;
		}
	}
}
