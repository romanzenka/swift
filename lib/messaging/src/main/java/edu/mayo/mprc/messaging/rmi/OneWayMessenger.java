package edu.mayo.mprc.messaging.rmi;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @deprecated This would not work in firewalled environment.
 */
public interface OneWayMessenger extends Remote, Serializable {
	/**
	 * This method is to called on the remote instance of this object.
	 *
	 * @param message
	 * @throws java.rmi.RemoteException
	 */
	void sendMessage(Object message) throws RemoteException;
}
