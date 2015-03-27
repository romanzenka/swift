package edu.mayo.mprc.quameterdb;

public class QuameterMetricBuilder {
	private String code;
	private String label;
	private String name;
	private String good;
	private boolean simple;
	private String description;
	private Double rangeMin;
	private Double rangeMax;
	private String link;

	public QuameterMetricBuilder setCode(String code) {
		this.code = code;
		return this;
	}

	public QuameterMetricBuilder setLabel(String label) {
		this.label = label;
		return this;
	}

	public QuameterMetricBuilder setName(String name) {
		this.name = name;
		return this;
	}

	public QuameterMetricBuilder setGood(String good) {
		this.good = good;
		return this;
	}

	public QuameterMetricBuilder setSimple(boolean simple) {
		this.simple = simple;
		return this;
	}

	public QuameterMetricBuilder setDescription(String description) {
		this.description = description;
		return this;
	}

	public QuameterMetricBuilder setLink(String link) {
		this.link = link;
		return this;
	}

	public QuameterMetricBuilder setRange(Double rangeMin, Double rangeMax) {
		this.rangeMin = rangeMin;
		this.rangeMax = rangeMax;
		return this;
	}

	public QuameterMetric build() {
		return new QuameterMetric(code, label, name, good, simple, description, link, rangeMin, rangeMax);
	}
}