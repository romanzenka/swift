package edu.mayo.mprc.messaging.rmi;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Object contains a Messenger object information and the Messenger itself. The Messenger
 * object is assume to be bound to a RMI Registry object.
 *
 * @deprecated This would not work in firewalled environment.
 */
public final class BoundMessenger<T> {

	private MessengerInfo messengerInfo;
	private T messenger;
	private RemoteObjectHandler remoteObjectHandler;

	public BoundMessenger(final MessengerInfo messengerInfo, final T messenger, final RemoteObjectHandler remoteObjectHandler) {
		this.messengerInfo = messengerInfo;
		this.messenger = messenger;
		this.remoteObjectHandler = remoteObjectHandler;
	}

	public MessengerInfo getMessengerInfo() {
		return messengerInfo;
	}

	public void setMessengerInfo(final MessengerInfo messengerInfo) {
		this.messengerInfo = messengerInfo;
	}

	public T getMessenger() {
		return messenger;
	}

	public void setMessenger(final T messenger) {
		this.messenger = messenger;
	}

	/**
	 * Removes Messenger object from the local registry.
	 *
	 * @throws RemoteException
	 * @throws UnknownHostException
	 */
	public void dispose() throws RemoteException, UnknownHostException {
		if (messengerInfo.getRegistryInfo().getHostName().equals(InetAddress.getLocalHost().getHostName())) {
			try {
				remoteObjectHandler.unregisterRemoteObject(messengerInfo.getMessengerRemoteName());
			} catch (NotBoundException e) {
				//SWALLOWED
			}
		}
	}
}
