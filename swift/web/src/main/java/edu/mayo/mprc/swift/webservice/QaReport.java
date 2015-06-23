package edu.mayo.mprc.swift.webservice;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.SwiftSearchDefinition;
import edu.mayo.mprc.swift.search.task.QaTask;
import edu.mayo.mprc.utilities.FileUtilities;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

/**
 * Sends QA files to the user, mapping them via the {@link edu.mayo.mprc.swift.dbmapping.SearchRun} id.
 *
 * @author Roman Zenka
 */
@Controller
public final class QaReport {
	@Resource(name = "swiftDao")
	private SwiftDao swiftDao;

	public static final FileTypeMap TYPE_MAP = makeDefaultFileTypeMap();

	static FileTypeMap makeDefaultFileTypeMap() {
		final MimetypesFileTypeMap defaultMap = new MimetypesFileTypeMap();

		defaultMap.addMimeTypes("image/png png PNG");
		defaultMap.addMimeTypes("image/jpeg jpg jpeg JPG JPEG");
		defaultMap.addMimeTypes("application/vnd.ms-excel xls");
		defaultMap.addMimeTypes("text/html htm html");
		defaultMap.addMimeTypes("text/css css");

		return defaultMap;
	}

	@Bean(name = "defaultFileTypeMap")
	public static FileTypeMap getDefaultFileTypeMap() {
		return TYPE_MAP;
	}

	@RequestMapping(value = "/service/qa/{searchRunId}/{fileName:.*}", method = RequestMethod.GET)
	public void getQaResource(@PathVariable final int searchRunId,
	                          @PathVariable final String fileName,
	                          final HttpServletResponse response) {

		final SwiftSearchDefinition searchDefinition = getSearchDefinition(searchRunId);

		final File outputFolder = searchDefinition.getOutputFolder();
		final File qaFolder = new File(outputFolder, QaTask.QA_SUBDIRECTORY);
		if (!qaFolder.isDirectory()) {
			throw new MprcException(String.format("QA analysis for search %d not found", searchRunId));
		}

		final File file = new File(qaFolder, fileName);
		final String contentType = TYPE_MAP.getContentType(file);
		response.setContentType(contentType);

		streamFileToResponse(response, file);
	}

	private SwiftSearchDefinition getSearchDefinition(final int searchRunId) {
		final SwiftSearchDefinition searchDefinition;
		swiftDao.begin();
		try {
			final SearchRun searchRun = swiftDao.getSearchRunForId(searchRunId);
			searchDefinition = swiftDao.getSwiftSearchDefinition(searchRun.getSwiftSearch());
			swiftDao.commit();
		} catch (Exception e) {
			swiftDao.rollback();
			throw new MprcException(String.format("Could not serve QA files for search %d", searchRunId), e);
		}
		return searchDefinition;
	}

	public static void streamFileToResponse(final HttpServletResponse response, final File file) {
		streamFileToResponse(response, file, true);
	}

	public static void streamFileToResponse(final HttpServletResponse response, final File file, final boolean close) {
		BufferedInputStream bis = null;
		try {
			bis = new BufferedInputStream(new FileInputStream(file));
			final OutputStream os = response.getOutputStream();
			final byte[] buffer = new byte[10240];
			int i = bis.read(buffer);
			while (i != -1) {
				os.write(buffer, 0, i);
				i = bis.read(buffer);
			}
		} catch (IOException ex) {
			throw new MprcException(String.format("Could not send data for file %s", file.getAbsolutePath()), ex);
		} finally {
			if (close) {
				FileUtilities.closeQuietly(bis);
			}
		}
	}
}
