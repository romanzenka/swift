package edu.mayo.mprc.swift.ui.server;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.swift.dbmapping.PeptideReport;
import edu.mayo.mprc.swift.dbmapping.SpectrumQa;
import edu.mayo.mprc.swift.params2.Instrument;
import edu.mayo.mprc.swift.ui.client.rpc.ClientInstrument;
import edu.mayo.mprc.swift.ui.client.rpc.ClientPeptideReport;
import edu.mayo.mprc.swift.ui.client.rpc.ClientSpectrumQa;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public final class ClientProxyGeneratorTest {

	private ClientProxyGenerator generator;

	@BeforeClass
	public void setup() {
		generator = new ClientProxyGenerator(null, null, null, null);
	}

	@Test
	public void shouldConvertInstrumentToClient() {
		final Instrument orbi = Instrument.ORBITRAP;
		final ClientInstrument clientInstrument = generator.convertTo(orbi);
		final ClientInstrument expected = new ClientInstrument(orbi.getName());
		Assert.assertEquals(clientInstrument, expected);
	}

	@Test
	public void shouldConvertInstrumentFromClient() {
		final Instrument orbi = Instrument.ORBITRAP;
		final Instrument instrument = generator.convertFrom(new ClientInstrument(orbi.getName()), Instrument.getInitial());
		Assert.assertEquals(instrument, orbi);
	}

	@Test(expectedExceptions = MprcException.class)
	public void shouldFailMissingInstrument() {
		generator.convertFrom(new ClientInstrument("Hello"), Instrument.getInitial());
	}

	@Test
	public void shouldConvertSpectrumQaFromClient() {
		final ClientSpectrumQa qa = new ClientSpectrumQa();
		Assert.assertNull(generator.convertFrom(qa));

		final ClientSpectrumQa qaEnabled = new ClientSpectrumQa("test");
		Assert.assertEquals(generator.convertFrom(qaEnabled), new SpectrumQa("test", SpectrumQa.DEFAULT_ENGINE));
	}

	@Test
	public void shouldConvertScaffoldReportFromClient() {
		final ClientPeptideReport report = new ClientPeptideReport(false);
		Assert.assertNull(generator.convertFrom(report));

		final ClientPeptideReport report2 = new ClientPeptideReport(true);
		Assert.assertEquals(generator.convertFrom(report2), new PeptideReport());
	}

}
