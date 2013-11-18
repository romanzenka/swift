package edu.mayo.mprc.dbcurator.server;

import edu.mayo.mprc.dbcurator.model.CurationContext;
import edu.mayo.mprc.fasta.FASTAInputStream;
import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Eric Winter
 */
public final class FASTAFileUploadServlet implements HttpRequestHandler {
	protected static final int MAX_FILE_SIZE = 1000000000;

	private CurationContext curationContext;

	private File getUploadFolder() {
		return curationContext.getFastaUploadFolder();
	}

	@Override
	public void handleRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

		response.setContentType("text/html;charset=UTF-8");

		final Writer responseWriter = response.getWriter();

		//if we are trying to perform an uplaod then perform the upload
		if (ServletFileUpload.isMultipartContent(request)) {


			final DiskFileItemFactory fileFactory = new DiskFileItemFactory();

			fileFactory.setRepository(getUploadFolder());

			final ServletFileUpload upload = new ServletFileUpload(fileFactory);


			upload.setFileSizeMax(MAX_FILE_SIZE);

			List<FileItem> items = null;
			try {
				items = upload.parseRequest(request);

			} catch (FileUploadException e) {
				e.printStackTrace();
				throw new IOException(e.getMessage());
			}

			final StringBuilder responseMessage = new StringBuilder();
			File toSave = null;
			//go through the
			for (final FileItem item : items) {
				if (item.isFormField()) {
					continue;
				}
				//save the file somewhere good
				toSave = saveFile(item);

				//if it was a file then just go on else we want to delete the file that was just created
				final String errorMessage = FASTAInputStream.isFASTAFileValid(toSave, false);
				if (errorMessage == null) {
					break; //we only need to worry about the first file
				} else {
					responseMessage.append(errorMessage);
					FileUtilities.quietDelete(toSave);
				}
			}

			if (responseMessage.length() == 0 && toSave != null) {
				responseWriter.write(toSave.getAbsolutePath());
			} else {
				responseWriter.append("<Error> ");
				responseWriter.append(responseMessage.toString());
				responseWriter.append("\n");
			}
		} else {
			responseWriter.append("<Error> ");
			responseWriter.append("You are not uploading a file");
			responseWriter.append("\n");
		}
	}

	private File saveFile(final FileItem item) throws IOException {
		String fileName = item.getName();

		//make sure the path information is not included...it might be but probably not depending on browser
		int finalSlash = fileName.lastIndexOf('/');
		if (finalSlash == -1) {
			finalSlash = fileName.lastIndexOf('\\');
		}

		if (finalSlash != -1) {
			fileName = fileName.substring(finalSlash + 1);
		}

		final File serverFile = new File(getUploadFolder(), new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()) + "_" + fileName);
		try {
			item.write(serverFile);
		} catch (Exception e) {
			throw new IOException(e.getMessage(), e);
		}

		return checkForSimilarFile(serverFile, /*deleteThisOne*/true);
	}

	/**
	 * looks in the same directory as forFile and sees if there are any identical files and if so will
	 * return that file and delete
	 *
	 * @param forFile
	 * @return
	 */
	private static File checkForSimilarFile(final File forFile, final boolean deleteForFile) {
		if (!forFile.exists()) { //if the file doesn't exist then don't worry about it
			return forFile;
		}

		final File subFile = FileUtilities.findSingleSimilarFile(forFile, forFile.getParentFile());

		if (subFile == null) {
			return forFile;
		} else {
			if (deleteForFile) {
				FileUtilities.quietDelete(forFile);
			}
			return subFile;
		}
	}

	public CurationContext getCurationContext() {
		return curationContext;
	}

	public void setCurationContext(CurationContext curationContext) {
		this.curationContext = curationContext;
	}
}