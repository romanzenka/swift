package edu.mayo.mprc.swift.configuration.client.model;

import edu.mayo.mprc.swift.configuration.client.view.UiBuilderReplayer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class ResourceModel implements Serializable {
	private static int count = 0;
	public static final String NAME = "_name";
	public static final String TYPE = "_type";
	private static final long serialVersionUID = -3201869140244160793L;
	private String id;
	private HashMap<String, String> properties = new HashMap<String, String>();
	private ArrayList<ResourceModel> children = new ArrayList<ResourceModel>();
	private ResourceModel parent;
	private UiBuilderReplayer replayer;

	private ArrayList<ResourceModelListener> modelListeners = new ArrayList<ResourceModelListener>();

	public ResourceModel() {
	}

	public ResourceModel(final String name, final String type) {
		properties.put(NAME, name);
		properties.put(TYPE, type);
		count++;
		id = type + count;
	}

	public ResourceModel(final String id, final String name, final String type) {
		this.id = id;
		properties.put(NAME, name);
		properties.put(TYPE, type);
	}

	public void setId(final String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return getProperty(NAME);
	}

	public void setName(final String name) {
		setProperty(NAME, name);
		fireNameChanged();
	}

	public String getType() {
		return getProperty(TYPE);
	}

	public void setType(final String type) {
		setProperty(TYPE, type);
	}

	public HashMap<String, String> getProperties() {
		return properties;
	}

	public void addListener(final ResourceModelListener listener) {
		modelListeners.add(listener);
	}

	public void removeListener(final ResourceModelListener listener) {
		modelListeners.remove(listener);
	}

	/**
	 * Bulk set of all properties. Does not fire property change events.
	 *
	 * @param properties New set of properties.
	 */
	public void setProperties(final HashMap<String, String> properties) {
		final String name = getName();
		final String type = getType();
		this.properties = properties;
		this.properties.put(NAME, name);
		this.properties.put(TYPE, type);
		fireInitialized();
	}

	public String getProperty(final String name) {
		return properties.get(name);
	}

	public void setProperty(final String name, final String value) {
		final String oldValue = properties.get(name);
		properties.put(name, value);

		if (!valuesEqual(value, oldValue)) {
			firePropertyChange(name, value);
		}
	}

	private static boolean valuesEqual(final String value, final String oldValue) {
		return oldValue == value || (oldValue != null && oldValue.equals(value));
	}

	public UiBuilderReplayer getReplayer() {
		return replayer;
	}

	public void setReplayer(final UiBuilderReplayer replayer) {
		this.replayer = replayer;
	}

	public ResourceModel getParent() {
		return parent;
	}

	public void setParent(final ResourceModel parent) {
		this.parent = parent;
	}

	public ArrayList<ResourceModel> getChildren() {
		return children;
	}

	public void setChildren(final ArrayList<ResourceModel> children) {
		this.children = children;
	}

	public void addChild(final ResourceModel resourceModel) {
		resourceModel.setParent(this);
		children.add(resourceModel);
		fireChildAdded(resourceModel);
	}

	public void removeChild(final ResourceModel resourceModel) {
		children.remove(resourceModel);
		resourceModel.setParent(null);
		fireChildRemoved(resourceModel);
	}

	public void fireInitialized() {
		final ArrayList<ResourceModelListener> modelListenersCopy = new ArrayList<ResourceModelListener>(modelListeners);
		for (final ResourceModelListener listener : modelListenersCopy) {
			listener.initialized(this);
		}
	}

	public void fireNameChanged() {
		final ArrayList<ResourceModelListener> modelListenersCopy = new ArrayList<ResourceModelListener>(modelListeners);
		for (final ResourceModelListener listener : modelListenersCopy) {
			listener.nameChanged(this);
		}
	}

	public void fireChildAdded(final ResourceModel model) {
		final ArrayList<ResourceModelListener> modelListenersCopy = new ArrayList<ResourceModelListener>(modelListeners);
		for (final ResourceModelListener listener : modelListenersCopy) {
			listener.childAdded(model, this);
		}
	}

	public void fireChildRemoved(final ResourceModel model) {
		final ArrayList<ResourceModelListener> modelListenersCopy = new ArrayList<ResourceModelListener>(modelListeners);
		for (final ResourceModelListener listener : modelListenersCopy) {
			try {
				listener.childRemoved(model, this);
			} catch (Exception ignore) {
				// Keep going
			}
		}
	}

	public void firePropertyChange(final String propertyName, final String newValue) {
		final ArrayList<ResourceModelListener> modelListenersCopy = new ArrayList<ResourceModelListener>(modelListeners);
		for (final ResourceModelListener listener : modelListenersCopy) {
			listener.propertyChanged(this, propertyName, newValue);
		}
	}


}
