package edu.mayo.mprc.swift.ui.client.service;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.mayo.mprc.swift.ui.client.rpc.*;
import edu.mayo.mprc.swift.ui.client.rpc.files.Entry;
import edu.mayo.mprc.swift.ui.client.rpc.files.FileInfo;

import java.util.List;

public interface ServiceAsync {
	/**
	 * Lists contents of given directory. The listed of expanded paths will be expanded in the output (to save multiple calls).
	 *
	 * @param relativePath  Path to directory that is to be listed. The path is relative to system setting for the RAW file root.
	 * @param expandedPaths Listing of paths to be expanded in the output. The paths are relative to the relativePath argument.
	 * @return Virtual root of the listing, to be discarded on the client. The function can also return an {@link edu.mayo.mprc.swift.ui.client.rpc.files.ErrorEntry} object.
	 */
	void listFiles(String relativePath, String[] expandedPaths, AsyncCallback<Entry> async);

	/**
	 * Retrieves a listing of .raw and .mgf files for given list of paths. The paths can lead either to a file
	 * or to a directory. They can even lead to directories that contain each other. The result is a listing of
	 * files that were discovered within these directories, complete with the file sizes.
	 *
	 * @param relativePaths Listing of paths to examine.
	 * @return Listing of files that were discovered within these paths.
	 */
	void findFiles(String[] relativePaths, AsyncCallback<FileInfo[]> async);

	/**
	 * @return List of all users.
	 */
	void listUsers(AsyncCallback<ClientUser[]> async);

	/**
	 * Returns a list of {@link edu.mayo.mprc.swift.ui.client.rpc.ClientSearchEngine} classes that describe the search engines in sufficient detail for the
	 * UI.
	 *
	 * @return List of {@link edu.mayo.mprc.swift.ui.client.rpc.ClientSearchEngine}
	 * @throws edu.mayo.mprc.common.client.GWTServiceException
	 *
	 */
	void listSearchEngines(AsyncCallback<List<ClientSearchEngine>> async);

	/**
	 * @return A list of {@link edu.mayo.mprc.swift.ui.client.rpc.SpectrumQaParamFileInfo} objects defining the spectrum QA parameter files.
	 *         The returned list can be empty, in which case the spectrum QA service should be disabled.
	 * @throws edu.mayo.mprc.common.client.GWTServiceException
	 *
	 */
	void listSpectrumQaParamFiles(AsyncCallback<List<SpectrumQaParamFileInfo>> async);

	/**
	 * Returns true if swift configuration has scaffold report enabled.
	 *
	 * @return
	 * @throws edu.mayo.mprc.common.client.GWTServiceException
	 *
	 */
	void isScaffoldReportEnabled(AsyncCallback<Boolean> async);

	/**
	 * @return Message to be displayed for the user.
	 * @throws edu.mayo.mprc.common.client.GWTServiceException
	 *
	 */
	void getUserMessage(AsyncCallback<String> async);

	/**
	 * Logins in to the Service, which sets a random session cookie that is used later to
	 * authenticate the client back to the service.  This random session cookie is to
	 * be sent back in each request as a "token"
	 *
	 * @param userName
	 * @param password
	 * @return True if login succeeded. In case of failure, an exception is thrown.
	 */
	void login(String userName, String password, AsyncCallback<Boolean> async);

	/**
	 * Fetch list of available ParamSets, along with selected ParamSet.
	 */
	void getParamSetList(AsyncCallback<ClientParamSetList> async);

	/**
	 * Fetch contents of ParamSet.
	 */
	void getParamSetValues(ClientParamSet paramSet, AsyncCallback<ClientParamSetValues> async);

	/**
	 * Fetch an updated list of allowedValues for the given list of Params, passing the mappingData through to the
	 * abstract param (may be null).
	 */
	void getAllowedValues(String[] params, AsyncCallback<List<List<ClientValue>>> async);

	/**
	 * Push value change for Param and receive Validation list back. Since a change of a single parameter can invalidate other parameters,
	 * we are forced to return full validation for everything in the form.
	 */
	void update(ClientParamSet paramSet, String param, ClientValue value, AsyncCallback<ClientParamsValidations> async);

	/**
	 * Save a copy of the given parameter set (either as a temporary or as a permanent).
	 *
	 * @param toCopy     the existing ClientParamSet to save a copy of.
	 * @param newName    name to give the new param set; ignored if permanent is false
	 * @param ownerEmail email of owner of new param set; ignored if permanent is false.
	 * @param permanent  if false, a temporary paramset is created and stored in the users's session.
	 * @return the new ClientParamSet
	 */
	void save(ClientParamSet toCopy, String newName, String ownerEmail, String ownerInitials,
	          boolean permanent, AsyncCallback<ClientParamSet> async);

	/**
	 * Deletes given parameter set.
	 *
	 * @param paramSet the {@link edu.mayo.mprc.swift.ui.client.rpc.ClientParamSet} object to delete
	 * @throws edu.mayo.mprc.common.client.GWTServiceException
	 *
	 */
	void delete(ClientParamSet paramSet, AsyncCallback<Void> async);

	/**
	 * Fetch generated params files for display to user.
	 *
	 * @param paramSet the clientParamSet to obtain files from
	 * @return array of client param files
	 */
	void getFiles(ClientParamSet paramSet, AsyncCallback<ClientParamFile[]> async);

	/**
	 * Undeploys database idenfify by given dbToUndeploy paramater.
	 * dbToUndeploy parameter value is used to find Curation object to be undeployed
	 * by calling findCuration(dbToUndeploy) on the global CurationDao object.
	 *
	 * @param dbToUndeploy
	 * @return returns progress message.
	 * @throws edu.mayo.mprc.common.client.GWTServiceException
	 *
	 */
	void undeployDatabase(String dbToUndeploy, AsyncCallback<ClientDatabaseUndeployerProgress> async);

	/**
	 * Gets next progress message on queue from given database undeployment task id.
	 * taskId parameter value is obtained by calling first undeployDatabase(String dbToUndeploy) first
	 * to start a undeployment task.
	 *
	 * @param taskId
	 * @return
	 * @throws edu.mayo.mprc.common.client.GWTServiceException
	 *
	 */
	void getProgressMessageForDatabaseUndeployment(Long taskId, AsyncCallback<ClientDatabaseUndeployerProgress> async);

	/**
	 * @return
	 * @throws edu.mayo.mprc.common.client.GWTServiceException
	 *
	 */
	void isDatabaseUndeployerEnabled(AsyncCallback<Boolean> async);

	/**
	 * Starts search for given definition.
	 *
	 * @param def Search definition.
	 * @throws edu.mayo.mprc.common.client.GWTServiceException
	 *          When anything fails.
	 */
	void startSearch(ClientSwiftSearchDefinition def, AsyncCallback<Void> async);

	/**
	 * Return search definition for a previous search with given search id.
	 *
	 * @param searchRunId Id of a search run to load
	 * @return Search definition
	 * @throws edu.mayo.mprc.common.client.GWTServiceException
	 *          Typically when the given search definition does not exist.
	 */
	void loadSearch(int searchRunId, AsyncCallback<ClientLoadedSearch> async);

	void getInitialPageData(Integer previousSearchId, AsyncCallback<InitialPageData> async);

	/**
	 * @param outputFolder
	 * @return True if the given output folder exists. The folder is entered relative to browse root.
	 */
	void outputFolderExists(String outputFolder, AsyncCallback<Boolean> async);
}
