package edu.mayo.mprc.dbundeploy;

import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Class contains results of multiple database undeployment tasks.
 */
public final class DatabaseUndeployerResult implements ProgressInfo {
	private static final long serialVersionUID = 20101221L;

	private Map<String, UndeploymentTaskResult> undeploymentTaskResults;

	public DatabaseUndeployerResult() {
		undeploymentTaskResults = new HashMap<String, UndeploymentTaskResult>();
	}

	public Map<String, UndeploymentTaskResult> getDatabaseUndeployerResults() {
		return undeploymentTaskResults;
	}

	public void addUndeploymentTaskResult(final String connectionName, final UndeploymentTaskResult taskResult) {
		undeploymentTaskResults.put(connectionName, taskResult);
	}
}
