package edu.mayo.mprc.swift.webservice;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.LogData;
import edu.mayo.mprc.swift.dbmapping.TaskData;
import edu.mayo.mprc.utilities.StringUtilities;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Sends a log file to the user, eventually with nice formatting etc.
 *
 * @author Roman Zenka
 */
@Controller
public final class LogView {
	@Resource(name = "swiftDao")
	private SwiftDao swiftDao;

	@Resource(name = "fileTokenFactory")
	private DatabaseFileTokenFactory fileTokenFactory;

	enum LogType {
		OUTPUT_LOG,
		ERROR_LOG
	}

	@RequestMapping(value = "/task-log/{taskId}/{logType}", method = RequestMethod.GET)
	public void getQaResource(@PathVariable final int taskId,
	                          @PathVariable final String logType,
	                          final HttpServletResponse response) {

		final List<LogData> logList = getLogData(taskId);
		LogType type = getLogTypeFromString(logType);

		response.setContentType("text/plain");

		try {
			final ServletOutputStream outputStream = response.getOutputStream();
			outputStream.println("Log files for task " + taskId);
			outputStream.println();

			for (final LogData data : logList) {

				final File logFile;
				if (type == LogType.OUTPUT_LOG) {
					logFile = data.getOutputLog();
				} else {
					logFile = data.getErrorLog();
				}

				outputStream.println(StringUtilities.repeat('-', 80));
				outputStream.println("Log file " + logFile.getAbsolutePath());
				outputStream.println();
				QaReport.streamFileToResponse(response, logFile, false);
			}
		} catch (final IOException e) {
			throw new MprcException(e);
		}


	}

	private static LogType getLogTypeFromString(final String logType) {
		if ("out".equals(logType)) {
			return LogType.OUTPUT_LOG;
		}
		if ("err".equals(logType)) {
			return LogType.ERROR_LOG;
		}
		throw new MprcException(String.format("Unknown log type %s", logType));
	}

	private List<LogData> getLogData(final int taskId) {
		final List<LogData> result;
		swiftDao.begin();
		try {
			final TaskData data = swiftDao.getTaskData(taskId);
			result = swiftDao.getLogsForTask(data);
			swiftDao.commit();
		} catch (final Exception e) {
			swiftDao.rollback();
			throw new MprcException(String.format("Could not serve log file for task %d", taskId), e);
		}
		return result;
	}

}
