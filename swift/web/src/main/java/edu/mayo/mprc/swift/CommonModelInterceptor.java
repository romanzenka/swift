package edu.mayo.mprc.swift;

import edu.mayo.mprc.ReleaseInfoCore;
import edu.mayo.mprc.swift.resources.WebUi;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Add common model parameters to all models.
 *
 * @author Roman Zenka
 */
public final class CommonModelInterceptor extends HandlerInterceptorAdapter {
	private WebUiHolder webUiHolder;

	private String getPathPrefix() {
		final String prefix = getWebUi().getFileTokenFactory().fileToDatabaseToken(getWebUi().getBrowseRoot());
		if (!prefix.endsWith("/")) {
			return prefix + "/";
		}
		return prefix;
	}

	@Override
	public void postHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler, final ModelAndView modelAndView) throws Exception {
		if (modelAndView != null) {
			// Adding parameters messes up the XML interface. Skip if XML
			if (!request.getRequestURI().endsWith(".xml")) {
				final ModelMap model = modelAndView.getModelMap();
				model.addAttribute("title", getWebUi().getTitle());
				model.addAttribute("messageDefined", getWebUi().getUserMessage().messageDefined());
				model.addAttribute("userMessage", getWebUi().getUserMessage().getMessage());
				model.addAttribute("pathPrefix", getPathPrefix());
				model.addAttribute("pathWebPrefix", getWebUi().getBrowseWebRoot());
				model.addAttribute("cookiePrefix", getWebUi().getCookiePrefix());
				model.addAttribute("ver", ReleaseInfoCore.buildRevision());
			}
		}
		super.postHandle(request, response, handler, modelAndView);
	}

	public WebUiHolder getWebUiHolder() {
		return webUiHolder;
	}

	@Resource(name = "webUiHolder")
	public void setWebUiHolder(final WebUiHolder webUiHolder) {
		this.webUiHolder = webUiHolder;
	}

	private WebUi getWebUi() {
		return getWebUiHolder().getWebUi();
	}
}
