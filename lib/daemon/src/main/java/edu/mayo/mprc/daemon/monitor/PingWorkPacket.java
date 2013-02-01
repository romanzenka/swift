package edu.mayo.mprc.daemon.monitor;

import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.files.FileTokenSynchronizer;
import edu.mayo.mprc.daemon.files.ReceiverTokenTranslator;
import edu.mayo.mprc.daemon.files.SenderTokenTranslator;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Set;

/**
 * @author Roman Zenka
 */
public final class PingWorkPacket implements WorkPacket {
	private static final long serialVersionUID = 4327500764702975292L;

	/**
	 * Low ping priority.
	 */
	private int priority = -1;

	@Override
	public String getTaskId() {
		return "ping";
	}

	@Override
	public boolean isFromScratch() {
		return true;
	}

	@Override
	public void translateOnSender(final SenderTokenTranslator translator) {
		// Nothing needs to be done - no files moved
	}

	@Override
	public void translateOnReceiver(final ReceiverTokenTranslator translator, final FileTokenSynchronizer synchronizer, @Nullable final Set<File> filesThatShouldExist) {
		// Nothing needs to be done - no files moved
	}

	@Override
	public void synchronizeFileTokensOnReceiver() {
		// Nothing needs to be done - no files moved
	}

	@Override
	public void setPriority(final int priority) {
		this.priority = priority;
	}

	@Override
	public int getPriority() {
		return priority;
	}
}
