package edu.mayo.mprc.sge;

import edu.mayo.mprc.MprcException;
import org.apache.log4j.Logger;
import org.ggf.drmaa.*;

import java.io.Closeable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;


/**
 * this supports submission and handling of results for grid engine jobs
 */
public final class GridEngineJobManager implements Closeable {
	private static final Logger LOGGER = Logger.getLogger(GridEngineJobManager.class);

	private static final String QUEUE_SPEC_OPTION = "-q";
	private static final String MEMORY_SPEC_OPTION = "-l s_vmem=";
	private static final String MEMORY_SPEC_OPTION_MB_UNIT = "M";
	private static final int MAX_GRID_ENGINE_COMMAND = 1024;

	private Session gridEngineSession;

	private final Map<String, GridEngineWorkPacket> jobIdToWorkPacket = new HashMap<String, GridEngineWorkPacket>();

	private final Semaphore waitForAnotherSubmission = new Semaphore(0);

	public GridEngineJobManager() {
	}

	/**
	 * You can call this method repeatedly, once it succeeds, it does nothing.
	 */
	public synchronized void initialize() {
		if (gridEngineSession == null) {
			try {
				final SessionFactory factory = SessionFactory.getFactory();
				gridEngineSession = factory.getSession();
				gridEngineSession.init(null);
				initializeListenerThread();
			} catch (Error error) {
				gridEngineSession = null;
				throw new MprcException("Sun Grid Engine not available, the DRMAA library is probably missing", error);
			} catch (Exception e) {
				gridEngineSession = null;
				throw new MprcException("Sun Grid Engine not available, DRMAA library initialization failed", e);
			}
		}
	}

	private synchronized Session getGridEngineSession() {
		return gridEngineSession;
	}

	public void close() {
		if (getGridEngineSession() != null) {
			try {
				getGridEngineSession().exit();
			} catch (DrmaaException ignore) {
				// SWALLOWED: We do not care, there is no real reporting of drmaa failing in finalizer anyway
				LOGGER.debug("session already released", ignore);
			}
		}
	}

	private void initializeListenerThread() {
		final Runner monitoringThreadRunner = new Runner();
		final Thread pThread = new Thread(monitoringThreadRunner, "Grid Engine Monitor");
		pThread.start();
	}

	private void storeJobSuccessfulStatus(final String jobid, final JobInfo pInfo) {
		final GridEngineWorkPacket pPacket = jobIdToWorkPacket.get(jobid);
		if (pPacket != null) {
			pPacket.jobUpdateSucceeded();
			jobIdToWorkPacket.put(jobid, pPacket);
			// signal the task
			pPacket.fireStateChanged();
		} else {
			// have an error condition, don't recognize this job id
			LOGGER.error("StoreJobSuccessfulStatus Error: packet for jobid:" + jobid + " is not registered.");
		}
	}

	private void storeJobFailedStatus(final String jobid, final JobInfo pInfo, final String message) {
		final GridEngineWorkPacket pPacket = jobIdToWorkPacket.get(jobid);
		if (pPacket != null) {
			pPacket.jobUpdateFailed(message);
			jobIdToWorkPacket.put(jobid, pPacket);
			// signal the task
			pPacket.fireStateChanged();
		} else {
			// have an error condition, don't recognize this job id
			LOGGER.error("StoreJobFailedStatus Error: packet for jobid:" + jobid + " is not registered.");
		}
	}

	private void setJobInfo(final String jobid, final JobInfo pInfo) {
		final GridEngineWorkPacket pPacket = jobIdToWorkPacket.get(jobid);
		if (pPacket != null) {
			pPacket.setJobInfo(pInfo);
		} else {
			LOGGER.error("Error: packet for jobid:" + jobid + " is not registered.");
		}
	}

	private String getApplicationCallParameters(final GridEngineWorkPacket pPacket) {
		final StringBuilder parmessage = new StringBuilder();
		for (final String s : pPacket.getParameters()) {
			parmessage.append(s).append(" ");
		}
		return parmessage.toString();
	}

	/**
	 * creates job template based on packet content and runs it
	 *
	 * @param pgridPacket - the information about the job
	 * @return id of SGE assigned job id.
	 */
	public String passToGridEngine(final GridWorkPacket pgridPacket) {
		initialize();

		final GridEngineWorkPacket pPacket = new GridEngineWorkPacket(pgridPacket);

		final String taskString = pPacket.getApplicationName() + " " + getApplicationCallParameters(pPacket);

		try {
			LOGGER.debug("Runing grid engine job: " + taskString);

			final String jobid = runJob(pPacket);
			LOGGER.info("Your job has been submitted with id " + jobid);

			// For debugging purposes only - display the immediate job status
			logCurrentJobStatus(jobid);

			return jobid;
		} catch (Exception t) {
			throw new MprcException("Error submitting to grid engine: " + taskString, t);
		}
	}

	private String runJob(final GridEngineWorkPacket pPacket) throws DrmaaException {
		String jobid = null;
		JobTemplate jt = null;

		try {
			LOGGER.debug("Setting up job template for " + pPacket.getApplicationName());
			jt = getGridEngineSession().createJobTemplate();
			setupJobTemplate(jt, pPacket);

			// Run the job in grid engine
			jobid = getGridEngineSession().runJob(jt);
			jobIdToWorkPacket.put(jobid, pPacket);
		} finally {
			waitForAnotherSubmission.release();
			if (jt != null) {
				getGridEngineSession().deleteJobTemplate(jt);
			}
		}
		return jobid;
	}

	private void setupJobTemplate(final JobTemplate jt, final GridEngineWorkPacket pPacket) throws DrmaaException {
		// for now are assuming will not force a queue and memory
		// may need to consider making these options pass through
		if (pPacket.getWorkingFolder() != null) {
			jt.setWorkingDirectory(pPacket.getWorkingFolder());
			try {
				jt.setOutputPath(InetAddress.getLocalHost().getHostName() + ":" + pPacket.getOutputLogFilePath());
				jt.setErrorPath(InetAddress.getLocalHost().getHostName() + ":" + pPacket.getErrorLogFilePath());
			} catch (UnknownHostException e) {
				throw new MprcException("Unable to get host name.", e);
			}
		}

		String spec = "";
		if (pPacket.getNativeSpecification() != null) {
			spec += pPacket.getNativeSpecification();
			LOGGER.debug("Task has native specification: " + pPacket.getNativeSpecification());
		}
		if (pPacket.getQueueName() != null) {
			if (spec.length() > 0) {
				spec += " ";
			}
			spec += QUEUE_SPEC_OPTION + " " + pPacket.getQueueName();
			LOGGER.debug("Task forces a job queue: " + pPacket.getQueueName());
		}
		if (pPacket.getMemoryRequirement() != null) {
			if (spec.length() > 0) {
				spec += " ";
			}
			spec += MEMORY_SPEC_OPTION + pPacket.getMemoryRequirement() + MEMORY_SPEC_OPTION_MB_UNIT;
			LOGGER.warn("Task forces memory requirement: " + pPacket.getMemoryRequirement());
		}
		LOGGER.debug("Resulting native specification passed to grid engine:\n" + spec);
		jt.setNativeSpecification(spec);

		if (pPacket.getApplicationName().length() >= MAX_GRID_ENGINE_COMMAND) {
			throw new MprcException("Command too long - Grid Engine only accepts commands up to " + MAX_GRID_ENGINE_COMMAND + " characters in length:\n" + pPacket.getApplicationName());
		}

		jt.setRemoteCommand(pPacket.getApplicationName());

		jt.setArgs(pPacket.getParameters());
	}

	private void logCurrentJobStatus(final String jobid) {
		try {
			final int status = getGridEngineSession().getJobProgramStatus(jobid);
			final String statusString = jobStatusToString(status);
			LOGGER.debug("Drmaa status report for " + jobid + ": " + statusString);
		} catch (Exception e) {
			// SWALLOWED: purely informative
			LOGGER.error("Drmaa status report for " + jobid + ": failed to obtain", e);
		}
	}

	private static String jobStatusToString(final int status) {
		String statusString = "";
		switch (status) {
			case Session.UNDETERMINED:
				statusString = "UNDETERMINED: process status cannot be determined";
				break;
			case Session.QUEUED_ACTIVE:
				statusString = "QUEUED_ACTIVE: job is queued and active";
				break;
			case Session.SYSTEM_ON_HOLD:
				statusString = "SYSTEM_ON_HOLD: job is queued and in system hold";
				break;
			case Session.USER_ON_HOLD:
				statusString = "USER_ON_HOLD: job is queued and in user hold";
				break;
			case Session.USER_SYSTEM_ON_HOLD:
				statusString = "USER_SYSTEM_ON_HOLD: job is queued and in user and system hold";
				break;
			case Session.RUNNING:
				statusString = "RUNNING: job is running";
				break;
			case Session.SYSTEM_SUSPENDED:
				statusString = "SYSTEM_SUSPENDED: job is system suspended";
				break;
			case Session.USER_SUSPENDED:
				statusString = "USER_SUSPENDED: job is user suspended";
				break;
			case Session.DONE:
				statusString = "DONE: job finished normally";
				break;
			case Session.FAILED:
				statusString = "FAILED: job finished, but failed";
				break;
			default:
				statusString = "Unknown status!";
				break;
		}
		return statusString;
	}

	void monitorForJobs() throws InterruptedException {
		JobInfo info = null;

		try {
			info = getGridEngineSession().wait(Session.JOB_IDS_SESSION_ANY, Session.TIMEOUT_WAIT_FOREVER);

			setJobInfo(info.getJobId(), info);

			if (info.wasAborted()) {
				LOGGER.debug("Job " + info.getJobId() + " never ran");
				storeJobFailedStatus(info.getJobId(), info, " never ran");
			} else if (info.hasExited()) {
				LOGGER.debug("Job " + info.getJobId() +
						" finished regularly with exit status" +
						info.getExitStatus());
				if (info.getExitStatus() == 0) {
					storeJobSuccessfulStatus(info.getJobId(), info);
				} else {
					storeJobFailedStatus(info.getJobId(), info, "non 0 return code=" + info.getExitStatus());
				}
			} else if (info.hasSignaled()) {
				LOGGER.debug("Job " + info.getJobId() +
						" finished due to signal " +
						info.getTerminatingSignal());
				storeJobFailedStatus(info.getJobId(), info, " finished due to signal " +
						info.getTerminatingSignal());
			} else {
				LOGGER.debug("Job " + info.getJobId() +
						" finished with unclear conditions");
				storeJobFailedStatus(info.getJobId(), info, " finished with unclear conditions");
			}
		} catch (InvalidJobException ije) {
			// SWALLOWED: see explanation below
			// Our job ID (Session.JOB_IDS_SESSION_ANY) is invalid
			// This is only true if Grid Engine is running NO jobs at all right now.
			// We will wait for a new job submission, otherwise we will be spinning infinitely
			waitForAnotherSubmission.acquire();
		} catch (ExitTimeoutException ete) {
			// SWALLOWED: the thread just stores it as text
			setFailure("Time out exception, " + ete.getMessage(), info);
		} catch (DrmaaException e) {
			// SWALLOWED: the thread just stores it as text
			setFailure("Drmaa exception, " + e.getMessage(), info);
		} catch (Exception t) {
			// SWALLOWED: the thread just stores it as text
			setFailure("failed with " + t.getMessage(), info);
		}
	}

	private void setFailure(final String errormessage, final JobInfo info) {
		LOGGER.error(errormessage);
		if (info != null) {
			try {
				storeJobFailedStatus(info.getJobId(), info, errormessage);
			} catch (Exception e) {
				// SWALLOWED, why?
				LOGGER.error("Storing job status failed", e);
			}
		} else {
			LOGGER.error("The information about failed job is not available. Error message: " + errormessage);
		}
	}

	/**
	 * implements the thread that listens for grid engine responses
	 */
	class Runner implements Runnable {
		public Runner() {
		}

		public void run() {
			while (true) {
				try {
					monitorForJobs();
				} catch (InterruptedException ignore) {
					// Swallowed: The interrupted exception means our spinning is over (the application is terminating).
					LOGGER.debug("Exiting grid engine thread monitor.");
					return;
				} catch (Exception t) {
					LOGGER.error("Terminating the grid job manager thread", t);
				}
			}
		}
	}
}