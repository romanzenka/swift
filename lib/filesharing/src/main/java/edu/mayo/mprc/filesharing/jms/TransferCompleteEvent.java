package edu.mayo.mprc.filesharing.jms;

import java.io.File;

/**
 * Event represent file transfer completion. If file transfer operation
 * is successful, exception is null. Otherwise, exception object should
 * reference error exception.
 */
final class TransferCompleteEvent {

	private Exception exception;
	private File file;

	public TransferCompleteEvent(final File file, final Exception exception) {
		this.exception = exception;
		this.file = file;
	}

	public Exception getException() {
		return exception;
	}

	public void setException(final Exception exception) {
		this.exception = exception;
	}

	public File getFile() {
		return file;
	}

	public void setFile(final File file) {
		this.file = file;
	}
}
