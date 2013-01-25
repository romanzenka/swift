package edu.mayo.mprc.utilities;

import com.google.common.base.CharMatcher;
import com.google.common.io.ByteStreams;
import edu.mayo.mprc.MprcException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Wraps {@link ProcessBuilder#start()} so it does not leak resources, logs outputs, etc.
 * <p/>
 * Usage:
 * <pre>
 * ProcessBuilder builder = ..setup however you like;
 * ProcessCaller caller=new ProcessCaller(builder, true, true); // true, true = log output, log error
 * caller.{@link #run}();
 * if(caller.{@link #getExitValue}()!=0) {
 *   LOGGER.error(caller.getFailedCallDescription());
 * } else {
 *   LOGGER.debug("Output is:" + caller.getOutputLog());
 * }
 * </pre>
 * <ul>
 * <li>Properly drain all outputs and either retain their values or just count the number of lines</li>
 * <li>Properly close all streams</li>
 * <li>Call {@link Process#destroy()} on termination of the process, after all the draining finished.
 * </ul>
 */
public final class ProcessCaller implements Runnable {
	private static final Logger LOGGER = Logger.getLogger(ProcessCaller.class);

	/**
	 * How long we wait after the end for the pipe to deliver the rest of output
	 */
	private static final int PIPE_TIMEOUT = 1000 * 60;
	/**
	 * How long after starting the process do we warn about the possibility of killing it?
	 * Ideally the process would produce output so frequently that the warning never gets issued.
	 */
	public static final int WARN_ABOUT_KILLING_MILLIS = 30 * 1000;

	private final ProcessBuilder builder;
	private Process process;
	private Logger outputLogger = LOGGER;
	private Logger errorLogger = LOGGER;
	private boolean retainLogs = true;

	private LogMonitor outputMonitor;
	private LogMonitor errorMonitor;

	private InputStream inputStream;
	private StreamDrainer outputStreamDrainer;
	private StreamDrainer errorStreamDrainer;
	private Timer timer;
	private AtomicLong killAtTime = new AtomicLong();
	private AtomicLong warnAtTime = new AtomicLong();
	private boolean killed;

	private static final NumberFormat KILL_TIME_FORMAT = new DecimalFormat("#0.00");

	/**
	 * Create a new process caller wrapping the given builder. As the process executes,
	 * its output stream is redirected to the output logger and error stream to the error logger.
	 * Last 100 log lines are retained. If you do not wish to log to console use
	 * {@link #setLogToConsole}. If you do not wish to retain last log lines, use {@link #setRetainLogs}.
	 *
	 * @param builder {@link ProcessBuilder} that defines what to execute.
	 */
	public ProcessCaller(final ProcessBuilder builder) {
		this.builder = builder;
	}

	/**
	 * Execute the process defined by the string builder. This method blocks until the call is over, so it is a good
	 * candidate to be run within a {@link Thread}.
	 */
	@Override
	public void run() {
		if (isAlreadyStarted()) {
			throw new MprcException("The process was already started");
		}
		try {
			runProcess();
		} catch (Exception t) {
			throw new MprcException("Process execution failed", t);
		} finally {
			clearKillTimeout();
		}
	}

	/**
	 * Utility method that runs a given command, throwing an exception if it returns with nonzero exit value +
	 * reporting in detail what went wrong.
	 *
	 * @param callName Shorthand name for the process to be used in the error messages.
	 */
	public void runAndCheck(final String callName) {
		try {
			run();
		} catch (Exception t) {
			throw new MprcException(callName + " process failed: " + getFailedCallDescription(), t);
		}
		LOGGER.debug(callName + " process returned " + getExitValue());
		if (getExitValue() != 0) {
			throw new MprcException(callName + " process failed: " + getFailedCallDescription());
		}

	}

	/**
	 * The process will be killed in the specified amount of milliseconds, unless it terminates first.
	 *
	 * @param millis How many milliseconds to wait before the process gets terminated.
	 */
	public void setKillTimeout(final long millis) {
		killAtTime.set(System.currentTimeMillis() + millis);
		warnAtTime.set(System.currentTimeMillis() + WARN_ABOUT_KILLING_MILLIS);
		if (process != null) {
			setupKillTimer();
		}
	}

	private void setupKillTimer() {
		if (timer == null) {
			// Execute the timer after 10 milliseconds and then every second until the kill time arrives
			timer = new Timer("Process watchdog", true);
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					if (killAtTime.get() > 0L && System.currentTimeMillis() >= killAtTime.get()) {
						LOGGER.debug("Killing process");
						killed = true;
						process.destroy();
						cancel();
					} else {
						if (killAtTime.get() == 0) {
							cancel();
							return;
						}

						if (warnAtTime.get() > 0L && System.currentTimeMillis() >= warnAtTime.get()) {
							final double remainingTime = (killAtTime.get() - System.currentTimeMillis()) / 1000.0;
							LOGGER.debug("Time to kill process " + KILL_TIME_FORMAT.format(remainingTime) + " seconds.");
							// And schedule next warning
							warnAtTime.set(System.currentTimeMillis() + WARN_ABOUT_KILLING_MILLIS);
						}
					}
				}
			}, 10, 1000);
		}
	}

	/**
	 * The process will not get killed after a given timeout.
	 */
	public void clearKillTimeout() {
		if (timer != null) {
			killAtTime.set(0);
			warnAtTime.set(0);
			timer.cancel();
			timer = null;
		}
	}

	/**
	 * @return Error log if any was produced.
	 */
	public String getErrorLog() {
		throwIfNotRun();
		return errorStreamDrainer.getLog();
	}

	/**
	 * @return Output log if any was produced.
	 */
	public String getOutputLog() {
		throwIfNotRun();
		return outputStreamDrainer.getLog();
	}

	/**
	 * @return Exit value of the process. Call this only after the run method terminates.
	 */
	public int getExitValue() {
		return process == null ? -1 : process.exitValue();
	}

	private void runProcess() throws InterruptedException {
		if (outputLogger != null) {
			LOGGER.debug("Running process:" + getCallDescription());
		}
		Thread outputPipe = null;
		Thread errorPipe = null;
		try {
			process = builder.start();

			outputStreamDrainer = new StreamDrainer(process.getInputStream(), outputLogger, Level.INFO,
					retainedLogLines(), outputMonitor);
			outputPipe = new Thread(outputStreamDrainer, "Process stdout drain");
			outputPipe.start();

			errorStreamDrainer = new StreamDrainer(process.getErrorStream(), errorLogger, Level.ERROR,
					retainedLogLines(), errorMonitor);
			errorPipe = new Thread(errorStreamDrainer, "Process stderr drain");
			errorPipe.start();

			if (getInputStream() != null) {
				ByteStreams.copy(getInputStream(), process.getOutputStream());
			}
			FileUtilities.closeQuietly(process.getOutputStream());

			// The user wanted to kill the process after a certain timeout
			// Now that the process is running, start the timer
			if (killAtTime.get() != 0) {
				setupKillTimer();
			}

			process.waitFor();
		} catch (IOException e) {
			throw new MprcException(e);
		} finally {
			// Wait for the pipes to stop piping - give them up to a minute
			try {
				if (outputPipe != null) {
					outputPipe.join(PIPE_TIMEOUT);
				}
				if (errorPipe != null) {
					errorPipe.join(PIPE_TIMEOUT);
				}
			} finally {
				if (process != null) {
					FileUtilities.closeQuietly(process.getErrorStream());
					FileUtilities.closeQuietly(process.getInputStream());
					FileUtilities.closeQuietly(process.getOutputStream());
					// Destroy the process
					process.destroy();
				}
			}
		}
		if (killed) {
			throw new MprcException("The process hung and was killed: " + getFailedCallDescription());
		}
	}

	private int retainedLogLines() {
		return isRetainLogs() ? StreamDrainer.DEFAULT_RETAIN_SIZE : 0;
	}

	private static String commandListToString(final List<String> command, final boolean windowsPlatform) {
		final StringBuilder result = new StringBuilder(100);
		for (final String cmd : command) {
			final String escapedCmd = escapeCommandPart(cmd, windowsPlatform);
			if (result.length() > 0) {
				result.append(' ');
			}
			result.append(escapedCmd);
		}
		return result.toString();
	}

	private static String escapeCommandPart(final String cmd, final boolean windowsPlatform) {
		if (null == cmd) {
			return "<null>";
		}
		if ((cmd.contains(" ") || cmd.contains("\\")) && !windowsPlatform) {
			return '\'' + CharMatcher.is('\'').replaceFrom(cmd, "\\'") + '\'';
		}
		if (cmd.contains(" ") && windowsPlatform) {
			return '"' + cmd + '"';
		}
		return cmd;
	}

	private void throwIfNotRun() {
		if (process == null) {
			throw new MprcException("The process was not run");
		}
	}

	private boolean isAlreadyStarted() {
		return process != null || outputStreamDrainer != null || errorStreamDrainer != null;
	}

	/**
	 * @return Description of this call that lets the user to run it on their own.
	 */
	public String getCallDescription() {
		return getChangeDirCommand()
				+ "\n\t" + commandListToString(builder.command(), FileUtilities.isWindowsPlatform());
	}

	public void setLogToConsole(final boolean logToConsole) {
		if (logToConsole) {
			outputLogger = LOGGER;
			errorLogger = LOGGER;
		} else {
			outputLogger = null;
			errorLogger = null;
		}
	}

	public boolean isRetainLogs() {
		return retainLogs;
	}

	public void setRetainLogs(final boolean retainLogs) {
		this.retainLogs = retainLogs;
	}

	/**
	 * @return Description of failed call that lets the user to replicate it. Provides logs.
	 */
	public String getFailedCallDescription() {
		if (process == null) {
			return getChangeDirCommand()
					+ "\n\t" + commandListToString(builder.command(), FileUtilities.isWindowsPlatform())
					+ "\n\tProcess was not run";
		} else {
			return getChangeDirCommand()
					+ "\n\t" + commandListToString(builder.command(), FileUtilities.isWindowsPlatform())
					+ "\n\tError code: " + getExitValue()
					+ "\n\tStandard error:\n" + getErrorLog()
					+ "\n\tStandard output:\n" + getOutputLog();
		}
	}

	/**
	 * @return 'cd <dir>' string if current directory is set on the builder.
	 */
	private String getChangeDirCommand() {
		if (builder.directory() != null) {
			return "\n\tcd " + builder.directory();
		} else {
			return "";
		}
	}

	public LogMonitor getOutputMonitor() {
		return outputMonitor;
	}

	public void setOutputMonitor(final LogMonitor outputMonitor) {
		this.outputMonitor = outputMonitor;
	}

	public LogMonitor getErrorMonitor() {
		return errorMonitor;
	}

	public void setErrorMonitor(final LogMonitor errorMonitor) {
		this.errorMonitor = errorMonitor;
	}

	public void setInputStream(final InputStream input) {
		this.inputStream = input;
	}

	public InputStream getInputStream() {
		return inputStream;
	}
}

