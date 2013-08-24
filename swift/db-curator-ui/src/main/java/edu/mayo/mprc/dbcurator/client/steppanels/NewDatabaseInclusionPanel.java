package edu.mayo.mprc.dbcurator.client.steppanels;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.common.client.ExceptionUtilities;
import edu.mayo.mprc.dbcurator.client.curatorstubs.CurationStepStub;
import edu.mayo.mprc.dbcurator.client.curatorstubs.NewDatabaseInclusionStub;
import edu.mayo.mprc.dbcurator.client.services.CommonDataRequester;
import edu.mayo.mprc.dbcurator.client.services.CommonDataRequesterAsync;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Eric Winter
 */
public final class NewDatabaseInclusionPanel extends AbstractStepPanel {

	private NewDatabaseInclusionStub containedStep = new NewDatabaseInclusionStub();

	private TextBox url = new TextBox();
	private ListBox lstCommonSites = new ListBox();
	private Map<String, String> commonSites = new HashMap<String, String>();
	public static final String TITLE = "Download Sequence Database";

	public NewDatabaseInclusionPanel() {
		final VerticalPanel panel = new VerticalPanel();

		lstCommonSites.addItem("Manual Entry");
		getCommonSites(); //generate the list of common sites
		lstCommonSites.addChangeListener(new ChangeListener() {
			@Override
			public void onChange(final Widget widget) {
				final ListBox source = (ListBox) widget;
				final String selection = source.getItemText(source.getSelectedIndex());
				if (!selection.equalsIgnoreCase("Manual Entry")) {
					url.setText(commonSites.get(selection));
				} else {
					url.setText("ftp://");
				}
			}
		});

		panel.add(new Label("Choose a database to include..."));
		panel.add(lstCommonSites);

		panel.setWidth("100%");
		url.setWidth("100%");
		panel.add(url);

		panel.setSpacing(5);
		setTitle(TITLE);

		initWidget(panel);
	}

	@Override
	public CurationStepStub getContainedStep() {
		containedStep.url = url.getText();
		return containedStep;
	}

	@Override
	public void setContainedStep(final CurationStepStub step) throws ClassCastException {
		if (!(step instanceof NewDatabaseInclusionStub)) {
			ExceptionUtilities.throwCastException(step, NewDatabaseInclusionStub.class);
			return;
		}
		containedStep = (NewDatabaseInclusionStub) step;
		update();
	}

	@Override
	public String getStyle() {
		return "shell-header-newdb";
	}

	/**
	 * This site currently is static but it will eventually use a call to the server to get a list of sites that are both hard coded
	 * and may also go to the database to find the top 5 ftp paths that were manually entered.
	 *
	 * @return a list of sites that are commonly available
	 */
	private void getCommonSites() {

		if (commonSites.size() < 2) {

			//then make an rpc call to the server requesting the list of common transforms
			final CommonDataRequesterAsync dataRequester = (CommonDataRequesterAsync) GWT.create(CommonDataRequester.class);
			final ServiceDefTarget endpoint = (ServiceDefTarget) dataRequester;
			endpoint.setServiceEntryPoint(GWT.getModuleBaseURL() + "CommonDataRequester");
			dataRequester.getFTPDataSources(new AsyncCallback<Map<String, String>>() {

				@Override
				public void onFailure(final Throwable throwable) {
					//do nothing we just can't add common formatters
				}

				@Override
				public void onSuccess(final Map<String, String> trans) {
					commonSites.putAll(trans);

					for (final String s : commonSites.keySet()) {
						lstCommonSites.addItem(s);
					}
				}
			});
		}

		for (final String s : commonSites.keySet()) {
			lstCommonSites.addItem(s);
		}

	}

	@Override
	public void update() {
		url.setText(containedStep.url);
	}

	@Override
	public String getImageURL() {
		return "images/step-icon-add.png";
	}
}
