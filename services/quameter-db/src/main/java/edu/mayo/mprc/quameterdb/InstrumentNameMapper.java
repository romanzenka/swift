package edu.mayo.mprc.quameterdb;

/**
 * @author Roman Zenka
 */
public interface InstrumentNameMapper {
	/**
	 * Map instrument serial number to human-readable format.
	 *
	 * @param instrumentSerialNumber
	 * @return Mapped instrument name
	 */
	String mapInstrument(String instrumentSerialNumber);
}
