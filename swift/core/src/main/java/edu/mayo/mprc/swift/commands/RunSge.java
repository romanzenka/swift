package edu.mayo.mprc.swift.commands;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.ReleaseInfoCore;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.daemon.DaemonLoggerFactory;
import edu.mayo.mprc.daemon.DaemonProgress;
import edu.mayo.mprc.daemon.DaemonProgressMessage;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.daemon.files.FileTokenHolder;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.daemon.worker.Worker;
import edu.mayo.mprc.messaging.Request;
import edu.mayo.mprc.messaging.ServiceFactory;
import edu.mayo.mprc.sge.SgePacket;
import edu.mayo.mprc.swift.resources.ResourceTable;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.log.ParentLog;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Roman Zenka
 */
@Component("sge-command")
public class RunSge implements SwiftCommand {
	private static final Logger LOGGER = Logger.getLogger(RunSge.class);
	public static final String COMMAND = "sge";

	private ResourceTable resourceTable;
	private ServiceFactory serviceFactory;
	private DaemonLoggerFactory daemonLoggerFactory;

	@Resource(name = "fileTokenFactory")
	private FileTokenFactory fileTokenFactory;

	@Override
	public String getDescription() {
		return "Internal command to execute a piece of work within SGE";
	}

	@Override
	public ExitCode run(final SwiftEnvironment environment) {
		final String xmlConfigFilePath = environment.getParameters().get(0);
		return run(new File(xmlConfigFilePath), environment.getApplicationConfig());
	}

	/**
	 * Takes a name with work packet serialized into xml. Executes the work packet and communicates the results.
	 *
	 * @param workPacketXmlFile File containing the serialized work packet.
	 */
	public ExitCode run(final File workPacketXmlFile, final ApplicationConfig applicationConfig) {
		// Wait for the work packet to fully materialize in case it was transferred over a shared filesystem
		FileUtilities.waitForFileBlocking(workPacketXmlFile);

		try {
			LOGGER.info("Running grid job in host: " + InetAddress.getLocalHost().getHostName());
		} catch (final UnknownHostException e) {
			LOGGER.error("Could not get host name.", e);
		}

		Request request = null;
		FileInputStream fileInputStream = null;
		SgePacket sgePacket = null;
		try {
			LOGGER.debug(ReleaseInfoCore.buildVersion());
			LOGGER.info("Parsing xml file: " + workPacketXmlFile.getAbsolutePath());

			final XStream xStream = new XStream(new DomDriver());

			fileInputStream = new FileInputStream(workPacketXmlFile);

			sgePacket = (SgePacket) xStream.fromXML(fileInputStream);

			request = getServiceFactory().deserializeRequest(sgePacket.getSerializedRequest());

			//If the work packet is an instance of a FileTokenHolder, set the the FileTokenFactory on it. The FileTokenFactory object
			//needs to be reset because it is a transient object.
			if (sgePacket.getWorkPacket() instanceof FileTokenHolder) {
				final FileTokenHolder fileTokenHolder = (FileTokenHolder) sgePacket.getWorkPacket();
				fileTokenFactory.setDaemonConfigInfo(sgePacket.getDaemonConfigInfo());
				fileTokenHolder.translateOnReceiver(fileTokenFactory, null);
			}
			daemonLoggerFactory = new DaemonLoggerFactory(sgePacket.getLogFolder());

			final DependencyResolver dependencies = new DependencyResolver(resourceTable);

			final ResourceConfig config = getConfigForServiceName(applicationConfig, sgePacket.getServiceName());
			final Worker daemonWorker = (Worker) resourceTable.createSingleton(config, dependencies);
			final DaemonWorkerProgressReporter progressReporter = new DaemonWorkerProgressReporter(request);
			final WorkPacket workPacket = (WorkPacket) sgePacket.getWorkPacket();
			final ParentLog parentLog = daemonLoggerFactory.createLog(workPacket.getTaskId(), progressReporter);
			progressReporter.setParentLog(parentLog);
			daemonWorker.processRequest(workPacket, progressReporter);
		} catch (final Exception e) {
			final String errorMessage = "Failed to process work packet " + ((sgePacket == null || sgePacket.getWorkPacket() == null) ? "null" : sgePacket.getWorkPacket().toString());
			LOGGER.error(errorMessage, e);

			try {
				reportProgress(request, e);
			} catch (final Exception ex) {
				LOGGER.error("Error sending exception " + MprcException.getDetailedMessage(e) + " to GridRunner", ex);
				// SWALLOWED
			}

			return ExitCode.Error;
		} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (final IOException e) {
					LOGGER.warn("Error closing file input stream.", e);
					// SWALLOWED
				}
			}
			getServiceFactory().stop();
		}

		LOGGER.info("Work packet " + sgePacket.getWorkPacket().toString() + " successfully processed.");
		return ExitCode.Ok;
	}

	private ResourceConfig getConfigForServiceName(final ApplicationConfig applicationConfig, final String serviceName) {
		for (final DaemonConfig daemonConfig : applicationConfig.getDaemons()) {
			for (final ServiceConfig service : daemonConfig.getServices()) {
				if (service.getName().equals(serviceName)) {
					return service.getRunner().getWorkerConfiguration();
				}
			}
		}
		throw new MprcException(String.format("No service named %s found", serviceName));
	}

	public ResourceTable getResourceTable() {
		return resourceTable;
	}

	@Resource(name = "resourceTable")
	public void setResourceTable(final ResourceTable resourceTable) {
		this.resourceTable = resourceTable;
	}

	public ServiceFactory getServiceFactory() {
		return serviceFactory;
	}

	@Resource(name = "serviceFactory")
	public void setServiceFactory(final ServiceFactory serviceFactory) {
		this.serviceFactory = serviceFactory;
	}

	private static void reportProgress(final Request request, final Serializable serializable) {
		request.sendResponse(serializable, false);
	}

	class DaemonWorkerProgressReporter implements ProgressReporter {
		private Request request;
		private ParentLog parentLog;

		DaemonWorkerProgressReporter(final Request request) {
			this.request = request;
		}

		@Override
		public void reportStart(final String hostString) {
			try {
				RunSge.reportProgress(request, new DaemonProgressMessage(hostString));
			} catch (final Exception t) {
				try {
					RunSge.reportProgress(request, t);
				} catch (final Exception ex) {
					LOGGER.error("Error sending exception " + MprcException.getDetailedMessage(t) + " to GridRunner", ex);
					// SWALLOWED
				}
				System.exit(1);
			}
		}

		@Override
		public void reportProgress(final ProgressInfo progressInfo) {
			try {
				RunSge.reportProgress(request, new DaemonProgressMessage(DaemonProgress.UserSpecificProgressInfo, progressInfo));
			} catch (final Exception e) {
				LOGGER.error("Error reporting daemon worker progress.", e);
				//SWALLOWED
			}
		}

		public void setParentLog(final ParentLog parentLog) {
			this.parentLog = parentLog;
		}

		@Override
		public ParentLog getLog() {
			return parentLog;
		}

		@Override
		public void reportSuccess() {
			//Do nothing. GridRunner gets notified of completion by SGE.
		}

		@Override
		public void reportFailure(final Throwable t) {
			try {
				RunSge.reportProgress(request, t);
			} catch (final Exception e) {
				LOGGER.error("Error reporting daemon worker failure.", e);
				//SWALLOWED
			}
		}
	}

}
