package edu.mayo.mprc.swift;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Roman Zenka
 */
public final class AsgardHandlerInterceptor extends HandlerInterceptorAdapter {
	private final static String ASGARD_PREFIX = "http://asgard";
	public static final String ASGARD_HANDLED = "asgardHandled";

	public AsgardHandlerInterceptor() {
		int i = 0;
	}

	@Override
	public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) throws Exception {
		if (request.getRequestURL().toString().startsWith(ASGARD_PREFIX) && request.getAttribute(ASGARD_HANDLED) == null) {
			request.setAttribute(ASGARD_HANDLED, true);
			if (request.getRequestURL().toString().contains("images")) {
				return super.preHandle(request, response, handler);
			} else {
				request.getRequestDispatcher("/dashboard").forward(request, response);
			}
			return false;
		} else {
			return super.preHandle(request, response, handler);
		}
	}
}
