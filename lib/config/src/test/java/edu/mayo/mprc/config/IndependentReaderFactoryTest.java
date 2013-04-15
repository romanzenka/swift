package edu.mayo.mprc.config;

import edu.mayo.mprc.config.generic.GenericFactory;
import edu.mayo.mprc.config.generic.GenericResource;
import edu.mayo.mprc.utilities.ResourceUtilities;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.Reader;

/**
 * @author Roman Zenka
 */
public final class IndependentReaderFactoryTest {
	@Test
	public void shouldReadConfig() throws IOException {
		final Reader reader = ResourceUtilities.getReader("classpath:edu/mayo/mprc/config/test.conf", IndependentReaderFactoryTest.class);
		final AppConfigReader appReader = new AppConfigReader(reader, new GenericFactory());
		final ApplicationConfig load = appReader.load();
		appReader.close();
		Assert.assertEquals(load.getDaemons().size(), 1);
		final DaemonConfig daemon = load.getDaemons().get(0);
		Assert.assertEquals(
				((GenericResource) daemon.getResources().get(0)).get("title"), "Swift 2.5");
		Assert.assertEquals(
				((GenericResource) daemon.getServices().get(0).getRunner().getWorkerConfiguration())
						.get("mascotUrl"), "http://localhost/mascot/");
	}
}
