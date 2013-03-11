package edu.mayo.mprc.filesharing;

import java.io.File;

/**
 * Class provides methods for synchronizing files between different systems. Each system is identify by a unique id.
 */
public interface FileSynchronizer {
	/**
	 * Synchronizes file in remote system, identified by the destinationId value, with local source file.
	 *
	 * @param destinationId
	 * @param localSourceFile
	 * @param destinationFilePath
	 * @return
	 */
	FileTransfer uploadFile(String destinationId, File localSourceFile, String destinationFilePath);

	/**
	 * Synchronizes content of folder in remote system, identified by the destinationId value, with content of local source folder.
	 *
	 * @param destinationId
	 * @param localSourceFolder
	 * @param destinationFolderPath
	 * @return
	 */
	FileTransfer uploadFolder(String destinationId, File localSourceFolder, String destinationFolderPath);

	/**
	 * Synchronizes local file with remote source file in system, identified by the sourceId value.
	 *
	 * @param sourceId             system id of source file
	 * @param localDestinationFile local file to be synchronized
	 * @param sourceFilePath       source file
	 * @return
	 */
	FileTransfer downloadFile(String sourceId, File localDestinationFile, String sourceFilePath);
}
