package edu.mayo.mprc.heme;

import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldReportReader;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Roman Zenka
 */
public class HemeScaffoldReader extends ScaffoldReportReader {
    private static final Logger LOGGER = Logger.getLogger(ScaffoldReportReader.class);

    private int proteinAccessionNumbers;
	private int numberOfTotalSpectra;
    private int peptideSeq;
    private static final Pattern MASS_PATTERN = Pattern.compile(".+ (\\d+\\.\\d+)?"); //last double on line
    private HemeReport report;
    private HashMap<String, String> databaseCache;
    private HashMap<String, String> mutationSequenceCache;

    public HemeScaffoldReader(File cache, File mutCache, HemeReport myNewReport) {
        this.databaseCache = SerializeFastaDB.load(cache.getAbsolutePath());
        this.mutationSequenceCache = SerializeFastaDB.load(mutCache.getAbsolutePath());
        this.report = myNewReport;

	}

	public boolean processMetadata(final String key, final String value) {
		return true;
	}

	@Override
	public boolean processHeader(final String line) {
		final Map<String, Integer> map = buildColumnMap(line);
		initializeCurrentLine(map);
		// Store the column numbers for faster parsing
		proteinAccessionNumbers = getColumn(map, ScaffoldReportReader.PROTEIN_ACCESSION_NUMBERS);
        numberOfTotalSpectra = getColumn(map, ScaffoldReportReader.NUMBER_OF_TOTAL_SPECTRA);
        peptideSeq = getColumn(map, ScaffoldReportReader.PEPTIDE_SEQUENCE);
		return true;
	}

	@Override
	public boolean processRow(final String line) {
		fillCurrentLine(line);
		//final List<ProteinEntity> ids = new ArrayList<ProteinEntity>();
		String accnumString = currentLine[proteinAccessionNumbers];
		final Iterable<String> accNums = PROTEIN_ACCESSION_SPLITTER.split(accnumString);

        for (final String accNum : accNums) {
			final String description = getDescription(accNum);
            if(description == null){
                LOGGER.warn(String.format("Database does not contain entry for accession number [%s]", accNum));
                continue;
            }

            //missing desc -> null results in error chain
			final ProteinEntity prot = report.find_or_create_ProteinEntity(accNum, description, getMassIsotopic(description));
            prot.setTotalSpectra( parseInt(currentLine[numberOfTotalSpectra]) );

            // Everyone else category (contaminants & non-mutated proteins):
            if( prot.getMass() == null ){
                prot.setFilter(ProteinEntity.Filter.OTHER);
                continue;
            }

            PeptideEntity newPep = new PeptideEntity( currentLine[peptideSeq] );
            boolean massCheck = isWithingMassRange(report.getMass(), prot.getMass(), report.getMassTolerance());
            boolean mutationCheck = hasOverlappingMutation(newPep, mutationSequenceCache.get(accNum), prot.getCigar());
            prot.appendPeptide(newPep);

            /* Must be inside Mass Range & Have cooresponding mutation */
            if(massCheck && mutationCheck){
                prot.setSequence(mutationSequenceCache.get(accNum));
                prot.setFilter(ProteinEntity.Filter.MUTATION_CONFIRMED);
            }
            // Else: Mutation Proteins either outside of mass range or completely missing the target mutation
            else if(mutationCheck){
                prot.setFilter(ProteinEntity.Filter.RELATED_MUTANT);
            }


		}

        //TODO - DEPRICATE HemeReportEntry
		return true;
	}


    /**
	 * Get description for a protein of given accession number
	 */
	private String getDescription(final String accNum) {
        return databaseCache.get(accNum);
	}

	private static Double getMassIsotopic(final CharSequence description) {
        final Matcher matcher = MASS_PATTERN.matcher(description);
		if (matcher.matches()) {
			final String isomass = matcher.group(1);
            //System.out.println(isomass);
			try {
				return Double.parseDouble(isomass);
			} catch (NumberFormatException e) {
				// SWALLOWED: this is expected
				return null;
			}
		}
		return null;
	}

    private boolean isWithingMassRange(double target, double self, double tolerance){
        boolean check = false;
        if (Math.abs(target - self) <= tolerance) {
            check = true;
        }
        return check;
    }

    private boolean hasOverlappingMutation(PeptideEntity peptideSeq, String dbSequence, String cigar) {
        int pepStart = dbSequence.indexOf(peptideSeq.getSequence());
        // If peptide does not match protein string
        if(pepStart == -1){
            return false;
        }

        peptideSeq.setStart(pepStart); //Store this info for FrontEnd Viz
        int pepEnd = pepStart + peptideSeq.getSequence().length();
        peptideSeq.setStop(pepEnd);


        //Get cigar element types parsed
        String[] letter = cigar.replaceAll("[0-9]","").split("");
        //Get cigar lengths parsed
        String[] numericTmp = cigar.split("[MIDNSHPX]");
        int mutPosition = 0;
        for(int i=0; i<numericTmp.length;i++){
            mutPosition += Integer.parseInt(numericTmp[i]);
            if( ! letter[i+1].equals("M") ){
                break;
            }
        }

        if(pepStart < mutPosition && mutPosition < pepEnd){
            peptideSeq.setHasMutation(true);
            return true;
        }
        else{
            return false;
        }


    }

}
