package edu.mayo.mprc.searchdb.dao;

import com.google.common.base.Strings;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.StringUtilities;

import java.io.IOException;
import java.io.Writer;

/**
 * Helps reporting the loaded data.
 *
 * @author Roman Zenka
 */
public class Report {
	private Writer w;

	private boolean rowStarted = false;

	public Report(final Writer w) {
		this.w = w;
	}

	public static String esc(final String s) {
		return StringUtilities.escapeHtml(s);
	}


	public Report write(final String s) {
		try {
			w.write(s);
		} catch (IOException e) {
			throw new MprcException("Could not write out data", e);
		}
		return this;
	}

	/**
	 * Start a table with a title on the top.
	 *
	 * @param title Title on the top of the table.
	 */
	public Report startTable(final String title) {
		rowStarted = false;
		if (title != null) {
			header(title);
		}
		write("<table>");
		return this;
	}

	public Report nextRow() {
		write("</tr>\n");
		rowStarted = false;
		return this;
	}

	public Report cell(final String text) {
		return cell(text, 1, null);
	}

	public Report cell(final String text, final String clazz) {
		return cell(text, 1, clazz);
	}

	public Report cell(final String text, final int colspan, final String clazz) {
		checkRow();
		String classString = clazz == null ? "" : " class=\"" + clazz + "\"";
		if (colspan == 1) {
			write("<td" + classString + ">");
		} else {
			write("<td" + classString + " colspan=\"" + colspan + "\">");
		}
		if (Strings.isNullOrEmpty(text)) {
			write("&nbsp;");
		} else {
			write(esc(text));
		}
		write("</td>\n");
		return this;
	}

	private void checkRow() {
		if (!rowStarted) {
			write("<tr>");
			rowStarted = true;
		}
	}

	public Report hCell(final String text) {
		checkRow();
		write("<th>" + esc(text) + "</th>\n");
		return this;
	}

	public Report hCellRaw(final String text) {
		checkRow();
		write("<th>" + text + "</th>\n");
		return this;
	}

	public Report hCellRaw(final String text, final String clazz) {
		checkRow();
		write("<th class=\"" + clazz + "\">" + text + "</th>\n");
		return this;
	}

	/**
	 * Add a key-value pair to a table on a separate row.
	 *
	 * @param key   Key, displayed in bold.
	 * @param value Value.
	 */
	public Report addKeyValueTable(final String key, final Object value) {
		hCell(key);
		cell(value == null ? "<null>" : value.toString());
		nextRow();
		return this;
	}

	/**
	 * Close the table.
	 */
	public Report endTable() {
		write("</table>\n");
		return this;
	}

	public Report header(final String text) {
		write("<h2>" + esc(text) + "</h2>\n");
		return this;
	}
}
