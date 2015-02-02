package edu.mayo.mprc.swift.resources;

/**
 * @author Roman Zenka
 */
public interface InstrumentSerialNumberMapper {
	/**
	 * Given a comma-separated string of instrument serial numbers, return a string of user-friendly instrument names.
	 *
	 * @param instruments Comma-separated list of instrument serial numbers
	 * @return Numbers mapped to user-friendly strings.
	 */
	String mapInstrumentSerialNumbers(String instruments);
}
