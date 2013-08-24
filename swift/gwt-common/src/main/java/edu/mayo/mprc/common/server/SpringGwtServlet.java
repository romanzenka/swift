package edu.mayo.mprc.common.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import edu.mayo.mprc.MprcException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

/**
 * @author Roman Zenka
 */
public abstract class SpringGwtServlet extends RemoteServiceServlet implements HttpRequestHandler,
		ServletContextAware, BeanNameAware {

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getMethod().equalsIgnoreCase("GET")) {
			doGet(request, response);
		} else if (request.getMethod().equalsIgnoreCase("POST")) {
			doPost(request, response);
		}
	}

	@Override
	public void setServletContext(final ServletContext servletContext) {
		try {
			final ServletConfig config = new ServletConfig() {
				@Override
				public String getServletName() {
					return getBeanName();
				}

				@Override
				public ServletContext getServletContext() {
					return servletContext;
				}

				@Override
				public String getInitParameter(String s) {
					return servletContext.getInitParameter(s);
				}

				@Override
				public Enumeration getInitParameterNames() {
					return servletContext.getInitParameterNames();
				}
			};
			init(config);
		} catch (ServletException e) {
			throw new MprcException(e);
		}
	}

	private String beanName;

	@Override
	public void setBeanName(String name) {
		beanName = name;
	}

	public String getBeanName() {
		return beanName;
	}

}
