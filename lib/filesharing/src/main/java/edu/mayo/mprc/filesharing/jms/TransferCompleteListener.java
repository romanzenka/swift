package edu.mayo.mprc.filesharing.jms;

import java.util.EventListener;

interface TransferCompleteListener extends EventListener {
	void transferCompleted(TransferCompleteEvent event);
}
