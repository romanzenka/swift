package edu.mayo.mprc.swift.report.test;

import edu.mayo.mprc.swift.db.LogInfo;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.TaskData;
import edu.mayo.mprc.swift.dbmapping.TaskStateData;
import edu.mayo.mprc.swift.report.JsonWriter;
import edu.mayo.mprc.swift.report.ReportInfo;
import edu.mayo.mprc.workflow.persistence.TaskState;
import edu.mayo.mprc.workspace.User;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author Roman Zenka
 */
public final class JsonCommunicationTest {

	private ByteArrayOutputStream outputStream;
	private JsonWriter out;
	private SearchRun searchRun;
	private static final String SEARCH_RUN_JSON =
			"{\"_\":0, \"id\":null, \"title\":\"Transaction title\", \"user\":\"Roman Zenka\", " +
					"\"submitted\":\"5/6/05 10:22 AM\", \"duration\":\"397 days, 13:11:11\", \"subtasks\":123, " +
					"\"search\":0, " +
					"\"errormsg\":\"Transaction error message\", " +
					"\"ok\":13, \"failures\":14, \"warnings\":15, \"running\":3, " +
					"\"results\":[{\"reportId\":34, \"path\":\"/test1.sf3\", \"analysis\":1}, {\"reportId\":35, \"path\":\"/test2.sf3\", \"analysis\":0}], " +
					"\"details\":{\"total\":123}}";

	private TaskData status;
	private static final String STATUS_JSON =
			"{\"_\":0, \"taskid\":123, \"title\":\"'Mascot Search'\", \"status\":\"Completed Successfully\", \"time\":\"Actual time: 0:00:01 after 0:00:02 spent in queue\", \"queuestamp\":1182963939000, \"startstamp\":1182963941000, \"endstamp\":1182963942000, \"errormsg\":\"error\\nmessage\", \"warningmsg\":\"warning message\", \"jobid\":\"12345\", \"host\":\"foo\", \"percentDone\":null, " +
					"\"logs\":[{\"type\":\"" + LogInfo.STD_OUT_LOG_TYPE + "\", \"longname\":\"<file>shared:/out.txt</file>\"}, {\"type\":\"" + LogInfo.STD_ERR_LOG_TYPE + "\", \"longname\":\"<file>shared:/err.txt</file>\"}]}";

	@BeforeMethod
	public void init() {
		this.outputStream = new ByteArrayOutputStream();
		final PrintWriter out = new PrintWriter(outputStream);
		this.out = new JsonWriter(out);

		setUpSearchRun();
		setUpTaskData();
	}

	private void setUpTaskData() {
		this.status = new TaskData();
		status.setId(123);
		status.setTaskState(new TaskStateData(TaskState.COMPLETED_SUCCESFULLY.getText()));
		status.setDescriptionLong("'Mascot Search'");
		status.setQueueTimestamp(new Date(1182963939000L));
		status.setStartTimestamp(new Date(1182963941000L));
		status.setEndTimestamp(new Date(1182963942000L));
		status.setExceptionString("exception text");
		status.setErrorMessage("error\nmessage");
		status.setWarningMessage("warning message");
		status.setGridJobId("12345");
		status.setHostString("foo");
		status.setOutputLogDatabaseToken("shared:/out.txt");
		status.setErrorLogDatabaseToken("shared:/err.txt");
	}

	private void setUpSearchRun() {
		searchRun = new SearchRun();

		final Date startTimestamp;
		{
			final Calendar start = Calendar.getInstance();
			start.set(2005, 4, 6, 10, 22, 34);
			startTimestamp = start.getTime();
		}

		final Date endTimestamp;
		{
			final Calendar end = Calendar.getInstance();
			end.set(2006, 5, 7, 23, 33, 45);
			endTimestamp = end.getTime();
		}

		searchRun.setEndTimestamp(endTimestamp);
		searchRun.setErrorCode(13);
		searchRun.setErrorMessage("Problem with m_transaction");
		searchRun.setNumTasks(123);

		searchRun.setStartTimestamp(startTimestamp);

		final User user = new User();
		user.setFirstName("Roman");
		user.setLastName("Zenka");
		user.setUserName("Zenka.Roman@mayo.edu");
		user.setUserPassword("password");
		searchRun.setSubmittingUser(user);

		searchRun.setTasksCompleted(13);
		searchRun.setTasksFailed(14);
		searchRun.setTasksWithWarning(15);
		searchRun.setTitle("Transaction title");
		searchRun.setErrorMessage("Transaction error message");
		searchRun.setErrorCode(12321);
	}

	@AfterMethod
	public void cleanup() {
		out.close();
		out = null;
		outputStream = null;
	}

	@Test(enabled = true, groups = {"fast", "unit"}, sequential = true)
	public void testNothing() {
		out.close();
		Assert.assertEquals(outputStream.toString(), JsonWriter.TARGET + ".fireOnChange();", "No request at all");
	}

	@Test(enabled = true, groups = {"fast", "unit"}, dependsOnMethods = {"testNothing"}, sequential = true)
	public void testClearAll() {
		out.clearAll();
		out.close();
		Assert.assertEquals(outputStream.toString(), JsonWriter.TARGET + ".clearAll();" + JsonWriter.TARGET + ".fireOnChange();", "Clear all");
	}

	@Test(enabled = true, groups = {"fast", "unit"}, dependsOnMethods = {"testClearAll"}, sequential = true)
	public void testInsertSearchRun() {
		out.processSearchRun(0, searchRun, 3, getReportInfos(), "insert");
		out.close();
		Assert.assertEquals(outputStream.toString(), JsonWriter.TARGET + ".insert([" + SEARCH_RUN_JSON + "]);" + JsonWriter.TARGET + ".fireOnChange();", "Insert transaction");
	}

	private List<ReportInfo> getReportInfos() {
		final List<ReportInfo> reportInfos = new ArrayList<ReportInfo>(2);
		reportInfos.add(new ReportInfo(34, "/test1.sf3", true));
		reportInfos.add(new ReportInfo(35, "/test2.sf3", false));
		return reportInfos;
	}

	@Test(enabled = true, groups = {"fast", "unit"}, dependsOnMethods = {"testInsertSearchRun"}, sequential = true)
	public void testInsertClearInsert() {
		out.processSearchRun(0, searchRun, 3, getReportInfos(), "insert");
		out.clearAll();
		out.processSearchRun(0, searchRun, 3, getReportInfos(), "insert");
		out.close();
		Assert.assertEquals(outputStream.toString(),
				JsonWriter.TARGET + ".insert([" + SEARCH_RUN_JSON + "]);" +
						JsonWriter.TARGET + ".clearAll();" +
						JsonWriter.TARGET + ".insert([" + SEARCH_RUN_JSON + "]);" + JsonWriter.TARGET + ".fireOnChange();", "Insert clear insert sequence");
	}

	@Test(enabled = true, groups = {"fast", "unit"}, dependsOnMethods = {"testInsertClearInsert"}, sequential = true)
	public void testDoubleInsert() {
		out.processSearchRun(0, this.searchRun, 3, getReportInfos(), "insert");
		out.processSearchRun(0, this.searchRun, 3, getReportInfos(), "insert");
		out.close();
		Assert.assertEquals(outputStream.toString(),
				JsonWriter.TARGET + ".insert([" + SEARCH_RUN_JSON + ", " + SEARCH_RUN_JSON + "]);" + JsonWriter.TARGET + ".fireOnChange();", "Double insert");
	}

	@Test(enabled = true, groups = {"fast", "unit"}, dependsOnMethods = {"testDoubleInsert"}, sequential = true)
	public void testTaskData() {
		final StringBuilder builder = new StringBuilder();
		JsonWriter.appendTaskDataJson(builder, 0, this.status);
		Assert.assertEquals(builder.toString(), STATUS_JSON, "Task status");
	}

}
