package edu.mayo.mprc.unimod;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.database.Change;
import edu.mayo.mprc.utilities.ResourceUtilities;

import java.util.Map;

/**
 * Loads unimod from a default .xml file.
 */
public final class MockUnimodDao implements UnimodDao {
	private Unimod unimod;

	@Override
	public Unimod load() {
		if (unimod == null) {
			unimod = new Unimod();
			try {
				unimod.parseUnimodXML(ResourceUtilities.getStream("classpath:edu/mayo/mprc/unimod/unimod.xml", Unimod.class));
			} catch (Exception t) {
				throw new MprcException("Could not obtain default unimod database", t);
			}
		}
		return unimod;
	}

	@Override
	public UnimodUpgrade upgrade(final Unimod unimod, final Change request) {
		throw new MprcException("Upgrading the unimod database is not supported with the mock implementation");
	}

	@Override
	public Unimod getDefaultUnimod() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void begin() {
	}

	@Override
	public void commit() {
	}

	@Override
	public void rollback() {
	}

	@Override
	public String check(final Map<String, String> params) {
		return null;
	}

	@Override
	public void initialize(final Map<String, String> params) {
	}
}
