package edu.mayo.mprc.dbcurator.model.curationsteps;

import edu.mayo.mprc.dbcurator.model.CurationExecutor;
import edu.mayo.mprc.dbcurator.model.CurationStatus;
import edu.mayo.mprc.dbcurator.model.CurationStep;
import edu.mayo.mprc.dbcurator.model.StepValidation;
import edu.mayo.mprc.dbcurator.model.persistence.CurationDao;
import edu.mayo.mprc.fasta.DBInputStream;
import edu.mayo.mprc.fasta.DBOutputStream;
import edu.mayo.mprc.fasta.filter.MatchMode;
import edu.mayo.mprc.fasta.filter.RegExTextFilter;
import edu.mayo.mprc.fasta.filter.SimpleStringTextFilter;
import edu.mayo.mprc.fasta.filter.TextFilter;

import java.io.IOException;
import java.util.Locale;

/**
 * A CurationStep that takes filter string to search through the input database and only if the filter conditions are
 * met by the sequence header does the sequence get copied to the output string.  There are different types of filters
 * as you can choose to do either a simple text filter or a regular expression filter.  There are also different
 * criteria such as if you want to match any, all, or none of the conditions specified in the criteria string.
 *
 * @author Eric J. Winter Date: Apr 9, 2007 Time: 9:30:27 AM
 */

public class HeaderFilterStep implements CurationStep {
	private static final long serialVersionUID = 20071220L;


	/**
	 * the persistence identifier of this HeaderFilterStep
	 * PERSISTENT
	 */
	private Integer id = null;


	/**
	 * the match mode of this filter (defaults to ANY)
	 * <p/>
	 * PERSISTENT
	 */
	private MatchMode matchMode = MatchMode.ANY;

	/**
	 * the text mode of this filter (defaults to RAW)
	 * <p/>
	 * PERSISTENT
	 */
	private TextMode textMode = TextMode.SIMPLE;

	/**
	 * the StepValidation that was created the last time that performStep() was run.
	 * <p/>
	 * transient
	 */
	private transient StepValidation runValidation;


	/**
	 * The string that the user entered and should be searched
	 * <p/>
	 * PERSISTENT
	 */
	private String criteriaString = "";

	/**
	 * Creates a filter with the passed in string, should be comma seperated list of search criteria
	 */
	public HeaderFilterStep() {
		super();
	}

	/**
	 * perfom the step on a given local database.  If the step could not be performed then a CurationStepException is
	 * thrown.  This indicates that the PostValidation will be unsuccessful and will contain a message indicating why it
	 * was unsuccesfull.
	 * <p/>
	 * There are obviously a wide variety of things that could go wrong with a call to perform step.
	 *
	 * @param exec the executor that we are working for and will need to query to get information from
	 * @return the post validation.
	 */
	@Override
	public StepValidation performStep(final CurationExecutor exec) {

		//create a new validation object for this run
		this.runValidation = new StepValidation();

		final DBInputStream in = exec.getCurrentInStream(); //the file we should be reading from (may be null)
		final DBOutputStream out = exec.getCurrentOutStream(); // the file we should be writing to
		final CurationStatus status = exec.getStatusObject(); //the status objec we want to update
		final TextFilter filter = this.getAppropriateTextFilter();
		final float sequencesToFilter = status.getLastStepSequenceCount();
		int numberFilteredSoFar = 0;

		//iterate through each sequence in the database and if the sequence matches
		//the filter then copy the sequence to the output database file else don't
		//effectively removing the sequence from the database.
		in.beforeFirst();
		while (in.gotoNextSequence()) {
			status.setCurrentStepProgress(Math.round(++numberFilteredSoFar * 100f / sequencesToFilter));
			if (filter.matches(in.getHeader())) {
				try {
					out.appendSequence(in.getHeader(), in.getSequence());
				} catch (IOException e) {
					this.runValidation.addMessageAndException("Error filtering steps", e);
				}
			}
		}
		this.runValidation.setCompletionCount(out.getSequenceCount());
		this.setLastRunCompletionCount(out.getSequenceCount());
		return this.runValidation;
	}

	/**
	 * Call this method if you want to see if the step is ready to be run and if any issues have been predicted.  NOTE:
	 * succesfull prevalidation can not guarentee<sp> successful processing.
	 *
	 * @param curationDao
	 * @return the @see StepValidation to interrogate for issues
	 */
	@Override
	public StepValidation preValidate(final CurationDao curationDao) {
		//the validation we will return
		final StepValidation preValidation = new StepValidation();

		//create a test filter from the current set of properties
		final TextFilter toTest = this.getAppropriateTextFilter();

		//ask the TextFilter to check itself for validity
		final String testResults = toTest.testCriteria();

		//if the criteria is valid then just return a successful step validation
		//if not valid then we want to report the problems with the filter expression
		if (!TextFilter.VALID.equals(testResults)) {
			preValidation.addMessage(testResults);
		}

		return preValidation;
	}


	/**
	 * Creates a copy of this step.  Only persistent properties are included in the copy.  Ids are not so they will
	 * be a seperate entity in persistent store
	 *
	 * @return a cropy of this step
	 */
	@Override
	public CurationStep createCopy() {
		final HeaderFilterStep copy = new HeaderFilterStep();
		copy.matchMode = this.matchMode;
		copy.textMode = this.textMode;
		copy.criteriaString = this.criteriaString;
		return copy;
	}


	/**
	 * Gets a TextFilter instance that is appropriate for the currently set properties of this HeaderFilterStep
	 * basically returns  a regex or simple text filter but others may be added.
	 *
	 * @return an appropriate TextFilter object
	 */
	protected TextFilter getAppropriateTextFilter() {
		final TextFilter toCreate;
		if (this.textMode == TextMode.SIMPLE) {
			toCreate = new SimpleStringTextFilter(this.criteriaString);
		} else if (this.textMode == TextMode.REG_EX) {
			toCreate = new RegExTextFilter(this.criteriaString);
		} else {
			return null;
		}
		toCreate.setMatchMode(this.matchMode);
		return toCreate;
	}

	/**
	 * the mode such as all, any, none
	 *
	 * @return the currently set match mode
	 */
	public MatchMode getMatchMode() {
		return matchMode;
	}

	/**
	 * sets the match mode
	 *
	 * @param matchMode the mode to use
	 * @see MatchMode
	 */
	public void setMatchMode(final MatchMode matchMode) {
		this.matchMode = matchMode;
	}

	/**
	 * gets the text mode such as simple or regular expression
	 *
	 * @return the text mode being used
	 */
	public TextMode getTextMode() {
		return textMode;
	}

	/**
	 * sets the text mode that should be used such as simple or regular expression
	 *
	 * @param textMode the text mode to use
	 */
	public void setTextMode(final TextMode textMode) {
		this.textMode = textMode;
	}

	/**
	 * get the string that should be used by the filter
	 *
	 * @return the criteria taht we are using
	 */
	public String getCriteriaString() {
		return criteriaString;
	}

	/**
	 * set the string that should be used as a filter
	 *
	 * @param criteriaString the current criteria
	 */
	public void setCriteriaString(final String criteriaString) {
		this.criteriaString = criteriaString;
	}

	@Override
	public Integer getId() {
		return id;
	}

	@Override
	public void setId(final Integer id) {
		this.id = id;
	}

	/**
	 * the number of sequences that were present in the curation after this step was last run
	 */
	private Integer lastRunCompletionCount = null;

	@Override
	public Integer getLastRunCompletionCount() {
		return this.lastRunCompletionCount;
	}

	@Override
	public void setLastRunCompletionCount(final Integer count) {
		this.lastRunCompletionCount = count;
	}


	@Override
	public String simpleDescription() {
		return "filtered " + this.getMatchMode().toString().toLowerCase(Locale.ENGLISH) + "\"" + getCriteriaString() + "\"";
	}
}
