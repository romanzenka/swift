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
	@Override
	public String getTaskId() {
		return "ping";
	}

	@Override
	public boolean isFromScratch() {
		return true;
	}

	@Override
	public void translateOnSender(SenderTokenTranslator translator) {
		// Nothing needs to be done - no files moved
	}

	@Override
	public void translateOnReceiver(ReceiverTokenTranslator translator, FileTokenSynchronizer synchronizer, @Nullable Set<File> filesThatShouldExist) {
		// Nothing needs to be done - no files moved
	}

	@Override
	public void synchronizeFileTokensOnReceiver() {
		// Nothing needs to be done - no files moved
	}
}
