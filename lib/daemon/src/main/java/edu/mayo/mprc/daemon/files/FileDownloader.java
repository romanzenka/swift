package edu.mayo.mprc.daemon.files;

import java.io.File;

/**
 * @author Roman Zenka
 */
public interface FileDownloader {
	File downloadFile(FileToken fileToken, File result);
}
