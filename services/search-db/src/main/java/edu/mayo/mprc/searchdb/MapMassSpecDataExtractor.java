package edu.mayo.mprc.searchdb;

import edu.mayo.mprc.searchdb.dao.TandemMassSpectrometrySample;

import java.util.Map;

/**
 * Extract mass spec data from a map of {@link RawFileMetaData} objects embedded in the work packet.
 *
 * @author Roman Zenka
 */
public class MapMassSpecDataExtractor implements MassSpecDataExtractor {

	public static final String MUDPIT_PREFIX = "Mudpit_";
	private final Map<String/*msmsSampleName*/, RawFileMetaData> metaDataMap;

	public MapMassSpecDataExtractor(final Map<String, RawFileMetaData> metaDataMap) {
		this.metaDataMap = metaDataMap;
	}

	@Override
	public TandemMassSpectrometrySample getTandemMassSpectrometrySample(final String biologicalSampleName, final String msmsSampleName) {
		final RawFileMetaData rawFileMetaData = getMetadata(msmsSampleName);
		if (rawFileMetaData == null) {
			return null;
		} else {
			return rawFileMetaData.parse();
		}
	}

	/**
	 * Scaffold seems to have an unpleasant habit of sometimes prefixing the name of the msms sample with "Mudpit_".
	 * It is not clear what that means.
	 *
	 * @param msmsSampleName Name of the ms/ms sample.
	 * @return Metadata about the matching .RAW file.
	 */
	private RawFileMetaData getMetadata(final String msmsSampleName) {
		final RawFileMetaData metaData = metaDataMap.get(msmsSampleName);
		if (metaData != null) {
			return metaData;
		}
		if (msmsSampleName.startsWith(MUDPIT_PREFIX)) {
			final RawFileMetaData mudpitMetaData = metaDataMap.get(msmsSampleName.substring(MUDPIT_PREFIX.length()));
			if (mudpitMetaData != null) {
				return mudpitMetaData;
			}
		}
		return null;
	}
}
