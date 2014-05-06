package edu.mayo.mprc.swift.webservice;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.TaskData;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

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

		final LogType type = getLogTypeFromString(logType);
		final String token = getLogFileToken(taskId, type);
		final File file = fileTokenFactory.databaseTokenToFile(token);

		response.setContentType("text/plain");

		QaReport.streamFileToResponse(response, file);
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

	private String getLogFileToken(final int taskId, final LogType log) {
		final String token;
		swiftDao.begin();
		try {
			final TaskData data = swiftDao.getTaskData(taskId);
			switch (log) {
				case OUTPUT_LOG:
					token = data.getOutputLogDatabaseToken();
					break;
				case ERROR_LOG:
					token = data.getErrorLogDatabaseToken();
					break;
				default:
					throw new MprcException(String.format("Unknown log type %s", log));
			}
			swiftDao.commit();
		} catch (Exception e) {
			swiftDao.rollback();
			throw new MprcException(String.format("Could not serve log file for task %d", taskId), e);
		}
		return token;
	}

}
