package edu.mayo.mprc.swift.controller;

import com.google.common.collect.Lists;
import edu.mayo.mprc.searchdb.dao.SearchDbDao;
import edu.mayo.mprc.swift.db.SwiftDao;
import edu.mayo.mprc.swift.resources.InstrumentSerialNumberMapper;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import edu.mayo.mprc.workspace.User;
import edu.mayo.mprc.workspace.WorkspaceDao;
import org.springframework.ui.ModelMap;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Zenka
 */
public final class TestReport {
	private Report report;

	@BeforeTest
	public void setup() {
		report = new Report();

		final SwiftDao swiftDao = mock(SwiftDao.class);

		final WorkspaceDao workspaceDao = mock(WorkspaceDao.class);
		final User user1 = new User("First", "Last", "firstlast", "pass");
		user1.setId(1);

		final User user2 = new User("2F", "2L", "22", "pass2");
		user2.setId(2);

		final List<User> users = Lists.newArrayList(user1, user2);
		when(workspaceDao.getUsers(false)).thenReturn(users);

		final SearchDbDao searchDbDao = mock(SearchDbDao.class);
		when(searchDbDao.listAllInstrumentSerialNumbers()).thenReturn(Lists.newArrayList("Orbi1", "Orbi2", "Velos1"));

		final InstrumentSerialNumberMapper mapper = mock(InstrumentSerialNumberMapper.class);
		final WebUiHolder holder = mock(WebUiHolder.class);
		when(holder.getInstrumentSerialNumberMapper()).thenReturn(mapper);
		when(mapper.mapInstrumentSerialNumbers("Orbi1")).thenReturn("Orbi");
		when(mapper.mapInstrumentSerialNumbers("Orbi2")).thenReturn("Orbi");
		when(mapper.mapInstrumentSerialNumbers("Velos1")).thenReturn("Velos");

		report.setSwiftDao(swiftDao);
		report.setWorkspaceDao(workspaceDao);
		report.setSearchDbDao(searchDbDao);
		report.setWebUiHolder(holder);
	}

	@Test
	public void shouldSetModel() {
		final ModelMap map = new ModelMap();

		report.report(map);

		final String usersJson = (String) map.get("usersJson");
		Assert.assertEquals(usersJson, "{\"1\":\"First Last\",\"2\":\"2F 2L\"}");

		final String instrumentsJson = (String) map.get("instrumentsJson");
		Assert.assertEquals(instrumentsJson, "{\"Orbi1,Orbi2\":\"Orbi\",\"Velos1\":\"Velos\"}");
	}
}
