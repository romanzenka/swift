package edu.mayo.mprc.dbcurator.client.steppanels;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.*;
import edu.mayo.mprc.common.client.ExceptionUtilities;

import java.util.ArrayList;
import java.util.List;

/**
 * A StepPanel that will handle the step transformation of the sequence headers
 *
 * @author Eric Winter
 */
public final class HeaderTransformPanel extends AbstractStepPanel {
	public static final String TITLE = "Transform FASTA Headers";

	private static List<HeaderTransformStub> commonHeaderTransformers = new ArrayList<HeaderTransformStub>();

	/**
	 * the text box for displaying the description
	 */
	private TextBox txtDescription;

	/**
	 * the text box for displaying the matching pattern
	 */
	private TextBox txtMatchPattern;

	/**
	 * the text box for displaying the transform pattern
	 */
	private TextBox txtSubPattern;

	/**
	 * the list box for allowing the user to choose from a list of common transforms
	 */
	private ListBox lstCommonTransforms;

	/**
	 * the step that this panel renders and allow editing of
	 */
	private HeaderTransformStub containedStep;


	/**
	 * creates a new panel for rendering and editing a HeaderTransformStep
	 */
	public HeaderTransformPanel() {

		final VerticalPanel mainPanel = new VerticalPanel();

		mainPanel.setSpacing(5);
		initWidget(mainPanel);

		setTitle(TITLE);

		Panel tempLayoutPanel = new HorizontalPanel();
		//tempLayoutPanel.setWidth("100%");

		//create a line that lets user select a common
		tempLayoutPanel.add(new Label("Choose a transform:"));
		lstCommonTransforms = new ListBox();
		lstCommonTransforms.addItem("Manual Entry");
		lstCommonTransforms.setSelectedIndex(0);
		lstCommonTransforms.setStyleName("steppanel-headertransform-lstcommontransforms");
		requestTransformers(); //populate the list box making an rpc call if necessary
		lstCommonTransforms.addChangeListener(new ChangeListener() {
			@Override
			public void onChange(final Widget widget) {
				final int selectedIndex = ((ListBox) widget).getSelectedIndex();
				if (selectedIndex == 0) {
					txtDescription.setText("");
					txtMatchPattern.setText("");
					txtSubPattern.setText("");
					return;
				}
				//if we have enough to handle the request then handle it
				if (commonHeaderTransformers.size() > selectedIndex - 1) {
					final HeaderTransformStub transform = (HeaderTransformStub) commonHeaderTransformers.get(selectedIndex - 1);
					txtDescription.setText(transform.description);
					txtMatchPattern.setText(transform.matchPattern);
					txtSubPattern.setText(transform.subPattern);
				}
			}
		});
		tempLayoutPanel.add(lstCommonTransforms);
		mainPanel.add(tempLayoutPanel);

		tempLayoutPanel = new HorizontalPanel();
		tempLayoutPanel.add(new Label("Transform Description: "));
		txtDescription = new TextBox();
		txtDescription.setStyleName("steppanel-headertransform-textentry");
		tempLayoutPanel.add(txtDescription);
		mainPanel.add(tempLayoutPanel);

		tempLayoutPanel = new HorizontalPanel();
		final Label matchLabel = new Label("Group Definition Pattern: ");
		matchLabel.setTitle("This is used to help identify classes in a regular expression so that the header can be transformed");
		tempLayoutPanel.add(matchLabel);
		txtMatchPattern = new TextBox();
		txtMatchPattern.setStyleName("steppanel-headertransform-textentry");
		tempLayoutPanel.add(txtMatchPattern);
		mainPanel.add(tempLayoutPanel);

		tempLayoutPanel = new HorizontalPanel();
		final Label subLabel = new Label("Output Pattern: ");
		subLabel.setTitle("Use the classes identified in the Group Definition Pattern and show how you want them layed out.");
		tempLayoutPanel.add(subLabel);
		txtSubPattern = new TextBox();
		txtSubPattern.setStyleName("steppanel-headertransform-textentry");
		tempLayoutPanel.add(txtSubPattern);
		mainPanel.add(tempLayoutPanel);

	}

	@Override
	public CurationStepStub getContainedStep() {
		containedStep.description = txtDescription.getText();
		containedStep.matchPattern = txtMatchPattern.getText();
		containedStep.subPattern = txtSubPattern.getText();
		return containedStep;
	}

	/**
	 * populates the commonHeaderTransformers if they are not already populated and then fills in the list
	 */
	private void requestTransformers() {
		final CommonDataRequesterAsync dataRequester = (CommonDataRequesterAsync) GWT.create(CommonDataRequester.class);
		final ServiceDefTarget endpoint = (ServiceDefTarget) dataRequester;
		endpoint.setServiceEntryPoint(GWT.getModuleBaseURL() + "CommonDataRequester");
		dataRequester.getHeaderTransformers(new AsyncCallback<List<HeaderTransformStub>>() {

			@Override
			public void onFailure(final Throwable throwable) {
				//do nothing we just can't add common formatters
			}

			@Override
			public void onSuccess(final List<HeaderTransformStub> trans) {
				for (final HeaderTransformStub tran : trans) {
					commonHeaderTransformers.add(tran);
					lstCommonTransforms.addItem(tran.toString());
				}
			}
		});
	}

	@Override
	public String getImageURL() {
		return "images/step-icon-add.png"; //todo: change icon
	}

	@Override
	public String getStyle() {
		return "shell-header-headertransformstep";
	}

	@Override
	public void setContainedStep(final CurationStepStub step) throws ClassCastException {
		if (!(step instanceof HeaderTransformStub)) {
			ExceptionUtilities.throwCastException(step, HeaderTransformStub.class);
			return;
		}
		containedStep = (HeaderTransformStub) step;
		update();
	}

	@Override
	public void update() {
		if (containedStep != null) {
			txtDescription.setText(containedStep.description);
			txtMatchPattern.setText(containedStep.matchPattern);
			txtSubPattern.setText(containedStep.subPattern);
		}
	}
}
