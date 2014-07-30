package edu.mayo.mprc.swift.configuration.server;

import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.MapConfigReader;
import edu.mayo.mprc.config.MapConfigWriter;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.StringPropertyValues;
import edu.mayo.mprc.config.ui.UiResponse;
import edu.mayo.mprc.swift.configuration.client.model.UiChangesReplayer;

import java.util.ArrayList;
import java.util.Map;

public class SerializingUiChanges implements UiResponse {
	private ArrayList<String> commands = new ArrayList<String>(4);
	private DependencyResolver resolver;

	public SerializingUiChanges(final DependencyResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public void displayPropertyError(final ResourceConfig config, final String propertyName, final String error) {
		commands.add(UiChangesReplayer.DISPLAY_PROPERTY_ERROR);
		commands.add(resolver.getIdFromConfig(config));
		commands.add(propertyName);
		commands.add(error);
	}

	@Override
	public void setProperty(final ResourceConfig config, final String propertyName, final String newValue) {
		commands.add(UiChangesReplayer.SET_PROPERTY);
		commands.add(resolver.getIdFromConfig(config));
		commands.add(propertyName);
		commands.add(newValue);

		// Actually set the property value on our config
		MapConfigWriter writer = new MapConfigWriter(resolver);
		config.save(writer);
		final Map<String, String> values = writer.getMap();
		values.put(propertyName, newValue);
		MapConfigReader reader = new MapConfigReader(resolver, new StringPropertyValues(values));
		config.load(reader);
	}

	public UiChangesReplayer getReplayer() {
		return new UiChangesReplayer(commands);
	}
}
