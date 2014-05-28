package edu.mayo.mprc.swift.ui.server;

import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.swift.params2.*;
import edu.mayo.mprc.swift.ui.client.rpc.ClientParamSet;
import edu.mayo.mprc.unimod.ModSet;
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
	private ParameterSetCache cache;
	private Map<String, Object> sessionData = new HashMap<String, Object>();

	@BeforeMethod
	public void setup() {
		final HttpSession session = mock(HttpSession.class);
		ParamsDao dao = mock(ParamsDao.class);

		when(session.getAttribute(anyString()))
				.thenAnswer(new Answer<Object>() {
					@Override
					public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
						return sessionData.get((String) invocationOnMock.getArguments()[0]);
					}
				});

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
				Object[] args = invocationOnMock.getArguments();
				sessionData.put((String) args[0], args[1]);
				return null;
			}
		}).when(session).setAttribute(anyString(), anyObject());

		cache = new ParameterSetCache(session, dao);
	}

	@AfterMethod
	public void teardown() {
		sessionData.clear();
	}

	@Test
	public void shouldSupportTemporarySets() {
		ClientParamSet clientParamSet = cache.installTemporary("Hello", "owner@owner.com", "OO", makeParams());
		Assert.assertEquals(clientParamSet.getId(), -1);

		// Two identical sets store as separate entries
		ClientParamSet clientParamSet2 = cache.installTemporary("Hello", "owner@owner.com", "OO", makeParams());
		Assert.assertEquals(clientParamSet2.getId(), -2);

		ClientParamSet match = cache.findMatchingTemporaryParamSet(makeParams());
		Assert.assertEquals(match.getId(), -1);

		SearchEngineParameters params2 = makeParams();
		params2.setInstrument(Instrument.MALDI_TOF_TOF);

		ClientParamSet noMatch = cache.findMatchingTemporaryParamSet(params2);
		Assert.assertNull(noMatch);
	}

	@Test
	public void shouldNotModifyInternalState() {
		SearchEngineParameters params = makeParams();
		ClientParamSet clientParamSet = cache.installTemporary("Hello", "owner@owner.com", "OO", params);
		Assert.assertEquals(clientParamSet.getId(), -1);

		ClientParamSet match1 = cache.findMatchingTemporaryParamSet(makeParams());
		SearchEngineParameters fromCache = cache.getFromCache(match1);

		// Modify parameter set we got out
		fromCache.setInstrument(Instrument.MALDI_TOF_TOF);

		// Find matching parameter set. We must find one!
		ClientParamSet match2 = cache.findMatchingTemporaryParamSet(makeParams());
		Assert.assertEquals(match1, match2);
	}

	private SearchEngineParameters makeParams() {
		SearchEngineParameters searchEngineParameters = new SearchEngineParameters(
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
