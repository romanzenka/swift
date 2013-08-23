package edu.mayo.mprc.swift.webservice.diff;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import edu.mayo.mprc.swift.dbmapping.FileSearch;

/**
 * @author Roman Zenka
 */
@XStreamAlias("inputFile")
public final class InputFile {
	private final int id;
	private final String path;
	private final String name;

	public InputFile(final int id, final String path, final String name) {
		this.id = id;
		this.path = path;
		this.name = name;
	}

	public InputFile(final int id, final FileSearch file) {
		this(id, file.getInputFile().getPath(), file.getInputFile().getName());
	}

	public int getId() {
		return id;
	}

	public String getPath() {
		return path;
	}

	public String getName() {
		return name;
	}
}
