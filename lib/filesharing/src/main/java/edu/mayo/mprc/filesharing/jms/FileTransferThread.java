package edu.mayo.mprc.filesharing.jms;

/**
 * Super class for
 */
abstract class FileTransferThread extends Thread {

	private TransferCompleteListener listener;

	protected FileTransferThread(final String name) {
		super(name);
	}

	public TransferCompleteListener getTransferCompleteListener() {
		return listener;
	}

	public void setTransferCompleteListener(final TransferCompleteListener listener) {
		this.listener = listener;
	}
}
