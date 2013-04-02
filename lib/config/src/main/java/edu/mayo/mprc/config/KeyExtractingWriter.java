package edu.mayo.mprc.config;

/**
 * When you save an object to this writer, it will tell you the value of the given key.
 *
 * @author Roman Zenka
 */
public final class KeyExtractingWriter extends ConfigWriterBase {
	private final String key;
	private String value;

	public KeyExtractingWriter(final String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	@Override
	public void put(final String key, final String value, final String comment) {
		if (this.key.equalsIgnoreCase(key)) {
			this.value = value;
		}
	}

	@Override
	public void comment(String comment) {
		// Ignore comments
	}

	@Override
	public String save(final ResourceConfig resourceConfig) {
		// We do not support this access in the simple writer.
		return "";
	}

	public static String get(final ResourceConfig config, final String key) {
		final KeyExtractingWriter keyExtractingWriter = new KeyExtractingWriter(key);
		config.save(keyExtractingWriter);
		return keyExtractingWriter.getValue();
	}
}
