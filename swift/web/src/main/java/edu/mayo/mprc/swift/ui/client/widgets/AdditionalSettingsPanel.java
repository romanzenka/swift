package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;

public class AdditionalSettingsPanel extends HorizontalPanel {
	private final CheckBox publicMgfs;
	private final CheckBox publicSearchFiles;
	private final CheckBox fromScratch;
	private final CheckBox lowPriority;

	/**
	 * Null Constructor
	 */
	public AdditionalSettingsPanel() {
		this(false, false);
	}

	/**
	 * Panel constructor.
	 *
	 * @param publicMgfs        True if .mgfs should be made public.
	 * @param publicSearchFiles True if search files should be made public.
	 */
	public AdditionalSettingsPanel(final boolean publicMgfs, final boolean publicSearchFiles) {
		this.publicMgfs = new CheckBox("Provide .mgf files");
		this.publicMgfs.setChecked(publicMgfs);
		add(this.publicMgfs);

		this.publicSearchFiles = new CheckBox("Provide intermediate search results");
		this.publicSearchFiles.setChecked(publicSearchFiles);
		add(this.publicSearchFiles);

		fromScratch = new CheckBox("From scratch (no cache)");
		fromScratch.setChecked(false);
		add(fromScratch);

		lowPriority = new CheckBox("Low priority");
		lowPriority.setChecked(false);
		add(lowPriority);
	}

	/**
	 * @param publicMgfs True if .mgf files should be made public (not kept in cache).
	 */
	public void setPublicMgfs(final boolean publicMgfs) {
		this.publicMgfs.setChecked(publicMgfs);
	}

	/**
	 * @return True if .mgf file should be made public (not kept in cache).
	 */
	public boolean isPublicMgfs() {
		return publicMgfs.isChecked();
	}

	public void setPublicSearchFiles(final boolean publicSearchFiles) {
		this.publicSearchFiles.setChecked(publicSearchFiles);
	}

	public boolean isPublicSearchFiles() {
		return publicSearchFiles.isChecked();
	}

	public void setFromScratch(final boolean fromScratch) {
		this.fromScratch.setChecked(fromScratch);
	}

	public boolean isFromScratch() {
		return fromScratch.isChecked();
	}

	public void setLowPriority(final boolean lowPriority) {
		this.lowPriority.setChecked(lowPriority);
	}

	public boolean isLowPriority() {
		return lowPriority.isChecked();
	}
}
