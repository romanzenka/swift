package edu.mayo.mprc.xtandem;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

public final class TestTandemWorkPacket {

	@Test
	public static void shouldXStreamSerialize() throws IOException {
		final XTandemWorkPacket packet = new XTandemWorkPacket(
				new File("input"),
				"parameters",
				new File("output"),
				new File("database"),
				true,
				"task",
				false);

		final XStream xStream = new XStream(new DomDriver());
		final String xml = xStream.toXML(packet);

		final Object result = xStream.fromXML(xml);
		Assert.assertTrue(packet.equals(result), "Deserialized object must be identical");
	}

}
