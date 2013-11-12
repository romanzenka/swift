package edu.mayo.mprc.swift.resources;

public final class UserMessage {

	private String message;

	public UserMessage() {
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public boolean messageDefined() {
		return message != null && !message.trim().isEmpty();
	}
}
