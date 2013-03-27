package edu.mayo.mprc.mascot;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.enginedeployment.DeploymentRequest;
import edu.mayo.mprc.enginedeployment.DeploymentResult;
import edu.mayo.mprc.enginedeployment.DeploymentService;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.StringUtilities;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A MascotDeploymentService will sit on the machine that runs Mascot and will accept a FASTA file as a path on a shared filesystem.
 * <p/>
 * This service will set modify the {@link #MASCOT_DAT} file (and all infrastructure involved) and will then look into the {@link #MONITOR_LOG} to verify that
 * the changes were accepted before issuing a response to the process that called this service.
 * <p/>
 * This will extract shortname from the filename since they should be the same for Mascot and the filename is the more important of the two.
 * <p/>
 *
 * @author Eric Winter
 */
public final class MascotDeploymentService extends DeploymentService<DeploymentResult> {
	/**
	 * the logger for this class
	 */
	private static final Logger LOGGER = Logger.getLogger(MascotDeploymentService.class);
	public static final String TYPE = "mascotDeployer";
	public static final String NAME = "Mascot DB Deployer";
	public static final String DESC = "Deploys FASTA databases to Mascot. You need either this or Mock Mascot Deployer to use Mascot.";

	private static final String ENGINE_ROOT_FOLDER = "engineRootFolder";
	private static final String DEPLOYABLE_DB_FOLDER = "deployableDbFolder";
	private static final String MASCOT_DB_MAINTENANCE_URI = "mascotDbMaintenanceUri";

	/**
	 * the parameters that should go after the fasta file in the mascot.dat file, these are parameters that shouldn't change
	 */
	private final String datParameters;

	/**
	 * holds the line that goes in the WWW section of the mascot.dat file with _REP suffix
	 */
	private final String repLine;

	/**
	 * holds the line that goes in the WWW section of the mascot.dat file with _SEQ suffix
	 */
	private final String seqLine;

	/**
	 * Mascot database maintenance url
	 */
	private final URI dbMaintenanceUri;

	/**
	 * Mascot database maintenance web interface.
	 */
	private MascotDatabaseMaintenance databaseMaintenance;

	/**
	 * This needs to be checked by all threads in this vm since a FileLock applies to an entire virtual machine we can't rely on it for concurrency protection
	 * This may be overkill since we think that only a single request will be able to come off the JMSQueue at a time but just to be sure.
	 */
	private static Semaphore _intraVMLock = new Semaphore(/*permits*/1, /*fair*/true);

	private File datFile;
	private File logFile;

	/**
	 * Mascot.dat relative path. We modify this file to deploy a new DB.
	 */
	public static final String MASCOT_DAT = "config/mascot.dat";

	/**
	 * Monitor.log relative path. We observe the log file for changes to determine when the deployment is over.
	 */
	public static final String MONITOR_LOG = "logs/monitor.log";

	/**
	 * Deployment file extentions.
	 */
	public static final List<String> DF_EXTENTIONS = Arrays.asList("a00", "i00", "s00", "stats");

	/**
	 * Create a new deployment service.
	 *
	 * @param seqLine          holds the line that goes in the WWW section of the mascot.dat file with _SEQ suffix
	 * @param repLine          holds the line that goes in the WWW section of the mascot.dat file with _REP suffix
	 * @param datParameters    the parameters that should go after the fasta file in the mascot.dat file, these are parameters that shouldn't change
	 * @param dbMaintenanceUri Mascot database maintenance uri
	 */
	public MascotDeploymentService(final String seqLine, final String repLine, final String datParameters, final URI dbMaintenanceUri) {
		this.seqLine = seqLine;
		this.repLine = repLine;
		this.datParameters = datParameters;
		this.dbMaintenanceUri = dbMaintenanceUri;
	}

	/**
	 * this should be used for testing purposes alone
	 */
	public static MascotDeploymentService createForTesting(final File datFile, final File logFile) {
		final MascotDeploymentService service = new MascotDeploymentService(
				"%shortname%_SEQ%tab%\"8\"%tab%\"localhost\"%tab%\"80\"%tab%\"${mascotDeployer.mascotDir}/x-cgi/ms-getseq.exe %shortname% #ACCESSION# seq\"",
				"%shortname%_REP%tab%\"24\"%tab%\"localhost\"%tab%\"80\"%tab%\"${mascotDeployer.mascotDir}/x-cgi/ms-getseq.exe %shortname% #ACCESSION# all\"",
				"AA 1234 32 1 1 1 0 0 12 13 0 0",
				null
		);
		service.datFile = datFile;
		service.logFile = logFile;

		service.setEngineVersion("2.2");

		try {
			final File tmpFile = File.createTempFile("tempfile", ".tmp");
			tmpFile.deleteOnExit();
			service.setDeployableDbFolder(tmpFile.getParentFile());
		} catch (IOException e) {
			throw new RuntimeException("Could not initialize the MascotDeploymentService for testing", e);
		}
		service.setEngineRootFolder(datFile.getParentFile().getParentFile());

		return service;
	}

	private synchronized MascotDatabaseMaintenance getMascotDatabaseMaintenance() {
		if (databaseMaintenance == null) {
			databaseMaintenance = new MascotDatabaseMaintenance(dbMaintenanceUri, new DefaultHttpClient());
		}

		return databaseMaintenance;
	}

	/**
	 * Looks in the mascot.dat file to see if the database has already been deployed.
	 *
	 * @param request    the unique name that might appear in the mascot.dat file
	 * @param reportInto Deployment result to set a message on if deployment was already done.
	 * @return true if the mascot.dat file already contains this entry else false.
	 * @link super#wasCurationPreviouslyDeployed(String)
	 */
	public boolean wasPreviouslyDeployed(final DeploymentRequest request, final DeploymentResult reportInto) {
		assert request != null : "The request must not be null";
		assert request.getCurationFile() != null : "The curation to deploy must not be null";
		assert request.getShortName() != null : "The short name of the curation to deploy must not be null";

		final String previousDeploymentPath = getPreviousDeploymentPath(request.getShortName());
		if (previousDeploymentPath != null) {
			reportInto.addMessage("The deployment was already performed please look at " + previousDeploymentPath);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * if the file is contained in the mascot.dat file already you can use this to get the path of the deployed .FASTA file
	 * so you can fix the database.
	 *
	 * @param uniquename the name of the database that would appear in the mascot.dat file
	 * @return the File that appears in the mascot.dat file as the fasta file that was deployed.  Null if there were no entries mascot.dat file.
	 */
	public String getPreviousDeploymentPath(final String uniquename) {
		try {
			final String pstring = Pattern.quote(uniquename) + "\\s+\"?([^\r\n]+\\.fasta)\"?";
			final Pattern toLookInDatFileFor = Pattern.compile(pstring, Pattern.CASE_INSENSITIVE);
			final File datFile = getDatFile();
			final String datFileContents = Files.toString(datFile, Charsets.UTF_8);
			LOGGER.debug("Searching " + datFile.getAbsolutePath() + " (size " + datFileContents.length() + " bytes) for " + pstring);
			final Matcher m = toLookInDatFileFor.matcher(datFileContents);

			if (m.find()) {
				final String mascotDatPath = m.group(1);
				if (m.find()) {
					throw new MprcException("Multiple mascot.dat entries match " + pstring);
				}
				String path = null;
				LOGGER.debug("Found mascot.dat entry " + mascotDatPath);
				if (mascotDatPath.contains("*")) {
					// We have to search for the file
					final int firstIndexOfSplat = mascotDatPath.indexOf('*');
					if (mascotDatPath.lastIndexOf('*') != firstIndexOfSplat) {
						throw new FileNotFoundException("Mascot fasta path contains multiple '*'s.");
					}
					final String prepath = mascotDatPath.substring(0, firstIndexOfSplat);
					final int lastSlashIndex = prepath.lastIndexOf('/');
					final String dir = prepath.substring(0, lastSlashIndex);
					final File dirf = new File(dir);
					if (!dirf.exists()) {
						throw new FileNotFoundException("Can't find mascot fasta directory " + dir);
					}

					final String file = Pattern.quote(mascotDatPath.substring(lastSlashIndex + 1, firstIndexOfSplat)) + ".*" + Pattern.quote(mascotDatPath.substring(firstIndexOfSplat + 1));
					final Pattern p = Pattern.compile(file, Pattern.CASE_INSENSITIVE);

					LOGGER.debug("Looking for " + file + " in " + dir);
					for (final String f : dirf.list()) {
						if (p.matcher(f).matches()) {
							path = new File(dirf, f).getAbsolutePath();
							LOGGER.debug("File " + path + " matches " + file);
							break;
						}
					}
				} else {
					path = mascotDatPath;
				}

				if (path != null && new File(path).exists()) {
					LOGGER.info("Found mascot deployment of " + uniquename + " at " + path);
					return path;
				} else {
					throw new FileNotFoundException("There was an entry in mascot.dat for " + uniquename + " but the file " + mascotDatPath + " could not be found.");
				}
			} else {
				LOGGER.debug("Found no previously matching deployments in the mascot.dat file.");
				return null;
			}


		} catch (IOException unableToScanDatFile) {
			LOGGER.error(unableToScanDatFile);
			throw new MprcException(unableToScanDatFile);
		}
	}

	/**
	 * @see edu.mayo.mprc.enginedeployment.DeploymentService#performDeployment(edu.mayo.mprc.enginedeployment.DeploymentRequest)
	 */
	@Override
	public DeploymentResult performDeployment(final DeploymentRequest request) {
		final DeploymentResult reportInto = new DeploymentResult();
		try {

			if (wasPreviouslyDeployed(request, reportInto)) {
				return reportInto;
			}

			long startTime = new Date().getTime();
			final MonitorLogPoller poller = new MonitorLogPoller(getLogFile(), request.getShortName());
			new Thread(poller).start();

			final File toDeploy = this.getFileToDeploy(request.getShortName());
			FileUtilities.ensureFolderExists(toDeploy.getParentFile());

			if (!toDeploy.exists()) {
				FileUtilities.copyFile(getCurationFile(request), toDeploy, /*overwrite*/true);
			}

			//if we have a curation in mind then deploy it with the unique name of that curation if not we have to use the curation shortname given in the request
			this.updateMascotDatFile(request.getShortName(), toDeploy);
			final Integer lastLogSize = 0;
			int currentSleep = 50; // Start at 50 milliseconds, go up to 1 second delays
			while (poller.getStatus() == MonitorLogPoller.STATUS_MONITORING) {
				Thread.sleep(currentSleep);
				currentSleep = Math.min(currentSleep * 2, 1000);

				final long currentTime = new Date().getTime();
				if ((currentTime - startTime) > 5 * 60 * 1000) {
					if (lastLogSize == poller.getLog().length()) {
						//there hasn't been a change in the log so there is a problem with mascot
						throw new MprcException("A request on Mascot Monitor timed out (5 minutes with no progress");
					} else {
						startTime = currentTime;
					}
				}
			}

			if (poller.getStatus() == MonitorLogPoller.STATUS_FAILED) {
				throw new MprcException("The deployment failed check monitor\n" + poller.getLog());
			}
			reportInto.addMessage("The deployment was successful\n" + poller.getLog());
		} catch (Exception e) {
			throw new MprcException(e);
		}
		return reportInto;
	}

	@Override
	public DeploymentResult performUndeployment(final DeploymentRequest request) {
		final DeploymentResult reportResult = new DeploymentResult();
		final File deployedFile = this.getFileToDeploy(request.getShortName());

		try {
			final MascotDatabaseMaintenance databaseMaintenance = getMascotDatabaseMaintenance();

			if (!databaseMaintenance.isDatabaseDeployed(request.getShortName())) {
				LOGGER.info("Database " + request.getShortName() + " is not deployed to Mascot. Skipping database undeployment.");

				return reportResult;
			}

			databaseMaintenance.deleteDatabase(request.getShortName()).getRequestResponseBody();

			if (!databaseMaintenance.isDatabaseDeployed(request.getShortName())) {
				LOGGER.info("Database " + request.getShortName() + " has been undeployed from Mascot successfully.");

				/**
				 * Clean up deployment files.
				 */
				this.cleanUpDeployedFiles(deployedFile, reportResult);
			}
		} catch (Exception e) {
			throw new MprcException("Error occurred while undeploying database " + request.getShortName() + " from Mascot.", e);
		}

		return reportResult;
	}

	protected void validateAndDeleteDeploymentRelatedFiles(final File deployedFastaFile, final File deploymentFolder, final List<File> deletedFiles, final List<File> notDeletedFiles) {
		final File[] deploymentFiles = deploymentFolder.listFiles();

		for (final File deploymentFile : deploymentFiles) {
			if ((!deploymentFile.isDirectory()
					&& FileUtilities.getFileNameWithoutExtension(deploymentFile).equals(FileUtilities.getFileNameWithoutExtension(deploymentFolder))
					&& DF_EXTENTIONS.contains(FileUtilities.getExtension(deploymentFile.getName())))
					|| deploymentFile.getName().equals(deployedFastaFile.getName())) {
				if (FileUtilities.deleteNow(deploymentFile)) {
					deletedFiles.add(deploymentFile);
					continue;
				}
			}

			notDeletedFiles.add(deploymentFile);
		}
	}

	/**
	 * Finds out the path to the file that we will want to deploy to.  This does not perform the copy action but only finds
	 * out where we want to place the FASTA file.
	 */
	protected File getFileToDeploy(final String shortName) {
		final File deploymentFolder = getCurrentDeploymentFolder(shortName);
		return new File(deploymentFolder, shortName + ".fasta");
	}

	private File getCurrentDeploymentFolder(final String shortName) {
		return new File(getConfigDeploymentFolder(), shortName);
	}

	/**
	 * Modifies the mascot.dat file by adding a deployment line to it.
	 *
	 * @param shortname the name we want to deploy to the mascot.dat file
	 * @param toDeploy  the file that we want to deploy.  The file name will be used to generate the shortname.
	 */
	public synchronized void updateMascotDatFile(final String shortname, final File toDeploy) {
		String errorMessage = "";

		File newDatFileBackup = null;

		BufferedWriter out = null;
		BufferedReader isr = null;
		final File datFile = getDatFile();
		try {
			LOGGER.debug("Acquiring intra-VM Lock");
			_intraVMLock.acquire();

			//create a backup of the mascot.dat file and copy into it but if we can't just warn about it			
			int i = getLastDatFileSuffix(datFile.getParentFile()) + 1;

			newDatFileBackup = new File(datFile.getParent(), "mascot.dat." + i);
			LOGGER.debug("Creating backup of mascot.dat file to file: " + newDatFileBackup.getAbsolutePath());
			FileUtilities.copyFile(datFile, newDatFileBackup, false);

			i++;

			newDatFileBackup = new File(datFile.getParent(), "mascot.dat." + i);
			LOGGER.debug("Writing mascot.dat file to file: " + newDatFileBackup.getAbsolutePath());

			final FileOutputStream fos = new FileOutputStream(newDatFileBackup);
			out = new BufferedWriter(new OutputStreamWriter(fos));
			isr = new BufferedReader(new InputStreamReader(new FileInputStream(datFile)));
			String in = isr.readLine();
			String inSection = "";

			final String newLineRep = this.repLine.replaceAll("%shortname%", Matcher.quoteReplacement(shortname)).replaceAll("%tab%", "\t");
			final String newLineSeq = this.seqLine.replaceAll("%shortname%", Matcher.quoteReplacement(shortname)).replaceAll("%tab%", "\t");
			boolean containsLineRep = false;
			boolean containsLineSeq = false;
			String currentLine = null;

			while (in != null) {

				/**
				 * If this deployment was previosly deployed, undeployed,and now is being redeployed; ensure that the seq line
				 * and the rep line are in order. If database has been undeployed, the seq line should be present in the data
				 * file.
				 */
				if (inSection.equals("WWW")) {
					currentLine = in.trim();
					if (!containsLineSeq) {
						containsLineSeq = currentLine.startsWith(newLineSeq);
					} else if (!containsLineRep) {
						if (!(containsLineRep = currentLine.startsWith(newLineRep))) {
							out.write(newLineRep + "\n");
							containsLineRep = true;
						}
					}
				}

				if (in.startsWith("end")) {
					if (inSection.equals("Databases")) {
						out.write(generateDeploymentLine(shortname, toDeploy) + "\n");
					} else if (inSection.equals("WWW")) {
						if (!containsLineSeq) {
							out.write(newLineSeq + "\n");
						}
						if (!containsLineRep) {
							out.write(newLineRep + "\n");
						}
					}
					inSection = "";
				} else if (in.startsWith("Databases")) {
					inSection = "Databases";
				} else if (in.startsWith("WWW")) {
					inSection = "WWW";
				}
				out.write(in + "\n");
				in = isr.readLine();
			}
			isr.close();
			out.close();

			FileUtilities.rename(newDatFileBackup, datFile);

			LOGGER.debug("mascot.dat updated.");
		} catch (IOException e) {
			errorMessage = "Error modifying mascot.dat file. " + e.getMessage();
			throw new MprcException(errorMessage, e);
		} catch (InterruptedException e) {
			errorMessage = "Could not acquire a lock on mascot.dat file. " + e.getMessage();
			throw new MprcException(errorMessage, e);
		} catch (Exception e) {
			errorMessage = "Error modifying mascot.dat file. " + e.getMessage();
			throw new MprcException(errorMessage, e);
		} finally {

			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					LOGGER.warn("Not an error but could not close the stream.", e);
				}
			}

			if (isr != null) {
				try {
					isr.close();
				} catch (IOException e) {
					LOGGER.warn("Not an error but could not close the stream.", e);
				}
			}

			//close up resources and check for error conditions that indicate that we need to rollback any changes that might have been made
			//if there was a failure at some point we want ot roll back to the backup that was created
			if (errorMessage.length() != 0) {
				LOGGER.warn(errorMessage);
				LOGGER.warn("Rolling back to the original mascot.dat file");
				if (newDatFileBackup != null && newDatFileBackup.exists() && newDatFileBackup.length() > 0) {
					//if there was an original, delete it and then move the backup back to being the mascot.dat file
					if (datFile != null) {
						if (datFile.delete()) {
							FileUtilities.rename(newDatFileBackup, datFile);
						} else {
							LOGGER.warn("Could not delete the modified mascot.dat file.");
						}
					}
				}
			}

			LOGGER.debug("Releasing intravm lock");
			_intraVMLock.release();
		}

	}

	/**
	 * goes through the files matching mascot.dat.# and finds the highest value of #.  If the pattern is not matched then
	 * 0 is returned since no mascot.dat.# was found.
	 *
	 * @param folderToLookIn the folder where we expect to see the files
	 * @return the highest suffix number
	 */
	private int getLastDatFileSuffix(final File folderToLookIn) {
		final File[] fileList = folderToLookIn.listFiles(new FilenameFilter() {
			public boolean accept(final File dir, final String name) {
				return StringUtilities.startsWithIgnoreCase(name, "mascot.dat");
			}
		});

		int lastSuffix = 0;
		for (final File file : fileList) {
			final String fname = file.getName().toLowerCase(Locale.ENGLISH);
			final String cropfname = fname.replace("mascot.dat.", "");
			try {
				final int fnameNumber = Integer.parseInt(cropfname);
				if (fnameNumber > lastSuffix) {
					lastSuffix = fnameNumber;
				}
			} catch (NumberFormatException e) {
				LOGGER.debug(e);
			}
		}
		return lastSuffix;
	}

	/**
	 * Generates a line that should be added to the mascot.dat file to deploy the given file
	 *
	 * @param fileToDeploy the file we want to deploy
	 * @return the line that can be used in deployment or null if we couldn't adeqately create a deployment line
	 */
	private String generateDeploymentLine(final String uniqueName, final File fileToDeploy) {

		//createa  StringBuilder that should be used to generate the
		final StringBuilder sb = new StringBuilder();

		sb.append(uniqueName).append("\t");

		sb.append(FileUtilities.stripExtension(fileToDeploy.getAbsolutePath().replace("\\", "/")))
				.append("*.")
				.append(FileUtilities.getExtension(fileToDeploy.getAbsolutePath()));

		//append the parameter string tells mascot how to do certain things.  This should be uniform between all deployed curations
		sb.append(this.getDeploymentParameterList());

		return sb.toString();
	}

	private static final Map<String, List<ProgressReporter>> CO_DEPLOYMENTS = new HashMap<String, List<ProgressReporter>>();

	public Map<String, List<ProgressReporter>> getCoDeployments() {
		return CO_DEPLOYMENTS;
	}

	@Override
	public void setEngineRootFolder(final File engineRootFolder) {
		super.setEngineRootFolder(engineRootFolder);
	}

	/**
	 * retreives the mascot.datParameters property and makes sure it is complete and valid.  There will be a tab before the first and between parameters.
	 *
	 * @return the validated property or null if we were unable to validate it a returned value of null should ensure no deployment occurs
	 */
	private synchronized String getDeploymentParameterList() {
		try {
			final String parameterList = this.datParameters;

			final String[] individualParameters = parameterList.split("[\\s]");

			if (individualParameters.length < 12) {
				return null;
			}

			if (!(individualParameters[0].equals("AA") || individualParameters[0].equals("NA"))) {
				return null;
			}

			//go through the rest of the parameters and make sure they are numbers
			for (int i = 2; i < individualParameters.length; ++i) {
				Integer.parseInt(individualParameters[i]);
			}

			final StringBuilder sb = new StringBuilder();
			for (final String param : individualParameters) {
				sb.append("\t").append(param);
			}
			return sb.toString();

		} catch (Exception e) {//likely a null pointer exception as a result of the parameter not being correct
			LOGGER.warn("Error validating parameter list: " + e.getMessage());
		}
		return null;
	}

	/**
	 * this is a class that is in charge of monitorying the monitor.log file for changes to the
	 */
	private class MonitorLogPoller implements Runnable {

		public static final int STATUS_MONITORING = 0;
		public static final int STATUS_SUCCESSFUL = 1;
		public static final int STATUS_FAILED = 2;

		private final File logFile;
		private final Pattern failedIndicator;
		private final Pattern successIndicator;
		private final Pattern outOfDatabasesIndicator;
		private final String shortname;
		private final StringBuilder log = new StringBuilder();
		private int status = 0;

		/**
		 * @param logFile            the file that we should look for log messages on
		 * @param shortnameToLookFor the shortname that will appear in lines about our current deployment
		 * @throws java.io.IOException if there is a problem setting up the deployment
		 */
		public MonitorLogPoller(final File logFile, final String shortnameToLookFor) throws IOException {

			this.logFile = logFile;
			if (!logFile.exists()) {
				throw new IOException("Could not find the monitor.log file");
			}

			shortname = shortnameToLookFor;

			failedIndicator = Pattern.compile("^.*" + shortnameToLookFor + "0?.*to Halted.*");

			outOfDatabasesIndicator = Pattern.compile("^.*Maximum number of active databases has been exceeded.*" + shortnameToLookFor + "0?.*will not be available.*");

			successIndicator = Pattern.compile("^.*" + shortnameToLookFor + "\\s*\\d? Just enabled memory mapping\\s+to In use.*");
		}

		/**
		 * goes through and watches the monitor.log file and reads the new lines
		 */
		public void run() {
			LOGGER.debug("Monitoring " + logFile.getAbsolutePath());
			RandomAccessFile logRAF = null;
			try {
				//open the log file read only and go to the end and read in the position at the end of the file
				logRAF = new RandomAccessFile(this.logFile, "r");

				LOGGER.info("Monitoring file: " + logFile.getAbsolutePath());

				long lastWriteCheck = this.logFile.length();
				final List<String> linesSinceLastCheck = new ArrayList<String>();

				long lastReadLinePosition = logRAF.length() - 1;
				logRAF.seek(lastReadLinePosition);

				final int maximumTimeout = 500;
				int currentTimeout = 10;
				while (status == STATUS_MONITORING) {
					final long currentWriteCheck = this.logFile.length();

					//if the size of the file has changed
					if (currentWriteCheck > lastWriteCheck) {
						LOGGER.debug("File size change detected");
						lastWriteCheck = currentWriteCheck;//update filesize tracker

						logRAF.seek(lastReadLinePosition);

						String newLine = logRAF.readLine();
						while (newLine != null) {
							//I want the cursor to stay at the last line read since the current line may not be complete yet and next time I want to re-read that line
							linesSinceLastCheck.add(newLine);
							lastReadLinePosition = logRAF.getFilePointer();
							newLine = logRAF.readLine();
						}

						if (linesSinceLastCheck.size() > 0) {
							for (final String line : linesSinceLastCheck) {
								//if the line contains the shortname then we want to do something with it
								if (line.contains(shortname)) {
									//log any lines that contain the short name

									synchronized (this) {
										this.log.append(line).append("\n");
									}

									if (this.successIndicator.matcher(line).matches()) {
										LOGGER.debug("Mascot deploying  " + shortname + ": success indicator matched: " + line);
										this.status = STATUS_SUCCESSFUL;
									} else if (this.failedIndicator.matcher(line).matches()) {
										LOGGER.debug("Mascot deploying " + shortname + ": failed indicator matched: " + line);
										this.status = STATUS_FAILED;
									} else if (this.outOfDatabasesIndicator.matcher(line).matches()) {
										LOGGER.debug("Mascot deploying " + shortname + ": out of databases indicator matched: " + line);
										this.status = STATUS_FAILED;
									} else {
										LOGGER.debug("Mascot deploying " + shortname + ": relevant line: " + line);
									}
								}
							}
						}
					}

					if (status == STATUS_MONITORING) {
						Thread.sleep(currentTimeout);
						currentTimeout = Math.min(currentTimeout * 2, maximumTimeout);
					}
				}

			} catch (FileNotFoundException e) {
				//this should not happen since we checked when the object was created to make sure the file existed
				LOGGER.error(e);
			} catch (IOException e) {
				LOGGER.error(e);
			} catch (InterruptedException e) {
				LOGGER.error(e);
			} finally {

				if (logRAF != null) {
					try {
						logRAF.close();
					} catch (IOException e) {
						LOGGER.error(e);
					}
				}
			}
		}


		/**
		 * gets the current status of the monitor.  This is used to determine if the monitor is finished or not and
		 * if it completed successfully.
		 *
		 * @return
		 */
		public synchronized int getStatus() {
			return this.status;
		}

		/**
		 * any information that the caller may be interested in will be contained here.
		 *
		 * @return the current log information of this monitor which monitors the monitor :)
		 */
		public synchronized String getLog() {
			return log.toString();
		}
	}

	/**
	 * @return The previously set log file. If none was set, default log location is returned (<tt>mascot install dir/config/mascot.dat)</tt>)
	 */
	public File getDatFile() {
		if (datFile == null) {
			return new File(getEngineRootFolder(), MASCOT_DAT);
		}
		return datFile;
	}

	public void setDatFile(final File datFile) {
		this.datFile = datFile;
	}

	/**
	 * @return The previously set log file. If none was set, default log location is returned (<tt>mascot install dir/logs/monitor.log)</tt>)
	 */
	public File getLogFile() {
		if (logFile == null) {
			return new File(getEngineRootFolder(), MONITOR_LOG);
		}
		return logFile;
	}

	public void setLogFile(final File logFile) {
		this.logFile = logFile;
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {
		private String engineRootFolder;
		private String mascotDbMaintenanceUri;
		private String deployableDbFolder;

		public Config() {
		}

		public Config(final String engineRootFolder, final String mascotDbMaintenanceUri, final String deployableDbFolder) {
			this.engineRootFolder = engineRootFolder;
			this.deployableDbFolder = deployableDbFolder;
			this.mascotDbMaintenanceUri = mascotDbMaintenanceUri;
		}

		public String getEngineRootFolder() {
			return engineRootFolder;
		}

		public void setEngineRootFolder(final String engineRootFolder) {
			this.engineRootFolder = engineRootFolder;
		}

		public String getDeployableDbFolder() {
			return deployableDbFolder;
		}

		public void setDeployableDbFolder(final String deployableDbFolder) {
			this.deployableDbFolder = deployableDbFolder;
		}

		public String getMascotDbMaintenanceUri() {
			return mascotDbMaintenanceUri;
		}

		public void setMascotDbMaintenanceUri(final String mascotDbMaintenanceUri) {
			this.mascotDbMaintenanceUri = mascotDbMaintenanceUri;
		}

		public Map<String, String> save(final DependencyResolver resolver) {
			final Map<String, String> map = new TreeMap<String, String>();
			map.put(ENGINE_ROOT_FOLDER, engineRootFolder);
			map.put(DEPLOYABLE_DB_FOLDER, deployableDbFolder);
			map.put(MASCOT_DB_MAINTENANCE_URI, mascotDbMaintenanceUri);
			return map;
		}

		public void load(final Map<String, String> values, final DependencyResolver resolver) {
			engineRootFolder = values.get(ENGINE_ROOT_FOLDER);
			deployableDbFolder = values.get(DEPLOYABLE_DB_FOLDER);
			mascotDbMaintenanceUri = values.get(MASCOT_DB_MAINTENANCE_URI);
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	/**
	 * A factory capable of creating the worker
	 */
	public static final class Factory extends WorkerFactoryBase<Config> {

		private boolean deploymentEnabled;
		private String engineVersion;
		private String datParameters;
		private String repLine;
		private String seqLine;
		private String mascotDatabaseMaintenanceUriPostfix;

		private static final Pattern MASCOT_ROOT_PATTERN = Pattern.compile("MASCOT-INSTALLATION-ROOT");

		public String getEngineVersion() {
			return engineVersion;
		}

		public void setEngineVersion(final String engineVersion) {
			this.engineVersion = engineVersion;
		}

		public String getDatParameters() {
			return datParameters;
		}

		public void setDatParameters(final String datParameters) {
			this.datParameters = datParameters;
		}

		public String getRepLine() {
			return repLine;
		}

		public void setRepLine(final String repLine) {
			this.repLine = repLine;
		}

		public String getSeqLine() {
			return seqLine;
		}

		public void setSeqLine(final String seqLine) {
			this.seqLine = seqLine;
		}

		public boolean isDeploymentEnabled() {
			return deploymentEnabled;
		}

		public void setDeploymentEnabled(final boolean deploymentEnabled) {
			this.deploymentEnabled = deploymentEnabled;
		}

		public String getMascotDatabaseMaintenanceUriPostfix() {
			return mascotDatabaseMaintenanceUriPostfix;
		}

		public void setMascotDatabaseMaintenanceUriPostfix(final String mascotDatabaseMaintenanceUriPostfix) {
			this.mascotDatabaseMaintenanceUriPostfix = mascotDatabaseMaintenanceUriPostfix;
		}

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			MascotDeploymentService worker = null;
			try {
				final URI dbMaintenanceUri;
				if (config.getMascotDbMaintenanceUri().indexOf(mascotDatabaseMaintenanceUriPostfix) != -1) {
					dbMaintenanceUri = new URI(config.getMascotDbMaintenanceUri());
				} else {
					if (config.getMascotDbMaintenanceUri().endsWith("/")) {
						dbMaintenanceUri = new URI(config.getMascotDbMaintenanceUri() + mascotDatabaseMaintenanceUriPostfix);
					} else {
						dbMaintenanceUri = new URI(config.getMascotDbMaintenanceUri() + "/" + mascotDatabaseMaintenanceUriPostfix);
					}
				}
				final String mascotInstallationRoot = new File(config.getEngineRootFolder()).getAbsolutePath();
				worker = new MascotDeploymentService(
						getSeqLine().replaceAll(MASCOT_ROOT_PATTERN.pattern(), mascotInstallationRoot),
						getRepLine().replaceAll(MASCOT_ROOT_PATTERN.pattern(), mascotInstallationRoot),
						getDatParameters(),
						dbMaintenanceUri
				);
				worker.setDeployableDbFolder(new File(config.getDeployableDbFolder()).getAbsoluteFile());
				worker.setEngineVersion(getEngineVersion());
				worker.setEngineRootFolder(new File(config.getEngineRootFolder()).getAbsoluteFile());
			} catch (Exception e) {
				throw new MprcException("Mascot deployment service worker could not be created.", e);
			}
			return worker;
		}
	}

	public static final class Ui implements ServiceUiFactory {
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.property(ENGINE_ROOT_FOLDER, "Mascot Installation Folder", "The deployer modifies <tt>config/mascot.dat</tt> in this folder and watches <tt>logs/monitor.log</tt> to determine if the deployment succeeded.").existingDirectory().required()
					.property(MASCOT_DB_MAINTENANCE_URI, "Mascot Database Maintenance Url", "Mascot database maintenance url, for example, <tt>http://mascot/x-cgi/db_gui.pl</tt>").required()
					.property(DEPLOYABLE_DB_FOLDER, "Database Index Folder",
							"The deployer links the .fasta file to this folder, so Mascot can create its indices around it.").required()
					.existingDirectory().defaultValue("var/mascot_index");
		}
	}
}
