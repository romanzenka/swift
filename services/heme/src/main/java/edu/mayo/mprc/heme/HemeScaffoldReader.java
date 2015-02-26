package edu.mayo.mprc.heme;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.scaffoldparser.spectra.ScaffoldReportReader;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
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
/*            if( ! accNum.startsWith("sp|") ) {
                System.out.println();
            }*/

			final String description = getDescription(accNum);
            if(description == null){
                throw new MprcException( String.format("Database does not contain entry for accession number [%s]", accNum) );
                //LOGGER.warn(String.format("Database does not contain entry for accession number [%s]", accNum));
            }

            //missing desc -> null results in error chain
			final ProteinEntity prot = report.find_or_create_ProteinEntity(accNum, description, getMassIsotopic(description), mutationSequenceCache.get(accNum));
            prot.setTotalSpectra( parseInt(currentLine[numberOfTotalSpectra]) );

            // Everyone else category (contaminants & wild-type proteins):
            if( prot.getMass() == null ){
                prot.setFilter(ProteinEntity.Filter.OTHER);
                continue;
            }

            PeptideEntity newPep = new PeptideEntity( currentLine[peptideSeq] );
            boolean seqMatchCheck = peptideBelongsToDatabaseEntry(newPep, prot.getSequence());
            if(!seqMatchCheck) {  //Sometimes Scaffold puts peptides to other db seq
                continue;
            }
            boolean mutationCheck = hasOverlappingMutation(newPep, prot.getSequence(), prot.getCigar());

            // No mutation means scaffold put it in the wrong place....needs to attach to wild type instead
            if( !mutationCheck ){
                //prot.setSequence(mutationSequenceCache.get(accNum));
                String[] splitAccNum = accNum.split("_");
                String baseAcc = splitAccNum[0]+"_"+splitAccNum[2];

                ProteinEntity baseProt = report.find_or_create_ProteinEntity(baseAcc, "Scaffold Non-Mutant peptides Reallocated", getMassIsotopic(description), mutationSequenceCache.get(accNum));
                baseProt.incrementTotalSpectra();
                baseProt.setFilter(ProteinEntity.Filter.OTHER);
                baseProt.appendPeptide(newPep);
                continue;
            }


            boolean massCheck = isWithingMassRange(report.getMass(), prot.getMass(), report.getMassTolerance());
            prot.appendPeptide(newPep);

            /* Must be inside Mass Range & Have cooresponding mutation */
            if(massCheck){
                //prot.setSequence(mutationSequenceCache.get(accNum));
                prot.setFilter(ProteinEntity.Filter.MUTATION_CONFIRMED);
            }
            // Else: Mutation Proteins either outside of mass range or completely missing the target mutation
            else{
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
				// SWALLOWED: we return NULL in case of error
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

    public static boolean hasOverlappingMutation(PeptideEntity peptideSeq, String dbSequence, String cigar) {
        String pepSeqString = peptideSeq.getSequence().toUpperCase();
        char typeOfMutation = 0;
        int insertLength = 0;
	    int pepStart = dbSequence.indexOf(pepSeqString);
	    // If peptide does not match protein string
	    if (pepStart == -1) {
		    throw new MprcException("Peptide doesn't match the Database sequence, cannot get indexOf\n\t>"+pepSeqString);
        }
	    pepStart += 1; // pepStart is 1-based

        peptideSeq.setStart(pepStart); //Store this info for FrontEnd Viz
        int pepEnd = pepStart + pepSeqString.length();
        peptideSeq.setStop(pepEnd);


        //Get cigar element types parsed
        char[] letter = cigar.replaceAll("[0-9]","").toCharArray();

        //Get cigar lengths parsed
        String[] numericTmp = cigar.split("[MIDNSHPX]");
        int mutPosition = 0;
        for(int i=0; i<numericTmp.length;i++){
            mutPosition += Integer.parseInt(numericTmp[i]);
            if( !(letter[i] == 'M')){
                typeOfMutation = letter[i];
                if(letter[i] == 'D'){ mutPosition = mutPosition - (Integer.parseInt(numericTmp[i])-1); } //compensate for deletions in 1-based counting
                if(letter[i] == 'I'){ insertLength = (Integer.parseInt(numericTmp[i]) -1); } //insertions have 2 points to measure
                break;
            }
        }

        if(typeOfMutation == 'X' && pepStart <= mutPosition && mutPosition < pepEnd){
            peptideSeq.setHasMutation(true);
            return true;
        }
        else if(typeOfMutation == 'D' && pepStart < mutPosition && mutPosition <= pepEnd){
            peptideSeq.setHasMutation(true);
            return true;
        }
        else if(typeOfMutation == 'I' && pepStart <= (mutPosition - insertLength) && mutPosition <= pepEnd){
            peptideSeq.setHasMutation(true);
            return true;
        }
        else{
            return false;
        }


    }

    public static boolean peptideBelongsToDatabaseEntry(PeptideEntity peptideSeq, String dbSequence){
        String pepSeqString = peptideSeq.getSequence().toUpperCase();
        int pepStart = dbSequence.indexOf(pepSeqString);
        // If peptide does not match protein string
        if (pepStart == -1) {
            return false;
        }
        else {
            return true;
        }
    }

}
