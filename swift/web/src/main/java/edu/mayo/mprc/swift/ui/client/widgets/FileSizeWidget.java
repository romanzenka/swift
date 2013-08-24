package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

public final class FileSizeWidget extends FlowPanel {
	private long fileSize;
	private Label text;
	private Label bar;
	private static final String[] units = {"B", "K", "M", "G", "T", "P", "E", "Z", "Y"};

	/**
	 * @param fileSize Size of the file. When negative, it means the file does not exist.
	 */
	public FileSizeWidget(final long fileSize) {
		addStyleName("file-size");
		if (fileSize >= 0) {
			this.fileSize = fileSize;
			text = new Label(sizeToText(this.fileSize));
			text.addStyleName("file-size-text");
			bar = new Label();
			bar.addStyleName("file-size-bar");
			bar.setWidth("0");
			add(bar);
		} else {
			this.fileSize = 0;
			text = new Label("missing");
			text.addStyleName("file-size-text-missing");
		}
		add(text);
	}

	public void setMaxSize(final long maxSize) {
		if (bar != null) {
			if (maxSize <= 0) {
				return;
			}
			final double percent = 100.0 * (double) fileSize / (double) maxSize;
			bar.setWidth(String.valueOf((int) (percent + 0.5)) + '%');
		}
	}

	public long getFileSize() {
		return fileSize;
	}

	private static String sizeToText(final long fileSize) {
		double size = fileSize;
		int unit = 0;
		while (size >= 1024) {
			unit++;
			size /= 1024;
		}

		return NumberFormat.getDecimalFormat().format(size) + units[unit];
	}
}

