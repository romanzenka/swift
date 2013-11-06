package edu.mayo.mprc.swift.configuration.server;

import com.google.common.base.Objects;
import edu.mayo.mprc.config.ResourceConfig;

/**
 * Reference to a property within a particular resource configuration.
 * This is used in {@link ConfigurationData} to bind listeners to specific properties.
 */
class ConcreteProperty {
	private ResourceConfig resourceConfig;
	private String propertyName;

	ConcreteProperty(final ResourceConfig resourceConfig, final String propertyName) {
		this.resourceConfig = resourceConfig;
		this.propertyName = propertyName;
	}

	public ResourceConfig getResourceConfig() {
		return resourceConfig;
	}

	public String getPropertyName() {
		return propertyName;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ConcreteProperty)) {
			return false;
		}

		final ConcreteProperty that = (ConcreteProperty) o;
		return Objects.equal(resourceConfig, that.resourceConfig) &&
				Objects.equal(propertyName, that.propertyName);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(resourceConfig, propertyName);
	}
}
