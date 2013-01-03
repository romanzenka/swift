package edu.mayo.mprc.messaging.rmi;

import java.io.Serializable;

/**
 * @deprecated This would not work in firewalled environment.
 */
public interface MessageListener extends Serializable {
	void messageReceived(Object message);
}
