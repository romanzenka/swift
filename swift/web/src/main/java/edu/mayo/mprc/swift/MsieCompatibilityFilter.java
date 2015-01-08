package edu.mayo.mprc.swift;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Roman Zenka
 */
public final class MsieCompatibilityFilter implements Filter {
	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws ServletException, IOException {
		if (servletResponse instanceof HttpServletResponse) {
			((HttpServletResponse) servletResponse).addHeader("X-UA-Compatible", "IE=edge");
		}
		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {

	}

}
