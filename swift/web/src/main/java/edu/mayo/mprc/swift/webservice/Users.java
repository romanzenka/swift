package edu.mayo.mprc.swift.webservice;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.workspace.User;
import edu.mayo.mprc.workspace.WorkspaceDao;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman Zenka
 */
@Controller
public final class Users {
	@Resource(name="workspaceDao")
	private WorkspaceDao workspaceDao;

	public Users() {
	}

	@RequestMapping(value="/users", method = RequestMethod.GET)
	public ModelAndView listUsers() {
		try {
			getWorkspaceDao().begin();
			final List<User> users = getWorkspaceDao().getUsers();
			final ArrayList<edu.mayo.mprc.swift.webservice.User> result =
					new ArrayList<edu.mayo.mprc.swift.webservice.User>(users.size());
			for(User user : users) {
				result.add(new edu.mayo.mprc.swift.webservice.User(user));
			}

			getWorkspaceDao().commit();
			ModelAndView modelAndView=new ModelAndView();
			modelAndView.addObject("users", result);
			return modelAndView;
		} catch (Exception t) {
			getWorkspaceDao().rollback();
			throw new MprcException("Could not list users", t);
		}
	}

	public WorkspaceDao getWorkspaceDao() {
		return workspaceDao;
	}

	public void setWorkspaceDao(WorkspaceDao workspaceDao) {
		this.workspaceDao = workspaceDao;
	}
}
