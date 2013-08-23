package edu.mayo.mprc.dbcurator.model;

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPException;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.dbcurator.model.persistence.CurationDao;
import edu.mayo.mprc.utilities.AsyncFileWriter;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;


/**
 * A class that represents a file that we previously downloaded (or uploaded). We want to try to eliminate redundancy so
 * there is a factory method that must be used to create a new SourceDatabaseArchive.  This factory method will check to
 * make sure that we don't already have the file somewhere to avoid downloading it.  If the archive is already around
 * then it get that object from persistent store and return that one.  There is no reason to modify this class so all
 * setter methods are protected (for ORM usage).
 */
public class SourceDatabaseArchive implements Serializable {
	private static final long serialVersionUID = 20071220L;

	private static final Logger LOGGER = Logger.getLogger(SourceDatabaseArchive.class);
	/**
	 * a unique id used for persistence
	 */
	private Integer id = null;

	/**
	 * the url where we got the file from
	 */
	private String sourceURL;

	/**
	 * the path to the local archive file
	 */
	private File archive;

	/**
	 * supposedly when the file was placed on the ftp server
	 */
	private DateTime serverDate;

	/**
	 * the date when we downloaded the file
	 */
	private DateTime downloadDate;

	private static final String CLASSPATH_URL_PREFIX = "classpath:";

	/**
	 * A constructor that we want to hide so we first try to see if the archive already exists in our system.
	 */
	public SourceDatabaseArchive() {
		super();
	}

	protected SourceDatabaseArchive(final String url, final File file, final DateTime serverDate, final DateTime downloadDate) {
		super();
		this.setSourceURL(url);
		this.setArchive(file);
		this.setServerDate(serverDate);
		this.setDownloadDate(downloadDate);
	}


	/**
	 * A factory method which will create a new archive but will first try to see if we already have database file from
	 * the specified URL at a date newer than any associated with SourceDatabaseArchives already in our system.  It does
	 * this by checking to the URL specified and getting the modification date of the file and if the modification date
	 * is newer then it will start the download of the file.  Otherwise it will take the file that we already have
	 * locally and point towards that file as a source.
	 * <p/>
	 *
	 * @param urlString the url you want to create an archive of. Will get cleaned up by passing through URL constructor and toString.
	 * @param status    the status object you want to notify of progress (null if not notification desired)
	 * @return the archive representing the newest database file available form that URL
	 * @throws java.io.IOException if there a problem with the local file such as trying to overwrite
	 */
	public SourceDatabaseArchive createArchive(String urlString, final CurationStatus status, final File archFolder, final CurationDao curationDao) throws IOException {
		URL url = null;
		if (!urlString.startsWith(CLASSPATH_URL_PREFIX)) {
			// Cleanup the URL to canonical foramt
			try {
				url = new URL(urlString);
			} catch (MalformedURLException e) {
				throw new MprcException("The URL of database to download [" + urlString + "] is malformed", e);
			}
			urlString = url.toString();
		}

		//if the string passed in is just a title to one of our common ones translate to the actual url
		final FastaSource commonMatch = curationDao.getDataSourceByName(urlString);
		if (commonMatch != null) {
			urlString = commonMatch.getUrl();
		}

		final File dstFile = new File(archFolder, getFileName(urlString));
		if (dstFile.exists()) {
			throw new IOException("the database archive file " + dstFile.getAbsolutePath() + " already exists. Please specify a different destination file.");
		}
		FileUtilities.ensureFolderExists(dstFile.getParentFile());

		SourceDatabaseArchive retSource = null;

		if (urlString.startsWith(CLASSPATH_URL_PREFIX)) {

			final FastaSource safetyMatch = curationDao.getDataSourceByUrl(urlString);
			if (!urlString.endsWith("ShortTest.fasta.gz") && !urlString.endsWith("abrf_contam_2007.fasta.gz") && safetyMatch == null) {
				throw new MprcException("The classpath resource given isn't in acceptable list this check is done for security reasons.");
			}

			InputStream istream = null;
			try {
				final String streamPath = urlString.replace(CLASSPATH_URL_PREFIX, "");
				istream = SourceDatabaseArchive.class.getResourceAsStream(streamPath);
				FileUtilities.writeStreamToFile(istream, dstFile);

				final File similarFile = FileUtilities.findSingleSimilarFile(dstFile, archFolder);
				retSource = new SourceDatabaseArchive();
				retSource.setDownloadDate(new DateTime());
				retSource.setServerDate(new DateTime());
				retSource.setSourceURL(urlString);
				if (similarFile != null) {
					if (status != null) {
						status.addMessage("Using a similar file to " + dstFile.getAbsoluteFile() + ", found in the archive at: " + similarFile.getAbsolutePath());
					}
					FileUtilities.quietDelete(dstFile);
					retSource.setArchive(similarFile);
				} else {
					retSource.setArchive(dstFile);
				}

			} finally {
				FileUtilities.closeQuietly(istream);
			}

		} else {
			DateTime ftpLastModified = null;
			// ftp doesn't support mod date so I need to have a seperate path for getting the mod date if our url specifies an ftp protocol
			if (url.getProtocol().equalsIgnoreCase("ftp")) {
				FTPClient ftp = null;
				try {
					ftp = new FTPClient();
					ftp.setRemoteHost(url.getHost());
					ftp.setRemotePort((url.getPort() == -1 ? 21 : url.getPort()));

					ftp.connect();
					if (url.getUserInfo() == null) {
						ftp.login("anonymous", "mprc@mayo.edu");
					} else {
						final String[] credentials = url.getUserInfo().split(":");
						ftp.login(credentials[0], credentials[1]);
					}

					final String[] path = url.getPath().split("/");
					for (int i = 0; i < path.length - 1; i++) {
						if (!path[i].isEmpty()) {
							ftp.chdir(path[i]);
						}
					}
					ftpLastModified = new DateTime(ftp.modtime(path[path.length - 1]));

				} catch (Exception e) {
					LOGGER.debug(e);
					throw new MprcException("There was a problem retrieving data from ["+url.getProtocol()+"://"+url.getHost()+(url.getPort()>0?":"+url.getPort():"")+url.getPath()+ "]", e);
				} finally {
					if (ftp != null) {
						try {
							ftp.quitImmediately();
						} catch (FTPException e) {
							throw new MprcException("Could not quit ftp client.", e);
						}
					}
				}
			}

			final URLConnection connection = url.openConnection();
			connection.connect();

			final DateTime urlLastMod = (ftpLastModified == null ? new DateTime(connection.getLastModified()) : ftpLastModified);

			try {
				retSource = curationDao.findSourceDatabaseInExistence(urlString, urlLastMod);
			} catch (Exception e) {
				if (status != null) {
					status.addMessage("Could not find existing file so downloading.");
				}
				retSource = null;
			}

			//if retSource.getServerDate().getTime() returns -1 or 0 that means server date wasn't available and we should create a new one.
			if (retSource == null || retSource.getArchive() == null || retSource.getServerDate().toDate().getTime() < 1 || !retSource.getArchive().exists()) {

				final AsyncFileWriter writeStatus = AsyncFileWriter.writeURLToFile(url, dstFile, null);

				while (!writeStatus.isDone() && !writeStatus.isCancelled()) {

					try {
						Thread.sleep(2500);
					} catch (InterruptedException e) {
						throw new MprcException(e);
					}

					if (status != null) {
						status.setCurrentStepProgress(writeStatus.getProgress());
					}
				}

				try {
					final File downloadedFile = writeStatus.get(500, TimeUnit.MILLISECONDS);
					final File similarFile = FileUtilities.findSingleSimilarFile(dstFile, archFolder);

					retSource = new SourceDatabaseArchive();
					retSource.setDownloadDate(new DateTime());
					retSource.setServerDate(urlLastMod);
					retSource.setSourceURL(urlString);
					if (similarFile != null) {
						if (status != null) {
							status.addMessage("Using a similar file to " + dstFile.getAbsoluteFile() + ", found in the archive at: " + similarFile.getAbsolutePath());
						}
						FileUtilities.quietDelete(downloadedFile);
						retSource.setArchive(similarFile);
					} else {
						retSource.setArchive(downloadedFile);
					}
				} catch (Exception e) {
					throw new MprcException(e);
				}
			} else {
				if (status != null) {
					status.addMessage("We could use an existing archive file.");
				}
			}
		}

		try {
			curationDao.save(retSource);
			curationDao.flush();
		} catch (Exception e) {
			LOGGER.warn("Could not save the source.", e);
		}

		return retSource;
	}

	/**
	 * @return Copy of all persistent attributes (except DB id).
	 */
	public SourceDatabaseArchive createCopy() {
		final SourceDatabaseArchive copy = new SourceDatabaseArchive();
		copy.downloadDate = this.downloadDate;
		copy.sourceURL = this.sourceURL;
		copy.serverDate = this.serverDate;
		copy.archive = this.archive;
		return copy;
	}

	/**
	 * create a unique timestamped name for a given file.  It will take the filename as the file before the first period ('.')
	 * and add a timestamp and a .FASTA extention.
	 *
	 * @param urlString the name of the url to determine a good name
	 * @return the timestamped name
	 */
	private String getFileName(final String urlString) {
		final StringTokenizer t = new StringTokenizer(urlString, "/");
		String filename = null;
		while (t.hasMoreTokens()) {
			filename = t.nextToken();
		}
		if (filename == null) {
			filename = "";
		}
		final int firstDotIndex = filename.indexOf('.', 0);
		final String name = filename.substring(0, firstDotIndex);
		return name + "_" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".fasta.gz";
	}

	public File getArchive() {
		return this.archive;
	}

	public void setArchive(final File file) {
		this.archive = file;
	}

	protected Integer getId() {
		return id;
	}

	protected void setId(final Integer id) {
		this.id = id;
	}

	public String getSourceURL() {
		return sourceURL;
	}

	public void setSourceURL(final String sourceURL) {
		this.sourceURL = sourceURL;
	}

	public DateTime getServerDate() {
		return serverDate;
	}

	public void setServerDate(final DateTime serverDate) {
		this.serverDate = serverDate;
	}

	public DateTime getDownloadDate() {
		return downloadDate;
	}

	public void setDownloadDate(final DateTime downloadDate) {
		this.downloadDate = downloadDate;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SourceDatabaseArchive)) {
			return false;
		}

		final SourceDatabaseArchive that = (SourceDatabaseArchive) o;

		if (getArchive() != null ? !getArchive().equals(that.getArchive()) : that.getArchive() != null) {
			return false;
		}
		if (getDownloadDate() != null ? !getDownloadDate().equals(that.getDownloadDate()) : that.getDownloadDate() != null) {
			return false;
		}
		if (getServerDate() != null ? !getServerDate().equals(that.getServerDate()) : that.getServerDate() != null) {
			return false;
		}
		if (getSourceURL() != null ? !getSourceURL().equals(that.getSourceURL()) : that.getSourceURL() != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = getSourceURL() != null ? getSourceURL().hashCode() : 0;
		result = 31 * result + (getArchive() != null ? getArchive().hashCode() : 0);
		result = 31 * result + (getServerDate() != null ? getServerDate().hashCode() : 0);
		result = 31 * result + (getDownloadDate() != null ? getDownloadDate().hashCode() : 0);
		return result;
	}
}
