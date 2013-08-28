package edu.mayo.mprc.swift.ui.client;

import com.google.gwt.user.client.rpc.RemoteService;
import edu.mayo.mprc.common.client.GWTServiceException;
import edu.mayo.mprc.swift.ui.client.rpc.*;
import edu.mayo.mprc.swift.ui.client.rpc.files.Entry;
import edu.mayo.mprc.swift.ui.client.rpc.files.ErrorEntry;
import edu.mayo.mprc.swift.ui.client.rpc.files.FileInfo;

import java.util.List;

/**
 * Provide for server-side validation, saving, and restoring of parameters.
 * <p/>
 * Key concepts:
 * <p/>
 * * Server side is stateful: the process of validating a given Param
 * might depend on the state of other Params.
 * Consequence: must choose between:
 * * Pushing the complete state back and forth -or-
 * * Caching the state server side.
 * <p/>
 * Preferred method: an evictor like design, where the server-side state can
 * be stored to database at any point and then retrieved later.  A cache
 * could eliminate unnecessary round trips to database.
 * <p/>
 * <p/>
 * * Want to be able to handle both persistent and transient parameter collections.
 * Want to be able to edit parameter collections that already exist without
 * invalidating already running or future searches.
 * <p/>
 * Consequence: don't ever edit permanent ParamSets, only copies.  Maybe relax this
 * with transient ParamSets?
 * <p/>
 * * Want to eventually present a user-specific set of paramter sets; or
 * a user-specific default value, etc.
 * <p/>
 * How to do authentication across XMLRPC
 */
public interface Service extends RemoteService {
	/**
	 * Lists contents of given directory. The listed of expanded paths will be expanded in the output (to save multiple calls).
	 *
	 * @param relativePath  Path to directory that is to be listed. The path is relative to system setting for the RAW file root.
	 * @param expandedPaths Listing of paths to be expanded in the output. The paths are relative to the relativePath argument.
	 * @return Virtual root of the listing, to be discarded on the client. The function can also return an {@link ErrorEntry} object.
	 */
	Entry listFiles(String relativePath, String[] expandedPaths) throws GWTServiceException;

	/**
	 * Retrieves a listing of .raw and .mgf files for given list of paths. The paths can lead either to a file
	 * or to a directory. They can even lead to directories that contain each other. The result is a listing of
	 * files that were discovered within these directories, complete with the file sizes.
	 *
	 * @param relativePaths Listing of paths to examine.
	 * @return Listing of files that were discovered within these paths.
	 */
	FileInfo[] findFiles(String[] relativePaths) throws GWTServiceException;

	/**
	 * @return List of all users.
	 */
	ClientUser[] listUsers() throws GWTServiceException;

	/**
	 * Starts search for given definition.
	 *
	 * @param def Search definition.
	 * @throws GWTServiceException When anything fails.
	 */
	void startSearch(ClientSwiftSearchDefinition def) throws GWTServiceException;

	/**
	 * Return search definition for a previous search with given search id.
	 *
	 * @param searchRunId Id of a search to load.
	 * @return Search definition
	 * @throws GWTServiceException Typically when the given search definition does not exist.
	 */
	ClientLoadedSearch loadSearch(int searchRunId) throws GWTServiceException;

	/**
	 * Returns a list of {@link ClientSearchEngine} classes that describe the search engines in sufficient detail for the
	 * UI.
	 *
	 * @return List of {@link ClientSearchEngine}
	 * @throws GWTServiceException
	 */
	List<ClientSearchEngine> listSearchEngines() throws GWTServiceException;

	/**
	 * @return A list of {@link SpectrumQaParamFileInfo} objects defining the spectrum QA parameter files.
	 *         The returned list can be empty, in which case the spectrum QA service should be disabled.
	 * @throws GWTServiceException
	 */
	List<SpectrumQaParamFileInfo> listSpectrumQaParamFiles() throws GWTServiceException;

	/**
	 * Returns true if swift configuration has scaffold report enabled.
	 *
	 * @throws GWTServiceException
	 */
	boolean isScaffoldReportEnabled() throws GWTServiceException;

	/**
	 * @return Message to be displayed for the user.
	 * @throws GWTServiceException
	 */
	String getUserMessage() throws GWTServiceException;

	/**
	 * Save a copy of the given parameter set (either as a temporary or as a permanent).
	 *
	 * @param toCopy        the existing ClientParamSet to save a copy of.
	 * @param newName       name to give the new param set; ignored if permanent is false
	 * @param ownerEmail    email of owner of new param set; ignored if permanent is false.
	 * @param ownerInitials initials of owner of new param set; ignored if permanent is false.
	 * @param permanent     if false, a temporary paramset is created and stored in the users's session.
	 * @return the new ClientParamSet
	 */
	ClientParamSet save(ClientParamSet toCopy, String newName, String ownerEmail, String ownerInitials,
	                    boolean permanent) throws GWTServiceException;

	/**
	 * Logins in to the Service, which sets a random session cookie that is used later to
	 * authenticate the client back to the service.  This random session cookie is to
	 * be sent back in each request as a "token"
	 *
	 * @param userName
	 * @param password
	 * @return True if login succeeded. In case of failure, an exception is thrown.
	 */
	Boolean login(String userName, String password) throws GWTServiceException;

	/**
	 * Fetch list of available ParamSets, along with selected ParamSet.
	 */
	ClientParamSetList getParamSetList() throws GWTServiceException;

	/**
	 * Fetch contents of ParamSet.
	 */
	ClientParamSetValues getParamSetValues(ClientParamSet paramSet) throws GWTServiceException;

	/**
	 * Fetch an updated list of allowedValues for the given list of Params, passing the mappingData through to the
	 * abstract param (may be null).
	 */
	List<List<ClientValue>> getAllowedValues(String[] params) throws GWTServiceException;

	/**
	 * Push value change for Param and receive Validation list back. Since a change of a single parameter can invalidate other parameters,
	 * we are forced to return full validation for everything in the form.
	 */
	ClientParamsValidations update(ClientParamSet paramSet, String param, ClientValue value) throws GWTServiceException;

	/**
	 * Deletes given parameter set.
	 *
	 * @param paramSet the {@link ClientParamSet} object to delete
	 * @throws GWTServiceException
	 */
	void delete(ClientParamSet paramSet) throws GWTServiceException;

	/**
	 * Fetch generated params files for display to user.
	 *
	 * @param paramSet the clientParamSet to obtain files from
	 * @return array of client param files
	 */
	ClientParamFile[] getFiles(ClientParamSet paramSet) throws GWTServiceException;

	/**
	 * Undeploys database idenfify by given dbToUndeploy paramater.
	 * dbToUndeploy parameter value is used to find Curation object to be undeployed
	 * by calling findCuration(dbToUndeploy) on the global CurationDao object.
	 *
	 * @param dbToUndeploy
	 * @return returns progress message.
	 * @throws GWTServiceException
	 */
	ClientDatabaseUndeployerProgress undeployDatabase(String dbToUndeploy) throws GWTServiceException;

	/**
	 * Gets next progress message on queue from given database undeployment task id.
	 * taskId parameter value is obtained by calling first undeployDatabase(String dbToUndeploy) first
	 * to start a undeployment task.
	 *
	 * @param taskId
	 * @throws GWTServiceException
	 */
	ClientDatabaseUndeployerProgress getProgressMessageForDatabaseUndeployment(Long taskId) throws GWTServiceException;

	boolean isDatabaseUndeployerEnabled() throws GWTServiceException;

	InitialPageData getInitialPageData(Integer previousSearchId) throws GWTServiceException;

	/**
	 * @param outputFolder
	 * @return True if the given output folder exists. The folder is entered relative to browse root.
	 */
	boolean outputFolderExists(String outputFolder) throws GWTServiceException;
}
