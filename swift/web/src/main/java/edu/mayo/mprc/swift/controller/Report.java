package edu.mayo.mprc.swift.controller;

import com.google.common.base.Functions;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.google.gson.Gson;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.swift.ReportUtils;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.dbmapping.SearchRun;
import edu.mayo.mprc.swift.dbmapping.TaskData;
import edu.mayo.mprc.swift.resources.InstrumentSerialNumberMapper;
import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import edu.mayo.mprc.utilities.StringUtilities;
import edu.mayo.mprc.workspace.User;
import edu.mayo.mprc.workspace.WorkspaceDao;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Zenka
 */
@Controller
public final class Report {
	private SwiftDao swiftDao;
	private WorkspaceDao workspaceDao;
	private SearchDbDao searchDbDao;
	private WebUiHolder webUiHolder;

	@RequestMapping(value = "/report", method = RequestMethod.GET)
	public String report(final ModelMap model) {
		final InstrumentSerialNumberMapper mapper = getWebUiHolder().getInstrumentSerialNumberMapper();

		getSwiftDao().begin();
		try {
			final List<User> userInfos = getWorkspaceDao().getUsers(false);
			final Map<String, String> users = Maps.newTreeMap();
			for (final User userInfo : userInfos) {
				users.put(
						String.valueOf(userInfo.getId()),
						userInfo.getFirstName() + ' ' + userInfo.getLastName());
			}
			final String usersJson = new Gson().toJson(users);
			model.addAttribute("usersJson", usersJson);

			final List<String> instrumentSerials = getSearchDbDao().listAllInstrumentSerialNumbers();
			final BiMap<String, String> nameToSerial = HashBiMap.create(instrumentSerials.size());
			for (final String instrumentSerial : instrumentSerials) {
				if (Strings.isNullOrEmpty(instrumentSerial)) {
					continue;
				}
				final String instrumentName = mapper.mapInstrumentSerialNumbers(instrumentSerial);
				if (nameToSerial.containsKey(instrumentName)) {
					nameToSerial.put(instrumentName, nameToSerial.get(instrumentName) + ',' + instrumentSerial);
				} else {
					nameToSerial.put(instrumentName, instrumentSerial);
				}
			}

			final Ordering<String> ordering = Ordering.natural().onResultOf(Functions.forMap(nameToSerial.inverse(), null));
			final ImmutableSortedMap<String, String> sortedMap = ImmutableSortedMap.copyOf(nameToSerial.inverse(), ordering);
			final String instrumentsJson = new Gson().toJson(sortedMap);
			model.addAttribute("instrumentsJson", instrumentsJson);

			getSwiftDao().commit();
		} catch (final Exception e) {
			getSwiftDao().rollback();
			throw new MprcException("Could not obtain data for report", e);
		}


		return "report/report";
	}

	@RequestMapping(value = "/taskerror")
	public String taskError(final ModelMap model,
	                        @RequestParam(value = "id", required = false) final Integer taskId,
	                        @RequestParam(value = "tid", required = false) final Integer searchRunId) {

		if (taskId != null) {
			getSwiftDao().begin();
			try {
				final TaskData data = getSwiftDao().getTaskData(taskId);
				final String taskName = ReportUtils.replaceTokensWithHyperlinks(data.getTaskName(),
						getWebUi().getBrowseRoot(),
						getWebUi().getBrowseWebRoot(), getWebUi().getFileTokenFactory());
				model.addAttribute("taskName", taskName);

				final String taskDescription = ReportUtils.replaceTokensWithHyperlinks(data.getDescriptionLong(),
						getWebUi().getBrowseRoot(),
						getWebUi().getBrowseWebRoot(), getWebUi().getFileTokenFactory());
				model.addAttribute("taskDescription", taskDescription);

				final String exception = ReportUtils.newlineToBr(
						ReportUtils.replaceTokensWithHyperlinks(data.getErrorMessage(),
								getWebUi().getBrowseRoot(), getWebUi().getBrowseWebRoot(), getWebUi().getFileTokenFactory()));
				model.addAttribute("errorMessage", exception);

				model.addAttribute("exception", data.getExceptionString());
				getSwiftDao().commit();

				return "report/taskerror";
			} catch (Exception e) {
				getSwiftDao().rollback();
				throw new MprcException(e);
			}
		}

		if (searchRunId != null) {
			getSwiftDao().begin();
			try {
				final SearchRun data = getSwiftDao().getSearchRunForId(searchRunId);
				model.addAttribute("data", data);
				final String exception = ReportUtils.replaceTokensWithHyperlinks(
						StringUtilities.escapeHtml(data.getErrorMessage()),
						getWebUi().getBrowseRoot(),
						getWebUi().getBrowseWebRoot(),
						getWebUi().getFileTokenFactory());

				model.addAttribute("exception", exception);
				getSwiftDao().commit();

				return "report/searchrunerror";
			} catch (Exception e) {
				getSwiftDao().rollback();
				throw new MprcException(e);
			}
		}

		throw new MprcException("Neither task nor transaction ids were specified");

	}

	public SwiftDao getSwiftDao() {
		return swiftDao;
	}

	@Resource(name = "swiftDao")
	public void setSwiftDao(final SwiftDao swiftDao) {
		this.swiftDao = swiftDao;
	}

	public WorkspaceDao getWorkspaceDao() {
		return workspaceDao;
	}

	@Resource(name = "workspaceDao")
	public void setWorkspaceDao(final WorkspaceDao workspaceDao) {
		this.workspaceDao = workspaceDao;
	}

	public SearchDbDao getSearchDbDao() {
		return searchDbDao;
	}

	@Resource(name = "searchDbDao")
	public void setSearchDbDao(final SearchDbDao searchDbDao) {
		this.searchDbDao = searchDbDao;
	}

	public WebUiHolder getWebUiHolder() {
		return webUiHolder;
	}

	@Resource(name = "webUiHolder")
	public void setWebUiHolder(WebUiHolder webUiHolder) {
		this.webUiHolder = webUiHolder;
	}

	private WebUi getWebUi() {
		return getWebUiHolder().getWebUi();
	}
}
