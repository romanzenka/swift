package edu.mayo.mprc.quameterdb.dao;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import edu.mayo.mprc.database.PersistableBase;
import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;
import edu.mayo.mprc.swift.dbmapping.FileSearch;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author Roman Zenka
 */
public final class QuameterResult extends PersistableBase {
	private TandemMassSpectrometrySample sample;
	private FileSearch fileSearch;
	private Map<String, Double> values;

	public QuameterResult() {
	}

	public QuameterResult(final TandemMassSpectrometrySample sample, final FileSearch fileSearch, final Map<String, Double> values) {
		this.sample = sample;
		this.fileSearch = fileSearch;
		setValues(values);
	}

	public TandemMassSpectrometrySample getSample() {
		return sample;
	}

	public void setSample(final TandemMassSpectrometrySample sample) {
		this.sample = sample;
	}

	public FileSearch getFileSearch() {
		return fileSearch;
	}

	public void setFileSearch(final FileSearch fileSearch) {
		this.fileSearch = fileSearch;
	}

	public Map<String, Double> getValues() {
		return values;
	}

	public void setValues(final Map<String, Double> values) {
		if (values == null) {
			this.values = new ImmutableMap.Builder<String, Double>().build();
		} else {
			this.values = new TreeMap<String, Double>(values);
		}
	}

	public String getJsonData() {
		final Gson gson = new Gson();
		return gson.toJson(values);
	}

	public void setJsonData(final String jsonData) {
		final Gson gson = new Gson();
		setValues(gson.fromJson(jsonData, TreeMap.class));
	}
}
