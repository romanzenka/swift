package edu.mayo.mprc.mascot;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.*;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Collections2;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.dbcurator.model.Curation;
import org.w3c.dom.Node;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Wrapper around Mascot 2.4's database manager.
 * <p/>
 * Deploys/undeploys a Mascot database through the web interface.
 *
 * @author Roman Zenka
 */
public final class DatabaseManager implements Closeable {
	public static final String DB_MANAGER_TITLE = "Mascot Database Manager";
	private String mascotServerUrl;
	private WebClient client;
	private static final Function<String, String> TO_LOWER = new ToLowerFunction();

	/**
	 * Relative path to the database manager.
	 */
	private static final String DB_MANAGER_URL = "/x-cgi/db_manager.pl";

	public DatabaseManager(final String mascotServerUrl) {
		this.mascotServerUrl = mascotServerUrl;
		client = new WebClient();
	}

	/**
	 * Obtain list of currently deployed database (active/inactive)
	 *
	 * @return List of databases
	 */
	public List<String> listDatabases() {
		final List<String> strings = new ArrayList<String>(64);
		try {
			final HtmlPage page = getPage(mascotServerUrl + DB_MANAGER_URL);

			if (!DB_MANAGER_TITLE.equals(page.getTitleText().trim())) {
				wrongDatabaseListFormat("wrong page title: " + page.getTitleText());
			}

			final HtmlTable databaseTable = (HtmlTable) page.getFirstByXPath("//table[@class=\"database-list\"]");
			if (databaseTable == null) {
				wrongDatabaseListFormat("missing database table");
			}
			final HtmlTableRow headerRow = databaseTable.getHeader().getRows().get(0);
			if (!"Name".equals(headerRow.getCell(0).getTextContent())) {
				wrongDatabaseListFormat("the database table does not have expected header");
			}

			for (final HtmlTableBody body : databaseTable.getBodies()) {
				for (final HtmlTableRow row : body.getRows()) {
					final HtmlTableCell firstCell = row.getCell(0);
					// We have a horizontal line in the list which uses colspan
					if (!firstCell.hasAttribute("colspan")) {
						final Node anchor = (Node) firstCell.getFirstByXPath("a");
						if (anchor == null) {
							wrongDatabaseListFormat("database table list cell does not contain a database link");
						}
						strings.add(anchor.getTextContent().trim());
					}
				}
			}

		} catch (MprcException e) {
			throw new MprcException("Mascot databases could not be listed", e);
		}
		return strings;
	}

	/**
	 * Deploy a new database.
	 *
	 * @param name  Name of the database.
	 * @param fasta Fasta file with the database data.
	 */
	public void deployDatabase(final String name, final File fasta) {
		validateDatabaseName(name);
		try {
			final HtmlPage page = getPage(mascotServerUrl + DB_MANAGER_URL);
			final HtmlForm newDbForm = clickNewDatabase(page);
			final HtmlForm dbDefForm = addCustomDatabase(name, newDbForm);

		} catch (Exception e) {
			throw new MprcException("Could not deploy mascot database " + name, e);
		}
	}

	private HtmlForm clickNewDatabase(HtmlPage page) throws IOException {
		final HtmlAnchor newLink = page.getAnchorByText("Create new");
		final HtmlPage newDbPage = (HtmlPage) newLink.click();
		if (!DB_MANAGER_TITLE.equals(newDbPage.getTitleText())) {
			throw new MprcException("The 'Create new' link in Mascot Deployer did not open a proper page.\nExpected " + DB_MANAGER_TITLE + "\nActual:" + newDbPage.getTitleText());
		}
		return getSingleForm(newDbPage);
	}

	private HtmlForm addCustomDatabase(String name, HtmlForm dbForm) throws IOException {
		final HtmlInput nameInput = dbForm.getInputByName("dbs.new.name");
		nameInput.setValueAttribute(name);
		checkRadioButton(dbForm, "dbs.new.new-mode", "");
		final HtmlPage next = (HtmlPage) dbForm.getInputByValue("Next").click();
		return getSingleForm(next);
	}

	private HtmlForm getSingleForm(final HtmlPage page) {
		if (page.getForms().size() != 1) {
			throw new MprcException("There should be only one form on the page " + page.getUrl() + " available, found " + page.getForms().size());
		}
		return page.getForms().get(0);
	}

	private void checkRadioButton(HtmlForm dbForm, String radioName, String radioValue) {
		final List<HtmlRadioButtonInput> radioButtonsByName = dbForm.getRadioButtonsByName(radioName);
		for (HtmlRadioButtonInput radio : radioButtonsByName) {
			if (Objects.equal(radioValue, radio.getValueAttribute())) {
				radio.setChecked(true);
			}
		}
	}

	/**
	 * Undeploy a previously deployed database.
	 *
	 * @param name Name of the database to undeploy.
	 */
	public void undeployDatabase(final String name) {
		final List<String> databases = listDatabases();
		if (!databases.contains(name)) {
			throw new MprcException("Cannot undeploy database " + name + ": it is not present on the server");
		}
	}

	/**
	 * Close all resources associated with the manager.
	 * After this call, the object is no longer useable.
	 */
	@Override
	public void close() {
		client.closeAllWindows();
	}

	private void wrongDatabaseListFormat(final String badContentInfo) {
		final String contextUrl = mascotServerUrl + DB_MANAGER_URL;
		final String contextPage = DB_MANAGER_TITLE;

		throw new MprcException("Unexpected page contents - " + badContentInfo + " (" + contextPage + " at " + contextUrl + ").\n" +
				"Resolution steps:\n" +
				"\t1) check that the page [" + contextUrl + "] lists the Mascot databases\n" +
				"\t2) check that the page contains a table with CSS class 'database-list'.\n" +
				"\t3) check that the table has a header line with 'Name' as the first field.\n" +
				"\t4) check that all other table rows contain the database names enclosed in a hyperlink.\n" +
				"If any of the above is not true, the Swift's parser might have to be updated to support your version of Mascot");
	}

	/**
	 * Gets a page for given URL, disabling the 'deflate' encoding that seems to be broken.
	 *
	 * @param url URL of the page to get
	 * @return Object representing the page.
	 */
	private HtmlPage getPage(final String url) {
		try {
			final WebRequest request = new WebRequest(new URL(url), HttpMethod.GET);
			request.setAdditionalHeader("Accept-Encoding", "gzip");
			return (HtmlPage) client.getPage(request);
		} catch (IOException e) {
			throw new MprcException("Cannot load web page " + url, e);
		}
	}

	/**
	 * Check that the database name is correct and the database is not deployed, modulo lower/uppercase.
	 * Although Mascot distinguishes case, we do not want to open this can of worms.
	 *
	 * @param name Database name
	 */
	private void validateDatabaseName(final String name) {
		Curation.validateShortNameLegalCharacters(name);

		final List<String> databases = listDatabases();
		final Collection<String> databasesLc = Collections2.transform(databases, TO_LOWER);
		if (databasesLc.contains(TO_LOWER.apply(name))) {
			throw new MprcException("The database " + name + " is already deployed in Mascot");
		}
	}

	private static class ToLowerFunction implements Function<String, String> {
		@Override
		public String apply(@Nullable final String from) {
			return from == null ? null : from.toLowerCase();
		}
	}
}
