package edu.mayo.mprc.daemon.exception;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.MonitorUtilities;

/**
 * Exception thrown by {@link edu.mayo.mprc.daemon.Worker} or by {@link edu.mayo.mprc.daemon.AbstractRunner}.
 * <p/>
 * Compared to a normal MprcException, this one contains information about the host where the error happened.
 */
public final class DaemonException extends MprcException {
	private static final long serialVersionUID = 20071220L;

	private String host;

	public DaemonException() {
		host = MonitorUtilities.getHostInformation();
	}

	public DaemonException(final String message) {
		super(message);
	}

	public DaemonException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public DaemonException(final Throwable cause) {
		super(cause);
	}

	public String getHostString() {
		return host;
	}
}
