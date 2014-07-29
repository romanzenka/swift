package edu.mayo.mprc.daemon.worker;

import edu.mayo.mprc.daemon.files.FileTokenHolder;
import edu.mayo.mprc.messaging.PrioritizedData;

import java.util.UUID;

/**
 * Any work packet sent to the daemon has to implement this interface.
 * All work packets must define serial id in following form:
 * {@code private static final long serialVersionUID = yyyymmdd;}
 * ... where {@code yyyymmdd} is the date of last modification.
 */
public interface WorkPacket extends FileTokenHolder, PrioritizedData {
	/**
	 * @return ID of the task. This is used to seed the parent log. The ID should be automatically created
	 * when a new task is made.
	 */
	UUID getTaskId();

	/**
	 * In rare cases, we might have to force the ID on the given work packet as provided from elsewhere.
	 *
	 * @param taskId New task ID
	 */
	void setTaskId(UUID taskId);

	/**
	 * @return True if the work requested should be redone from scratch, ignoring any previous cached results.
	 */
	boolean isFromScratch();
}
