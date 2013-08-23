package edu.mayo.mprc.enginedeployment;


import edu.mayo.mprc.daemon.files.FileHolder;
import edu.mayo.mprc.utilities.exceptions.CompositeException;
import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A highly synchronized way to allow concurrent deployments.  Contains mechanism for reporting exceptions and giving messages
 * as well as reporting when a deployment is done.
 * <p/>
 * This class is also used by the service to report back to the requester.  When the requester receives this in response it
 * will interrogate and see what the result of the deployment was and update the DeploymentResult that it is holding for
 * synchronization purposes.
 *
 * @author Eric Winter
 */
public class DeploymentResult extends FileHolder implements ProgressInfo {
	private static final long serialVersionUID = 20071220L;
	/**
	 * any exceptions that occured as part of the deployment, if any are set that indicates a failure during deployment
	 */
	private List<Exception> exceptions = new ArrayList<Exception>();
	/**
	 * any misc messages that occured during the deployment, not exceptions
	 */
	private List<String> messages = new ArrayList<String>();

	/**
	 * the path to the deployed fasta file.  This is dependant on the root path of the search engine on which it was deployed.
	 * This may need to be resolved somehow by a filesystem search.  Other files may be able to be found relative to the location
	 * of this file such as the indexed files.
	 */
	private File deployedFile;

	/**
	 * Files that have been undeployed. These files must be synchronized when and undeployment is performed.
	 */
	private List<File> undeployedFiles;

	/**
	 * this is a list of all files that were created as a result of the deployment.  This will not include the deployed fasta file
	 * only the files that were created as a result of the deployment itself.
	 */
	private List<File> generatedFiles = new ArrayList<File>();

	public DeploymentResult() {
	}

	/**
	 * the deployed file is meant to be a link to the FASTA file that was actually deployed.  Any index files will be found elsewhere
	 */
	public File getDeployedFile() {
		return deployedFile;
	}

	public void setDeployedFile(final File deployedFile) {
		this.deployedFile = deployedFile;
	}

	public List<File> getUndeployedFiles() {
		return undeployedFiles;
	}

	/**
	 * Files that have been undeployed. These files must are synchronized or receiver side when and undeployment is completed.
	 *
	 * @param undeployedFiles
	 */
	public void setUndeployedFiles(final List<File> undeployedFiles) {
		this.undeployedFiles = new LinkedList<File>();
		this.undeployedFiles.addAll(undeployedFiles);
	}

	public synchronized CompositeException getCompositeException() {
		if (this.exceptions.isEmpty()) {
			return null;
		}
		return new CompositeException(this.exceptions);
	}

	public synchronized void addException(final Exception toAdd) {
		this.exceptions.add(toAdd);
	}

	public synchronized List<String> getMessages() {
		return Collections.unmodifiableList(messages);
	}

	public synchronized void addMessage(final String toAdd) {
		this.messages.add(toAdd);
	}

	public List<File> getGeneratedFiles() {
		return generatedFiles;
	}

	public void setGeneratedFiles(final List<File> generatedFiles) {
		this.generatedFiles = new ArrayList<File>();
		this.generatedFiles.addAll(generatedFiles);
	}

	@Override
	public String toString() {
		return "DeploymentResult{" +
				"deployedFile=" + (deployedFile == null ? null : deployedFile) +
				'}';
	}
}
