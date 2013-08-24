package edu.mayo.mprc.swift.configuration.client.view;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

public final class ModuleWrapper extends SimplePanel {
	private final ModuleView module;

	public ModuleWrapper(final String moduleName, final ModuleView module, final String description) {
		this.module = module;
		addStyleName("module-wrapper");
		final FlowPanel panel = new FlowPanel();
		panel.addStyleName("module");
		final Label label = new Label(moduleName);
		label.addStyleName("module-label");
		panel.add(label);
		final HTML desc = new HTML(description);
		desc.addStyleName("module-description");
		panel.add(desc);
		panel.add(module.getModuleWidget());
		setWidget(panel);
	}

	public ModuleView getModule() {
		return module;
	}
}
