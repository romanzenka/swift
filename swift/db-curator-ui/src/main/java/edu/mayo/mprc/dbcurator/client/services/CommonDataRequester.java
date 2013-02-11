package edu.mayo.mprc.dbcurator.client.services;

import com.google.gwt.user.client.rpc.RemoteService;
import edu.mayo.mprc.common.client.GWTServiceException;
import edu.mayo.mprc.dbcurator.client.curatorstubs.CurationStub;
import edu.mayo.mprc.dbcurator.client.curatorstubs.HeaderTransformStub;

import java.util.List;
import java.util.Map;

/**
 * @author Eric Winter
 */
public interface CommonDataRequester extends RemoteService {
	List<HeaderTransformStub> getHeaderTransformers() throws GWTServiceException;

	Map<String, String> getFTPDataSources() throws GWTServiceException;

	Boolean isShortnameUnique(String toCheck) throws GWTServiceException;

	/**
	 * Takes a CurationStub that we want to have updated with status from the server and performs that update returning
	 * the updated curation.  It is up to the client to use the returned stub and swap the old stub with the new stub
	 * and then perform any updating that may be required.
	 *
	 * @param toUpdate the stub that you want to have updated
	 * @return the updated stub
	 */
	CurationStub performUpdate(CurationStub toUpdate) throws GWTServiceException;

	/**
	 * returns a CurationStub that was stored on the server and that we should display.  This object should be called
	 * "storedCuration" and be located in the HTTPSession.
	 *
	 * @return the stub for the curation that was already on the HttpSession object
	 */
	CurationStub lookForCuration() throws GWTServiceException;

	/**
	 * Attempt to find a curation with a given id from the server, if no curation with that id can be found null will
	 * be returned
	 *
	 * @param id the id of the curation to retreive
	 * @return the CurationStub with the given id
	 */
	CurationStub getCurationByID(Integer id) throws GWTServiceException;

	/**
	 * creates a copy of a given stub by finding the curation that the copy represents and making a copy of that and then
	 * making stub from the copy
	 *
	 * @param toCopy the stub you want to copy
	 * @return a stub for the copy
	 */
	CurationStub copyCurationStub(CurationStub toCopy) throws GWTServiceException;

	/**
	 * Runs a curation on the server.  This will execute a curation.  If you want to get the status of a curation call getStatus()
	 *
	 * @param toRun the curation you want to have run
	 * @return the curation stub that should be used after it is returned in place of the one that was run
	 */
	CurationStub runCuration(CurationStub toRun) throws GWTServiceException;

	// File listing

	/**
	 * gets a number of lines from the server from a given file.
	 *
	 * @param sharedPath         is the shared path to the file on the server.  This could just be a convention and the server will
	 *                           know what the client is actually asking for but it should be a context aware unique identifier.
	 * @param startLineInclusive the first line to include when returning
	 * @param numberOfLines      to retreive if there are not that many lines after the first then the return array may be shorter than expected.
	 * @return an array of Strings from a start line to an exclusive end line.  The array may be shorter than numberOfLines if there
	 *         were not enough lines left in the file.
	 */
	String[] getLines(String sharedPath, int startLineInclusive, int numberOfLines, String pattern) throws GWTServiceException;

	void setCancelMessage(boolean cancelMessage) throws GWTServiceException;

	String testPattern(String pattern) throws GWTServiceException;

	String[] getResults() throws GWTServiceException;
}
