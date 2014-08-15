package edu.mayo.mprc.messaging;

import edu.mayo.mprc.config.Lifecycle;
import edu.mayo.mprc.config.RunningApplicationContext;

import javax.jms.Connection;
import java.io.Serializable;

/**
 * @author Roman Zenka
 */
public interface ServiceFactory extends Lifecycle {
	Service createService(String queueName, ResponseDispatcher responseDispatcher);

	RunningApplicationContext getContext();

	SerializedRequest serializeRequest(Serializable message, ResponseDispatcher responseDispatcher, ResponseListener listener);

	Request deserializeRequest(SerializedRequest serializedRequest);

	Connection getConnection();
}
