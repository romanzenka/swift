package edu.mayo.mprc.dbcurator.model;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.EvolvableBase;
import edu.mayo.mprc.fasta.DatabaseAnnotation;
import edu.mayo.mprc.fasta.FastaFile;
import org.joda.time.DateTime;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that oversees the production of a local FASTADatabase file.<br> It maintains the state of many variables used
 * to define the database,<br> and its creation process.
 *
 * @author Eric J. Winter
 */
public class Curation extends EvolvableBase implements Serializable {
	private static final long serialVersionUID = 20071220L;
	private static final int EXPECTED_DESCRIPTION_SIZE = 500;

	public static final int SHORTNAME_MAX_LENGTH = 17;
	public static final int SHORTNAME_MIN_LENGTH = 5;

	/**
	 * any notes that were saved
	 */
	private String notes;
	/**
	 * A short title for quick identification.  This is the natural key and should be considered unique
	 */
	private String shortName;
	/**
	 * the date that this series of curations was first run.  When a curation is copied for refresh then this will
	 * also be copied.  This coupled with the shortName should be the natural key for.
	 */
	private DateTime firstRunDate;
	/**
	 * the FASTA file that was created as a result of this curation.  This should not change after being set.
	 */
	private File curationFile;
	/**
	 * A long title that should hopefully be unique (do we want to enforce that?)
	 */
	private String title;
	/**
	 * the email address of the owner (person who created it?)
	 */
	private String ownerEmail;
	/**
	 * the date that the curation was last run.  If this is not null then the curation has been run and should not be modified.
	 */
	private DateTime runDate;
	/**
	 * the date that this curation was deployed
	 */
	private DateTime deploymentDate;
	/**
	 * the steps involved in the curation
	 */
	private List<CurationStep> curationSteps = new ArrayList<CurationStep>();

	/**
	 * Regular expression (Scaffold-supported) describing which accession numbers belong to the decoy part of the database.
	 */
	private String decoyRegex;

	/**
	 * Creates a new curator given just the path.  It will be up to the caller to set a short title before which will
	 * generate the path and folder where this curation will be stored in.
	 */
	public Curation() {
		decoyRegex = DatabaseAnnotation.DEFAULT_DECOY_REGEX;
	}

	/**
	 * gets a filename that will be the final resting place for the results of this curation.  If a filename has not yet
	 * been assigned then we will generate a new one.
	 * <p/>
	 * Note: this will ensure that we don't ovewrite a file that we don't change the file that has already been created
	 * by this curation or change the filename of this curation.
	 *
	 * @return the name of the file that will contain this curations product
	 */
	public File getCurationFile() {
		return curationFile;
	}

	/**
	 * Method setCurationFile sets the curationFile of this Curation object.
	 *
	 * @param sharedFile the curationFile of this Curation object.
	 */
	public void setCurationFile(final File sharedFile) {
		curationFile = sharedFile;
	}

	/**
	 * The steps involved in this curation. Never modify the list manually!
	 *
	 * @return the list of steps in this curation
	 */
	public List<CurationStep> getCurationSteps() {
		return curationSteps;
	}

	/**
	 * set the curation steps.  This should not be done often and is included for ORM purposes.
	 *
	 * @param curationSteps the steps to set on this Curation
	 */
	protected void setCurationSteps(final List<CurationStep> curationSteps) {
		this.curationSteps = curationSteps;
	}

	/**
	 * Method getDeploymentDate returns the deploymentDate of this Curation object.
	 *
	 * @return the deploymentDate (type Date) of this Curation object.
	 */
	public DateTime getDeploymentDate() {
		return deploymentDate;
	}

	/**
	 * Method setDeploymentDate sets the deploymentDate of this Curation object.
	 *
	 * @param deploymentDate the deploymentDate of this Curation object.
	 */
	public void setDeploymentDate(final DateTime deploymentDate) {
		this.deploymentDate = deploymentDate;
	}

	/**
	 * Method getFirstRunDate returns the firstRunDate of this Curation object.
	 *
	 * @return the firstRunDate (type Date) of this Curation object.
	 */
	public DateTime getFirstRunDate() {
		return firstRunDate;
	}

	/**
	 * Method setFirstRunDate sets the firstRunDate of this Curation object.
	 *
	 * @param firstRunDate the firstRunDate of this Curation object.
	 */
	protected void setFirstRunDate(final DateTime firstRunDate) {
		this.firstRunDate = firstRunDate;
	}

	/**
	 * gets any long textual information that should be communicated
	 *
	 * @return the notes associated by
	 */
	public String getNotes() {
		return notes;
	}


	/**
	 * Method setNotes sets the notes of this Curation object.
	 *
	 * @param notes the notes of this Curation object.
	 */
	public void setNotes(final String notes) {
		this.notes = notes;
	}


	/**
	 * Method getOwnerEmail returns the ownerEmail of this Curation object.
	 *
	 * @return the ownerEmail (type String) of this Curation object.
	 */
	public String getOwnerEmail() {
		return ownerEmail;
	}

	/**
	 * Method setOwnerEmail sets the ownerEmail of this Curation object.
	 *
	 * @param ownerEmail the ownerEmail of this Curation object.
	 */
	public void setOwnerEmail(final String ownerEmail) {
		this.ownerEmail = ownerEmail;
	}

	/**
	 * Method getRunDate returns the runDate of this Curation object.
	 *
	 * @return the runDate (type Date) of this Curation object.
	 */
	public DateTime getRunDate() {
		return runDate;
	}


	/**
	 * Method getShortName returns the shortName of this Curation object.
	 *
	 * @return the shortName (type String) of this Curation object.
	 */
	public String getShortName() {
		return shortName;
	}


	/**
	 * Method setShortName sets the shortName of this Curation object.
	 *
	 * @param shortName the shortName of this Curation object.
	 */
	public void setShortName(final String shortName) {
		this.shortName = shortName;
	}

	public boolean hasBeenRun() {
		return curationFile != null;
	}

	/**
	 * gets the long title of the curation.  A long title should be something humanly descriptive.
	 *
	 * @return the long title of the curation
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Method setTitle sets the title of this Curation object.
	 *
	 * @param title the title of this Curation object.
	 */
	public void setTitle(final String title) {
		this.title = title;
	}

	/**
	 * @return Regular expression (Scaffold-supported) describing all decoy accession numbers. If the expression was not specified,
	 *         the default {@link DatabaseAnnotation#DEFAULT_DECOY_REGEX} is used.
	 */
	public String getDecoyRegex() {
		if (decoyRegex == null || decoyRegex.length() == 0) {
			return DatabaseAnnotation.DEFAULT_DECOY_REGEX;
		}
		return decoyRegex;
	}

	/**
	 * @param decoyRegex Regular expression (Scaffold-supported) describing all decoy accession numbers.
	 */
	public void setDecoyRegex(final String decoyRegex) {
		this.decoyRegex = decoyRegex;
	}

	public DatabaseAnnotation getDatabaseAnnotation() {
		return new DatabaseAnnotation(getDecoyRegex());
	}

	/**
	 * @return Information about the resulting FASTA file + important metadata.
	 */
	public FastaFile getFastaFile() {
		return new FastaFile(getShortName(), getTitle(), getCurationFile(), getDatabaseAnnotation());
	}

	/**
	 * Creates a copy of this curation object including the following data - short name - development status - refresh
	 * frequency - expiration date - title - owner email
	 * <p/>
	 * If the copy is not for a refresh but for a new development cycle then only the steps are copied and the other
	 * things will be left blank for the new developer to fill in.
	 * <p/>
	 * Note that if the new one will be used for refresh then it is the callers responsibility to call retire on this
	 * one once the copy has been successfully run.
	 *
	 * @param forRefresh true if you want to copy only for refresh or to make changes
	 * @return a copy of this curator
	 */
	public Curation createCopy(final boolean forRefresh) {
		final Curation copy = new Curation();

		// We want to retain the basics so the user can modify them and stay consistent
		copy.setShortName(shortName);
		copy.setTitle(title);
		copy.notes = notes;
		copy.setDecoyRegex(getDecoyRegex());

		// These fields are set only if the copy is done for refresh (should be identical)
		if (forRefresh) {
			copy.setDeploymentDate(deploymentDate);
			copy.setOwnerEmail(ownerEmail);
			copy.firstRunDate = firstRunDate;
		}

		//for each step we want to create a copy or use original step if this is for a refresh only.
		for (final CurationStep curationStep : getCurationSteps()) {
			copy.addStep(forRefresh ? curationStep : curationStep.createCopy(), -1);
		}

		return copy;
	}

	/**
	 * add a step to a given location in the flow. e.g. 0 = first step, 1 = second step, -1 = last step, -2 = second to
	 * last step
	 *
	 * @param toAdd    the step to add
	 * @param toMoveTo where you want to add the step to
	 */
	public Curation addStep(final CurationStep toAdd, final int toMoveTo) {
		curationSteps.add(translateStepIndex(toMoveTo), toAdd);
		return this;
	}

	/**
	 * Remove all steps from the curation.
	 */
	public void clearSteps() {
		curationSteps.clear();
	}

	/**
	 * takes an index of a step that may be in negative notation and converts it to the equivalent positive notation
	 * Negative notation means that the last step would be at -1 and the second at -2 etc.
	 *
	 * @param step the index that you want to translate
	 * @return the translated index
	 */
	protected int translateStepIndex(final int step) {
		if (step > curationSteps.size()) {
			return curationSteps.size() - 1;
		}
		if (curationSteps.size() == 0) {
			return 0;
		}
		if (step < 0) {
			return curationSteps.size() + step + 1;
		}
		return step;
	}

	/**
	 * gets the step at a particlar position in the index
	 *
	 * @param index the index you want to return the index of
	 * @return the step that you requested
	 */
	public CurationStep getStepByIndex(final int index) {
		return curationSteps.get(translateStepIndex(index));
	}

	/**
	 * gets the number of steps in this curation
	 *
	 * @return the number of steps
	 */
	public int stepCount() {
		return curationSteps.size();
	}

	/**
	 * Method setRunDate sets the runDate of this Curation object.
	 *
	 * @param runDate the runDate of this Curation object.
	 */
	public void setRunDate(final DateTime runDate) {
		this.runDate = runDate;
		if (firstRunDate == null) {
			firstRunDate = runDate;
		}
	}

	public String simpleDescription() {
		final StringBuilder sb = new StringBuilder(EXPECTED_DESCRIPTION_SIZE);
		for (final CurationStep step : curationSteps) {
			if (step.simpleDescription() != null) {
				sb.append(step.simpleDescription()).append(", ");
			}
		}

		if (sb.length() <= 2) {
			return "";
		}

		sb.replace(sb.length() - 2, sb.length(), ""); //strip off the trailing ", "
		return sb.toString();
	}

	/**
	 * Creates a full clone of the curation. Meant to be used only for db migration.
	 */
	public Curation copyFull() {
		final Curation copy = new Curation();

		copy.setCurationFile(curationFile);

		//for each step we want to create a copy or use original step if this is for a refresh only.
		for (final CurationStep curationStep : getCurationSteps()) {
			copy.addStep(curationStep.createCopy(), -1);
		}

		copy.setDeploymentDate(deploymentDate);
		copy.setFirstRunDate(getFirstRunDate());
		copy.setNotes(notes);
		copy.setOwnerEmail(ownerEmail);
		copy.setRunDate(runDate);
		copy.setShortName(shortName);
		copy.setTitle(title);
		copy.setDecoyRegex(getDecoyRegex());

		return copy;
	}

	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Curation)) {
			return false;
		}

		final Curation curation = (Curation) obj;

		if (getCurationFile() != null ? !getCurationFile().equals(curation.getCurationFile()) : curation.getCurationFile() != null) {
			return false;
		}
		if (getDeploymentDate() != null ? !getDeploymentDate().equals(curation.getDeploymentDate()) : curation.getDeploymentDate() != null) {
			return false;
		}
		if (getFirstRunDate() != null ? !getFirstRunDate().equals(curation.getFirstRunDate()) : curation.getFirstRunDate() != null) {
			return false;
		}
		if (getNotes() != null ? !getNotes().equals(curation.getNotes()) : curation.getNotes() != null) {
			return false;
		}
		if (getOwnerEmail() != null ? !getOwnerEmail().equals(curation.getOwnerEmail()) : curation.getOwnerEmail() != null) {
			return false;
		}
		if (getRunDate() != null ? !getRunDate().equals(curation.getRunDate()) : curation.getRunDate() != null) {
			return false;
		}
		if (getShortName() != null ? !getShortName().equals(curation.getShortName()) : curation.getShortName() != null) {
			return false;
		}
		if (getTitle() != null ? !getTitle().equals(curation.getTitle()) : curation.getTitle() != null) {
			return false;
		}
		return getDecoyRegex().equals(curation.getDecoyRegex());
	}

	/**
	 * @param toValidate Short database name.
	 */
	public static void validateShortNameLegalCharacters(final String toValidate) {
		if (!toValidate.matches("^[a-zA-Z0-9:_.\\-()]*$")) {
			throw new MprcException("Database name must not contain anything but a-z A-Z 0-9 : _ . - ( ) (no spaces)");
		} else if (toValidate.length() > SHORTNAME_MAX_LENGTH || toValidate.length() < SHORTNAME_MIN_LENGTH) {
			throw new MprcException("Database name must be between " + SHORTNAME_MIN_LENGTH + " and " + SHORTNAME_MAX_LENGTH + " characters");
		}
	}

	public int hashCode() {
		int result = getNotes() != null ? getNotes().hashCode() : 0;
		result = 31 * result + (getShortName() != null ? getShortName().hashCode() : 0);
		result = 31 * result + (getFirstRunDate() != null ? getFirstRunDate().hashCode() : 0);
		result = 31 * result + (getCurationFile() != null ? getCurationFile().hashCode() : 0);
		result = 31 * result + (getTitle() != null ? getTitle().hashCode() : 0);
		result = 31 * result + (getOwnerEmail() != null ? getOwnerEmail().hashCode() : 0);
		result = 31 * result + (getRunDate() != null ? getRunDate().hashCode() : 0);
		result = 31 * result + (getDeploymentDate() != null ? getDeploymentDate().hashCode() : 0);
		result = 31 * result + getDecoyRegex().hashCode();
		return result;
	}

	public String toString() {
		return "Curation{" +
				"notes='" + getNotes() + '\'' +
				", id=" + getId() +
				", shortName='" + getShortName() + '\'' +
				", firstRunDate=" + getFirstRunDate() +
				", curationFile=" + getCurationFile() +
				", title='" + getTitle() + '\'' +
				", ownerEmail='" + getOwnerEmail() + '\'' +
				", runDate=" + getRunDate() +
				", deploymentDate=" + getDeploymentDate() +
				", decoyRegex=" + getDecoyRegex() +
				'}';
	}
}
