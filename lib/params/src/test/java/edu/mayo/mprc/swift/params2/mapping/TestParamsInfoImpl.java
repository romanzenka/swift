package edu.mayo.mprc.swift.params2.mapping;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.dbcurator.model.Curation;
import edu.mayo.mprc.dbcurator.model.CurationDao;
import edu.mayo.mprc.dbcurator.model.CurationStep;
import edu.mayo.mprc.dbcurator.model.curationsteps.ManualInclusionStep;
import edu.mayo.mprc.swift.params2.ParamsDao;
import edu.mayo.mprc.unimod.UnimodDao;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Roman Zenka
 */
public final class TestParamsInfoImpl {
	private ParamsInfoImpl impl;
	private boolean inSession;

	@BeforeTest
	public void setup() {
		final CurationDao mock = mock(CurationDao.class);
		final Curation curation = mock(Curation.class);
		when(curation.copyFull()).thenCallRealMethod();
		when(curation.hasBeenRun()).thenReturn(true);
		when(curation.getCurationSteps()).thenAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
				if (inSession) {
					final List<CurationStep> result = new ArrayList<CurationStep>(1);
					result.add(new ManualInclusionStep());
					return result;
				} else {
					throw new MprcException("The lazy-loaded list of curation steps is accessed outside of session");
				}
			}
		});

		final List<Curation> curations = new ArrayList<Curation>();
		curations.add(curation);
		when(mock.getAllCurations()).thenReturn(curations);
		impl = new ParamsInfoImpl(
				mock,
				mock(UnimodDao.class),
				mock(ParamsDao.class));
	}

	/**
	 * Getting the list of database allowed values has to produce a list that is useable outside of
	 * Hibernate session.
	 */
	@Test
	public void shouldGetDbAllowedValues() {
		// Simulate database session
		inSession = true;
		final List<Curation> databases = impl.getDatabaseAllowedValues();
		inSession = false;

		final Curation db = databases.get(0);

		Assert.assertNotNull(db.getCurationSteps(), "The step must be available independently on Hibernate lazy loading");
	}
}
