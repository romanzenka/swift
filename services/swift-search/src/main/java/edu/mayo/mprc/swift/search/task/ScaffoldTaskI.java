package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.workflow.engine.Task;

import java.io.File;

public interface ScaffoldTaskI extends Task, ScaffoldSpectrumExportProducer {
	String getScaffoldVersion();

	File getResultingFile();

	File getScaffoldXmlFile();

	void addInput(FileSearch fileSearch, FileProducingTask search);

	/**
	 * Which input file/search parameters tuple gets outputs from which engine search.
	 */
	void addDatabase(String id, DatabaseDeployment dbDeployment);

	/**
	 * @return The first database that was used to produce the output. Typically there will be just one.
	 */
	DatabaseDeployment getMainDatabase();
}
