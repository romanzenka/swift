package edu.mayo.mprc.dbcurator.client.steppanels;

import com.allen_sauer.gwt.dnd.client.*;
import com.allen_sauer.gwt.dnd.client.drop.IndexedDropController;
import com.google.gwt.user.client.ui.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A mainPanel that will hold the StepPanels
 */
public final class StepPanelContainer extends Composite {


	/**
	 * the underlying stepOrganizer where the widgets will go
	 */
	private VerticalPanel stepOrganizer = new VerticalPanel();

	/**
	 * the controller that we need to register widgets on to make them draggable when they are added to the container
	 */
	private PickupDragController dragController = null;

	/**
	 * the list of steps that are represented in this stepOrganizer
	 */
	private List<CurationStepStub> containedSteps = new ArrayList<CurationStepStub>();

	private IndexedDropController dropController = null;

	private AbsolutePanel boundaryPanel = new AbsolutePanel();

	/**
	 * takes a list of steps that will be updated througout the life of this container to reflect the order of the widgets
	 * in this mainPanel.
	 *
	 * @param curationStepStubs the list of steps to represent in this mainPanel
	 */
	public StepPanelContainer(final List<CurationStepStub> curationStepStubs) {

		final ScrollPanel scrollPanel = new ScrollPanel();
		scrollPanel.setAlwaysShowScrollBars(true);
		scrollPanel.add(stepOrganizer);

		scrollPanel.addStyleName("stepeditor-scrollpanel");
		boundaryPanel.add(scrollPanel);
		boundaryPanel.addStyleName("stepeditor-dndboundarypanel");

		initWidget(boundaryPanel);

		//all dragging to occur through whole page but dropping can only happen in the container and
		dragController = new PickupDragController( /*dropBoundary*/ boundaryPanel, /*allowDroppingOnBoundaryPanel*/ false);

		dragController.addDragHandler(new DragHandler() {

			@Override
			public void onDragEnd(final DragEndEvent event) {
				updateStepOrderFromUI();
			}

			@Override
			public void onDragStart(final DragStartEvent event) {
			}

			@Override
			public void onPreviewDragEnd(final DragEndEvent event) throws VetoDragException {
			}

			@Override
			public void onPreviewDragStart(final DragStartEvent event) throws VetoDragException {
			}
		});

		dropController = new IndexedDropController(stepOrganizer);
		dragController.registerDropController(dropController);

		//insert the respective widgets for each one of the respective steps

		for (final CurationStepStub curationStepStub : curationStepStubs) {
			add(curationStepStub);
		}

	}

	public void setModificationEnabled(final boolean enabled) {

		for (final CurationStepStub containedStep : containedSteps) {
			containedStep.setEditable(enabled);
		}

		if (enabled) {
			dragController.registerDropController(dropController);
		} else {
			dragController.unregisterDropController(dropController);
		}

	}

	private void updateStepOrderFromUI() {
		final List<CurationStepStub> newOrder = new ArrayList<CurationStepStub>();
		final int widgetCount = stepOrganizer.getWidgetCount();
		for (int i = 0; i < widgetCount; i++) {
			final StepPanelShell shell = (StepPanelShell) stepOrganizer.getWidget(i);
			newOrder.add(shell.getContainedStepPanel().getContainedStep());
		}
		containedSteps = newOrder;
		refresh();
	}

	/**
	 * remove a widget from this container
	 *
	 * @param toRemove the widget you want to remove
	 * @return true if a widget was removed else false
	 */
	public boolean remove(final Widget toRemove) {
		return remove(stepOrganizer.getWidgetIndex(toRemove));
	}

	/**
	 * remove a widget at a specified index
	 *
	 * @param indexToRemove the index of the widget you want to remove
	 * @return true if a widget wsa removed
	 */
	public boolean remove(final int indexToRemove) {
		final boolean wasRemoved = stepOrganizer.remove(indexToRemove);
		if (wasRemoved) {
			containedSteps.remove(indexToRemove);
		}
		return wasRemoved;
	}

	/**
	 * takes a curation step and adds it to the end of the curation
	 *
	 * @param stepToInsert the step that you want to insert
	 */
	public void add(final CurationStepStub stepToInsert) {
		if (containedSteps.isEmpty()) {
			stepOrganizer.clear();
		}
		containedSteps.add(stepToInsert);
		final StepPanelShell shell = stepToInsert.getStepPanel().getShell(this);

		//if the step has not been run allow drag and drop
		if (shell.getContainedStepPanel().getContainedStep().getCompletionCount() == null) {
			dragController.makeDraggable(shell, shell.getStepTitle());
		}

		stepOrganizer.add(shell);

		shell.update();
	}

	/**
	 * get the index of a certain widget
	 *
	 * @param w the widget to get an index of
	 * @return the index of widget w
	 */
	public int getWidgetIndex(final Widget w) {
		return stepOrganizer.getWidgetIndex(w);
	}

	/**
	 * Updates each of the steps in the container and returns the list of step
	 *
	 * @return the list of curation steps in the container
	 */
	public List<CurationStepStub> getContainedSteps() {

		final List<CurationStepStub> retList = new ArrayList();

		for (int i = 0; i < stepOrganizer.getWidgetCount(); i++) {
			retList.add((((StepPanelShell) stepOrganizer.getWidget(i)).getContainedStepPanel()).getContainedStep());
		}

		return retList;
	}

	/**
	 * tell this container to refresh itself
	 *
	 * @param steps the steps that should be refreshed.  It is assumed the step haven't been changed so that the refresh will not have any problems
	 */
	public void refresh(final List<CurationStepStub> steps) {
		//have each step update itself to reflect the current state of the widget
		for (int i = 0; i < stepOrganizer.getWidgetCount(); i++) {
			((StepPanelShell) stepOrganizer.getWidget(i)).update();
		}

		//if the number of steps has changed for some reason then remove them all and re-add them
		if (containedSteps.size() != steps.size()) {
			for (int i = 0; i < containedSteps.size(); i++) {
				remove(i);
			}
			containedSteps.clear();

			for (final CurationStepStub step : steps) {
				add(step);
			}
		} else {
			//update to refect new state
			for (int i = 0; i < steps.size(); i++) {
				final StepPanelShell shell = (StepPanelShell) stepOrganizer.getWidget(i);
				final CurationStepStub stub = steps.get(i);
				shell.update(stub);
			}
		}
	}

	public void refresh() {
		refresh(containedSteps);
	}
}