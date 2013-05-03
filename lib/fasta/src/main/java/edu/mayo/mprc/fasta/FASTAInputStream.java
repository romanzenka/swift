package edu.mayo.mprc.fasta;

import com.google.common.base.Charsets;
import com.google.common.io.CountingInputStream;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.GZipUtilities;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * An input stream to handle FASTA data files.  It will read in the files and allow access to each header and sequence in
 * the file.
 * <p/>
 * WARNING: This class requires you to call {@link #beforeFirst()} before you start actually reading the data. This is
 * not very intuitive. If you fail to do so, the class will quietly not load anything.
 *
 * @author Eric J. Winter Date: Apr 10, 2007 Time: 9:00:59 AM
 */
public final class FASTAInputStream implements DBInputStream {
	private static final char FASTA_HEADER = '>';
	public static final long AVERAGE_FASTA_RECORD = 500L /*Avg protein sequence size in bytes*/ + 100L /* AVG header size in bytes */;

	/**
	 * The file that we are using as input
	 */
	private File fastaFile;

	/**
	 * A reader to access the above file
	 */
	private BufferedReader reader;

	/**
	 * Counting input stream to know how much we read so far.
	 */
	private CountingInputStream countingInputStream;

	/**
	 * How much data is there total in the FASTA file.
	 */
	private float totalBytesToRead;

	/**
	 * the header of the sequence at the current location
	 */
	private String currentHeader;

	/**
	 * the sequence that is at the current position in the file
	 */
	private String currentSequence;

	/**
	 * the header that comes next and will become the currentHeader when next is called
	 */
	private String nextHeader;

	private static final int MAX_ACCNUM_LENGTH = 34;
	public static final int MAX_HEADER_LENGTH = 200;
	private static final String VALID_CHARACTERS = "a-z A-Z 0-9 _\\-.*+|=";
	private static final Pattern VALID_ACCNUM = Pattern.compile("[a-zA-Z0-9_\\-.*+|=]+");

	private static final Logger LOGGER = Logger.getLogger(FASTAInputStream.class);

	/**
	 * create a new stream with the given file
	 *
	 * @param file the file that you want to create an input stream of
	 *             ¬
	 */
	public FASTAInputStream(final File file) {
		this.fastaFile = file;
	}

	private void reopenReader() throws IOException {
		totalBytesToRead = fastaFile.length();
		countingInputStream = new CountingInputStream(new FileInputStream(fastaFile));
		if (GZipUtilities.isGZipped(fastaFile)) {
			this.reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(countingInputStream), Charsets.ISO_8859_1));
		} else {
			this.reader = new BufferedReader(new InputStreamReader(countingInputStream, Charsets.ISO_8859_1));
		}
	}

	/**
	 * goes to the first sequence in the sequence database file so that the next call to getHeader() will return the
	 * first header in the file.
	 */
	public void beforeFirst() {
		try {
			FileUtilities.closeQuietly(this.reader);
			reopenReader();
			this.nextHeader = this.reader.readLine();
		} catch (Exception e) {
			throw new MprcException("Cannot open FASTA file [" + this.fastaFile.getAbsolutePath() + "]", e);
		}
	}

	/**
	 * Advances to the next sequence in the database file and returns true unless there is no more sequences in the file
	 * and then false is returned.
	 *
	 * @return false if we are already at the end of the file else true
	 */
	public boolean gotoNextSequence() {
		if (reader == null) {
			throw new MprcException("FASTA stream not initalized properly. Call beforeFirst() before reading first sequence");
		}
		//we should have been left after reading a header because that is how we detect
		//the end of the next sequence
		if (nextHeader == null) {
			return false;
		}
		setHeader(nextHeader);

		//read in lines until we reach the next header
		final StringBuilder sequenceBuilder = new StringBuilder();
		String nextLine = null;
		try {
			nextLine = this.reader.readLine();
		} catch (IOException e) {
			LOGGER.warn(e);
		}
		//if the next line is not a header or an end of line then append it to the sequence
		while (isSequence(nextLine)) {
			sequenceBuilder.append(cleanupSequence(nextLine));
			try {
				//read in the next line
				nextLine = this.reader.readLine();
			} catch (IOException e) {
				LOGGER.warn(e);
			}
		}

		while (nextLine != null && !isHeader(nextLine)) {
			try {
				nextLine = this.reader.readLine();
			} catch (IOException e) {
				LOGGER.warn(e);
			}
		}
		this.nextHeader = nextLine;

		//set the current sequence to the concatenation of all strings
		// If the sequence ends with an * signalizing end codon, quietly drop it
		if (sequenceBuilder.length() > 0 && sequenceBuilder.charAt(sequenceBuilder.length() - 1) == '*') {
			currentSequence = sequenceBuilder.substring(0, sequenceBuilder.length() - 1);
		} else {
			currentSequence = sequenceBuilder.toString();
		}

		//return true since we will have a next header
		return true;
	}

	public float percentRead() {
		return countingInputStream.getCount() / totalBytesToRead;
	}

	private String cleanupSequence(final String nextLine) {
		return nextLine.trim().toUpperCase(Locale.US);
	}

	/**
	 * Determines if a String is part of a sequence (lack of '>' character while containing some characters
	 *
	 * @param potentialSequence the string that is suspected to be a header
	 * @return true if the string is a header or if it is null (meaning end of file)
	 */
	private static boolean isSequence(final String potentialSequence) {
		return !(potentialSequence == null || potentialSequence.length() == 0 || potentialSequence.charAt(0) == FASTA_HEADER);
	}

	private static boolean isHeader(final String potentialHeader) {
		return !(potentialHeader == null || potentialHeader.length() == 0) && potentialHeader.charAt(0) == FASTA_HEADER;
	}

	/**
	 * gets the header of the current sequence in the file.
	 *
	 * @return the current sequence's header
	 */
	public String getHeader() {
		return this.currentHeader;
	}

	private void setHeader(final String header) {
		currentHeader = header;
	}

	/**
	 * gets the sequence portion of the curent sequence in the file
	 *
	 * @return the current sequence
	 */
	public String getSequence() {
		return this.currentSequence;
	}

	/**
	 * performs any cleaning up that may be necessary.  Always call when you are done.
	 */
	public void close() {
		FileUtilities.closeQuietly(this.reader);
	}

	/**
	 * goes through each header in a fasta file and checks to make sure it is a valid fasta header.  If any problems
	 * are encountered or a header does not check out then false is returned.
	 * <p/>
	 * A valid fasta header must use an unique accession number for each sequence in the file.
	 *
	 * @param toCheck the file you want to see is a valid FASTA file
	 * @return null if the file is a valid fasta file. Error description otherwise.
	 */
	public static String isFASTAFileValid(final File toCheck, final boolean checkHeaderLength) {
		final HashSet<String> accessionNumbers = new HashSet<String>((int) (toCheck.length() / AVERAGE_FASTA_RECORD));
		DBInputStream in = null;
		try {
			in = new FASTAInputStream(toCheck);
			int sequenceCount = 0;
			in.beforeFirst();
			while (in.gotoNextSequence()) {
				final String header = in.getHeader();
				if (isHeader(header)) {
					sequenceCount++;
					final String accNum = getAccNum(header);
					final String error = checkHeader(header, accNum, checkHeaderLength);
					if (error != null) {
						return error;
					}
					if (!accessionNumbers.add(accNum)) {
						return "Duplicate accession number: [" + accNum + "]";
					}
				} else {
					return "Invalid header [" + header + "]";
				}
			}
			if (sequenceCount == 0) {
				return "No sequences present";
			}
		} catch (Exception e) {
			// SWALLOWED: Convert into error message
			return "Fasta file [" + toCheck.getAbsolutePath() + "] is not valid: " + MprcException.getDetailedMessage(e);
		} finally {
			FileUtilities.closeQuietly(in);
		}
		return null;
	}

	/**
	 * Get accession number part of the FASTA sequence header.
	 *
	 * @param header Header to process (with >)
	 * @return The accession number.
	 */
	static String getAccNum(final String header) {
		final int spacePos = header.indexOf(' ');
		return spacePos >= 0 ? header.substring(1, spacePos) : header.substring(1);
	}

	static String checkHeader(final String header, final String accNum, final boolean checkHeaderLength) {
		String error = null;
		if (accNum.length() > MAX_ACCNUM_LENGTH) {
			error = "Accession number too long: [" + accNum + "]. Length: " + accNum.length() + ", max: " + MAX_ACCNUM_LENGTH;
		}
		if (checkHeaderLength && header != null && header.length() - 1 > MAX_HEADER_LENGTH) {
			error = "Sequence header for accession number: [" + accNum + "] too long: " + header.length() + ", max: " + MAX_HEADER_LENGTH;
		}
		if (!VALID_ACCNUM.matcher(accNum).matches()) {
			error = "Invalid accession number: [" + accNum + "], allowed characters: " + VALID_CHARACTERS;
		}
		return error;
	}
}
