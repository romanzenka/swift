package edu.mayo.mprc.swift.webservice;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Roman Zenka
 */
@XStreamAlias("user")
public final class User {
	private final int id;
	private final String firstName;
	private final String lastName;
	private final String email;

	public User(final edu.mayo.mprc.workspace.User user) {
		id = user.getId();
		firstName = user.getFirstName();
		lastName = user.getLastName();
		email = user.getUserName();
	}

	public int getId() {
		return id;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getEmail() {
		return email;
	}
}
