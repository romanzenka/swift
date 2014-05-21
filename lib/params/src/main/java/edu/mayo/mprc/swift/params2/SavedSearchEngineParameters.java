package edu.mayo.mprc.swift.params2;

import edu.mayo.mprc.database.EvolvableBase;
import edu.mayo.mprc.workspace.User;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

/**
 * A parameter set that was saved and named by a user.
 */
public class SavedSearchEngineParameters extends EvolvableBase {
	private String name;
	private User user;
	private SearchEngineParameters parameters;

	public SavedSearchEngineParameters() {
	}

	public SavedSearchEngineParameters(final String name, final User user, final SearchEngineParameters parameters) {
		this.name = name;
		this.user = user;
		this.parameters = parameters;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public User getUser() {
		return user;
	}

	public void setUser(final User user) {
		this.user = user;
	}

	public SearchEngineParameters getParameters() {
		return parameters;
	}

	public void setParameters(final SearchEngineParameters parameters) {
		this.parameters = parameters;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SavedSearchEngineParameters)) {
			return false;
		}

		final SavedSearchEngineParameters that = (SavedSearchEngineParameters) o;

		if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) {
			return false;
		}
		if (getParameters() != null ? !getParameters().equals(that.getParameters()) : that.getParameters() != null) {
			return false;
		}
		if (getUser() != null ? !getUser().equals(that.getUser()) : that.getUser() != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = getName() != null ? getName().hashCode() : 0;
		result = 31 * result + (getUser() != null ? getUser().hashCode() : 0);
		result = 31 * result + (getParameters() != null ? getParameters().hashCode() : 0);
		return result;
	}

	@Override
	public Criterion getEqualityCriteria() {
		return Restrictions.eq("name", getName());
	}
}
