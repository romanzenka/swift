package edu.mayo.mprc.swift.ui.server;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This provides functionality the retrieve the structure under a folder in
 * XML format. Satisfies POJO requirements.
 */
public final class FileSearchBean {
	private static final String RESULTS_TAG = "results";
	private static final String DIR_TAG = "dir";
	private static final String FILE_TAG = "file";
	private static final String ERROR_TAG = "error";
	private static final String NAME_ATTR = "name";
	private static final String ERROR_MESSAGE_PREFIX = "Directory content evaluation failed...\n";

	public enum SortBy {
		NAME,
		DATE
	}


	/**
	 * the path under which will find children files and directory names
	 */
	private File[] expandedPaths;
	private String path;
	private String basePath;

	private SortBy sortBy;
	private static final File[] EMPTY_FILE_ARRAY = new File[0];

	/**
	 * to satify POJO has no arguments in constructor
	 *
	 * @param basePath Path to the root of the search bean (the {@link #setPath} and {@link @setExpandPath} are entered relative to this root.
	 */
	public FileSearchBean(final String basePath) {
		this.basePath = basePath;
		expandedPaths = EMPTY_FILE_ARRAY;
	}

	public void setPath(final String path) {
		this.path = new File(basePath, path).getPath();
	}

	public String getPath() {
		return path;
	}

	public SortBy getSortBy() {
		return sortBy;
	}

	public void setSortBy(SortBy sortBy) {
		this.sortBy = sortBy;
	}

	public void setExpandedPaths(final String paths) {
		final String[] parts = paths.split("\\|");
		expandedPaths = new File[parts.length];
		for (int i = 0; i < parts.length; i++) {
			expandedPaths[i] = new File(basePath, parts[i]);
		}
	}

	public File[] getExpandedPaths() {
		return expandedPaths;
	}


	/**
	 * Write folder content out using a given writer.
	 * The format is following:
	 * <?xml version="1.0"?>
	 * <results>
	 * <dir name="" />
	 * <dir name="" />
	 * <file name="" />
	 * <file name="" />
	 * </results>
	 * otherwise
	 * <error>
	 * errormessage
	 * </error>
	 *
	 * @param writer Writer to output the contents to.
	 */
	public void writeFolderContent(final Writer writer) {
		try {
			final File dir = checkFolderPath(path);

			final AttributesImpl atts = new AttributesImpl();
			final StreamResult streamResult = new StreamResult(writer);
			final SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
			// SAX2.0 ContentHandler.
			final TransformerHandler hd = tf.newTransformerHandler();
			final Transformer serializer = hd.getTransformer();
			serializer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
			serializer.setOutputProperty(OutputKeys.INDENT, "no");
			hd.setResult(streamResult);
			hd.startDocument();
			hd.startElement("", "", RESULTS_TAG, atts);
			writeOutFolder(hd, dir, expandedPaths);
			hd.endElement("", "", RESULTS_TAG);
			hd.endDocument();
		} catch (MprcException e) {
			// SWALLOWED: because the exception has to be sent to the client to be displayed
			writeErrorMessage(writer, e);
		} catch (TransformerConfigurationException e) {
			// SWALLOWED: because the exception has to be sent to the client to be displayed
			writeErrorMessage(writer, e);
		} catch (SAXException e) {
			// SWALLOWED: because the exception has to be sent to the client to be displayed
			writeErrorMessage(writer, e);
		}
	}

	private void writeErrorMessage(final Writer writer, final Throwable e) {
		try {
			writer.write(getErrorResult(ERROR_MESSAGE_PREFIX + e.getMessage()).toString());
		} catch (IOException e1) {
			throw new MprcException("Problems writing out XML contents of folder: " + path, e1);
		}
	}

	/**
	 * Checks whether given path is a proper directory, throws an exception otherwise.
	 *
	 * @param path Path to check.
	 * @return File representing the given path.
	 */
	private File checkFolderPath(final String path) {
		if (path == null || path.equals("")) {
			throw new MprcException("FileSearchBean.parseFolderContentforRawFiles : path is not set...");
		}
		final File pFile = new File(path);
		if (!pFile.exists()) {
			throw new MprcException("directory does not exist, name=" + path);
		}
		if (!pFile.isDirectory()) {
			throw new MprcException("file is not a directory, name=" + path);
		}
		return pFile;
	}

	StringBuilder getErrorResult(final String message) {
		final StringBuilder buffer = new StringBuilder("<" + ERROR_TAG + ">");
		buffer.append(message);
		buffer.append("</" + ERROR_TAG + ">" + "\n");
		return buffer;
	}

	/**
	 * Returns true if at least one of the subs is subfolder (direct or indirect) of dir.
	 * If subs are null, the result is false, if dir is null, the result is true.
	 *
	 * @param dir  The folder that has to contain at least one of the subs.
	 * @param subs Array of subfolder paths.
	 * @return True when at least one of the subs is inside dir (directly or not).
	 */
	private boolean isSubfolder(final File dir, final File[] subs) {
		if (subs == null) {
			return false;
		}
		if (dir == null) {
			return true;
		}
		for (final File sub : subs) {
			if (sub.getPath().startsWith(dir.getPath())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Writes out XML containing information about a given folder.
	 * <p/>
	 * We go through extra effort to perform as few isDirectory() calls as possible, as these are incredibly
	 * expensive as the directory size grows.
	 * <p/>
	 * TODO: Use something faster than isDirectory
	 *
	 * @param hd            Where to write the XML to.
	 * @param root          Directory to lists.
	 * @param expandedPaths Directories that are expanded within the listing.
	 */
	private void writeOutFolder(final TransformerHandler hd, final File root, final File[] expandedPaths) {
		// find all the files+directories
		final List<File> dirs = new ArrayList<File>(100);
		final List<File> files = new ArrayList<File>(100);

		FileUtilities.listFolderContents(root, ServiceImpl.FILTER, dirs, files);
		moveDirsToFiles(dirs, files);
		final AttributesImpl atts = new AttributesImpl();
		try {
			Collections.sort(dirs, new SimpleFileComparator(sortBy));
			Collections.sort(files, new SimpleFileComparator(sortBy));

			for (final File dir : dirs) {
				atts.clear();
				atts.addAttribute("", "", NAME_ATTR, "CDATA", dir.getName());
				hd.startElement("", "", DIR_TAG, atts);
				// If this directory should be expanded
				if (isSubfolder(dir, expandedPaths)) {
					writeOutFolder(hd, dir, expandedPaths);
				}
				hd.endElement("", "", DIR_TAG);
			}

			for (final File file : files) {
				atts.clear();
				atts.addAttribute("", "", NAME_ATTR, "CDATA", file.getName());
				hd.startElement("", "", FILE_TAG, atts);
				hd.endElement("", "", FILE_TAG);
			}
		} catch (SAXException e) {
			throw new MprcException("Failed writing out folder " + root.getPath(), e);
		}
	}

	/**
	 * We have a special case - some directories should actually be treated as
	 * 'files'. This is the case with Agilent's .d directories. We detect these,
	 * remove them from the list of dirs, add them to the list of files.
	 *
	 * @param dirs  List of dirs to take Agilent dirs from from.
	 * @param files Files to add the .d dirs to.
	 */
	static void moveDirsToFiles(List<File> dirs, List<File> files) {
		List<File> newDirs = new ArrayList<File>(dirs.size());
		for (File dir : dirs) {
			if (isAgilentDir(dir)) {
				files.add(dir);
			} else {
				newDirs.add(dir);
			}
		}
		dirs.clear();
		dirs.addAll(newDirs);
	}

	/**
	 * @param dir File to check.
	 * @return True if the file is an existing directory with agilent files.
	 */
	public static boolean isAgilentDir(final File dir) {
		return dir.getName().endsWith(".d") && dir.isDirectory();
	}

	private static final class SimpleFileComparator implements Comparator<File>, Serializable {
		private static final long serialVersionUID = 20121221L;

		private SortBy sortBy;

		public SimpleFileComparator(final SortBy sortBy) {
			this.sortBy = sortBy;
		}

		@Override
		public int compare(final File o1, final File o2) {
			switch (sortBy) {
				case NAME:
					return o1.getName().compareToIgnoreCase(o2.getName());
				case DATE:
					return -Long.valueOf(o1.lastModified()).compareTo(o2.lastModified());
				default:
					throw new MprcException(MessageFormat.format("Unsupported sort {0}", sortBy));
			}
		}
	}
}
