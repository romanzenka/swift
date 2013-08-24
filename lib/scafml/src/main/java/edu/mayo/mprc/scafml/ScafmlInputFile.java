package edu.mayo.mprc.scafml;

import edu.mayo.mprc.daemon.files.FileHolder;

import java.io.File;

public final class ScafmlInputFile extends FileHolder {
	private static final long serialVersionUID = 8170940307629986726L;
	private String _sID;
	private File file;

	public ScafmlInputFile() {
	}

	public void setID(final String iD) {
		_sID = iD;
	}

	public String getID() {
		return _sID;
	}

	public File getFile() {
		return file;
	}

	public void setFile(final File file) {
		this.file = file;
	}

	public void appendToDocument(final StringBuilder result, final String indent) {
		/**
		 * this needs to be done for each search tool involved
		 * right now assuming sequest, mascot, xtandem
		 */
		result.append(indent)
				.append("<" + "InputFile" + ">")
				.append(file.getAbsoluteFile())
				.append("</" + "InputFile" + ">\n");
	}
}
