package edu.mayo.mprc.swift.webservice;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.db.DatabaseFileTokenFactory;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.LogData;
import edu.mayo.mprc.swift.dbmapping.TaskData;
import edu.mayo.mprc.utilities.FileUtilities;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	private static final Pattern TIME_STAMP = Pattern.compile("^(\\d{1,4}-\\d{1,2}-\\d{1,2}) (\\d{1,2}:\\d{1,2}:\\d{1,2},\\d{1,3}) (.*)");
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

	enum LogType {
		OUTPUT_LOG,
		ERROR_LOG
	}

	@RequestMapping(value = "/task-log/{taskId}/{logType}", method = RequestMethod.GET)
	public void getQaResource(@PathVariable final int taskId,
	                          @PathVariable final String logType,
	                          final HttpServletResponse response) {

		final List<LogData> logList = getLogData(taskId);
		final LogType type = getLogTypeFromString(logType);

		response.setContentType("text/html");

		try {
			final ServletOutputStream outputStream = response.getOutputStream();
			outputStream.println("<!DOCTYPE html>\n" +
					"<html>\n" +
					"<head lang=\"en\">\n" +
					"    <meta charset=\"UTF-8\">\n" +
					"    <title>Log file for task " + taskId + "</title>\n" +
					"    <style type=\"text/css\">\n" +
					"        log {\n" +
					"            font-family: monospace;\n" +
					"            font-size: 12px;\n" +
					"        }\n" +
					"\n" +
					"        row {\n" +
					"            position: relative;\n" +
					"            display: block;\n" +
					"        }\n" +
					"\n" +
					"        prg {\n" +
					"            display: block;\n" +
					"            background-color: #efe;\n" +
					"            position: absolute;\n" +
					"            left: 0px;\n" +
					"            top: 0px;\n" +
					"            z-index: -1;\n" +
					"            height: 12px;\n" +
					"        }\n" +
					"\n" +
					"        time {\n" +
					"            display: inline;\n" +
					"            font-weight: bold;\n" +
					"            margin-right: 1em;\n" +
					"        }\n" +
					"    </style>\n" +
					"</head>\n" +
					"<body>\n");

			for (final LogData data : logList) {
				final File logFile;
				if (type == LogType.OUTPUT_LOG) {
					logFile = data.getOutputLog();
				} else {
					logFile = data.getErrorLog();
				}

				printLogFile(response, outputStream, logFile);
			}
			outputStream.println("</body>\n" +
					"</html>");

		} catch (final IOException e) {
			throw new MprcException(e);
		}
	}

	private void printLogFile(final HttpServletResponse response, final ServletOutputStream outputStream, final File logFile) throws IOException {
		outputStream.println(String.format("<h3>Log file %s</h3>", logFile.getAbsolutePath()));
		outputStream.println("<log>");

		// Long logs are just dumped
		if (logFile.length() > 5000000) {
			QaReport.streamFileToResponse(response, logFile, false);
		} else {
			final List<String> lines = FileUtilities.readLines(logFile);

			// Establish min and max date
			Date min = new Date();
			Date max = null;
			for (final String line : lines) {
				final Matcher matcher = TIME_STAMP.matcher(line);
				if (matcher.matches()) {
					final String dateTime = matcher.group(1);
					try {
						final Date date = DATE_FORMAT.parse(dateTime);
						if (date.before(min)) {
							min = date;
						}
						if (max == null) {
							max = date;
						} else if (max.after(date)) {
							max = date;
						}
					} catch (final ParseException ignore) {
						// SWALLOWED: We do not care
					}
				}
			}
			final long minTime = min.getTime();
			final long maxTime = max != null ? max.getTime() + 1 : new Date().getTime() + 1;
			long prevTime = minTime;
			long time = minTime;

			for (final String line : lines) {
				final Matcher matcher = TIME_STAMP.matcher(line);
				if (matcher.matches()) {
					final String dateTime = matcher.group(1) + ' ' + matcher.group(2);
					final String row = matcher.group(3);
					Date date = null;
					try {
						date = DATE_FORMAT.parse(dateTime);
						prevTime = time;
						time = date.getTime();
					} catch (final ParseException ignore) {
						// SWALLOWED: We do not care
					}

					final long width = (int) (1000.0 * (prevTime - minTime) / (maxTime - minTime));
					outputStream.println("<row>" +
							"<time title=\"" + dateTime + "\">" + matcher.group(2) + "</time>" +
							row +
							"<prg style=\"width: " + width + "px\"></prg>" +
							"</row>");
				} else {
					outputStream.println("<row>" + line + "</row>");
				}

			}
		}
		outputStream.println("</log>");
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
