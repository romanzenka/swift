package edu.mayo.mprc.sge;

import com.google.common.base.Joiner;
import edu.mayo.mprc.MprcException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * this packet needs to provide grid engine task run information
 * including
 * - application name
 * - parameter string
 */
public class GridWorkPacket {

	private static final String GRIDENGINE_STD_ERR_FILE_PREFIX = "e";
	private static final String GRIDENGINE_STD_OUT_FILE_PREFIX = "o";
	private static final String LOG_FILE_EXTENTION = ".sge.log";

	private final String applicationName;
	private List<String> parameters;
	private String queueName;
	private String minMemoryRequirement;
	private String nativeSpecification;
	private String workingFolder;
	private String logFolder;
	private boolean success;
	private boolean failure;
	private String errorMessage;
	private int priority;

	private static final AtomicLong workPacketUniqueIdBase;

	//This id is used to composed the output and error log files of the SGE.
	private final long workPacketUniqueId;

	private GridWorkPacketStateListener listener;

	private Long persistentRequestId;

	static {
		workPacketUniqueIdBase = new AtomicLong(System.currentTimeMillis());
	}

	public GridWorkPacket(
			final String applicationName,
			final List<String> parameters) {
		if (applicationName == null) {
			throw new MprcException("The application name for grid work packet was null");
		}

		this.applicationName = applicationName;
		setParameters(parameters);
		queueName = "none";
		minMemoryRequirement = "0";
		nativeSpecification = "none";
		workingFolder = "none";
		logFolder = "none";

		workPacketUniqueId = workPacketUniqueIdBase.getAndIncrement();
	}

	public GridWorkPacket(final GridWorkPacket packet) {
		parameters = packet.getParameters();
		applicationName = packet.getApplicationName();
		queueName = packet.getQueueName();
		minMemoryRequirement = packet.getMemoryRequirement();
		nativeSpecification = packet.getNativeSpecification();
		workingFolder = packet.getWorkingFolder();
		logFolder = packet.getLogFolder();
		persistentRequestId = packet.getPersistentRequestId();
		listener = packet.getListener();
		priority = packet.getPriority();

		workPacketUniqueId = packet.getWorkPacketUniqueId();
	}

	public long getWorkPacketUniqueId() {
		return workPacketUniqueId;
	}

	public void setParameters(final List<String> parameters) {
		this.parameters = parameters == null ? null : new ArrayList<String>(parameters);
	}

	public void setListener(final GridWorkPacketStateListener listener) {
		this.listener = listener;
	}

	public GridWorkPacketStateListener getListener() {
		return listener;
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(final String queueName) {
		this.queueName = queueName;
	}

	public void setWorkingFolder(final String workingFolder) {
		this.workingFolder = workingFolder;
	}

	public String getWorkingFolder() {
		return workingFolder;
	}

	public String getLogFolder() {
		return logFolder;
	}

	public void setLogFolder(final String logFolder) {
		this.logFolder = logFolder;
	}

	public String getOutputLogFilePath() {
		return getOutputFileName(false);
	}

	public String getErrorLogFilePath() {
		return getOutputFileName(true);
	}

	public String getNativeSpecification() {
		return nativeSpecification;
	}

	public void setNativeSpecification(final String nativeSpecification) {
		this.nativeSpecification = nativeSpecification;
	}

	public String getMemoryRequirement() {
		return minMemoryRequirement;
	}

	public void setMemoryRequirement(final String memoryRequirement) {
		minMemoryRequirement = memoryRequirement;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public List<String> getParameters() {
		return parameters;
	}

	public String getParametersAsCallString() {
		if (parameters == null || parameters.isEmpty()) {
			return "";
		}
		return Joiner.on(" ").join(parameters);
	}

	public Long getPersistentRequestId() {
		return persistentRequestId;
	}

	public void setPersistentRequestId(final Long persistentRequestId) {
		this.persistentRequestId = persistentRequestId;
	}

	public boolean getPassed() {
		return success;
	}

	public boolean getFailed() {
		return failure;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void fireStateChanged() {
		listener.stateChanged(this);
	}

	public void jobUpdateSucceeded() {
		success = true;
		fireStateChanged();
	}

	public void jobUpdateFailed(final String message) {
		failure = true;
		errorMessage = message;
		fireStateChanged();
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String toString() {
		return "GridWorkPacket: " + applicationName + " " + getParametersAsCallString() + " (queue=" + queueName + ")";
	}

	/**
	 * Generates output file name for given GridEngineWorkPacket object.
	 *
	 * @param isError If true, file name is error log. Otherwise, file name is standard log.
	 * @return
	 */
	private String getOutputFileName(final boolean isError) {
		String fileName = null;

		if (isError) {
			fileName = new File(getLogFolder(), GRIDENGINE_STD_ERR_FILE_PREFIX + getWorkPacketUniqueId() + LOG_FILE_EXTENTION).getAbsolutePath();
		} else {
			fileName = new File(getLogFolder(), GRIDENGINE_STD_OUT_FILE_PREFIX + getWorkPacketUniqueId() + LOG_FILE_EXTENTION).getAbsolutePath();
		}

		return fileName;
	}
}

