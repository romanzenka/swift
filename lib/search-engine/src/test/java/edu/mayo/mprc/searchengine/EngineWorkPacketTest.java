package edu.mayo.mprc.searchengine;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import edu.mayo.mprc.daemon.WorkPacket;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public final class EngineWorkPacketTest {

	@Test
	public static void shouldXStreamSerialize() throws IOException {
		final EngineWorkPacket packet = new TestEngineWorkPacket(
				new File("input"),
				new File("output"),
				new File("search"),
				new File("database"),
				true,
				"task",
				false);

		final XStream xStream = new XStream(new DomDriver());
		final String xml = xStream.toXML(packet);

		final Object result = xStream.fromXML(xml);
		Assert.assertTrue(packet.equals(result), "Deserialized object must be identical");
	}

	private static class TestEngineWorkPacket extends EngineWorkPacket {

		private static final long serialVersionUID = 4029468324506386517L;

		TestEngineWorkPacket(final File inputFile, final File outputFile, final File searchParamsFile, final File databaseFile, final boolean publishResultFiles, final String taskId, final boolean fromScratch) {
			super(inputFile, outputFile, searchParamsFile, databaseFile, publishResultFiles, taskId, fromScratch);
		}

		@Override
		public WorkPacket translateToWorkInProgressPacket(final File wipFolder) {
			return null; //TODO: implement me
		}
	}
}
