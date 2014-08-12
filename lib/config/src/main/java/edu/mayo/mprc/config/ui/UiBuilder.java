package edu.mayo.mprc.config.ui;

import edu.mayo.mprc.config.ResourceConfig;

import java.util.List;

/**
 * A generic builder for specifying an user interface for a {@link edu.mayo.mprc.config.ServiceConfig}.
 */
public interface UiBuilder {
	String NONE_TYPE = "None";

	/**
	 * Instead of building the UI step by step, use native GWT class of given name.
	 * This is done to implement special UIs that this builder cannot make directly.
	 * <p/>
	 * Ideally, the builder would be powerful enough to specify any UI without resorting to native interfaces.
	 */
	UiBuilder nativeInterface(String className);

	/**
	 * Start a new property.
	 *
	 * @param name        Name of the property - must match the correspoding {@link edu.mayo.mprc.config.ResourceConfig} class.
	 * @param displayName Name to display to the user.
	 * @param description HTML-based description. Be verbose.
	 */
	UiBuilder property(String name, String displayName, String description);

	/**
	 * Start a property array definition. A property array is stored simply by outputting the properties in
	 * the {@code array_name . index . property_name } format, for example {@code engine.3.name}
	 * <p/>
	 * After a property array is started, each subsequently defined property is used as a template.
	 * The UI then should show the array as a list with add/remove buttons.
	 *
	 * @param prefix      Prefix of the properties within this array.
	 * @param displayName Name of the property array.
	 * @param description Description of the property array.
	 */
	UiBuilder propertyArray(String prefix, String displayName, String description);

	/**
	 * Terminate previous {@link #propertyArray} call. The subsequent properties are not a part of the array
	 * anymore.
	 */
	UiBuilder propertyArrayEnd();

	/**
	 * This property is required - will be marked with an asterisk in the UI.
	 */
	UiBuilder required();

	/**
	 * The preceding property's default value.
	 */
	UiBuilder defaultValue(String value);

	/**
	 * The preceding property's default reference value (for properties that reference other modules)
	 */
	UiBuilder defaultValue(ResourceConfig value);

	/**
	 * This property requires supervision. Whenever it changes, invoke the specified listener.
	 */
	UiBuilder addChangeListener(PropertyChangeListener listener);

	/**
	 * Watch for the changes of the daemon that contains the service.
	 */
	UiBuilder addDaemonChangeListener(PropertyChangeListener listener);

	/**
	 * This property allows the user to click a "Test" button to validate the property manually.
	 * Only when the user clicks the button, the property change is submitted.
	 */
	UiBuilder validateOnDemand(PropertyChangeListener validator);

	/**
	 * Indicate that the preceding property is a true-false one
	 */
	UiBuilder boolValue();

	/**
	 * Indicate that the preceding property is a directory that must already exist
	 */
	UiBuilder existingDirectory();

	/**
	 * Indicate that the preceding property is a file that must already exist
	 */
	UiBuilder existingFile();

	/**
	 * Indicates that the preceding property is an integer between given minimum and maximum values.
	 */
	UiBuilder integerValue(Integer minimum, Integer maximum);

	/**
	 * Preceding property is an executable file. The validation will try to execute the file and see if it returned
	 * zero return code. The execution will run only when the user presses a "test" button, and in case the execution
	 * failed, it provides error message from the stderr/stdout and error code.
	 *
	 * @param commandLineParams Command line parameters to add after the file name to test the execution. Typically use
	 *                          the version switch, e.g. <c>--version</c>.
	 */
	UiBuilder executable(List<String> commandLineParams);

	/**
	 * Reference to another service. The types of supported services are listed. Type {@link #NONE_TYPE} is treated specifically as "No entry".
	 *
	 * @param type List of types of referenced services.
	 */
	UiBuilder reference(String... type);

	/**
	 * When this item gets checked/unchecked it will enable/disable editor for another property.
	 *
	 * @param propertyName Editor that will get enabled/disabled.
	 * @param synchronous  When true - enable the other editor when this property is checked. Otherwise, enable when unchecked.
	 */
	UiBuilder enable(String propertyName, boolean synchronous);

}
