package edu.mayo.mprc.swift;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Roman Zenka
 */
public final class AsgardHandlerInterceptor extends HandlerInterceptorAdapter {
	private final static String ASGARD_PREFIX = "http://asgard";

	public AsgardHandlerInterceptor() {
		int i = 0;
	}

	@Override
	public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) throws Exception {
		if (request.getRequestURL().toString().startsWith(ASGARD_PREFIX)) {

			request.getRequestDispatcher("/dashboard").forward(request, response);
			return false;
		} else {
			return super.preHandle(request, response, handler);
		}
	}
}
