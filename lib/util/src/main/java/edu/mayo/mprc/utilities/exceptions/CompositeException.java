package edu.mayo.mprc.utilities.exceptions;

import edu.mayo.mprc.MprcException;
import org.apache.log4j.Logger;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Eric Winter
 */
public final class CompositeException extends MprcException {
	private static final long serialVersionUID = 20071220L;
	private static final Logger LOGGER = Logger.getLogger(CompositeException.class);

	private List<Throwable> causes = new ArrayList<Throwable>(2);

	public CompositeException() {
		super();
	}

	public CompositeException(final Exception cause) {
		super(cause);
	}

	public CompositeException(final String message) {
		super(message);
	}

	public CompositeException(final String message, final Exception cause) {
		super(message, cause);
	}

	public CompositeException(final Collection<Exception> causes) {
		this.causes.addAll(causes);
	}

	public void addCauses(final Collection<Exception> causes) {
		this.causes.addAll(causes);
	}

	public void addCause(final Throwable cause) {
		causes.add(cause);
	}

	public Collection<Throwable> getCauses() {
		return causes;
	}

	public String getMainMessage() {
		if (super.getMessage() != null) {
			return super.getMessage();
		} else {
			return "Composite exception";
		}
	}

	@Override
	public String toString() {
		return getMessage();
	}

	@Override
	public String getMessage() {
		final StringBuilder sb = new StringBuilder();
		sb.append(getMainMessage()).append(":");
		int num = 1;
		for (final Throwable t : causes) {
			sb.append("\n").append(num).append(") ").append(MprcException.getDetailedMessage(t));
			num++;
		}
		return sb.toString();
	}

	@Override
	public void printStackTrace() {
		for (final Throwable t : causes) {
			t.printStackTrace();
		}
	}

	@Override
	public void printStackTrace(final PrintWriter p) {
		int i = 1;
		final int total = causes.size();
		for (final Throwable t : causes) {
			LOGGER.error(getMainMessage() + " (" + i + "/" + total + ")", t);
			i++;
		}
	}

	@Override
	public void printStackTrace(final PrintStream s) {
		for (final Throwable t : causes) {
			t.printStackTrace(s);
		}
	}

}
