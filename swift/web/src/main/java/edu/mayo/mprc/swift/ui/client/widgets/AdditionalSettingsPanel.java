package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import edu.mayo.mprc.swift.ui.client.rpc.ClientSearchEngine;
import edu.mayo.mprc.swift.ui.client.rpc.ClientSwiftSearchDefinition;

import java.util.List;

public final class AdditionalSettingsPanel extends HorizontalPanel {
	private final CheckBox publicMgfs;
	private final CheckBox publicMzxmls;
	private final CheckBox publicSearchFiles;
	private final CheckBox qualityControl;
	private final CheckBox fromScratch;
	private final CheckBox lowPriority;

	public AdditionalSettingsPanel(final List<ClientSearchEngine> searchEngineList) {
		publicMgfs = new CheckBox("Provide .mgf");
		publicMgfs.setTitle("Place converted .mgf files in the output directory of your project");
		publicMgfs.setValue(false);
		add(publicMgfs);

		publicMzxmls = new CheckBox("Provide .mzxml");
		publicMzxmls.setTitle("Place converted .mzxml files in the output directory of your project");
		publicMzxmls.setValue(false);
		add(publicMzxmls);

		publicSearchFiles = new CheckBox("Provide intermediates");
		publicSearchFiles.setTitle("Provide intermediate files (search engine results) in the output directory of your project");
		publicSearchFiles.setValue(false);
		add(publicSearchFiles);

		qualityControl = new CheckBox("QC");
		qualityControl.setTitle("Run Quality Control assessment (using quameter)");
		qualityControl.setValue(false);
		qualityControl.setVisible(isQualityControlAvailable(searchEngineList));
		add(qualityControl);

		fromScratch = new CheckBox("From scratch (no cache)");
		fromScratch.setValue(false);
		add(fromScratch);

		lowPriority = new CheckBox("Low priority");
		lowPriority.setValue(false);
		add(lowPriority);
	}

	/**
	 * Quality control switch is only displayed if we have myrimatch and idpqonvert.
	 *
	 * @param engines List of all present engines
	 * @return True if we can run QC
	 */
	static boolean isQualityControlAvailable(List<ClientSearchEngine> engines) {
		boolean idpQonvert = false;
		boolean myriMatch = false;
		for (final ClientSearchEngine engine : engines) {
			final String code = engine.getEngineConfig().getCode();
			if ("IDPQONVERT".equals(code)) {
				idpQonvert = true;
			} else if ("MYRIMATCH".equals(code)) {
				myriMatch = true;
			}
		}
		return myriMatch && idpQonvert;
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
