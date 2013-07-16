package edu.mayo.mprc.messaging;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;

public final class UriParsingTest {
	@Test
	public void shouldParseUserCredentialUris() throws URISyntaxException {
		final URI uri = new URI("vm://user:passwd@localhost");
		final UserInfo info = ServiceFactory.extractJmsUserinfo(uri);
		Assert.assertEquals(info.getUserName(), "user");
		Assert.assertEquals(info.getPassword(), "passwd");
	}

	@Test
	public void shouldNotParseWrappedUserCredentialUris() throws URISyntaxException {
		final URI uri = new URI("failover://(vm://user:passwd@localhost)?a=b");
		final UserInfo info = ServiceFactory.extractJmsUserinfo(uri);
		Assert.assertEquals(info.getUserName(), null);
		Assert.assertEquals(info.getPassword(), null);
	}

	@Test
	public void shouldParseNoUserCredentialUris() throws URISyntaxException {
		final URI uri = new URI("vm://localhost");
		final UserInfo info = ServiceFactory.extractJmsUserinfo(uri);
		Assert.assertEquals(info.getUserName(), null);
		Assert.assertEquals(info.getPassword(), null);
	}


}
