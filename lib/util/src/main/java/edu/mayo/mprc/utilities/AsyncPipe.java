package edu.mayo.mprc.utilities;

import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Asynchronous pipe thread body. Used when running a process to read the data from its output.
 */
public final class AsyncPipe implements Runnable {
	private static final Logger LOGGER = Logger.getLogger(AsyncPipe.class);
	private static final int BUFFER_SIZE = 1024;

	private final InputStream inputStream;
	private final OutputStream outputStream;

	public AsyncPipe(final InputStream inputStream, final OutputStream outputStream) {
		this.inputStream = inputStream;
		this.outputStream = outputStream;
	}

	@Override
	public void run() {
		try {
			final byte[] buffer = new byte[BUFFER_SIZE];
			while (true) {
				final int length = inputStream.read(buffer);
				if (length == -1) {
					break;
				}
				outputStream.write(buffer, 0, length);
			}
		} catch (Exception e) {
			LOGGER.error("Error reading process output", e);
		}
	}
}
