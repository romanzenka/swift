package edu.mayo.mprc.dbcurator.client.steppanels;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * A class that can be communicated between client and server that contains all of the information needed to represent
 * a Curation.
 */
public final class CurationStub implements Serializable {
	private static final long serialVersionUID = 20100212;

	private Integer id;
	private String shortName = "";
	private String title = "";
	private String ownerEmail = "";
	private String lastRunDate = "";
	private String pathToResult = "";
	private List<String> errorMessages = new ArrayList<String>();
	private String notes = "";
	private List<CurationStepStub> steps = new ArrayList<CurationStepStub>();
	private String decoyRegex = "Reversed_";

	public boolean hasBeenRun() {
		return (!getPathToResult().equals(""));
	}

	public CurationStub() {
	}

	/**
	 * the id of this curation.  This is used for synchronization with the server.
	 */
	public Integer getId() {
		return id;
	}

	public void setId(final Integer id) {
		this.id = id;
	}

	/**
	 * a short name for this curation that will be used for naming files.  This name should only contain letters, numbers, and underscores
	 */
	public String getShortName() {
		return shortName;
	}

	public void setShortName(final String shortName) {
		this.shortName = shortName;
	}

	/**
	 * the title of the curation
	 */
	public String getTitle() {
		return title;
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	/**
	 * the user id of the person who owns this curation
	 */
	public String getOwnerEmail() {
		return ownerEmail;
	}

	public void setOwnerEmail(final String ownerEmail) {
		this.ownerEmail = ownerEmail;
	}

	/**
	 * the date when the curation was last run if at all
	 */
	public String getLastRunDate() {
		return lastRunDate;
	}

	public void setLastRunDate(final String lastRunDate) {
		this.lastRunDate = lastRunDate;
	}

	/**
	 * the path to the resulting fasta file if it has been run
	 */
	public String getPathToResult() {
		return pathToResult;
	}

	public void setPathToResult(final String pathToResult) {
		this.pathToResult = pathToResult;
	}

	/**
	 * Any error messages that should be displayed with regards to the curation as a whole.  Examples would be failed validations
	 * or failed runs.
	 */
	public List<String> getErrorMessages() {
		return errorMessages;
	}

	public void setErrorMessages(final List<String> errorMessages) {
		this.errorMessages = errorMessages;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(final String notes) {
		this.notes = notes;
	}

	public List<CurationStepStub> getSteps() {
		return steps;
	}

	public void setSteps(final List<CurationStepStub> steps) {
		this.steps = steps;
	}

	/**
	 * @return A Scaffold-style regex for marking decoy sequences.
	 */
	public String getDecoyRegex() {
		return decoyRegex;
	}

	/**
	 * @param decoyRegex A Scaffold-style regex for marking decoy sequences.
	 */
	public void setDecoyRegex(final String decoyRegex) {
		this.decoyRegex = decoyRegex;
	}
}
