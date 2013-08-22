package edu.mayo.mprc.swift.ui.server;

import edu.mayo.mprc.MprcException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Roman Zenka
 */
public final class ServiceImplTest {
	@Test(expectedExceptions = MprcException.class)
	public void shouldFailTitle() {
		ServiceImpl.validateTitleCharacters("%", "Test");
		Assert.fail("Exception should have been thrown");
	}

	@Test
	public void shouldValidateTitle() {
		ServiceImpl.validateTitleCharacters("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-+._()[]{}=#", "test");
	}

}
