package edu.mayo.mprc.sge;

import com.google.common.collect.Lists;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Roman Zenka
 */
public final class TestGridWorkPacket {
	@Test
	public void shouldProduceToString() {
		GridWorkPacket packet = new GridWorkPacket("application", Lists.newArrayList("-p", "-q"));
		packet.setPriority(-5);
		packet.setQueueName("all.q");
		packet.setMemoryRequirement("123"); // 100 megs
		packet.setNativeSpecification("-j yes");
		packet.setWorkingFolder("working folder/with spc's");

		Assert.assertEquals(packet.toString(), "GridWorkPacket:\n\t" +
				"qsub -q all.q -wd 'working folder/with spc\\'s' -l s_vmem=123M -p -5 -j yes application -p -q");
	}
}
