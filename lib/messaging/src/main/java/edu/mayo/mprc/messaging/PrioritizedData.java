package edu.mayo.mprc.messaging;

import java.io.Serializable;

/**
 * A chunk of data with a delivery priority attached to it.
 *
 * @author Roman Zenka
 */
public interface PrioritizedData extends Serializable {

	/**
	 * @param priority Priority of this packet. 0 is default, -1 is low priority, +1 is high priority.
	 */
	void setPriority(int priority);

	/**
	 * @return Priority of this packet. 0 is default, -1 is low priority, +1 is high priority.
	 */
	int getPriority();
}
