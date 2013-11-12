package edu.mayo.mprc.daemon.worker;

import edu.mayo.mprc.config.DaemonConfigInfo;
import edu.mayo.mprc.daemon.files.FileHolder;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.daemon.files.FileTokenHolder;

/**
 * A base for all messaging packets.
 * Contains task identifier to be used in the nested diagnostic context.
 */
public abstract class WorkPacketBase extends FileHolder implements WorkPacket {
	private static final long serialVersionUID = 20071228L;
	private String taskId;
	private int priority;
	private boolean fromScratch;

	/**
	 * Supports {@link #simulateTransfer(FileTokenHolder)} by providing a dummy token factory that is set
	 * to a local daemon that shares everything.
	 */
	private static final FileTokenFactory NULL_TOKEN_FACTORY = new FileTokenFactory(new DaemonConfigInfo("null daemon", ""));

	/**
	 * @param taskId      Task identifier to be used for nested diagnostic context when logging.
	 * @param fromScratch
	 */
	public WorkPacketBase(final String taskId, final boolean fromScratch) {
		this.taskId = taskId;
		this.fromScratch = fromScratch;
	}

	@Override
	public int getPriority() {
		return priority;
	}

	/**
	 * @param priority Priority for this work packet. +1 - high, -1 low, 0 - normal
	 */
	@Override
	public void setPriority(final int priority) {
		this.priority = priority;
	}

	@Override
	public String getTaskId() {
		return taskId;
	}

	@Override
	public boolean isFromScratch() {
		return fromScratch;
	}

	/**
	 * This method pretends the packet was transfered via the messaging services, useful mostly for testing.
	 * <p/>
	 * Normally, work packets are designed to be transfered through our messaging services.
	 * However, for testing it is useful to directly create both packet and consumer and call both ends
	 * at once, as if local transfer took place.
	 */
	public static void simulateTransfer(final FileTokenHolder workPacket) {
		workPacket.translateOnSender(NULL_TOKEN_FACTORY);
		workPacket.translateOnReceiver(NULL_TOKEN_FACTORY, null);
	}
}
