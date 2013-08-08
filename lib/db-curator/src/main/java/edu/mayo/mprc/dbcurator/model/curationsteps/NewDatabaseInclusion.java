package edu.mayo.mprc.dbcurator.model.curationsteps;

import edu.mayo.mprc.dbcurator.model.*;
import edu.mayo.mprc.dbcurator.model.persistence.CurationDao;
import edu.mayo.mprc.fasta.DBInputStream;
import edu.mayo.mprc.fasta.DBOutputStream;
import edu.mayo.mprc.fasta.FASTAInputStream;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.StringUtilities;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A CurationStep that adds sequences to the database based on a url that is given to it.  If the URL contains a more
 * recent file then we have already downloaded then we will need to download the new file which may take several
 * minutes.
 *
 * @author Eric J. Winter Date: Apr 10, 2007 Time: 10:14:32 AM
 */
public class NewDatabaseInclusion implements CurationStep {
	private static final long serialVersionUID = 20071220L;

	/**
	 * the id that will be assigned upon persistence
	 */
	private Integer id;

	/**
	 * The validation that was the result (returned object) of the most recent call to performStep() TRANSIENT
	 */
	private transient StepValidation recentRunValidation = null;

	/**
	 * the url of the database we downloaded or need to download.  This will generally be an ftp url.  If we need to
	 * check for data at the specified URL and but we cannot find it for some reason we will raise an exception.
	 * PERSISTENT
	 */
	private String url;

	/**
	 * the source database if it has already been downloaded.  If we have not yet downloaded the database then set it to
	 * null.  The natural key for a SourceDatabaseArchive is the URL and timestamp.  If the source is null then we have
	 * no choice but to check for an sequence at the provided url. PERSISTENT
	 */
	private SourceDatabaseArchive source = null;

	private transient HeaderTransform headerTransformer;

	public NewDatabaseInclusion() {
		super();
	}

	public void setHeaderTransform(final HeaderTransform trans) {
		this.headerTransformer = trans;
	}

	public HeaderTransform getHeaderTransform(final CurationDao curationDao) {
		if (headerTransformer == null) {
			headerTransformer = curationDao.getHeaderTransformByUrl(this.url);
		}
		return headerTransformer;
	}

	/**
	 * Takes a database and writes in and writes it out but adds more sequences to it from the database indicated at
	 * with the curent source which may need to be created from the URL that has been set.  If the url has not been set
	 * then this step will fail and why will be returned in the StepValidation.  If this is the first step then
	 * we will not copy the sequences from the source but only pass a reference to the source to the next step.
	 *
	 * @param exec the executor that we are working for.  We will need to pull some information from the exector to do
	 *             our job
	 * @return the validation object that was created during this process.  Basically an unchecked error handling
	 *         mechanism.  You can check this object to see if there was any problems.
	 */
	public StepValidation performStep(final CurationExecutor exec) {
		//create a new validation object for this run
		this.recentRunValidation = new StepValidation();

		//get the data we will need
		final DBInputStream in = exec.getCurrentInStream(); //the file we should be reading from (may be null)
		final DBOutputStream out = exec.getCurrentOutStream(); // the file we should be writing to
		final CurationStatus status = exec.getStatusObject(); //the status objec we want to update

		// take the data from the input file and copy it to the output file.  We are not doing any filtering
		//  during this step so very one will be copied if any exist.
		if (in != null) {
			try {
				in.beforeFirst();
				out.appendRemaining(in);
			} catch (IOException e) {
				this.recentRunValidation.addMessageAndException("Error copying in stream to out stream", e);
			}
		}
		//if we want or need to pull down a new SourceDatabaseArchive then do it.
		if (this.getSource() == null) {
			try {
				//get a source database by first connecting to the ftp server and seeing what date the file at the given
				//url was updated and seeing if we have already downloaded it.  If so use then one else download it.
				this.source = new SourceDatabaseArchive().createArchive(
						this.url,
						status,
						exec.getFastaArchiveFolder(), exec.getCurationDao());
				status.addMessage("We have completed getting an archive.");
			} catch (Exception e) {
				//if we couldn't find or download a database then we had an error and the executor should be notified
				this.recentRunValidation.addMessageAndException("FTP error: \n" + e.getMessage(), e);
				return this.recentRunValidation;
			}
		}

		// we want to create a new DBInputStream from the archive file and copy the archive file into the output stream
		DBInputStream archiveIn = null;
		try {
			try {
				archiveIn = new FASTAInputStream(this.source.getArchive());
			} catch (Exception e) {
				//this is not expected to happen
				this.recentRunValidation.addMessageAndException("Error creating an input stream from the archive", e);
			}

			//next if we have an archive setup then we want to copy it to the output stream
			if (archiveIn != null) {
				try {
					final HeaderTransform transform = getHeaderTransform(exec.getCurationDao());
					archiveIn.beforeFirst();
					while (archiveIn.gotoNextSequence()) {
						if (transform == null) {
							out.appendSequence(archiveIn.getHeader(), archiveIn.getSequence());
						} else {
							out.appendSequence(transform.transform(archiveIn.getHeader()), archiveIn.getSequence());
						}
					}
				} catch (IOException e) {
					this.recentRunValidation.addMessageAndException("Error copying archive to output file", e);
					return this.recentRunValidation;
				}
			} else {
				this.recentRunValidation.addMessage("Error finding the input file");
			}
		} finally {
			FileUtilities.closeQuietly(archiveIn);
		}

		this.recentRunValidation.setCompletionCount(out.getSequenceCount());
		this.setLastRunCompletionCount(out.getSequenceCount());

		return this.recentRunValidation;
	}

	public StepValidation preValidate(final CurationDao curationDao) {
		final StepValidation preValidation = new StepValidation();

		if (this.url == null) {
			preValidation.addMessage("URL not set");
		} else {
			//check to see if the url passed in is actually a key into the map of common urls if so check the mapped value
			final FastaSource matchedSource = curationDao.getDataSourceByName(this.url);


			final String testURL = (matchedSource == null ? this.url : matchedSource.getUrl());

			if (!StringUtilities.startsWithIgnoreCase(testURL, "classpath:")) {
				try {
					final URL url = new URL(testURL);
					assert !"".equals(url.toString());
				} catch (MalformedURLException ignore) {
					preValidation.addMessage("Invalid URL");
				}
			}
			//TODO: how to see if a resource exists short of making an inputstream
		}
		return preValidation;
	}

	public CurationStep createCopy() {
		final NewDatabaseInclusion copy = new NewDatabaseInclusion();
		copy.setUrl(this.url);
		copy.setSource(this.source == null ? null : this.source.createCopy());
		return copy;
	}

	/**
	 * gets the URL that is associated with this inclusion.
	 *
	 * @return the associated url
	 */
	public String getUrl() {
		return this.url;
	}

	/**
	 * set the url that should be used for this inclusion
	 *
	 * @param url
	 */
	public void setUrl(final String url) {
		this.url = url;
	}

	/**
	 * gets the source of this inclusion
	 *
	 * @return the source database that was used by this step.  Null if the step has not been run yet
	 */
	public SourceDatabaseArchive getSource() {
		return source;
	}

	/**
	 * sets the source database that was used by this step
	 *
	 * @param source the source that was used by the setp
	 */
	protected void setSource(final SourceDatabaseArchive source) {
		this.source = source;
	}

	public Integer getId() {
		return id;
	}

	public void setId(final Integer id) {
		this.id = id;
	}


	/**
	 * the number of sequences that were present in the curation after this step was last run
	 */
	private Integer lastRunCompletionCount = null;

	public Integer getLastRunCompletionCount() {
		return this.lastRunCompletionCount;
	}

	public void setLastRunCompletionCount(final Integer count) {
		this.lastRunCompletionCount = count;
	}

	public String simpleDescription() {
		final int lastPathIndex = url.lastIndexOf('/');
		if (lastPathIndex > 0) {
			final String filename = url.substring(lastPathIndex + 1).replace(".gz", "").replace(".fasta", "");
			return "Database " + filename;
		} else {
			return url;
		}
	}

	protected static int getIntersectionLength(final String s1, final String s2) {
		if (s1.contains(s2)) {
			return s1.length() - s1.replace(s2, "").length();
		} else if (s2.contains(s1)) {
			return s2.length() - s2.replace(s1, "").length();
		} else {
			return 0;
		}
	}
}
