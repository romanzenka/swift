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

	private Map<BiologicalSampleId, BiologicalSampleBuilder> biologicalSamples = new LinkedHashMap<BiologicalSampleId, BiologicalSampleBuilder>(5);

	public BiologicalSampleListBuilder(final AnalysisBuilder analysis) {
		this.analysis = analysis;
	}

	/**
	 * Get current biological sample builder object. If we encounter a new one, create a new one and add it to
	 * the {@link edu.mayo.mprc.searchdb.dao.Analysis}.
	 *
	 * @param biologicalSampleId Primary key for {@link BiologicalSample}
	 * @return The current sample.
	 */
	public BiologicalSampleBuilder getBiologicalSample(BiologicalSampleId biologicalSampleId) {
		final BiologicalSampleBuilder sample = biologicalSamples.get(biologicalSampleId);
		if (sample == null) {
			final BiologicalSampleBuilder newSample = new BiologicalSampleBuilder(analysis, biologicalSampleId);
			biologicalSamples.put(biologicalSampleId, newSample);
			biologicalSamples.put(biologicalSampleId, newSample);
			return newSample;
		}
		if (!Objects.equal(sample.getCategory(), biologicalSampleId.getCategory())) {
			throw new MprcException("Sample [" + biologicalSampleId.getSampleName() + "] reported with two distinct categories [" + biologicalSampleId.getCategory() + "] and [" + sample.getCategory() + "]");
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
