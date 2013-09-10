package edu.mayo.mprc.config;

import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;

/**
 * @author Roman Zenka
 */
public final class TestResourceConfigBase {

	public class Config extends ResourceConfigBase {
		public Config() {
		}
	}

	public static class Ui implements ServiceUiFactory {
		@Override
		public void createUI(DaemonConfig daemon, ResourceConfig resource, UiBuilder builder) {
			builder.property("hello", "Hello Property", "Description of the property").required();
		}
	}

	@Test
	public void shouldWriteConfig() {
		Config c = new Config();
		c.put("hello", "world");
		c.put("test", "testing");

		ConfigWriter writer = mock(ConfigWriter.class);
		c.save(writer);

		verify(writer, times(1)).put("hello", "world", "Hello Property");
		verify(writer, times(1)).put("test", "testing", "");
	}
}
