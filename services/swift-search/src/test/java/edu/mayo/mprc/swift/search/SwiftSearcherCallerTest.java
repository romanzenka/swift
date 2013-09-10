package edu.mayo.mprc.swift.search;

import edu.mayo.mprc.MprcException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Roman Zenka
 */
public final class SwiftSearcherCallerTest {
	@Test(expectedExceptions = MprcException.class)
	public void shouldFailTitle() {
		DefaultSwiftSearcherCaller.validateTitleCharacters("%", "Test");
		Assert.fail("Exception should have been thrown");
	}

	@Test
	public void shouldValidateTitle() {
		DefaultSwiftSearcherCaller.validateTitleCharacters("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-+._()[]{}=#", "test");
	}

}
