package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.workflow.engine.Task;

import java.io.File;

/**
 * Any task that produces a a file.
 */
interface FileProducingTask extends Task {
	File getResultingFile();
}
