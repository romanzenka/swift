package edu.mayo.mprc.quameterdb;

import edu.mayo.mprc.MprcException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * @author Roman Zenka
 */
public final class QuameterDbWorkerTest {
	private QuameterDbWorker.Factory factory = new QuameterDbWorker.Factory();

	@Test
	public void shouldParseInstrumentMap() {
		Map<String, String> map = QuameterDbWorker.Config.parseInstrumentNameMap("{\"01475B\":\"Orbi\", \"Exactive Serie 3093\":\"QE1\", \"Q Exactive Plus 3093\":\"QE1\", \"LTQ30471\":\"LTQ-Velos\"}");
		Assert.assertEquals(map.size(), 4);
		Assert.assertEquals(map.get("LTQ30471"), "LTQ-Velos");
	}

	@Test(expectedExceptions = MprcException.class)
	public void shouldDetectBadJsonObject() {
		try {
			QuameterDbWorker.Config.parseInstrumentNameMap("Hello");
		} catch (MprcException e) {
			String message = "expected a JSON map";
			checkExceptionContains(e, message);
		}
	}

	private void checkExceptionContains(MprcException e, String message) {
		String detailedMessage = MprcException.getDetailedMessage(e);
		Assert.assertTrue(detailedMessage.contains(message), "We expect to see ["+message+"] in ["+detailedMessage+"]");
		throw e;
	}

	@Test(expectedExceptions = MprcException.class)
	public void shouldDetectComplexMap() {
		try {
			QuameterDbWorker.Config.parseInstrumentNameMap("{\"a\":\"b\", \"c\": {\"d\":123}}");
		} catch (MprcException e) {
			checkExceptionContains(e, "not a string");
		}
	}

	@Test(expectedExceptions = MprcException.class)
	public void shouldDetectBrokenRegex() {
		try {
			QuameterDbWorker.Config.parseConfigProteins("{\"a\":\"hello|world\",\"b\":\"wrong(paren\"}");
		} catch(MprcException e) {
			checkExceptionContains(e, "Bad pattern for key [b]");
		}
	}
}
