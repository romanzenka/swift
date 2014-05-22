package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import edu.mayo.mprc.swift.ui.client.rpc.ClientSearchEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Roman Zenka
 */
public final class EngineVersionSelector extends HorizontalPanel implements Comparable<EngineVersionSelector> {
	private final String code;
	private final CheckBox checkBox;
	private ListBox versions;
	private final List<String> versionList;
	private final int order;

	public EngineVersionSelector(final ClientSearchEngine engine) {
		code = engine.getEngineConfig().getCode();
		order = engine.getOrder();
		checkBox = new CheckBox(engine.getFriendlyName());
		checkBox.setValue(engine.isOnByDefault());
		versionList = new ArrayList<String>(2);
		versionList.add(engine.getEngineConfig().getVersion());
		setStyleName("engine-version-selector", true);
	}

	public void addEngine(final ClientSearchEngine engine) {
		final String code = engine.getEngineConfig().getCode();
		if (!this.code.equals(code)) {
			throw new RuntimeException("Adding engine with wrong code " + code + " to selector " + this.code + " - a programmer error");
		}
		versionList.add(engine.getEngineConfig().getVersion());
	}

	/**
	 * Call after last engine was added.
	 */
	public void done() {
		if (versionList.size() > 1) {
			versions = new ListBox(false);
			Collections.sort(versionList);
			for (final String version : versionList) {
				versions.addItem(version);
			}
			versions.setSelectedIndex(versions.getItemCount() - 1);
			add(checkBox);
			add(versions);
		} else {
			checkBox.setText(checkBox.getText() + " " + versionList.get(0));
			add(checkBox);
		}
	}

	/**
	 * Select a particular version. If this version is not offered, select the one that is 'closest'
	 * by ordering the required versions and the new one and picking the closest highest one.
	 *
	 * @param version Version to select.
	 * @return False if the version did not exist in the list.
	 */
	public boolean selectVersion(final String version) {
		if (versions == null) {
			return version.equals(versionList.get(0));
		} else {
			int index = 0;
			for (final String existing : versionList) {
				final int comparison = existing.compareTo(version);
				if (comparison == 0) {
					versions.setSelectedIndex(index);
					return true;
				}
				if (comparison > 0) {
					versions.setSelectedIndex(index);
					return false;
				}
				index++;
			}
			versions.setSelectedIndex(versionList.size() - 1);
			return false;
		}
	}

	public boolean isChecked() {
		return Boolean.TRUE.equals(checkBox.getValue());
	}

	public void setChecked(boolean checked) {
		checkBox.setValue(checked);
	}

	public String getVersion() {
		if (versions == null) {
			return versionList.get(0);
		} else {
			return versions.getValue(versions.getSelectedIndex());
		}
	}

	@Override
	public int compareTo(EngineVersionSelector o) {
		return order < o.order ? -1 : (order == o.order ? 0 : 1);
	}

	public String getCode() {
		return code;
	}

	public void focus() {
		checkBox.setFocus(true);
	}

	public void setEnabled(final boolean enabled) {
		checkBox.setEnabled(enabled);
		if (versions != null) {
			versions.setEnabled(enabled);
		}
	}
}
