package edu.mayo.mprc.swift;

import com.google.common.io.ByteStreams;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;

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
			String path = request.getServletPath();
			if ("/".equals(path)) {
				path = "index.html";
			}
			ByteStreams.copy(new FileInputStream(new File("\\\\mfad\\rchapp\\odin\\prod\\apps\\asgard", request.getServletPath())), response.getOutputStream());
			return false;
		} else {
			return super.preHandle(request, response, handler);
		}
	}
}
