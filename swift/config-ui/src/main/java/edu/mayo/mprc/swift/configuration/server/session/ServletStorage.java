package edu.mayo.mprc.swift.configuration.server.session;

import javax.servlet.Servlet;

/**
 * @author Roman Zenka
 */
public final class ServletStorage implements SessionStorage {
	private Servlet servlet;

	public ServletStorage(final Servlet servlet) {
		this.servlet = servlet;
	}

	@Override
	public Object get(final String key) {
		return servlet.getServletConfig().getServletContext().getAttribute(key);
	}

	@Override
	public void put(final String key, final Object object) {
		servlet.getServletConfig().getServletContext().setAttribute(key, object);
	}
}
