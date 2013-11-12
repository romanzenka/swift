package edu.mayo.mprc.fastadb;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.dbcurator.model.CurationDao;
import edu.mayo.mprc.utilities.FileUtilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Caches the first database it is asked to translate. Every subsequent database will be checked for identity,
 * if it does not match, error is thrown.
 */
public class SingleDatabaseTranslator implements ProteinSequenceTranslator {
	private FastaDbDao fastaDbDao;
	private CurationDao curationDao;
	private Curation database;
	private String currentDatabaseSources;
	private boolean stripDatabaseTimestamp = false;
	private static final Pattern TIMESTAMP = Pattern.compile("(.*)_[0-9]{6,8}");

	public SingleDatabaseTranslator(final FastaDbDao fastaDbDao, final CurationDao curationDao) {
		this.fastaDbDao = fastaDbDao;
		this.curationDao = curationDao;
	}

	@Override
	public ProteinSequence getProteinSequence(final String accessionNumber, final String databaseSources) {
		if (database == null) {
			if (databaseSources.contains(",")) {
				throw new MprcException("Multiple databases per Scaffold file not supported: [" + databaseSources + "]");
			}
			currentDatabaseSources = FileUtilities.stripGzippedExtension(databaseSources);
			database = curationDao.getLegacyCuration(currentDatabaseSources);
			if (database == null) {
				// Give it one more shot, removing the trailing timestamp in the _YYYYMMDD format
				final Matcher matcher = TIMESTAMP.matcher(currentDatabaseSources);
				if (matcher.matches()) {
					currentDatabaseSources = matcher.group(1);
					database = curationDao.getLegacyCuration(currentDatabaseSources);
					if (database == null) {
						throw new MprcException("Cannot find information about database [" + currentDatabaseSources + "] even after removing the timestamp");
					}
					stripDatabaseTimestamp = true;
				} else {
					throw new MprcException("Cannot find information about database [" + currentDatabaseSources + "]");
				}
			}
		} else {
			String cleanedDatabaseName = FileUtilities.stripGzippedExtension(databaseSources);
			if (stripDatabaseTimestamp) {
				final Matcher matcher = TIMESTAMP.matcher(cleanedDatabaseName);
				if (matcher.matches()) {
					cleanedDatabaseName = matcher.group(1);
				}
			}
			if (!cleanedDatabaseName.equals(currentDatabaseSources)) {
				throw new MprcException("Swift supports only a single FASTA database per Scaffold file. Two databases encountered: [" + currentDatabaseSources + "] and [" + cleanedDatabaseName + "]");
			}
		}
		return fastaDbDao.getProteinSequence(database, accessionNumber);
	}

}
