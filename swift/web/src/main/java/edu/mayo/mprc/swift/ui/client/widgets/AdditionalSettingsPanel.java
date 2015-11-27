package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import edu.mayo.mprc.swift.ui.client.rpc.ClientSwiftSearchDefinition;

public final class AdditionalSettingsPanel extends HorizontalPanel {
	private final CheckBox publicMgfs;
	private final CheckBox publicMzxmls;
	private final CheckBox publicSearchFiles;
	private final CheckBox fromScratch;
	private final CheckBox lowPriority;
	private final CheckBox mzIdentML;

	private final Label commentLabel;
	private final TextBox comment;

	public AdditionalSettingsPanel() {
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

		fromScratch = new CheckBox("From scratch (no cache)");
		fromScratch.setValue(false);
		add(fromScratch);

		lowPriority = new CheckBox("Low priority");
		lowPriority.setValue(false);
		add(lowPriority);

		mzIdentML = new CheckBox("Provide mzIdentML");
		mzIdentML.setTitle("mzIdentML will be produced by Scaffold even if the saved parameters do not enable this");
		mzIdentML.setValue(false);
		add(mzIdentML);

		commentLabel = new Label("Comment");
		add(commentLabel);

		comment = new TextBox();
		comment.setText("");
		comment.setTitle("Comment associated with the search. Can be used to look up searches quickly");
		add(comment);
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

	public boolean isMzIdentMl() {
		return Boolean.TRUE.equals(mzIdentML.getValue());
	}

	public void setMzIdentMl(final boolean mzIdentMl) {
		this.mzIdentML.setValue(mzIdentMl);
	}

	public void setDefinition(final ClientSwiftSearchDefinition definition) {
		setPublicMgfs(definition.isPublicMgfFiles());
		setPublicMzxmls(definition.isPublicMzxmlFiles());
		setPublicSearchFiles(definition.isPublicSearchFiles());
		setMzIdentMl("1".equals(definition.getMetadata().get("mzIdentMl")));
		final String comment = definition.getMetadata().get("comment");
		setComment(comment == null ? "" : comment);
	}

	public void setComment(final String comment) {
		this.comment.setText(comment);
	}

	public String getComment() {
		return this.comment.getText().trim();
	}
}
