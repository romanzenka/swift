package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.SelectionChangeEvent;
import edu.mayo.mprc.swift.ui.client.rpc.ClientFileSearch;
import edu.mayo.mprc.swift.ui.client.rpc.ClientSearchEngineConfig;
import edu.mayo.mprc.swift.ui.client.rpc.files.FileInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Roman Zenka
 */
public final class FileTable extends FlexTable implements HasValueChangeHandlers<Void>, ChangeHandler, SelectionChangeEvent.Handler {
	private static final int SELECT_COLUMN = 0;
	private static final int FILE_COLUMN = 1;
	private static final int SIZE_COLUMN = 2;
	private static final int REMOVE_COLUMN = 3;
	private static final int FILE_COUNT_COLUMN = FILE_COLUMN;
	private static final int SAMPLE_COLUMN = 4;
	private static final int EXPERIMENT_COLUMN = 5;
	private static final int CATEGORY_COLUMN = 6;

	private static final int HEADER_ROW_INDEX = 1;
	private static final int FIRST_DATA_ROW_INDEX = 2;

	private static final String ROW_SELECTED_STYLE = "file-selected";
	private static final String ROW_DESELECTED_STYLE = "file-deselected";
	private static final String REMOVE_IMAGE = "images/delete.gif";

	private String searchTitle = ""; // Current title of the search

	private final FileTableColumn[] staticHeaders = {
			new FileTableColumn(SELECT_COLUMN, "", new CheckBox(), "button-column"),
			new FileTableColumn(FILE_COLUMN, "File", null),
			new FileTableColumn(SIZE_COLUMN, "Size", null),
			new FileTableColumn(REMOVE_COLUMN, "Remove", new PushButton(new Image(REMOVE_IMAGE)), "button-column"),
			new FileTableColumn(SAMPLE_COLUMN, "<img src=\"images/scaffold_column.gif\" style=\"vertical-align: middle;\">&nbsp;Biological Sample", null),
			new FileTableColumn(EXPERIMENT_COLUMN, "<img src=\"images/scaffold_icon.gif\" style=\"vertical-align: middle;\">&nbsp;Experiment", null),
			new FileTableColumn(CATEGORY_COLUMN, "Category", null),
	};

	FileTableColumn[] getHeaders() {
		final FileTableColumn[] headers = new FileTableColumn[staticHeaders.length];
		System.arraycopy(staticHeaders, 0, headers, 0, staticHeaders.length);
		return headers;
	}

	public int getFirstDataRow() {
		return FIRST_DATA_ROW_INDEX;
	}

	public int getHeaderRowIndex() {
		return HEADER_ROW_INDEX;
	}

	@Override
	public void onSelectionChange(final SelectionChangeEvent event) {
		if (searchTypeList.equals(event.getSource())) {
			setSearchType(searchTypeList.getSelectedSearchType());
		}
	}

	@Override
	public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<Void> handler) {
		return addHandler(handler, ValueChangeEvent.getType());
	}

	private static final class MutableInteger {
		MutableInteger(final int i) {
			this.i = i;
		}

		public int i;
	}

	@Override
	public void onChange(final ChangeEvent event) {
		if (searchTypeList.equals(event.getSource())) {
			setSearchType(searchTypeList.getSelectedSearchType());
		}
	}

	/**
	 * A hash map allowing us to translate a widget to its index.
	 * We use {@link MutableInteger} so we can re-index the widgets when
	 * lines are added/removed just by assigning a different index.
	 */
	private Map<Widget, MutableInteger> widgetIndices = new HashMap<Widget, MutableInteger>();

	private SearchTypeList searchTypeList;
	private Label fileCountLabel;

	public FileTable() {
		searchTypeList = new SearchTypeList();
		searchTypeList.addSelectionChangeHandler(this);

		fileCountLabel = new Label("", false);

		getRowFormatter().addStyleName(0, "info-row");
		getCellFormatter().addStyleName(0, FILE_COUNT_COLUMN, "file-count");
		setWidget(0, FILE_COUNT_COLUMN, fileCountLabel);

		getFlexCellFormatter().setColSpan(0, SAMPLE_COLUMN, 2);
		getCellFormatter().addStyleName(0, SAMPLE_COLUMN, "search-type");
		setWidget(0, SAMPLE_COLUMN, searchTypeList);

		final FileTableColumn[] headers = getHeaders();
		for (final FileTableColumn header : headers) {
			header.init(this);
		}

		((CheckBox) headers[SELECT_COLUMN].getWidget()).addClickHandler(new ColumnSelectListener(SELECT_COLUMN, this));
		((PushButton) headers[REMOVE_COLUMN].getWidget()).addClickHandler(new ClickHandler() {
			@Override
			public void onClick(final ClickEvent event) {
				removeSelectedFiles();
			}
		});

		getRowFormatter().setStyleName(getHeaderRowIndex(), "table-header");

		// On every change, update count of selected files
		addValueChangeHandler(new ValueChangeHandler<Void>() {
			@Override
			public void onValueChange(final ValueChangeEvent<Void> event) {
				// List of files changed
				updateFileCount();
				updateMaxCommonPath();
				updateSizeDisplay();
			}
		});
		updateFileCount();
	}

	private void updateFileCount() {
		if (fileCountLabel != null) {
			final int count = getRowCount() - getFirstDataRow();
			fileCountLabel.setText(count + (count == 1 ? " file" : " files"));
		}
	}

	public void addFiles(final FileInfo[] fileInfos) {
		int lastRow = getRowCount();

		for (final FileInfo info : fileInfos) {
			final MutableInteger index = new MutableInteger(lastRow);

			final CheckBox selection = new CheckBox();
			selection.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(final ClickEvent event) {
					final Widget sender = (Widget) event.getSource();
					if (sender instanceof CheckBox) {
						setChecked(getWidgetRow(sender), SELECT_COLUMN, Boolean.TRUE.equals(((CheckBox) sender).getValue()));
					} else {
						throw new RuntimeException("Programmer error, type mismatch");
					}
				}
			});
			setWidget(lastRow, 0, selection);
			widgetIndices.put(selection, index);

			addNewLine(info, index, searchTitle, searchTypeList.getSelectedSearchType());

			lastRow++;
		}

		// fire change event
		ValueChangeEvent.fire(this, null);
	}

	public void setFiles(final List<ClientFileSearch> inputFiles, final SearchType searchType) {
		for (int i = getFirstDataRow(); i < getRowCount(); ) {
			removeTableRow(i);
		}

		searchTypeList.setSelectedSearchType(searchType, false/*This is coming from search load. Do not store the preference*/);
		setSearchType(searchType);

		int lastRow = getRowCount();

		for (final ClientFileSearch fileSearch : inputFiles) {
			final MutableInteger index = new MutableInteger(lastRow);

			final CheckBox selection = new CheckBox();
			selection.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(final ClickEvent event) {
					final Widget sender = (Widget) event.getSource();
					if (sender instanceof CheckBox) {
						setChecked(getWidgetRow(sender), SELECT_COLUMN, Boolean.TRUE.equals(((CheckBox) sender).getValue()));
					} else {
						throw new RuntimeException("Programmer error, type mismatch");
					}
				}
			});
			setWidget(lastRow, 0, selection);
			widgetIndices.put(selection, index);

			addNewLine(
					fileSearch.getPath(),
					fileSearch.getFileSize(),
					index,
					fileSearch.getCategoryName(),
					fileSearch.getExperiment(),
					fileSearch.getBiologicalSample());

			lastRow++;
		}

		ValueChangeEvent.fire(this, null);
	}

	/**
	 * The bars showing relative file size need to be updated when the list of files changes.
	 */
	private void updateSizeDisplay() {
		final long maxSize = getMaxFileSize();
		for (int i = getFirstDataRow(); i < getRowCount(); i++) {
			final FileSizeWidget fileSize = (FileSizeWidget) getWidget(i, SIZE_COLUMN);
			fileSize.setMaxSize(maxSize);
		}
	}

	/**
	 * @param info      Information about the file to add.
	 * @param lineIndex Index of this line.
	 * @param title     of the search
	 * @param type      type of the search (influences default experiment and sample names)
	 */
	private void addNewLine(final FileInfo info, final MutableInteger lineIndex, final String title, final SearchType type) {
		final String path = info.getRelativePath();
		final long fileSize = info.getSize();
		final String name = FilePathWidget.getFileNameWithoutExtension(path);
		final String sampleName = getDefaultSampleName(type, title, name);
		final String experimentName = getDefaultExperimentName(type, title, name);

		addNewLine(path, fileSize, lineIndex, "none", experimentName, sampleName);
	}

	private void addNewLine(final String path, final long fileSize, final MutableInteger lineIndex, final String categoryName, final String experimentName, final String sampleName) {
		final FilePathWidget filePathWidget = new FilePathWidget(path);

		final int rowNumber = lineIndex.i;

		setWidget(rowNumber, FILE_COLUMN, filePathWidget);
		setWidget(rowNumber, SIZE_COLUMN, new FileSizeWidget(fileSize));
		final PushButton removeButton = new PushButton(new Image(REMOVE_IMAGE));
		removeButton.addClickHandler(new RemoveButtonListener(lineIndex));
		setWidget(rowNumber, REMOVE_COLUMN, removeButton);

		final EditableLabel sampleLabel = new EditableLabel(sampleName, new TextChangeListener(SAMPLE_COLUMN, this));
		sampleLabel.addStyleName("editable-label");
		widgetIndices.put(sampleLabel, lineIndex);
		setWidget(rowNumber, SAMPLE_COLUMN, sampleLabel);

		final EditableLabel experimentLabel = new EditableLabel(experimentName, new TextChangeListener(EXPERIMENT_COLUMN, this));
		experimentLabel.addStyleName("editable-label");
		widgetIndices.put(experimentLabel, lineIndex);
		setWidget(rowNumber, EXPERIMENT_COLUMN, experimentLabel);

		final EditableLabel categoryLabel = new EditableLabel(categoryName, new TextChangeListener(CATEGORY_COLUMN, this));
		categoryLabel.addStyleName("editable-label");
		widgetIndices.put(categoryLabel, lineIndex);
		setWidget(rowNumber, CATEGORY_COLUMN, categoryLabel);

		// By default the files are selected
		setChecked(rowNumber, SELECT_COLUMN, true);
	}

	/**
	 * Return the default name of the sample according to the search type, the search title and the file name.
	 *
	 * @param type  Type of search.
	 * @param title Title of the search.
	 * @param name  Name of the input file.
	 * @return Sample name.
	 */
	private static String getDefaultSampleName(final SearchType type, final String title, final String name) {
		if (SearchType.ManyToOne.equals(type)) {
			return title;
		}
		return name;
	}

	/**
	 * Retrun the default name of the experiment according to the search type, the search title and the file name.
	 *
	 * @param type  Type of search.
	 * @param title Title of the search.
	 * @param name  Name of the input file.
	 * @return Sample name.
	 */
	private static String getDefaultExperimentName(final SearchType type, final String title, final String name) {
		if (SearchType.ManyToOne.equals(type) || SearchType.ManyToSamples.equals(type)) {
			return title;
		}
		return name;
	}

	private void updateMaxCommonPath() {
		String[] maxCommonPath = null;
		int maxCommonPathLength = 0;
		for (int i = getFirstDataRow(); i < getRowCount(); i++) {
			final FilePathWidget filePath = (FilePathWidget) getWidget(i, FILE_COLUMN);
			final String path = filePath.getFullPath();
			final String[] tokens = path.split("/");
			if (maxCommonPath == null) {
				maxCommonPath = tokens;
				maxCommonPathLength = tokens.length - 1;
			} else {
				for (int j = 0; j < maxCommonPathLength; j++) {
					if (!maxCommonPath[j].equals(tokens[j])) {
						maxCommonPathLength = j;
						break;
					}
				}

			}
		}

		final StringBuilder pathPrefix = new StringBuilder();
		if (maxCommonPath != null) {
			for (int i = 0; i < maxCommonPathLength; i++) {
				pathPrefix.append(maxCommonPath[i]).append("/");
			}
		}

		for (int i = getFirstDataRow(); i < getRowCount(); i++) {
			final FilePathWidget filePath = (FilePathWidget) getWidget(i, FILE_COLUMN);
			filePath.setPrefixPath(pathPrefix.toString());
		}
	}

	public void removeSelectedFiles() {
		for (int i = getFirstDataRow(); i < getRowCount(); ) {
			if (getSelectionCheckBox(i).isChecked()) {
				removeTableRow(i);
			} else {
				renumberTableRow(i);
				i++;
			}
		}

		ValueChangeEvent.fire(this, null);
	}

	public void removeFileAtRow(final int row) {
		removeTableRow(row);
		for (int i = getFirstDataRow(); i < getRowCount(); i++) {
			renumberTableRow(i);
		}
		ValueChangeEvent.fire(this, null);
	}

	private void renumberTableRow(final int i) {
		final Widget ww = getWidget(i, SELECT_COLUMN);
		final MutableInteger mi = widgetIndices.get(ww);
		if (mi != null) {
			mi.i = i;
		}
	}

	private void removeTableRow(final int i) {
		for (int j = 0; j < getHeaders().length; ++j) {
			final Widget ww = getWidget(i, j);
			if (ww != null) {
				widgetIndices.remove(ww);
			}
		}
		removeRow(i);
	}

	public void setSearchType(final SearchType searchType) {
		if (!SearchType.Custom.equals(searchType)) {
			rewriteSamplesAndExperiments(searchType);
		}
	}

	/**
	 * The title has changed, update the table
	 */
	public void updateSearchTitle(final String searchTitle) {
		this.searchTitle = searchTitle;
		final SearchType selectedSearchType = searchTypeList.getSelectedSearchType();
		if (SearchType.ManyToOne.equals(selectedSearchType) || SearchType.ManyToSamples.equals(selectedSearchType)) {
			rewriteSamplesAndExperiments(selectedSearchType);
		}
	}

	private void rewriteSamplesAndExperiments(final SearchType searchType) {
		for (int row = getFirstDataRow(); row < getRowCount(); row++) {
			final EditableLabel w = (EditableLabel) getWidget(row, SAMPLE_COLUMN);
			final String fileName = ((FilePathWidget) getWidget(row, FILE_COLUMN)).getFileNameWithoutExtension();
			w.setText(
					getDefaultSampleName(
							searchType,
							searchTitle,
							fileName));

			final EditableLabel w2 = (EditableLabel) getWidget(row, EXPERIMENT_COLUMN);
			w2.setText(
					getDefaultExperimentName(
							searchType,
							searchTitle,
							fileName));
		}
	}

	private CheckBox getSelectionCheckBox(final int row) {
		return ((CheckBox) getWidget(row, SELECT_COLUMN));
	}

	private long getMaxFileSize() {
		long max = 0;
		for (int i = getFirstDataRow(); i < getRowCount(); i++) {
			final FileSizeWidget fileSize = (FileSizeWidget) getWidget(i, SIZE_COLUMN);
			final long size = fileSize.getFileSize();
			if (size > max) {
				max = size;
			}
		}
		return max;
	}

	public void changeColumnText(final int row, final int column, final String text) {
		// If the row is selected, change text for all selected rows
		if (Boolean.TRUE.equals(getSelectionCheckBox(row).getValue())) {
			for (int i = getFirstDataRow(); i < getRowCount(); i++) {
				if (Boolean.TRUE.equals(getSelectionCheckBox(i).getValue()) && i != row) {
					final EditableLabel label = (EditableLabel) getWidget(i, column);
					label.setText(text);
				}
			}
		}
	}

	public List<String> getFileNames() {
		final List<String> results = new ArrayList<String>(getRowCount() - getFirstDataRow());

		for (int row = getFirstDataRow(); row < getRowCount(); row++) {

			final EditableLabel w = (EditableLabel) getWidget(row, SAMPLE_COLUMN);
			final String sampleName = w.getText();
			results.add(sampleName);
		}
		return results;

	}

	public List<ClientFileSearch> getData(final ArrayList<ClientSearchEngineConfig> enabledEngines) {
		final List<ClientFileSearch> results = new ArrayList<ClientFileSearch>(getRowCount() - getFirstDataRow());

		for (int row = getFirstDataRow(); row < getRowCount(); row++) {

			final EditableLabel w = (EditableLabel) getWidget(row, SAMPLE_COLUMN);
			final String sampleName = w.getText();

			final EditableLabel w2 = (EditableLabel) getWidget(row, EXPERIMENT_COLUMN);
			final String experimentName = w2.getText();

			final EditableLabel w3 = (EditableLabel) getWidget(row, CATEGORY_COLUMN);
			final String categoryName = w3.getText();
			results.add(new ClientFileSearch(
					((FilePathWidget) getWidget(row, FILE_COLUMN)).getFullPath(),
					sampleName,
					categoryName,
					experimentName,
					enabledEngines,
					null));
		}
		return results;
	}

	private int getWidgetRow(final Widget widget) {
		final MutableInteger mi = widgetIndices.get(widget);
		if (mi == null) {
			throw new RuntimeException("Cant' find widget index for " + widget.toString());
		}
		return mi.i;
	}

	public void setChecked(final int row, final int column, final boolean checked) {
		final CheckBox rowCheckBox = (CheckBox) getWidget(row, column);
		rowCheckBox.setValue(checked);
		// Select column also changes row style
		if (SELECT_COLUMN == column) {
			if (checked) {
				getRowFormatter().removeStyleName(row, ROW_DESELECTED_STYLE);
				getRowFormatter().addStyleName(row, ROW_SELECTED_STYLE);
			} else {
				getRowFormatter().removeStyleName(row, ROW_SELECTED_STYLE);
				getRowFormatter().addStyleName(row, ROW_DESELECTED_STYLE);
			}
		}
	}

	private class TextChangeListener implements ChangeListener {
		private int _column;
		private FileTable _fileTable;

		TextChangeListener(final int column, final FileTable fileTable) {
			_column = column;
			_fileTable = fileTable;
		}

		@Override
		public void onChange(final Widget widget) {
			if (widget instanceof EditableLabel) {
				final EditableLabel label = (EditableLabel) widget;
				_fileTable.changeColumnText(getWidgetRow(widget), _column, label.getText());
				_fileTable.setSearchType(SearchType.Custom);
				_fileTable.searchTypeList.setSelectedSearchType(SearchType.Custom, true);
			} else {
				throw new RuntimeException("Programmer error, type mismatch");
			}
		}
	}

	private class RemoveButtonListener implements ClickHandler {
		private MutableInteger rowIndex;

		RemoveButtonListener(final MutableInteger rowIndex) {
			this.rowIndex = rowIndex;
		}

		@Override
		public void onClick(final ClickEvent event) {
			removeFileAtRow(rowIndex.i);
		}
	}
}
