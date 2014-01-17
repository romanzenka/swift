package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import edu.mayo.mprc.swift.ui.client.rpc.ClientSwiftSearchDefinition;

public final class AdditionalSettingsPanel extends HorizontalPanel {
	private final CheckBox publicMgfs;
	private final CheckBox publicMzxmls;
	private final CheckBox publicSearchFiles;
	private final CheckBox qualityControl;
	private final CheckBox fromScratch;
	private final CheckBox lowPriority;

	/**
	 * Null Constructor
	 */
	public AdditionalSettingsPanel() {
		this(false, false, false, false);
	}

	/**
	 * Panel constructor.
	 *
	 * @param publicMgfs        True if .mgfs should be made public.
	 * @param publicMzxmls      True if .mzxmls should be made public.
	 * @param publicSearchFiles True if search files should be made public.
	 */
	public AdditionalSettingsPanel(final boolean publicMgfs, final boolean publicMzxmls, final boolean publicSearchFiles, final boolean qualityControl) {
		this.publicMgfs = new CheckBox("Provide .mgf");
		this.publicMgfs.setTitle("Place converted .mgf files in the output directory of your project");
		this.publicMgfs.setValue(publicMgfs);
		add(this.publicMgfs);

		this.publicMzxmls = new CheckBox("Provide .mzxml");
		this.publicMzxmls.setTitle("Place converted .mzxml files in the output directory of your project");
		this.publicMzxmls.setValue(publicMzxmls);
		add(this.publicMzxmls);

		this.publicSearchFiles = new CheckBox("Provide intermediates");
		this.publicSearchFiles.setTitle("Provide intermediate files (search engine results) in the output directory of your project");
		this.publicSearchFiles.setValue(publicSearchFiles);
		add(this.publicSearchFiles);

		this.qualityControl = new CheckBox("QC");
		this.qualityControl.setTitle("Run Quality Control assessment (using quameter)");
		this.qualityControl.setValue(qualityControl);
		add(this.publicSearchFiles);

		fromScratch = new CheckBox("From scratch (no cache)");
		fromScratch.setValue(false);
		add(fromScratch);

		lowPriority = new CheckBox("Low priority");
		lowPriority.setValue(false);
		add(lowPriority);
	}

	/**
	 * @param publicMgfs True if .mgf files should be made public (not kept in cache).
	 */
	public void setPublicMgfs(final boolean publicMgfs) {
		this.publicMgfs.setValue(publicMgfs);
	}

	/**
	 * @return True if .mgf file should be made public (not kept in cache).
	 */
	public boolean isPublicMgfs() {
		return Boolean.TRUE.equals(publicMgfs.getValue());
	}

	public void setPublicMzxmls(final boolean publicMzxmls) {
		this.publicMzxmls.setValue(publicMzxmls);
	}

	public boolean isPublicMzxmls() {
		return Boolean.TRUE.equals(publicMzxmls.getValue());
	}

	public void setPublicSearchFiles(final boolean publicSearchFiles) {
		this.publicSearchFiles.setValue(publicSearchFiles);
	}

	public boolean isPublicSearchFiles() {
		return Boolean.TRUE.equals(publicSearchFiles.getValue());
	}

	public void setQualityControl(final boolean qualityControl) {
		this.qualityControl.setValue(qualityControl);
	}

	public boolean isQualityControl() {
		return Boolean.TRUE.equals(qualityControl.getValue());
	}

	public void setFromScratch(final boolean fromScratch) {
		this.fromScratch.setValue(fromScratch);
	}

	public boolean isFromScratch() {
		return Boolean.TRUE.equals(fromScratch.getValue());
	}

	public void setLowPriority(final boolean lowPriority) {
		this.lowPriority.setValue(lowPriority);
	}

	public boolean isLowPriority() {
		return Boolean.TRUE.equals(lowPriority.getValue());
	}

	public void setDefinition(final ClientSwiftSearchDefinition definition) {
		setPublicMgfs(definition.isPublicMgfFiles());
		setPublicMzxmls(definition.isPublicMzxmlFiles());
		setPublicSearchFiles(definition.isPublicSearchFiles());
		setQualityControl(definition.isQualityControl());
	}
}
