package edu.mayo.mprc.mascot;

import au.id.jericho.lib.html.Element;
import au.id.jericho.lib.html.FormField;
import au.id.jericho.lib.html.FormFields;
import au.id.jericho.lib.html.Source;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.HttpClientUtility;
import org.apache.http.client.HttpClient;

import java.net.URI;
import java.util.*;

/**
 * Represents a mascot database maintenance web interface and provides utility methods to
 * manipulate mascot databases.
 * <p/>
 * Usage:
 * <ol>
 * <li>{@link #MascotDatabaseMaintenance}{@code ("http://[mascot server URI]/x-cgi/db_gui.pl", new HttpClient(new SimpleHttpConnectionManager()));}
 * <p>This will load an initial list of databases using {@link #refresh()}.
 * </li>
 * <li>{@link #isDatabaseDeployed}("mascot database name") - returns true/false from the cache obtained by {@link #refresh} </li>
 * <li>{@link #deleteDatabase}("mascot database name") - will either succeed or throw an exception. Returns response from Mascot, useful for debugging.</li>
 * </ul>
 * <p>Mascot requires four queries to delete a database:
 * <ol>
 * <li>Obtain initial list of databases</li>
 * <li>Select a database from the list</li>
 * <li>Post a delete button click</li>
 * <li>Post an apply button click</li>
 * </ol>
 * After the last request, we check whether the response still contains the database to be deleted.
 * If that is the case, the database deletion failed and an exception is thrown.
 */
final class MascotDatabaseMaintenance {
	private static final String DB_DISPLAY_PARAM = "DB_DISPLAY";
	private static final String STEP_PARAM = "STEP";
	private static final String REFERE_PARAM = "Referer";

	private static final String DEF_STEP = "db_defs";
	private static final String DELETE_STEP = "delete_db";
	private static final String APPLY_STEP = "apply";

	private Map<String, String> dbNameDisplayIdPairs;
	private Map<String, String> formParametersNameValuePairs;

	private URI dbMaintenanceUri;
	private HttpClient httpClient;

	private String referenceUri;

	MascotDatabaseMaintenance(final URI dbMaintenanceUri, final HttpClient httpClient) {
		this.dbMaintenanceUri = dbMaintenanceUri;
		this.httpClient = httpClient;
		referenceUri = dbMaintenanceUri.toString();

		refresh();
	}

	public synchronized MascotDbHttpRequestResult deleteDatabase(final String databaseDisplayName) {
		try {
			selectDatabase(databaseDisplayName);

			final Map<String, String> methodParams = new TreeMap<String, String>();
			methodParams.put(STEP_PARAM, DELETE_STEP);

			refreshFromResponsePage(executePostMethod(methodParams));

			final MascotDbHttpRequestResult requestResult = applyChanges();

			refreshFromResponsePage(getDbMaintenancePageLocal());

			if (isDatabaseDeployed(databaseDisplayName)) {
				throw new MprcException("Failed to deletion of database " + databaseDisplayName + " from Mascot system at " + referenceUri);
			}

			return requestResult;

		} catch (MprcException e) {
			throw new MprcException("Error occurred when deleting database " + databaseDisplayName + " from Mascot system at " + referenceUri, e);
		}
	}

	private synchronized void refresh() {
		refreshFromResponsePage(getDbMaintenancePageLocal());
	}

	private void refreshFromResponsePage(final String page) {

		formParametersNameValuePairs = new HashMap<String, String>();
		dbNameDisplayIdPairs = new HashMap<String, String>();

		final Source source = new Source(page);
		final FormFields formFields = source.findFormFields();
		FormField formField = null;
		Iterator valuesIterator = null;

		for (final Object formField1 : formFields) {
			formField = (FormField) formField1;

			valuesIterator = formField.getValues().iterator();

			if (valuesIterator.hasNext()) {
				formParametersNameValuePairs.put(formField.getName().toUpperCase(Locale.ENGLISH), valuesIterator.next().toString());
			} else {
				formParametersNameValuePairs.put(formField.getName().toUpperCase(Locale.ENGLISH), "");
			}

			if (formField.getName().equalsIgnoreCase(DB_DISPLAY_PARAM)) {
				Element element = null;
				for (Iterator dbIterator = formField.getFormControl().getOptionElementIterator(); dbIterator.hasNext(); ) {
					element = (Element) dbIterator.next();
					dbNameDisplayIdPairs.put(element.extractText(), element.getAttributeValue("value"));
				}
			}
		}
	}

	private void selectDatabase(final String databaseDisplayName) {
		try {
			final Map<String, String> methodParams = new TreeMap<String, String>();
			methodParams.put(STEP_PARAM, DEF_STEP);
			methodParams.put(DB_DISPLAY_PARAM, dbNameDisplayIdPairs.get(databaseDisplayName));

			refreshFromResponsePage(executePostMethod(methodParams));
		} catch (MprcException e) {
			throw new MprcException("Failed to select database " + databaseDisplayName + " from Mascot system at " + referenceUri, e);
		}
	}

	/**
	 * @param databaseDisplayName
	 * @return true if this database name is shown in the mascot database maintenance interface.
	 */
	public synchronized boolean isDatabaseDeployed(final String databaseDisplayName) {
		return dbNameDisplayIdPairs.containsKey(databaseDisplayName);
	}

	private MascotDbHttpRequestResult applyChanges() {
		MyMascotDbHttpRequestResult requestResult = null;

		try {
			final Map<String, String> methodParams = new TreeMap<String, String>();
			methodParams.put(STEP_PARAM, APPLY_STEP);

			requestResult = new MyMascotDbHttpRequestResult(executePostMethod(methodParams));
		} catch (MprcException e) {
			throw new MprcException("Failed to apply changes to Mascot system at " + referenceUri, e);
		}

		return requestResult;
	}

	/**
	 * If execution of http method fails, MprcException is thrown.
	 *
	 * @param methodParams Key-val
	 * @return Response body of executed method
	 */
	private String executePostMethod(final Map<String, String> methodParams) {
		final String tempReferenceUri = generateReferenceUri();
		methodParams.putAll(formParametersNameValuePairs);
		methodParams.put(REFERE_PARAM, referenceUri);

		String response = HttpClientUtility.getPostResponse(httpClient, tempReferenceUri, methodParams);

		referenceUri = tempReferenceUri;

		FileUtilities.out("************************************************************************************");
		FileUtilities.out(response);
		FileUtilities.out("************************************************************************************");

		return response;

	}

	private String getDbMaintenancePageLocal() {

		final String tempReferenceUri = generateReferenceUri();

		try {
			final String response = HttpClientUtility.httpGet(httpClient, tempReferenceUri);

			referenceUri = tempReferenceUri;

			return response;
		} catch (MprcException e) {
			throw new MprcException("Failed to get Mascot maintenance page from Mascot at " + tempReferenceUri, e);
		}
	}

	/**
	 * Mascot uses current amount of milliseconds to determine whether the page was reposted. We need to produce
	 * URIs with raising number to simulate this.
	 *
	 * @return URI with current milliseconds appended at the end.
	 */
	private String generateReferenceUri() {
		return dbMaintenanceUri.toString() + "?" + Long.toString(System.currentTimeMillis()).substring(0, 10);
	}

	private class MyMascotDbHttpRequestResult implements MascotDbHttpRequestResult {

		private String requestResponseBody;

		private MyMascotDbHttpRequestResult(final String requestResponseBody) {
			this.requestResponseBody = requestResponseBody;
		}

		@Override
		public String getRequestResponseBody() {
			return requestResponseBody;
		}
	}
}
