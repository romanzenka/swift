package edu.mayo.mprc.dbcurator.server;

import edu.mayo.mprc.GWTServiceExceptionFactory;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.common.client.GWTServiceException;
import edu.mayo.mprc.dbcurator.client.steppanels.CurationStub;
import edu.mayo.mprc.dbcurator.client.steppanels.HeaderTransformStub;
import edu.mayo.mprc.dbcurator.model.*;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Roman Zenka
 */
public final class CommonDataRequesterLogic {
	private CurationDao curationDao;
	private CurationContext curationContext;
	private AttributeStore attributeStore;

	private static final Logger LOGGER = Logger.getLogger(CommonDataRequesterLogic.class);

	public CommonDataRequesterLogic() {
	}

	public List<HeaderTransformStub> getHeaderTransformers() {
		final List<HeaderTransform> transforms;
		if (curationDao == null) {
			return new ArrayList<HeaderTransformStub>(0);
		}

		List<HeaderTransformStub> abstractList = null;
		curationDao.begin();
		try {
			transforms = curationDao.getCommonHeaderTransforms();

			abstractList = new ArrayList<HeaderTransformStub>();
			if (transforms != null) {
				for (final HeaderTransform transform : transforms) {
					final HeaderTransformStub stub = new HeaderTransformStub();
					stub.description = transform.getName();
					stub.matchPattern = transform.getGroupString();
					stub.subPattern = transform.getSubstitutionPattern();
					abstractList.add(stub);
				}
			}
			curationDao.commit();
		} catch (Exception t) {
			curationDao.rollback();
			throw new MprcException("Cannot get header transformer list", t);
		}
		return abstractList;
	}

	public Map<String, String> getFTPDataSources() {
		if (curationDao == null) {
			return new HashMap<String, String>();
		}

		Map<String, String> sourceMap = null;
		curationDao.begin();
		try {
			final List<FastaSource> sources = curationDao.getCommonSources();

			sourceMap = new HashMap<String, String>();

			for (final FastaSource source : sources) {
				sourceMap.put(source.getName(), source.getUrl());
			}
			curationDao.commit();
		} catch (Exception t) {
			curationDao.rollback();
			throw new MprcException("Could not obtain FTP data sources", t);
		}

		return sourceMap;
	}

	public Boolean isShortnameUnique(final String toCheck) {
		curationDao.begin();
		try {
			final Boolean result = isShortnameUniqueBody(toCheck);
			curationDao.commit();
			return result;
		} catch (Exception t) {
			curationDao.rollback();
			throw new MprcException("Could not determine if short curation name is unique", t);
		}
	}

	private Boolean isShortnameUniqueBody(final String toCheck) {
		return (curationDao.getCurationsByShortname(toCheck, /*ignoreCase*/true).isEmpty());
	}

	/**
	 * Takes a CurationStub that we want to have updated with status from the server and performs that update returning
	 * the updated curation.  It is up to the client to use the returned stub and swap the old stub with the new stub
	 * and then perform any updating that may be required.
	 *
	 * @param toUpdate the stub that you want to have updated
	 * @return the updated stub
	 */
	public CurationStub performUpdate(final CurationStub toUpdate) {
		curationDao.begin();
		try {
			final CurationHandler handler = getHandler();
			final CurationStub curationStub = handler.syncCuration(toUpdate);
			curationDao.commit();
			return curationStub;
		} catch (Exception t) {
			curationDao.rollback();
			throw new MprcException(t);
		}
	}

	public CurationStub lookForCuration() {
		curationDao.begin();
		try {
			final CurationStub cachedCurationStub = getHandler().getCachedCurationStub();
			curationDao.commit();
			return cachedCurationStub;
		} catch (Exception t) {
			curationDao.rollback();
			throw new MprcException(t);
		}
	}

	public CurationStub getCurationByID(final Integer id) {
		curationDao.begin();
		try {
			final CurationStub curationStub = getHandler().getCurationByID(id);
			curationDao.commit();
			return curationStub;
		} catch (Exception t) {
			curationDao.rollback();
			throw new MprcException(t);
		}
	}


	public CurationStub copyCurationStub(final CurationStub toCopy) {
		curationDao.begin();
		try {
			getHandler().syncCuration(toCopy);
			final Curation curation = getHandler().getCachedCuration();
			final Curation retCuration = curation.createCopy(false);
			final CurationStub stub = getHandler().createStub(retCuration);
			curationDao.commit();
			return stub;
		} catch (Exception t) {
			curationDao.rollback();
			throw new MprcException(t);
		}
	}

	/**
	 * looks for a CurationHandler titled "curationHandler" in the given session object and returns it.  If it is not found
	 * then a new one is created and placed on the given session with the given name ("curationHandler").
	 *
	 * @return the curation handler that was on the session or a new one that has been put on the session
	 */
	private CurationHandler getHandler() {
		CurationHandler perSession = null;
		try {
			perSession = (CurationHandler) attributeStore.getAttribute("curationHandler");
		} catch (Exception e) {
			LOGGER.warn("Error getting a curation handler", e);
			perSession = null;
		}

		if (perSession == null) {
			perSession = new CurationHandler();
			attributeStore.setAttribute("curationHandler", perSession);
		}
		perSession.setCurationDao(curationDao);
		perSession.setCurationContext(curationContext);
		return perSession;
	}

	/**
	 * Runs a curation on the server.  This will execute a curation.  If you want to get the status of a curation call getStatus()
	 *
	 * @param toRun the curation you want to have run
	 */
	public CurationStub runCuration(final CurationStub toRun) {
		curationDao.begin();
		try {
			if (!isShortnameUniqueBody(toRun.getShortName())) {
				if (toRun.getErrorMessages() == null) {
					toRun.setErrorMessages(new ArrayList<String>());
				}
				toRun.getErrorMessages().add("The shortname is not unique");
				return toRun;
			}

			final CurationHandler handler = getHandler();
			final CurationStub curationStub = handler.executeCuration(toRun);
			curationDao.commit();
			return curationStub;
		} catch (Exception t) {
			curationDao.rollback();
			throw new MprcException(t);
		}
	}

	public String testPattern(final String pattern) {
		try {
			Pattern.compile(pattern);
			return "";
		} catch (PatternSyntaxException e) {
			return e.getMessage();
		}
	}

	public String[] getLines(final String sharedPath, final int startLineInclusive, final int numberOfLines, String pattern) throws GWTServiceException {
		RandomAccessFile raf = null;
		try {
			setCancelMessage(false);
			clearResults();

			Pattern compiledPattern = null;
			if (pattern != null && !pattern.isEmpty()) {
				pattern = ".*" + pattern + ".*";
				pattern = pattern.replace("\\", "\\\\");
				compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
			}

			final SortedMap<Integer, Long> currentPositionMap = getCurrentPositionMap(sharedPath, pattern);

			raf = new RandomAccessFile(new File(sharedPath), "r");
			Long startLinePosition = currentPositionMap.get(startLineInclusive);
			if (startLinePosition == null) {
				//todo: find the position of this line
				final SortedMap<Integer, Long> headMap = currentPositionMap.headMap(startLineInclusive);

				Integer closestLessThanLine = 0;
				if (headMap.isEmpty()) {
					startLinePosition = 0L;
				} else {
					closestLessThanLine = Collections.max(headMap.keySet());
				}


				if (closestLessThanLine == null || closestLessThanLine == 0) {
					startLinePosition = 0L;
					closestLessThanLine = 0;
				} else {
					startLinePosition = headMap.get(closestLessThanLine);
				}

				raf.seek(startLinePosition);

				int matchCounter = closestLessThanLine;
				while (closestLessThanLine < startLineInclusive) {
					final long position = raf.getFilePointer();
					final String line = raf.readLine();
					if (line == null) {
						break;
					}
					if (compiledPattern == null || compiledPattern.matcher(line).matches()) {
						currentPositionMap.put(matchCounter++, position);
						closestLessThanLine++;
					}
				}
				//now we are at the startLine
			} else {
				raf.seek(startLinePosition);
			}

			//todo: probably just want to go through until we fill up the array taking the pattern into account.

			int resultCounter = 0;
			int fromStart = 0;
			while (resultCounter < numberOfLines && !getCancelMessage()) {
				final Long cachedPosition = currentPositionMap.get(startLineInclusive + fromStart);
				fromStart++;
				if (cachedPosition != null) {
					raf.seek(cachedPosition);
					addResult(raf.readLine());
					resultCounter++;
				} else {
					final long position = raf.getFilePointer();
					final String line = raf.readLine();
					if (line == null) { //if we have reached the end of the file then just don't get any more.
						break;
					}
					if (compiledPattern == null || compiledPattern.matcher(line).matches()) {
						currentPositionMap.put(startLineInclusive + resultCounter, position);
						addResult(line);
						resultCounter++;
					}
				}
			}
			return getResults();
		} catch (Exception e) {
			LOGGER.error("Could not obtain lines", e);
			throw GWTServiceExceptionFactory.createException("Could not obtain lines", e);
		} finally {
			FileUtilities.closeQuietly(raf);
		}

	}

	protected synchronized void clearResults() {
		attributeStore.setAttribute("results", null);
	}

	protected synchronized void addResult(final String result) {
		final List<String> results;
		final Object o = attributeStore.getAttribute("results");

		if (o == null) {
			results = new ArrayList<String>();
			attributeStore.setAttribute("results", results);
		} else {
			results = (List<String>) o;
		}

		results.add(result);
	}

	public synchronized String[] getResults() throws GWTServiceException {
		try {
			final List<String> results;
			final Object o = attributeStore.getAttribute("results");

			if (o == null) {
				results = new ArrayList<String>();
				attributeStore.setAttribute("results", results);
			} else {
				results = (List<String>) o;
			}

			final String[] retArray = new String[results.size()];
			for (int i = 0; i < results.size(); i++) {
				retArray[i] = results.get(i);
			}

			return retArray;
		} catch (Exception t) {
			LOGGER.error("Could not obtain results", t);
			throw GWTServiceExceptionFactory.createException("Could not obtain results", t);
		}
	}

	public void setCancelMessage(final boolean cancelMessage) throws GWTServiceException {
		try {
			attributeStore.setAttribute("cancelRequest", (cancelMessage ? true : null));
		} catch (Exception t) {
			LOGGER.error("Could not cancel request", t);
			throw GWTServiceExceptionFactory.createException("Could not cancel request", t);
		}
	}

	protected boolean getCancelMessage() {
		return !(attributeStore.getAttribute("cancelRequest") == null);
	}

	private SortedMap<Integer, Long> getCurrentPositionMap(final String filePath, final String pattern) {
		final Map<String, SortedMap<Integer, Long>> allPositionMaps = getPositionMaps();
		final String currentKey = (pattern == null || pattern.isEmpty() ? filePath : filePath + "_" + pattern);

		SortedMap<Integer, Long> currentPositionLookup = null;

		//get rid of the maps we are no longer interested in
		for (final Map.Entry<String, SortedMap<Integer, Long>> entry : allPositionMaps.entrySet()) {
			if (!entry.getKey().equals(currentKey)) {
				allPositionMaps.remove(entry.getKey());
			} else {
				currentPositionLookup = entry.getValue();
			}
		}

		if (currentPositionLookup == null) {
			currentPositionLookup = new TreeMap<Integer, Long>();
			allPositionMaps.put(currentKey, currentPositionLookup);
		}
		return currentPositionLookup;
	}

	private Map<String, SortedMap<Integer, Long>> getPositionMaps() {
		final Object attribute = attributeStore.getAttribute("FileLineMap");
		Map<String, SortedMap<Integer, Long>> positionMap = (Map<String, SortedMap<Integer, Long>>) attribute;
		if (positionMap == null) {
			positionMap = new HashMap<String, SortedMap<Integer, Long>>();
			attributeStore.setAttribute("FileLineMap", positionMap);
		}
		return positionMap;
	}

	public CurationDao getCurationDao() {
		return curationDao;
	}

	public void setCurationDao(final CurationDao curationDao) {
		this.curationDao = curationDao;
	}

	public CurationContext getCurationContext() {
		return curationContext;
	}

	public void setCurationContext(final CurationContext curationContext) {
		this.curationContext = curationContext;
	}

	public AttributeStore getAttributeStore() {
		return attributeStore;
	}

	public void setAttributeStore(final AttributeStore attributeStore) {
		this.attributeStore = attributeStore;
	}
}
