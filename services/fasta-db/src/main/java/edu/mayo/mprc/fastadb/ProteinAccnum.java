package edu.mayo.mprc.fastadb;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.PersistableBase;

/**
 * @author Roman Zenka
 */
public final class ProteinAccnum extends PersistableBase {
	private String accnum;
	private static final int MAX_ACCNUM_LENGTH = 50;

	public ProteinAccnum() {
		this("");
	}

	public ProteinAccnum(final String accnum) {
		setAccnum(accnum);
	}

	public String getAccnum() {
		return accnum;
	}

	void setAccnum(final String accnum) {
		if (accnum.length() > MAX_ACCNUM_LENGTH) {
			throw new MprcException("The accession number [" + accnum + "] is too long: " + accnum.length() + ", maximum allowed: " + MAX_ACCNUM_LENGTH);
		}
		this.accnum = accnum;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ProteinAccnum)) {
			return false;
		}

		ProteinAccnum that = (ProteinAccnum) o;

		if (!accnum.equals(that.accnum)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return accnum.hashCode();
	}
}
