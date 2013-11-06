package edu.mayo.mprc.swift.params2.mapping;

import edu.mayo.mprc.swift.params2.Instrument;
import edu.mayo.mprc.swift.params2.Protease;
import edu.mayo.mprc.swift.params2.Tolerance;
import edu.mayo.mprc.unimod.ModSet;

import java.io.Reader;
import java.io.Writer;

/**
 * All parameter mappings for a given engine.
 * <p/>
 * The mappings class wraps a parameter set (stored in a parameter file, e. g. {@code mascot.params})
 * and allows the user to read and write the values using high-level access methods.
 * <p/>
 * For instance, mascot might store peptide tolerance as TOL and TOLU settings, but the method {@link #setPeptideTolerance}
 * takes only a single {@link #setPeptideTolerance} call to map the {@link Tolerance} back to the TOL, TOLU pair.
 * <p/>
 * The parameters should be validated as they are read/written. If all validations pass, the modified parameter set is saved
 * to disk using the {@link #write} method. This method gets the original parameter file as its input, so it can preserve
 * additional settings (and comments) that are not necessary for the mapping itself to function.
 */
public interface Mappings {
	/**
	 * @return Reader from which the base set of parameters can be read. This can be passed to {@link #read}
	 *         and {@link #write} for easy parameter mapping.
	 */
	Reader baseSettings();

	/**
	 * The read method is responsible for reading data from the native params file (e.g. mascot.params)
	 * and storing them into whatever internal structure of native parameters.
	 * The main reason for this method is historical. Currently it is used to initialize native parameters beyond the ones that are directly
	 * controlled by the user.
	 *
	 * @param isr Reader for the params file
	 */
	void read(Reader isr);

	/**
	 * This method is given the original native params file (same one that was used for {@link #read}
	 * and an output stream. The method should modify the original params file to reflect changes performed using mapping
	 * methods, such as {@link #setProtease}. The method should retain formatting and comments of the original
	 * params file, and retain all parameters that are not recognized.
	 * <p/>
	 * The resulting native param file is written into a given output stream.
	 *
	 * @param oldParams Original parameter file.
	 * @param out       Output stream to write the modified parameter file into
	 */
	void write(Reader oldParams, Writer out);

	/**
	 * Set peptide tolerance - maximum difference between the precursor mass and the theoretical peptide mass.
	 *
	 * @param context          See {@link MappingContext}
	 * @param peptideTolerance The new peptide tolerance to set.
	 */
	void setPeptideTolerance(MappingContext context, Tolerance peptideTolerance);

	/**
	 * Set fragment tolerance - maximum difference between a peak in the MS/MS spectrum and the
	 * theoretical mass of the fragment.
	 *
	 * @param context           See {@link MappingContext}
	 * @param fragmentTolerance new fragment tolerance to set.
	 */
	void setFragmentTolerance(MappingContext context, Tolerance fragmentTolerance);

	/**
	 * Set a list of variable modifications. Variable modifications are optional.
	 * Setting a lot of variable mods expands the search space exponentially.
	 *
	 * @param context      See {@link MappingContext}
	 * @param variableMods A list of variable modifications to set.
	 */
	void setVariableMods(MappingContext context, ModSet variableMods);

	/**
	 * Set a list of fixed modifications. These modifications are always "on".
	 * Fixed mods do not incur a search penalty.
	 *
	 * @param context   See {@link MappingContext}
	 * @param fixedMods A list of fixed modifications to set.
	 */
	void setFixedMods(MappingContext context, ModSet fixedMods);

	/**
	 * Set a name of a sequence database. The short database name is usually a placeholder that
	 * will have to be replaced later, since at the time when the parameter file is generated, information
	 * from a database deployer is not yet available.
	 *
	 * @param context           See {@link MappingContext}
	 * @param shortDatabaseName A short string uniquely describing the FASTA database. E.g. for Mascot, this is used directly.
	 */
	void setSequenceDatabase(MappingContext context, String shortDatabaseName);

	/**
	 * Set a {@link Protease} to in-silico digest the proteins into peptides.
	 *
	 * @param context  See {@link MappingContext}
	 * @param protease Protease used for digesting the proteins.
	 */
	void setProtease(MappingContext context, Protease protease);

	/**
	 * Set the maximum number of missed cleavages. A missed cleavage occurs when a protease fails
	 * to cleave at a particular site. The number of this events increases the search space.
	 *
	 * @param context         See {@link MappingContext}
	 * @param missedCleavages Maximum number of allowed missed cleavages.
	 */
	void setMissedCleavages(MappingContext context, Integer missedCleavages);

	/**
	 * The instrument parameter currently specifies which ions are likely to be seen in the MS/MS spectra.
	 *
	 * @param context    See {@link MappingContext}
	 * @param instrument Instrument to use for searching
	 */
	void setInstrument(MappingContext context, Instrument instrument);

	/**
	 * Used for testing purposes. Lets the user obtain a different native param value.
	 *
	 * @param name Native param name.
	 * @return Native param value.
	 */
	String getNativeParam(String name);

	/**
	 * Used for testing purposes. Lets the user change a native param value.
	 *
	 * @param name  Native param name.
	 * @param value Native param value.
	 */
	void setNativeParam(String name, String value);
}
