package edu.mayo.mprc.swift.benchmark;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.common.client.StringUtilities;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.TaskData;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.workflow.persistence.TaskState;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Benchmark implements HttpRequestHandler {
	private transient SwiftDao swiftDao;

	public Benchmark() {
	}

	@Override
	public void handleRequest(final HttpServletRequest req, final HttpServletResponse resp) {
		final String idString = req.getParameter("id");
		final int searchId = Integer.parseInt(idString);
		if (idString != null) {
			swiftDao.begin();
			ServletOutputStream outputStream = null;
			try {
				final List<TaskData> taskDataList = swiftDao.getTaskDataList(searchId);
				resp.setContentType("text/csv");
				resp.setHeader("Content-Disposition", "attachment; filename=\"report_" + searchId + ".csv\"");
				outputStream = resp.getOutputStream();
				printTaskTable(outputStream, taskDataList);
				swiftDao.commit();
			} catch (Exception e) {
				swiftDao.rollback();
				throw new MprcException("Could not create the report", e);
			} finally {
				FileUtilities.closeQuietly(outputStream);
			}
		}
	}

	/**
	 * Output the table of tasks. The columns correspond to different task types, below are
	 * times (in seconds) corresponding
	 *
	 * @param outputStream
	 * @param taskDataList
	 * @throws IOException
	 */
	static void printTaskTable(final ServletOutputStream outputStream, final List<TaskData> taskDataList) throws IOException {
		final Map<String, Column> table = makeColumnMap(taskDataList);
		final String[] types = getTaskTypes(table);
		if (types.length > 0) {
			outputStream.println(StringUtilities.join(types, ","));
			printTableData(outputStream, table, types);
		}
	}

	/**
	 * Print the table of all collected data.
	 *
	 * @param outputStream Where to print data to.
	 * @param table        Input data, sorted into columns.
	 * @param types        Order in which to output the task types.
	 * @throws IOException When writing goes wrong.
	 */
	private static void printTableData(final ServletOutputStream outputStream, final Map<String, Column> table, final String[] types) throws IOException {
		int i = 0;
		boolean hasData = true;
		final StringBuilder line = new StringBuilder(types.length * 5);
		while (hasData) {
			hasData = false;
			boolean comma = false;
			for (final String type : types) {
				final String data = table.get(type).getData(i);
				if (!hasData && !"".equals(data)) {
					hasData = true;
				}
				if (comma) {
					line.append(",");
				}
				line.append(data);
				comma = true;
			}
			if (hasData) {
				outputStream.println(line.toString());
				line.setLength(0);
			}
			i++;
		}
	}

	private static String[] getTaskTypes(final Map<String, Column> taskTypes) {
		final String[] types = new String[taskTypes.size()];
		taskTypes.keySet().toArray(types);
		Arrays.sort(types);
		return types;
	}

	private static Map<String, Column> makeColumnMap(final List<TaskData> taskDataList) {
		final Map<String, Column> taskTypes = new HashMap<String, Column>(10);

		// Create list of types
		for (final TaskData data : taskDataList) {
			if (TaskState.COMPLETED_SUCCESFULLY.getText().equals(data.getTaskState().getDescription())) {
				final String key = data.getTaskName();
				Column column = taskTypes.get(key);
				if (column == null) {
					column = new Column(key);
					taskTypes.put(key, column);
				}

				final String value = getValue(data);
				column.addData(value);
			}
		}
		return taskTypes;
	}

	/**
	 * Return value to be reported per task.
	 *
	 * @param data Task information.
	 * @return String value for the table - currently the number of elapsed seconds.
	 */
	private static String getValue(final TaskData data) {
		String value = "";
		if (data.getEndTimestamp() != null && data.getStartTimestamp() != null) {
			value = String.valueOf((data.getEndTimestamp().getTime() - data.getStartTimestamp().getTime()) / 1000.0);
		}
		return value;
	}

	public SwiftDao getSwiftDao() {
		return swiftDao;
	}

	public void setSwiftDao(SwiftDao swiftDao) {
		this.swiftDao = swiftDao;
	}
}
