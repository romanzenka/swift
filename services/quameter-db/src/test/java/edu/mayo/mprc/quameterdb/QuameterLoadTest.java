package edu.mayo.mprc.quameterdb;

import edu.mayo.mprc.utilities.ResourceUtilities;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * @author Roman Zenka
 */
public final class QuameterLoadTest {
	@Test
	public void shouldLoadQuameterFile() {
		final Map<String, Double> map = QuameterDbWorker.loadQuameterResultFile(
				ResourceUtilities.getReader("classpath:edu/mayo/mprc/quameter/quameter.qual.txt", QuameterLoadTest.class)
		);
		Assert.assertEquals(map.get("C-1A"), 0.0);
		Assert.assertEquals(map.get("C-1A"), 0.0);
		Assert.assertEquals(map.get("DS-2A"), 1532.0);
		Assert.assertEquals(map.get("P-1"), 51.9627);
		Assert.assertEquals(map.get("P-3"), 0.0);
	}
}
