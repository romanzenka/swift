package edu.mayo.mprc.swift;

import com.google.common.io.ByteStreams;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.activation.FileTypeMap;
import javax.annotation.Resource;
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

	@Resource(name = "defaultTypeMap")
	private FileTypeMap typeMap;

	public AsgardHandlerInterceptor() {
		int i = 0;
	}

	@Override
	public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) throws Exception {
		if (request.getRequestURL().toString().startsWith(ASGARD_PREFIX) && request.getAttribute(ASGARD_HANDLED) == null) {
			request.setAttribute(ASGARD_HANDLED, true);
			String path = request.getServletPath();
			if ("/".equals(path) || path.isEmpty()) {
				path = "index.html";
			}

			final File file = new File("/odin/prod/apps/asgard", path);
			response.setHeader("content-type", typeMap.getContentType(file));

			ByteStreams.copy(new FileInputStream(file), response.getOutputStream());
			return false;
		} else {
			return super.preHandle(request, response, handler);
		}
	}
}
