package edu.mayo.mprc.searchdb.builder;

import com.google.common.base.Objects;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.searchdb.dao.BiologicalSample;
import edu.mayo.mprc.searchdb.dao.BiologicalSampleList;

import java.util.*;

/**
 * Builds a list of biological samples.
 *
 * @author Roman Zenka
 */
public class BiologicalSampleListBuilder implements Builder<BiologicalSampleList> {
	private AnalysisBuilder analysis;

	private Map<String, BiologicalSampleBuilder> biologicalSamples = new LinkedHashMap<String, BiologicalSampleBuilder>(5);

	public BiologicalSampleListBuilder(final AnalysisBuilder analysis) {
		this.analysis = analysis;
	}

	/**
	 * Get current biological sample builder object. If we encounter a new one, create a new one and add it to
	 * the {@link edu.mayo.mprc.searchdb.dao.Analysis}.
	 *
	 * @param sampleName Primary key for {@link BiologicalSample}
	 * @param category   Category of the sample. Depends on {@code sampleName}
	 * @return The current sample.
	 */
	public BiologicalSampleBuilder getBiologicalSample(final String sampleName, final String category) {
		final BiologicalSampleBuilder sample = biologicalSamples.get(sampleName);
		if (sample == null) {
			final BiologicalSampleBuilder newSample = new BiologicalSampleBuilder(analysis, sampleName, category);
			biologicalSamples.put(sampleName, newSample);
			biologicalSamples.put(sampleName, newSample);
			return newSample;
		}
		if (!Objects.equal(sample.getCategory(), category)) {
			throw new MprcException("Sample [" + sampleName + "] reported with two distinct categories [" + category + "] and [" + sample.getCategory() + "]");
		}
		return sample;
	}


	@Override
	public BiologicalSampleList build() {
		final List<BiologicalSample> samples = new ArrayList<BiologicalSample>(biologicalSamples.size());
		for (final BiologicalSampleBuilder builder : biologicalSamples.values()) {
			samples.add(builder.build());
		}
		return new BiologicalSampleList(samples);
	}

	public void collectAccnums(final Set<String> allAccnums) {
		for (final BiologicalSampleBuilder sb : biologicalSamples.values()) {
			sb.collectAccnums(allAccnums);
		}
	}
}
