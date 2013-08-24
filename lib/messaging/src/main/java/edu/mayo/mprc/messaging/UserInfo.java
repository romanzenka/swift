package edu.mayo.mprc.messaging;

import edu.mayo.mprc.MprcException;

import java.net.URI;

final class UserInfo {
	private String userName;
	private String password;

	public UserInfo(final URI uri) {
		final String userInfo = uri.getUserInfo();
		if ((userInfo == null) || userInfo.equals("")) {
			userName = null;
			password = null;
		} else {

			final int index = userInfo.indexOf(':');
			if (index < 0) {
				throw new MprcException("The URI does not contain proper user name:password pair: " + uri.toString());
			}
			userName = userInfo.substring(0, index);
			password = userInfo.substring(index + 1);
		}
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}
}
