package edu.mayo.mprc.fasta;

import java.io.Closeable;

/**
 * An abstraction around the file input around the sequence database files.
 *
 * @author Eric J. Winter Date: Apr 6, 2007 Time: 10:25:53 AM
 */
public interface DBInputStream extends Closeable {
	/**
	 * goes to the first sequence in the sequence database file so that the next call to getHeader() will return the
	 * first header in the file.
	 */
	void beforeFirst();

	/**
	 * Advances to the next sequence in the database file and returns true unless there is no more sequences in the file
	 * and then false is returned.
	 *
	 * @return false if there is no next sequence in the file else true
	 */
	boolean gotoNextSequence();

	/**
	 * gets the header of the current sequence in the file.
	 *
	 * @return the current sequence's header
	 */
	String getHeader();

	/**
	 * gets the sequence portion of the curent sequence in the file
	 *
	 * @return the current sequence
	 */
	String getSequence();

	/**
	 * Closes the stream.
	 */
	@Override
	void close();
}
