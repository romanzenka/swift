package edu.mayo.mprc.swift.search;

import edu.mayo.mprc.swift.dbmapping.LogData;
import edu.mayo.mprc.swift.dbmapping.TaskData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A map that can for given {@link edu.mayo.mprc.swift.dbmapping.TaskData}
 * map the internal log numbers to the serialized log IDs.
 *
 * @author Roman Zenka
 */
public final class LogMap {
	private final ConcurrentHashMap<TaskData, LogInfo> map = new ConcurrentHashMap<TaskData, LogInfo>(100);

	/**
	 * Task has completed, we no longer need to translate its log ids.
	 *
	 * @param task Task to remove
	 */
	public void removeTask(TaskData task) {
		map.remove(task);
	}

	/**
	 * Add a freshly created LogData object. This object should have been already
	 * saved to the database.
	 */
	public void addLogData(final long id, final LogData data) {
		final LogInfo info = map.putIfAbsent(data.getTask(), new LogInfo());
		info.add(id, data);
	}

	public LogData getLogData(final TaskData task, final long id) {
		final LogInfo logInfo = map.get(task);
		if (logInfo != null) {
			return logInfo.get(id);
		}
		return null;
	}

	private static final class LogInfo {
		private final Map<Long, LogData> map = new ConcurrentHashMap<Long, LogData>(5);

		public void add(final long id, final LogData data) {
			map.put(id, data);
		}

		public LogData get(final long id) {
			return map.get(id);
		}
	}
}
