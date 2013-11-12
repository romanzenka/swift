package edu.mayo.mprc.dbcurator.model;

import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.database.Evolvable;
import org.joda.time.DateTime;

import java.util.List;

public final class MockCurationDao implements CurationDao {
	@Override
	public Curation getCuration(final int curationID) {
		return null;
	}

	@Override
	public void begin() {

	}

	@Override
	public void commit() {

	}

	@Override
	public void rollback() {

	}

	@Override
	public void addCuration(final Curation curation) {

	}

	@Override
	public void save(final SourceDatabaseArchive archive) {
	}

	@Override
	public void deleteCuration(final Curation curation, final Change change) {
	}

	@Override
	public FastaSource getDataSourceByName(final String urlString) {
		return null;
	}

	@Override
	public FastaSource getDataSourceByUrl(final String urlString) {
		return null;
	}

	@Override
	public SourceDatabaseArchive findSourceDatabaseInExistence(final String s, final DateTime urlLastMod) {
		return null;
	}

	@Override
	public HeaderTransform getHeaderTransformByUrl(final String url) {
		return null;
	}

	@Override
	public HeaderTransform getHeaderTransformByName(final String name) {
		return null;
	}

	@Override
	public List<Curation> getAllCurations() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Curation findCuration(final String shortDbName) {
		return null;
	}

	@Override
	public Curation getCurationByShortName(final String uniqueName) {
		return null;
	}

	@Override
	public Curation getLegacyCuration(String uniqueName) {
		return null;
	}

	@Override
	public List<Curation> getCurationsByShortname(final String curationShortName) {
		return null;
	}

	@Override
	public List<Curation> getCurationsByShortname(final String shortname, final boolean ignoreCase) {
		return null;
	}

	@Override
	public List<FastaSource> getCommonSources() {
		return null;
	}

	@Override
	public void addHeaderTransform(final HeaderTransform sprotTrans) {
	}

	@Override
	public List<HeaderTransform> getCommonHeaderTransforms() {
		return null;
	}

	@Override
	public long countAll(final Class<? extends Evolvable> clazz) {
		return 0;
	}

	@Override
	public long rowCount(final Class<?> clazz) {
		return 0;
	}

	@Override
	public Curation addLegacyCuration(final String legacyName) {
		return null;
	}

	@Override
	public void addFastaSource(final FastaSource source) {
	}

	@Override
	public void flush() {
	}
}
