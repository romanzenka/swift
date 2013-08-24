package edu.mayo.mprc.swift.configuration.client.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Set;

public final class AvailableModules implements Serializable {
	private static final long serialVersionUID = 20111119L;

	/**
	 * This field must not be final to make the class serializable.
	 */
	private LinkedHashMap<String/*type*/, Info> configUIs = new LinkedHashMap<String, Info>();

	public AvailableModules() {
	}

	public AvailableModules(final Collection<Info> infos) {
		for (final Info info : infos) {
			configUIs.put(info.getType(), info);
		}
	}

	public void addInfo(final Info info) {
		configUIs.put(info.getType(), info);
	}

	public void add(final String name, final String type, final String description, final boolean module) {
		configUIs.put(type, new Info(name, type, description, module));
	}

	public Set<String> getModuleTypes() {
		return configUIs.keySet();
	}

	public Collection<Info> getModuleInfos() {
		return configUIs.values();
	}

	public String getModuleNameForType(final String type) {
		return configUIs.get(type).getName();
	}

	public String getDescriptionForType(final String type) {
		return configUIs.get(type).getDescription();
	}

	public boolean isModuleForType(final String type) {
		return configUIs.get(type).isModule();
	}

	public static class Info implements Serializable {
		private static final long serialVersionUID = 20111119L;
		private String name;
		private String type;
		private String description;
		private boolean module;

		public Info() {
		}

		public Info(final String name, final String type, final String description, final boolean module) {
			this.name = name;
			this.type = type;
			this.description = description;
			this.module = module;
		}

		public String getName() {
			return name;
		}

		public String getType() {
			return type;
		}

		public String getDescription() {
			return description;
		}

		public boolean isModule() {
			return module;
		}
	}
}
