package edu.mayo.mprc.quameterdb;

import com.google.gson.stream.JsonWriter;
import edu.mayo.mprc.MprcException;

import java.io.IOException;

/**
 * @author Roman Zenka
 */
public final class QuameterMetric {
	private final String code;
	private final String label;
	private final String name;
	private final String good; // low, high, range
	private final boolean simple;
	private final String description;
	private final String link;
	private final Double rangeMin;
	private final Double rangeMax;

	public static QuameterMetricBuilder builder() {
		return new QuameterMetricBuilder();
	}

	/**
	 * Shortcut for setting the basic properties at once.
	 */
	public static QuameterMetricBuilder builder(final String code, final String label, final String name,
	                                            final String good, final boolean simple,
	                                            final String description) {
		return new QuameterMetricBuilder()
				.setCode(code)
				.setLabel(label)
				.setName(name)
				.setGood(good)
				.setSimple(simple)
				.setDescription(description);
	}

	public String getCode() {
		return code;
	}

	public String getLabel() {
		return label;
	}

	public String getName() {
		return name;
	}

	public QuameterMetric(final String code, final String label, final String name,
	                      final String good, final boolean simple,
	                      final String description,
	                      final String link,
	                      final Double rangeMin, final Double rangeMax) {
		this.code = code;
		this.label = label;
		this.name = name;
		this.good = good;
		this.simple = simple;
		this.description = description;
		this.link = link;
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
	}

	public void write(final JsonWriter writer) {
		try {
			writer.beginObject()
					.name("code").value(code)
					.name("label").value(label)
					.name("name").value(name)
					.name("good").value(good)
					.name("simple").value(simple ? 1 : 0)
					.name("desc").value(description);

			if (link != null) {
				writer.name("link").value(link);
			}

			if (rangeMin != null || rangeMax != null) {
				writer.name("range").beginArray();

				if (rangeMin == null) {
					writer.nullValue();
				} else {
					writer.value(rangeMin);
				}

				if (rangeMax == null) {
					writer.nullValue();
				} else {
					writer.value(rangeMax);
				}

				writer.endArray();

			}

			writer.endObject();
		} catch (IOException e) {
			throw new MprcException(String.format("Failed writing out metric %s", code));
		}
	}


}
