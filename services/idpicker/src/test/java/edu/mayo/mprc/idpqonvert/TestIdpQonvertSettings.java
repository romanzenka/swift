package edu.mayo.mprc.idpqonvert;

import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * @author Roman Zenka
 */
public final class TestIdpQonvertSettings {
	@Test
	public static void shouldConvertSettings() {
		final IdpQonvertSettings settings = new IdpQonvertSettings();
		final List<String> parts = settings.toCommandLine();
		final StringBuilder output = new StringBuilder(1000);
		boolean odd = true;
		for (final String part : parts) {
			if (odd) {
				output.append(part.substring(1))
						.append(": ");
			} else {
				output.append('"')
						.append(part)
						.append("\"\n");
			}
			odd = !odd;
		}

		Assert.assertEquals(TestingUtilities.compareStringsByLine(output.toString(), TestingUtilities.resourceToString("edu/mayo/mprc/idpqonvert/idpQonvertDefaults"), true), null);
	}

}
