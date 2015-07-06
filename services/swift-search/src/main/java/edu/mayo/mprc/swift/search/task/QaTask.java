package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.worker.WorkPacket;
import edu.mayo.mprc.qa.ExperimentQa;
import edu.mayo.mprc.qa.QaFiles;
import edu.mayo.mprc.qa.QaWorkPacket;
import edu.mayo.mprc.qa.QaWorker;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.workflow.engine.WorkflowEngine;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public final class QaTask extends AsyncTaskBase {
	private List<QaTaskExperiment> experimentList;
	private QaTaskExperiment currentExperiment;
	// Name of the QA report
	private String searchRunName;
	// Folder where to put all the intermediate data
	private File qaReportFolder;
	// Name of the HTML report to be generated
	private File reportFile;
	// Reverse database entry prefix
	private String decoyRegex;

	public static final String QA_SUBDIRECTORY = "qa";

	public QaTask(final WorkflowEngine engine,
	              final DaemonConnection daemonConnection,
	              final DatabaseFileTokenFactory fileTokenFactory,
	              final String searchRunName,
	              final boolean fromScratch) {
		super(engine, daemonConnection, fileTokenFactory, fromScratch);

		this.searchRunName = searchRunName;
		experimentList = new ArrayList<QaTaskExperiment>(1);
		setName("Quality Assurance");
	}

	public void addExperiment(ScaffoldTaskI scaffoldTask) {
		final File scaffoldXmlFile = scaffoldTask.getScaffoldXmlFile();
		final File spectraFile = scaffoldTask.getScaffoldSpectraFile();
		decoyRegex = scaffoldTask.getMainDatabase().getDbToDeploy().getDecoyRegex();
		final QaTaskExperiment e = new QaTaskExperiment(
				getExperimentName(scaffoldXmlFile),
				spectraFile,
				getScaffoldVersion(scaffoldXmlFile));
		for (final QaTaskExperiment existing : experimentList) {
			if (existing.equals(e)) {
				currentExperiment = existing;
				return;
			}
		}
		experimentList.add(e);
		currentExperiment = e;

		if (experimentList.size() == 1) {
			qaReportFolder = getQaSubdirectory(scaffoldXmlFile);
			reportFile = new File(qaReportFolder, "index.html");
			setDescription("QA analysis report " + fileTokenFactory.fileToTaggedDatabaseToken(reportFile));
		}
	}

	/**
	 * @param scaffoldXmlFile Where the Scaffold .xml report is.
	 * @return Where should the QA directory for the particular Scaffold file be.
	 */
	public static File getQaSubdirectory(final File scaffoldXmlFile) {
		return new File(scaffoldXmlFile.getParentFile().getParentFile(), QA_SUBDIRECTORY);
	}

	public void addMgfToRawEntry(final FileProducingTask mgfFile, final File rawFile, final RAWDumpTask rawDumpTask) {
		currentExperiment.addMgfToRawEntry(mgfFile, rawFile, rawDumpTask);
	}

	public void addMgfToMsmsEvalEntry(final FileProducingTask mgfFile, final SpectrumQaTask spectrumQaTask) {
		currentExperiment.addMgfToMsmsEvalEntry(mgfFile, spectrumQaTask);
	}

	public File getQaReportFolder() {
		return qaReportFolder;
	}

	@Override
	public WorkPacket createWorkPacket() {
		// All present sfs files
		final HashSet<File> existingSfsFiles = new HashSet<File>(experimentList.size());
		if (qaReportFolder.exists() && qaReportFolder.isDirectory()) {
			FileUtilities.listFolderContents(qaReportFolder, new SfsFilter(), null, existingSfsFiles);
		}

		boolean outputAlreadyExists = true;
		final List<ExperimentQa> experimentQaList = new ArrayList<ExperimentQa>(experimentList.size());
		for (final QaTaskExperiment experiment : experimentList) {
			final List<QaFiles> inputFilePairs = new ArrayList<QaFiles>();

			for (final Map.Entry<FileProducingTask, QaTaskInputFiles> me : experiment.getMgfToQaMap().entrySet()) {
				final QaTaskInputFiles value = me.getValue();
				final QaFiles files = new QaFiles();
				files.setInputFile(me.getKey().getResultingFile());
				files.setRawInputFile(value.getRawInputFile());
				if (value.getSpectrumQa() != null) {
					files.setMsmsEvalOutputFile(value.getSpectrumQa().getMsmsEvalOutputFile());
				}
				if (value.getRawDump() != null) {

					files.setRawInfoFile(value.getRawDump().getRawInfoFile());
					files.setRawSpectraFile(value.getRawDump().getRawSpectraFile());
					files.setChromatogramFile(value.getRawDump().getChromatogramFile());
					files.setUvDataFile(value.getRawDump().getUvDataFile());
				}
				for (final EngineSearchTask engineSearchTask : value.getAdditionalSearches()) {
					files.addAdditionalSearchResult(engineSearchTask.getSearchEngine().getCode(), engineSearchTask.getOutputFile());
				}
				inputFilePairs.add(files);
				final File sfsFile = QaWorker.getSfsFileName(qaReportFolder,
						QaWorker.getAnalysisName(files.getInputFile()));

				existingSfsFiles.remove(sfsFile);

				if (outputAlreadyExists) {
					// If the sfs file that we are about to create does not exist or is too old, we will need to make a new one
					if (!sfsFile.exists() || sfsFile.length() == 0 || sfsFile.lastModified() < files.getNewestModificationDate()) {
						outputAlreadyExists = false;
					}
				}
			}
			final ExperimentQa experimentQa = new ExperimentQa(experiment.getName(), experiment.getSpectraFile(), inputFilePairs, experiment.getScaffoldVersion());
			experimentQaList.add(experimentQa);
		}

		// The folder contained sfs files that are not accounted for. We need to redo the QA report generation without the extra files.
		if (existingSfsFiles.size() > 0) {
			outputAlreadyExists = false;
		}

		if (outputAlreadyExists) {
			return null;
		}

		return new QaWorkPacket(searchRunName, experimentQaList, qaReportFolder, reportFile, decoyRegex, isFromScratch());
	}

	@Override
	public void onSuccess() {
		//Do nothing
	}

	@Override
	public void onProgress(final ProgressInfo progressInfo) {
		//Do nothing
	}

	private static String getExperimentName(final File scaffoldXmlFile) {
		return FileUtilities.getFileNameWithoutExtension(scaffoldXmlFile);
	}

	private static String getScaffoldVersion(final File scaffoldXmlFile) {
		return scaffoldXmlFile.getParentFile().getName().equals("scaffold3") ? "3" : "2";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof QaTask)) return false;

		QaTask qaTask = (QaTask) o;

		if (reportFile != null ? !reportFile.equals(qaTask.reportFile) : qaTask.reportFile != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return reportFile != null ? reportFile.hashCode() : 0;
	}

	private static class SfsFilter implements FilenameFilter {
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(".sfs");
		}
	}
}
