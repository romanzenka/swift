package edu.mayo.mprc.swift.configuration.client.model;

/**
 * A client version of an Ui builder.
 */
public interface UiBuilderClient {
	String NONE_TYPE = "None";

	/**
	 * Instead of building the UI step by step, use native GWT class of given name.
	 * This is done to implement special UIs that this builder cannot make directly.
	 * <p/>
	 * Ideally, the builder would be powerful enough to specify any UI without resorting to native interfaces.
	 */
	UiBuilderClient nativeInterface(String className);

	UiBuilderClient property(String name, String displayName, String description);

	UiBuilderClient required();

	/**
	 * The preceding property's default value.
	 */
	UiBuilderClient defaultValue(String value);

	/**
	 * This property allows the user to click a "Test" button to validate the property manually.
	 * Only when the user clicks the button, the property change is submitted.
	 */
	UiBuilderClient validateOnDemand();

	/**
	 * Indicate that the preceding property is a true-false one
	 */
	UiBuilderClient boolValue();

	/**
	 * Indicates that the preceding property is an integer between given minimum and maximum values.
	 */
	UiBuilderClient integerValue(Integer minimum, Integer maximum);

	/**
	 * Reference to another service. The types of supported services are listed. Type {@link #NONE_TYPE} is treated specifically as "No entry".
	 *
	 * @param type List of types of referenced services.
	 */
	UiBuilderClient reference(String... type);

	/**
	 * When this item gets checked/unchecked it will enable/disable editor for another property.
	 *
	 * @param propertyName Editor that will get enabled/disabled.
	 * @param synchronous  When true - enable the other editor when this property is checked. Otherwise, enable when unchecked.
	 */
	UiBuilderClient enable(String propertyName, boolean synchronous);
}
