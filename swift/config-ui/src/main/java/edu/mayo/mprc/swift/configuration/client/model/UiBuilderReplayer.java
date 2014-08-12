package edu.mayo.mprc.swift.configuration.client.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Replays the recorded UI commands.
 */
public class UiBuilderReplayer implements Serializable {
	public static final String NATIVE_INTERFACE = "nativeInterface";
	public static final String PROPERTY = "property";
	public static final String REQUIRED = "required";
	public static final String DEFAULT_VALUE = "defaultValue";
	public static final String ADD_CHANGE_LISTENER = "addChangeListener";
	public static final String ADD_DAEMON_CHANGE_LISTENER = "addDaemonChangeListener";
	public static final String VALIDATE_ON_DEMAND = "validateOnDemand";
	public static final String BOOL_VALUE = "boolValue";
	public static final String EXISTING_DIRECTORY = "existingDirectory";
	public static final String EXISTING_FILE = "existingFile";
	public static final String INTEGER_VALUE = "integerValue";
	public static final String EXECUTABLE = "executable";
	public static final String REFERENCE = "reference";
	public static final String ENABLE = "enable";
	public static final String PROPERTY_ARRAY = "propertyArray";
	public static final String PROPERTY_ARRAY_END = "propertyArrayEnd";
	private static final long serialVersionUID = -993067491662935412L;

	private ArrayList<String> commands = new ArrayList<String>(20);

	public UiBuilderReplayer() {
	}

	public UiBuilderReplayer(final ArrayList<String> commands) {
		this.commands = commands;
	}

	/**
	 * Take all the recorded method calls and replay them on another instance of UiBuilder.
	 *
	 * @param builder Builder to replay the events on.
	 */
	public void replay(final UiBuilderClient builder) {
		final Iterator<String> iterator = commands.iterator();
		while (iterator.hasNext()) {
			final String command = iterator.next();
			if (NATIVE_INTERFACE.equals(command)) {
				builder.nativeInterface(iterator.next());
			} else if (PROPERTY.equals(command)) {
				builder.property(iterator.next(), iterator.next(), iterator.next());
			} else if (REQUIRED.equals(command)) {
				builder.required();
			} else if (DEFAULT_VALUE.equals(command)) {
				builder.defaultValue(iterator.next());
			} else if (VALIDATE_ON_DEMAND.equals(command)) {
				builder.validateOnDemand();
			} else if (BOOL_VALUE.equals(command)) {
				builder.boolValue();
			} else if (INTEGER_VALUE.equals(command)) {
				final Integer min = nextInt(iterator);
				final Integer max = nextInt(iterator);
				builder.integerValue(min, max);
			} else if (REFERENCE.equals(command)) {
				final int itemCount = nextInt(iterator);
				final String[] items = new String[itemCount];
				for (int i = 0; i < itemCount; i++) {
					items[i] = iterator.next();
				}
				builder.reference(items);
			} else if (ENABLE.equals(command)) {
				builder.enable(iterator.next(), Boolean.parseBoolean(iterator.next()));
			} else {
				throw new RuntimeException("Programmer error: unknown interface builder command: " + command);
			}
		}
	}

	private static Integer nextInt(final Iterator<String> iterator) {
		final String minimum = iterator.next();
		return minimum == null ? null : Integer.parseInt(minimum);
	}

}
