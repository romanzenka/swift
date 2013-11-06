package edu.mayo.mprc.swift;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.ServletContext;

/**
 * When creating the web application context, use our existing ApplicationContext
 * as its parent.
 * <p/>
 * That way the beans that do not depend on the web portion of Swift will not get initialized twice,
 * leading to issues.
 * <p/>
 * This class is configured in {@code web.xml} and utilized by Spring's DispatcherServlet.
 *
 * @author Roman Zenka
 */
public final class SwiftContextLoaderListener extends ContextLoaderListener {
	public SwiftContextLoaderListener() {
	}

	@Override
	protected ApplicationContext loadParentContext(ServletContext servletContext) {
		return MainFactoryContext.getContext();
	}
}
