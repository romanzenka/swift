package edu.mayo.mprc.dbcurator.model.curationsteps;

import com.google.common.base.Objects;
import edu.mayo.mprc.dbcurator.model.*;
import edu.mayo.mprc.fasta.DBInputStream;
import edu.mayo.mprc.fasta.DBOutputStream;
import edu.mayo.mprc.fasta.filter.ReversalStringManipulator;
import edu.mayo.mprc.fasta.filter.ScrambleStringManipulator;
import edu.mayo.mprc.fasta.filter.StringManipulator;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A CurationStep that takes a CurationExecutor and randomizes the sequence.  If isOverwriteMode() returns true that
 * means that we are set to remove the original sequence otherwise both the original and randomized sequence will be
 * included.
 *
 * @author Eric J. Winter Date: Apr 10, 2007 Time: 12:03:54 PM
 */
public class MakeDecoyStep implements CurationStep {
	private static final long serialVersionUID = 20071220L;

	/**
	 * we want a reversal to be performed so the output sequence will be the reverse of the input sequence
	 */
	public static final int REVERSAL_MANIPULATOR = 1;

	/**
	 * we want the characters to be randomized within each sequence
	 */
	public static final int SCRAMBLE_MANIPULATOR = 2;

	// Helps inserting Reversed_ and (Reversed) in the header.
	private static final Pattern HEADER_TRANSFORM = Pattern.compile("^>\\s*(\\S+\\s*)(.*)$");

	private static final float PERCENT = 100.0f;

	/**
	 * whether this object will retain the original sequence or remove the original sequence
	 */
	private boolean overwriteMode = true;

	/**
	 * the string manipulator that is used.  This is not persisted but is created based on the {@link #manipulatorType}
	 */
	private transient StringManipulator manipulator;

	/**
	 * the manipulator type to associate with this object.  This is persistent and indicates which type of step this is
	 */
	private int manipulatorType;

	/**
	 * null Ctor defaults to Ovewrite Mode (true)
	 */
	public MakeDecoyStep() {

	}

	/**
	 * For testing only. Fast creation of a step.
	 */
	public MakeDecoyStep(final boolean overwriteMode, final int manipulatorType, final Integer lastRunCompletionCount) {
		this.overwriteMode = overwriteMode;
		this.manipulatorType = manipulatorType;
		this.lastRunCompletionCount = lastRunCompletionCount;
	}

	/**
	 * determines if we are in overwrite mode or in append mode. Append Mode (false) indicates that the original
	 * sequence will also be retained Overwrite Mode (true) indicates that the original sequence will be removed.
	 *
	 * @return true if we will effectively erase the original sequence
	 */
	public boolean isOverwriteMode() {
		return overwriteMode;
	}

	/**
	 * gets the appropriate StringManipulator based on the set manipulatorType
	 *
	 * @return
	 */
	private StringManipulator getManipulator() {
		if (manipulator == null) {
			if (manipulatorType == REVERSAL_MANIPULATOR) {
				manipulator = new ReversalStringManipulator();
			} else if (manipulatorType == SCRAMBLE_MANIPULATOR) {
				manipulator = new ScrambleStringManipulator();
			}
		}
		return manipulator;
	}

	/**
	 * @return the enumerated type of manipulator that we are set to use (see this classes *_MANIPULATOR enumerations)
	 */
	public int getManipulatorType() {
		return manipulatorType;
	}

	/**
	 * the enumerated type of manipulator we are set to use (see this classes *_MANIPULATOR enumerations)
	 *
	 * @param type
	 */
	public void setManipulatorType(final int type) {
		manipulatorType = type;
	}

	/**
	 * set the mode you want
	 *
	 * @param mode true if you want to be in overwrite mode else false
	 */
	public void setOverwriteMode(final boolean mode) {
		overwriteMode = mode;
	}

	/**
	 * perform the step on a given local database.  If the step could not be performed then a CurationStepException is
	 * thrown.  This indicates that the PostValidation will be unsuccessful and will contain a message indicating why it
	 * was unsuccessful.
	 * <p/>
	 * There are obviously a wide variety of things that could go wrong with a call to perform step.
	 *
	 * @param exec the CurationExecutor that we are performing this step for
	 * @return the post validation.
	 */
	@Override
	public StepValidation performStep(final CurationExecutor exec) {
		return performStep(exec.getCurrentInStream(), exec.getCurrentOutStream(), exec.getStatusObject(), exec.getCuration().getDecoyRegex());
	}

	/**
	 * Actual implementation of the performStep method (for easy testability)
	 *
	 * @param in     Input stream.
	 * @param out    Output stream.
	 * @param status The progress is updated here, we also take the amount of sequences from here.
	 * @return Information about how the step performed.
	 */
	StepValidation performStep(final DBInputStream in, final DBOutputStream out, final CurationStatus status, final String decoyRegex) {
		//make sure we meet at least the pre validation criteria, this will also make sure our manipulator is set
		/*
	  the validation that was created the last time this step object was run.  this will be null if the step has not been run
	 */
		final StepValidation lastRunValidation = preValidate(null);
		if (!lastRunValidation.isOK()) {
			return lastRunValidation;
		}

		//the the number of sequences we need to produce
		final float numberOfSequences = (float) (overwriteMode ? status.getLastStepSequenceCount() : 2 * status.getLastStepSequenceCount());
		try {
			//if we want to append the sequences we need to first write out he original sequences
			int currentSequence = 0;
			if (!overwriteMode) {
				in.beforeFirst();

				while (in.gotoNextSequence()) {
					currentSequence++;
					status.setCurrentStepProgress(PERCENT * (float) currentSequence / numberOfSequences);
					out.appendSequence(
							in.getHeader(),
							in.getSequence()
					);
				}
			}
			in.beforeFirst();
			while (in.gotoNextSequence()) {
				currentSequence++;
				status.setCurrentStepProgress(PERCENT * (float) currentSequence / numberOfSequences);

				out.appendSequence(
						modifyHeader(in.getHeader(), decoyRegex), //the modified header
						manipulator.manipulateString(in.getSequence()) //the manipulated sequence
				);
			}
			lastRunValidation.setCompletionCount(out.getSequenceCount());
			setLastRunCompletionCount(out.getSequenceCount());
		} catch (final IOException e) {
			lastRunValidation.addMessageAndException("Error in performing database IO", e);
		} catch (final Exception e) {
			lastRunValidation.addMessageAndException(e.getMessage(), e);
		}
		return lastRunValidation;

	}

	/**
	 * modify the header so that a description of the modification precedes the original header
	 *
	 * @param header      Header to modify, e.g. {@code >WHATEVER Whatever description}
	 * @param description Description of the change, e.g. {@code Reversed}
	 * @return Modified header {@code >Reversed_WHATEVER (Reversed) Whatever description}
	 */
	static String modifyHeader(final String header, final String description) {
		final String modifiedHeader;
		final Matcher matcher = HEADER_TRANSFORM.matcher(header);
		if (matcher.matches()) {
			String name = description;
			if (description.endsWith("_")) {
				name = description.substring(0, description.length() - 1);
			}

			modifiedHeader = ">" + description + matcher.group(1) + (!matcher.group(2).isEmpty() ? ("("
					+ name + ") " + matcher.group(2)) : "");
		} else {
			modifiedHeader = header;
		}
		return modifiedHeader;
	}

	/**
	 * Call this method if you want to see if the step is ready to be run and if any issues have been predicted.  NOTE:
	 * succesfull prevalidation can not guarentee<sp> successful processing.
	 *
	 * @param curationDao Data access.
	 * @return the @see StepValidation to interrogate for issues
	 */
	@Override
	public StepValidation preValidate(final CurationDao curationDao) {
		final StepValidation prevalidation = new StepValidation();

		//make sure we have a valid type set
		if (getManipulator() == null) {
			prevalidation.addMessage("Invalid manipulator selected");
		}

		return prevalidation;
	}

	/**
	 * Creates a copy of this step.  Only persistent properties are included in the copy.  The id is not included since
	 * this would make it identical in the database.
	 *
	 * @return a cropy of this step
	 */
	@Override
	public CurationStep createCopy() {
		final MakeDecoyStep copy = new MakeDecoyStep();
		copy.setOverwriteMode(overwriteMode);
		copy.setManipulatorType(manipulatorType);
		return copy;
	}

	/**
	 * the number of sequences that were present in the curation after this step was last run
	 */
	private Integer lastRunCompletionCount = null;

	@Override
	public Integer getLastRunCompletionCount() {
		return lastRunCompletionCount;
	}

	@Override
	public void setLastRunCompletionCount(final Integer count) {
		lastRunCompletionCount = count;
	}

	@Override
	public String simpleDescription() {
		return getManipulator().getDescription();
	}

    @Override
    public String getStepTypeName() {
        return "make_decoy";
    }

	@Override
	public int hashCode() {
		return Objects.hashCode(overwriteMode, manipulatorType, lastRunCompletionCount);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final MakeDecoyStep other = (MakeDecoyStep) obj;
		return Objects.equal(this.overwriteMode, other.overwriteMode) && Objects.equal(this.manipulatorType, other.manipulatorType) && Objects.equal(this.lastRunCompletionCount, other.lastRunCompletionCount);
	}
}
