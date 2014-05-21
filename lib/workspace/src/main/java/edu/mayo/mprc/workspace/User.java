package edu.mayo.mprc.workspace;

import edu.mayo.mprc.database.EvolvableBase;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class User extends EvolvableBase implements Serializable {
	private static final long serialVersionUID = 20100601L;

	private String firstName;
	private String lastName;
	private String userPassword;
	private String userName;
	private String initials;
	private Map<String, String> preferences = new HashMap<String, String>();

	/**
	 * Binary representation of rights the user is given.
	 */
	private Long rights;

	/**
	 * The "right to not see" parameter editor. Specified negatively, as the default is to see the editor.
	 */
	private static final String PARAMETER_EDITOR_DISABLED = "param.editor.disabled";

	/**
	 * Users with this right can change the output path of their searches.
	 */
	private static final String OUTPUT_PATH_CHANGEABLE = "output.path.changeable";

	public User() {
	}

	public User(final String firstName, final String lastName, final String userName, final String userPassword) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.userName = userName;
		initials = firstName.charAt(0) + "" + lastName.charAt(0);
		this.userPassword = userPassword;
		rights = 0L;
	}

	public User(final String firstName, final String lastName, final String userName, final String initials, final String userPassword) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.userName = userName;
		this.initials = initials;
		this.userPassword = userPassword;
		rights = 0L;
	}

	/**
	 * Copy constructor.
	 */
	public User(final User copyFrom) {
		firstName = copyFrom.firstName;
		lastName = copyFrom.lastName;
		userName = copyFrom.userName;
		userPassword = copyFrom.userPassword;
		rights = copyFrom.rights;
	}

	public void setFirstName(final String firstName) {
		this.firstName = firstName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setLastName(final String lastName) {
		this.lastName = lastName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setInitials(final String initials) {
		this.initials = initials;
	}

	public String getInitials() {
		if (initials != null && !initials.isEmpty()) {
			return initials;
		} else {
			return (firstName.charAt(0) + "" + lastName.charAt(0)).toLowerCase(Locale.ENGLISH);
		}
	}

	public void setUserPassword(final String userPassword) {
		this.userPassword = userPassword;
	}

	public String getUserPassword() {
		return userPassword;
	}

	public void setUserName(final String userName) {
		this.userName = userName;
	}

	public String getUserName() {
		return userName;
	}

	public Map<String, String> getPreferences() {
		return preferences;
	}

	void setPreferences(final Map<String, String> preferences) {
		this.preferences = preferences;
	}

	/**
	 * @param key   Preference key to set.
	 * @param value Preference value to set. When set to {@code null}, the preference is removed.
	 */
	void addPreference(final String key, final String value) {
		if (value == null) {
			preferences.remove(key);
		} else {
			preferences.put(key, value);
		}
	}

	String preferenceValue(final String key) {
		return preferences.get(key);
	}

	/**
	 * @deprecated use the getPreference/setPreference API
	 */
	public Long getRights() {
		if (rights == null) {
			return 0L;
		}
		return rights;
	}

	/**
	 * @deprecated use the getPreference/setPreference API
	 */
	public void setRights(final Long rights) {
		this.rights = rights;
	}

	/**
	 * @return {@code true} if the user can use the parameter editor.
	 */
	public boolean isParameterEditorEnabled() {
		return preferenceValue(PARAMETER_EDITOR_DISABLED) == null;
	}

	public boolean isOutputPathChangeEnabled() {
		return preferenceValue(OUTPUT_PATH_CHANGEABLE) != null;
	}

	/**
	 * By default, the editor is enabled.
	 *
	 * @param enabled True if the user can edit parameter sets.
	 */
	public void setParameterEditorEnabled(final boolean enabled) {
		addPreference(PARAMETER_EDITOR_DISABLED, enabled ? null : "1");
	}

	/**
	 * @param enabled Set to true to enable the users to change the output directory where Swift puts its results.
	 */
	public void setOutputPathChangeEnabled(final boolean enabled) {
		addPreference(OUTPUT_PATH_CHANGEABLE, enabled ? "1" : null);
	}

	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof User)) {
			return false;
		}

		final User that = (User) o;

		if (getFirstName() != null ? !getFirstName().equals(that.getFirstName()) : that.getFirstName() != null) {
			return false;
		}
		if (getLastName() != null ? !getLastName().equals(that.getLastName()) : that.getLastName() != null) {
			return false;
		}
		if (getUserName() != null ? !getUserName().equals(that.getUserName()) : that.getUserName() != null) {
			return false;
		}
		if (getUserPassword() != null ? !getUserPassword().equals(that.getUserPassword()) : that.getUserPassword() != null) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (getFirstName() != null ? getFirstName().hashCode() : 0);
		result = 31 * result + (getLastName() != null ? getLastName().hashCode() : 0);
		result = 31 * result + (getUserPassword() != null ? getUserPassword().hashCode() : 0);
		result = 31 * result + (getUserName() != null ? getUserName().hashCode() : 0);
		return result;
	}

	@Override
	public Criterion getEqualityCriteria() {
		return Restrictions.eq("userName", getUserName());
	}

	public String toString() {
		return MessageFormat.format("{0}: {1} {2} - {3}",
				getId(),
				getFirstName(),
				getLastName(),
				getUserName());
	}
}

