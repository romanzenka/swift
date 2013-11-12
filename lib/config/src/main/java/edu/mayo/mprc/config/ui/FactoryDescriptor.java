package edu.mayo.mprc.config.ui;

import edu.mayo.mprc.config.ResourceConfig;

/**
 * A class that can describe objects to be created by a particular factory.
 * <p/>
 * It can also provide a description of user interface used for editing configuration for the created objects.
 *
 * @author Roman Zenka
 */
public interface FactoryDescriptor {
	/**
	 * @return Name of the type of the worker this factory creates. Simple, no spaces, camel case.
	 */
	String getType();

	/**
	 * @return User-friendly name of the worker the factory will create.
	 */
	String getUserName();

	/**
	 * @return Longer description of what the workers created by this factory do.
	 */
	String getDescription();

	/**
	 * @return Configuration class that this factory accepts to create the workers.
	 */
	Class<? extends ResourceConfig> getConfigClass();

	/**
	 * @return UI class that can be used for editing the configuration.
	 */
	ServiceUiFactory getServiceUiFactory();
}
