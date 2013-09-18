package edu.mayo.mprc.fastadb;

/**
 * @author Roman Zenka
 */
public interface BulkLoadJobStarter {


	/**
	 * @return New bulk load job - mostly gives you an id to work with.
	 */
	BulkLoadJob startNewJob();

	/**
	 * Remove the bulk job entry once the job is over.
	 *
	 * @param job Job to remove.
	 */
	void endJob(BulkLoadJob job);
}
