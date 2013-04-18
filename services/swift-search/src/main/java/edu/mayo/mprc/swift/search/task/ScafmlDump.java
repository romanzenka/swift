package edu.mayo.mprc.swift.search.task;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.scafml.*;
import edu.mayo.mprc.swift.dbmapping.FileSearch;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.swift.params2.ScaffoldSettings;
import edu.mayo.mprc.swift.params2.StarredProteins;

import java.io.File;
import java.util.*;

/**
 * Utility class for dumping Scafml from swift search definition.
 */
class ScafmlDump {

	private ScafmlDump() {
	}

	/**
	 * Write the experiment information as a .scafml file directly parseable by Scaffold.
	 */
	public static ScafmlScaffold dumpScafmlFile(final String experiment, final SwiftSearchDefinition definition, final LinkedHashMap<FileSearch, InputFileSearches> inputs, final File workFolder, final SearchResults searchResults, final Map<String, File> deployedDatabases) {
		if (experiment == null) {
			throw new DaemonException("Experiment must not be null");
		}
		if (workFolder == null) {
			throw new DaemonException("Work folder must not be null");
		}
		if (searchResults == null) {
			throw new DaemonException("Search results must not be null");
		}
		if (deployedDatabases == null) {
			throw new DaemonException("Deployed databases must not be null");
		}

		final ScafmlScaffold sc = new ScafmlScaffold();
		final ScafmlExperiment se = new ScafmlExperiment(experiment);
		sc.setExperiment(se);

		final Map<String, String> path2db = new HashMap<String, String>();
		int dbIdNum = 0;

		// Get a list of all biological samples and their corresponding files in proper order
		final LinkedHashMap<String, List<FileSearch>> biologicalSamples = new LinkedHashMap<String, List<FileSearch>>();
		for (final FileSearch fileSearch : inputs.keySet()) {
			List<FileSearch> fileSearchList = biologicalSamples.get(fileSearch.getBiologicalSample());
			if (fileSearchList == null) {
				fileSearchList = new ArrayList<FileSearch>();
				biologicalSamples.put(fileSearch.getBiologicalSample(), fileSearchList);
			}
			fileSearchList.add(fileSearch);
		}

		// Output files a biological sample at a time
		// A single biological sample can be linked to a single database only, so we have to assume
		// that all files within the sample were searched in an identical manner
		for (final Map.Entry<String, List<FileSearch>> bioSample : biologicalSamples.entrySet()) {
			// First input file in the biological sample drives the per-sample settings.
			// This is a shortcoming of Scaffold (same biological sample cannot be analyzed using two
			// different databases, for instance).
			final FileSearch firstInputFile = bioSample.getValue().get(0);
			final String fastaDbId = definition.getSearchParameters().getDatabase().getShortName();
			final File deployedDatabase = deployedDatabases.get(fastaDbId);
			if (deployedDatabase == null) {
				throw new DaemonException(
						"Cannot find fasta db path for database " + fastaDbId
								+ " in the table of deployed databases:\n"
								+ deployedDatabaseTableToString(deployedDatabases));
			}
			String dbId = null;
			// Generate db id for given fasta file.
			// If we already got one, it's easy
			if (path2db.containsKey(deployedDatabase.getAbsolutePath())) {
				dbId = path2db.get(deployedDatabase.getAbsolutePath());
			} else {
				// We have to create fasta database and register it with the experiment
				final ScafmlFastaDatabase sfd = new ScafmlFastaDatabase(definition.getSearchParameters().getDatabase().getDatabaseAnnotation());
				sfd.setDatabase(deployedDatabase);
				// Generate db id
				dbId = "db" + String.valueOf(dbIdNum);
				dbIdNum++;
				sfd.setId(dbId);
				try {
					se.addFastaDatabase(sfd);
				} catch (MprcException sdpe) {
					throw new DaemonException(sdpe);
				}
				// Remember the generated id in our hashmap so it does not have to be recreated later
				path2db.put(deployedDatabase.getAbsolutePath(), dbId);
			}

			// add biological sample to the ScafmlExperiment
			final ScafmlBiologicalSample sb = new ScafmlBiologicalSample();
			sb.setId(firstInputFile.getBiologicalSample());
			try {
				se.addBiologicalSample(sb);
			} catch (MprcException sdpe) {
				throw new DaemonException(sdpe);
			}
			sb.setName(firstInputFile.getBiologicalSample());
			sb.setCategory(firstInputFile.getCategoryName() == null ? "none" : firstInputFile.getCategoryName());
			sb.setAnalyzeAsMudpit("true");
			sb.setDatabase(dbId);

			// add the input files to the biological sample
			for (final FileSearch inputFile : bioSample.getValue()) {
				// Find search results for given input file.
				final File inputFilePath = inputFile.getInputFile();
				final Map<String/*Search engine code*/, File> results =
						searchResults.getAllResults(inputFilePath);
				int i = 0;
				for (final Map.Entry<String/*Search engine code*/, File> result : results.entrySet()) {
					final File file = result.getValue();
					final String engineCode = result.getKey();
					// We add the particular search only if it is enabled for given engine
					if (file != null
							&& inputFile.isSearch(engineCode)
							&& isProcessedByScaffold(inputFile)) {
						// Add input file
						final ScafmlInputFile scafmlInputFile = new ScafmlInputFile();
						scafmlInputFile.setID(inputFile.getId() + '_' + String.valueOf(i));

						scafmlInputFile.setFile(file);

						try {
							sb.addInputFile(scafmlInputFile);
						} catch (MprcException sdpe) {
							throw new DaemonException(sdpe);
						}
					}
					i++;
				}
			}
		}

		final ScaffoldSettings scaffoldSettings = definition.getSearchParameters().getScaffoldSettings();

		se.setConnectToNCBI(scaffoldSettings.isConnectToNCBI());
		se.setAnnotateWithGOA(scaffoldSettings.isAnnotateWithGOA());

		final StarredProteins star = scaffoldSettings.getStarredProteins();
		final ScafmlExport export = new ScafmlExport(
				experiment,
				workFolder, // Output folder
				true, // Always export spectra for QA
				definition.getPeptideReport() != null, // Export peptide report
				scaffoldSettings.getProteinProbability(), // Thresholds
				scaffoldSettings.getPeptideProbability(),
				scaffoldSettings.getMinimumPeptideCount(),
				scaffoldSettings.getMinimumNonTrypticTerminii(),
				star != null ? star.getStarred() : "", // Starring
				star != null ? star.getDelimiter() : "",
				star != null && star.isRegularExpression(),
				scaffoldSettings.isSaveOnlyIdentifiedSpectra(), // Spectra
				scaffoldSettings.isSaveNoSpectra()
		);
		se.setExport(export);

		return sc;
	}

	private static boolean isProcessedByScaffold(final FileSearch inputFile) {
		return inputFile.isSearch("SCAFFOLD");
	}

	static String deployedDatabaseTableToString(final Map<String, File> deployedDatabases) {
		final StringBuilder table = new StringBuilder();
		for (final Map.Entry<String, File> entry : deployedDatabases.entrySet()) {
			table.append("\t")
					.append(entry.getKey())
					.append(" --} ")
					.append(entry.getValue().getAbsolutePath())
					.append("\n");
		}
		return table.toString();
	}
}
