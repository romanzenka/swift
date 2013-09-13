package edu.mayo.mprc.fastadb;

import edu.mayo.mprc.database.PersistableBase;

import java.util.Date;

/**
 * @author Roman Zenka
 */
public class BulkLoadJob extends PersistableBase {
	private Date jobDate;

	public BulkLoadJob() {
		jobDate = new Date();
	}

	public Date getJobDate() {
		return jobDate;
	}

	public void setJobDate(Date date) {
		this.jobDate = date;
	}
}
