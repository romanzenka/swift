package edu.mayo.mprc.swift.db;


public final class LogInfo {
	private String type;

	public static final String STD_ERR_LOG_TYPE = "STD_ERROR";
	public static final String STD_OUT_LOG_TYPE = "STD_OUT";

	public LogInfo(final String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setType(final String Type) {
		type = Type;
	}
}
