package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.daemon.DaemonConnection;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.files.FileTokenFactory;
import edu.mayo.mprc.qa.ExperimentQa;
import edu.mayo.mprc.qa.MgfQaFiles;
import edu.mayo.mprc.qa.QaWorkPacket;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.progress.ProgressInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class QaTask extends AsyncTaskBase {
	private List<QaTaskExperiment> experimentList;
	private QaTaskExperiment currentExperiment;
	// Folder where to put all the intermediate data
	private File qaReportFolder;
	// Name of the HTML report to be generated
	private File reportFile;

	public static final String QA_SUBDIRECTORY = "qa";

	public QaTask(final DaemonConnection daemonConnection, final FileTokenFactory fileTokenFactory, final boolean fromScratch) {
		super(daemonConnection, fileTokenFactory, fromScratch);

		experimentList = new ArrayList<QaTaskExperiment>(1);
		setName("Quality Assurance");
	}

	public void addExperiment(final File scaffoldXmlFile, final File spectraFile) {
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
			this.qaReportFolder = getQaSubdirectory(scaffoldXmlFile);
			this.reportFile = new File(qaReportFolder, "index.html");
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

	public void addMgfToAdditionalSearchEngineEntry(final FileProducingTask mgfFile, final EngineSearchTask engineSearchTask) {
		currentExperiment.addAdditionalSearchEntry(mgfFile, engineSearchTask);
	}

	public File getQaReportFolder() {
		return qaReportFolder;
	}

	@Override
	public WorkPacket createWorkPacket() {
		if (!isFromScratch() && reportFile.exists() && reportFile.isFile()) {
			return null;
		}

		final List<ExperimentQa> experimentQaList = new ArrayList<ExperimentQa>(experimentList.size());
		for (final QaTaskExperiment experiment : experimentList) {
			final List<MgfQaFiles> mgfInputFilePairs = new ArrayList<MgfQaFiles>();

			for (final Map.Entry<FileProducingTask, QaTaskInputFiles> me : experiment.getMgfToQaMap().entrySet()) {
				final QaTaskInputFiles value = me.getValue();
				final MgfQaFiles files = new MgfQaFiles();
				files.setMgfFile(me.getKey().getResultingFile());
				files.setRawInputFile(value.getRawInputFile());
				if (value.getSpectrumQa() != null) {
					files.setMsmsEvalOutputFile(value.getSpectrumQa().getMsmsEvalOutputFile());
				}
				if (value.getRawDump() != null) {

					files.setRawInfoFile(value.getRawDump().getRawInfoFile());
					files.setRawSpectraFile(value.getRawDump().getRawSpectraFile());
					files.setChromatogramFile(value.getRawDump().getChromatogramFile());
				}
				for (final EngineSearchTask engineSearchTask : value.getAdditionalSearches()) {
					files.addAdditionalSearchResult(engineSearchTask.getSearchEngine().getCode(), engineSearchTask.getOutputFile());
				}
				mgfInputFilePairs.add(files);
			}
			final ExperimentQa experimentQa = new ExperimentQa(experiment.getName(), experiment.getSpectraFile(), mgfInputFilePairs, experiment.getScaffoldVersion());
			experimentQaList.add(experimentQa);
		}

		return new QaWorkPacket(experimentQaList, qaReportFolder, reportFile, getFullId(), isFromScratch());
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

}
