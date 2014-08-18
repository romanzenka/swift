package edu.mayo.mprc.sge;

import edu.mayo.mprc.config.Lifecycle;

/**
 * @author Roman Zenka
 */
public interface GridEngineJobManager extends Lifecycle {
	/**
	 * creates job template based on packet content and runs it
	 *
	 * @param pgridPacket - the information about the job
	 * @return id of SGE assigned job id.
	 */
	String passToGridEngine(GridWorkPacket pgridPacket);
}
