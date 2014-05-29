package edu.mayo.mprc.swift.ui.server;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.swift.params2.*;
import edu.mayo.mprc.swift.ui.client.rpc.ClientParamSet;
import edu.mayo.mprc.unimod.ModSet;
import edu.mayo.mprc.workspace.User;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Roman Zenka
 */
public final class ParameterSetCacheTest {
	public static final int PARAMETERS_ID = 93482;
	public static final int SAVED_PARAMETERS_ID = 11233;
	private ParameterSetCache cache;
	private Map<String, Object> sessionData = new HashMap<String, Object>();

	@BeforeMethod
	public void setup() {
		final HttpSession session = mock(HttpSession.class);
		final ParamsDao dao = mock(ParamsDao.class);

		mockDao(dao);
		mockSessionAttributes(session);

		cache = new ParameterSetCache(session, dao);
	}

	private void mockDao(final ParamsDao dao) {
		final SearchEngineParameters parameters = makeParams();
		parameters.setId(PARAMETERS_ID);
		when(dao.getSavedSearchEngineParameters(SAVED_PARAMETERS_ID)).thenReturn(
				new SavedSearchEngineParameters("Saved Parameters",
						new User("First", "Last", "first@last.com", "FL", "dummy"),
						parameters)
		);
	}

	private void mockSessionAttributes(final HttpSession session) {
		when(session.getAttribute(anyString()))
				.thenAnswer(new Answer<Object>() {
					@Override
					public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
						return sessionData.get((String) invocationOnMock.getArguments()[0]);
					}
				});

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
				final Object[] args = invocationOnMock.getArguments();
				sessionData.put((String) args[0], args[1]);
				return null;
			}
		}).when(session).setAttribute(anyString(), anyObject());
	}

	@AfterMethod
	public void teardown() {
		sessionData.clear();
	}

	@Test
	public void shouldSupportTemporarySets() {
		final ClientParamSet clientParamSet = cache.installTemporary("Hello", "owner@owner.com", "OO", makeParams());
		Assert.assertEquals(clientParamSet.getId(), -1);

		// Two identical sets store as separate entries
		final ClientParamSet clientParamSet2 = cache.installTemporary("Hello", "owner@owner.com", "OO", makeParams());
		Assert.assertEquals(clientParamSet2.getId(), -2);

		final ClientParamSet match = cache.findMatchingTemporaryParamSet(makeParams());
		Assert.assertEquals(match.getId(), -1);

		final SearchEngineParameters params2 = makeParams();
		params2.setInstrument(Instrument.MALDI_TOF_TOF);

		final ClientParamSet noMatch = cache.findMatchingTemporaryParamSet(params2);
		Assert.assertNull(noMatch);
	}

	@Test
	public void shouldNotModifyInternalState() {
		final SearchEngineParameters params = makeParams();
		final ClientParamSet clientParamSet = cache.installTemporary("Hello", "owner@owner.com", "OO", params);
		Assert.assertEquals(clientParamSet.getId(), -1);

		final ClientParamSet match1 = cache.findMatchingTemporaryParamSet(makeParams());
		final SearchEngineParameters fromCache = cache.getFromCache(match1);

		// Modify parameter set we got out
		fromCache.setInstrument(Instrument.MALDI_TOF_TOF);
		fromCache.setId(123);

		// Find matching parameter set. We must find one!
		final ClientParamSet match2 = cache.findMatchingTemporaryParamSet(makeParams());
		Assert.assertEquals(match1, match2);

		// Get the cached parameters again. They must not have changed
		final SearchEngineParameters fromCache2 = cache.getFromCache(match1);
		Assert.assertNull(fromCache2.getId(), "Cached parameters must be impervious to changes outside of cache");
	}

	@Test
	public void shouldSupportSavedParams() {
		final ClientParamSet savedSet = new ClientParamSet(SAVED_PARAMETERS_ID,
				"irrelevant", "irrelevant", "IR");
		final SearchEngineParameters fromCache = cache.getFromCache(savedSet);
		Assert.assertNotNull(fromCache);
		Assert.assertNull(fromCache.getId());

		// Now do it again with the hibernate function
		final SearchEngineParameters fromCacheSaved = cache.getFromCacheHibernate(savedSet);
		Assert.assertNotNull(fromCacheSaved);
		Assert.assertEquals(fromCacheSaved.getId(), Integer.valueOf(PARAMETERS_ID), "Must be in hibernate");
	}

	@Test
	public void shouldCloneTemporary() {
		final ClientParamSet set1 = cache.installTemporary("Hello", "owner@owner.com", "OO", makeParams());
		checkSet(set1, "Hello", -1);

		final ClientParamSet set2 = cache.cloneTemporary(set1);
		checkSet(set2, "Copy of Hello", -2);

		final ClientParamSet set3 = cache.cloneTemporary(set1);
		checkSet(set3, "Copy 2 of Hello", -3);

		final ClientParamSet set4 = cache.cloneTemporary(set3);
		checkSet(set4, "Copy 3 of Hello", -4);
	}

	private void checkSet(final ClientParamSet set, final String name, final int id) {
		Assert.assertEquals(set.getName(), name, "Name does not match");
		Assert.assertEquals(set.getId(), id, "ID does not match");
	}

	@Test
	public void shouldRemoveFromCache() {
		final ClientParamSet set1 = cache.installTemporary("Hello", "owner@owner.com", "OO", makeParams());

		Assert.assertNotNull(cache.getFromCache(set1));

		cache.removeFromCache(set1);

		try {
			cache.getFromCache(set1);
			Assert.fail("Should have thrown an exception");
		} catch (MprcException e) {
			Assert.assertTrue(e.getMessage().contains("Cannot find"));
		}
	}

	@Test
	public void shouldUpdateCache() {
		final SearchEngineParameters params = makeParams();
		final ClientParamSet clientParamSet = cache.installTemporary("Hello", "owner@owner.com", "OO", params);
		Assert.assertEquals(clientParamSet.getId(), -1);

		final SearchEngineParameters params2 = makeParams();
		params2.setInstrument(Instrument.MALDI_TOF_TOF);
		Assert.assertFalse(params.equals(params2), "The parameters must be different");

		cache.updateCache(clientParamSet, params2);

		SearchEngineParameters modifiedParameters = cache.getFromCache(clientParamSet);
		Assert.assertEquals(modifiedParameters.getInstrument(), Instrument.MALDI_TOF_TOF);
	}

	private SearchEngineParameters makeParams() {
		final SearchEngineParameters searchEngineParameters = new SearchEngineParameters(
				new Curation(),
				Protease.getTrypsinAllowP(),
				2,
				1,
				new ModSet(),
				new ModSet(),
				new Tolerance(1, MassUnit.Da),
				new Tolerance(0.5, MassUnit.Ppm),
				Instrument.ORBITRAP,
				ExtractMsnSettings.getDefaultExtractMsnSettings(),
				ScaffoldSettings.defaultScaffoldSettings(),
				new EnabledEngines()
		);


		return searchEngineParameters;
	}
}
