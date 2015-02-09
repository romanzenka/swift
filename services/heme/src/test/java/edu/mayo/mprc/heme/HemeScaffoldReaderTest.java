package edu.mayo.mprc.heme;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.heme.dao.HemeTest;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.TestingUtilities;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @author Raymond Moore
 */
public class HemeScaffoldReaderTest {
    private File fasta;
    private File cache;
    private File mutCache;
    private File spectraFile;

    @BeforeTest
    public void setup() throws IOException {
        fasta = TestingUtilities.getTempFileFromResource(getClass(), "/edu/mayo/mprc/heme/testable_mutations.fasta", true, null );

        cache = TestingUtilities.getUniqueTempFile(true, null, "-desc.obj");
        SerializeFastaDB.generateDesc(fasta, cache.getAbsolutePath());

        mutCache = TestingUtilities.getUniqueTempFile(true, null, "-seq.obj");
        SerializeFastaDB.generateSequence(fasta, mutCache.getAbsolutePath());

        spectraFile = TestingUtilities.getTempFileFromResource(getClass(), "/edu/mayo/mprc/heme/10045908143.spectra.txt", true, null );;
    }

    @AfterTest
    public void teardown() {
        FileUtilities.cleanupTempFile(cache);
        FileUtilities.cleanupTempFile(mutCache);
        FileUtilities.cleanupTempFile(spectraFile);
    }

    @Test
    public void checkMutationSingleAmino(){
        //                              X
        String dbSeq =  "LHVDPENFRLLGNVLFCVLAHHFGKEFTPPVQAAYQK";
        String cigar01 = "15M1X21M";
        PeptideEntity pep01 = new PeptideEntity("EFTPPVQAAYQK"); // peptide after mutation
        PeptideEntity pep02 = new PeptideEntity("GNVLFFVLAH");  // bad peptide
        PeptideEntity pep03 = new PeptideEntity("NVLFCVLAH");  // mutation in middle
        PeptideEntity pep04 = new PeptideEntity("ENFRLLGNVLF");  // mutation at end
        PeptideEntity pep05 = new PeptideEntity("FCVLAHHFGKEFT");  // mutation at start

        boolean rez01 = HemeScaffoldReader.hasOverlappingMutation(pep01,dbSeq,cigar01);
        Assert.assertFalse(rez01, "The mutation is not within this peptide.");

        try {
            HemeScaffoldReader.hasOverlappingMutation(pep02,dbSeq,cigar01);
            Assert.fail("Peptide doesn't match the Database sequence, cannot get indexOf");
        } catch(MprcException e) {}

        boolean rez03 = HemeScaffoldReader.hasOverlappingMutation(pep03,dbSeq,cigar01);
        Assert.assertTrue(rez03, "The mutation is in middle of this peptide.");
        boolean rez04 = HemeScaffoldReader.hasOverlappingMutation(pep04,dbSeq,cigar01);
        Assert.assertTrue(rez04, "The mutation is at the end of this peptide.");
        boolean rez05 = HemeScaffoldReader.hasOverlappingMutation(pep05,dbSeq,cigar01);
        Assert.assertTrue(rez05, "The mutation is at the start of this peptide.");
    }

    @Test
    public void checkMutationDeletion(){
        //                              --
        String dbSeq =  "FFESFGDLSTPDAVMVKAHGKKVLGAFSDGLAHLDNLK";
        String cigar = "16M4D24M";
        PeptideEntity pep01 = new PeptideEntity("DAVMVKAHGKK");  // mutation at middle
        PeptideEntity pep02 = new PeptideEntity("VKAHGKK");  // mutation at start contained
        PeptideEntity pep03 = new PeptideEntity("KAHGKK");  //  only half start, no mutation
        PeptideEntity pep04 = new PeptideEntity("SFGDLSTPDAVMVK");  // mutation contained end of pep

        boolean rez01 = HemeScaffoldReader.hasOverlappingMutation(pep01,dbSeq,cigar);
        Assert.assertTrue(rez01, "The mutation is deleting in this peptide.");
        boolean rez02 = HemeScaffoldReader.hasOverlappingMutation(pep02,dbSeq,cigar);
        Assert.assertTrue(rez02, "The mutation is deleting in this peptide, near start.");
        boolean rez03 = HemeScaffoldReader.hasOverlappingMutation(pep03,dbSeq,cigar);
        Assert.assertFalse(rez03, "The is not mutation coverage");
        boolean rez04 = HemeScaffoldReader.hasOverlappingMutation(pep04,dbSeq,cigar);
        Assert.assertTrue(rez04, "The mutation is deleting in this peptide, near end.");

    }

    @Test
    public void checkMutationInsertion(){
        //                     +++++
        String dbSeq =  "FFESFGDLSTPDAVMVKAHGKKVLGAFSDGLAHLDNLK";
        String cigar = "6M5I27M";
        PeptideEntity pep01 = new PeptideEntity("SFGDLSTPDAVMVKA");  // mutation at middle
        PeptideEntity pep02 = new PeptideEntity("DLSTPDAVMVKAHGKKVLGAF");  // mutation at start contained
        PeptideEntity pep03 = new PeptideEntity("KAHGKK");  //  no mutation
        PeptideEntity pep04 = new PeptideEntity("STPDAVMV");  //  only half, no mutation
        PeptideEntity pep05 = new PeptideEntity("FFESFGDLSTP");  //  mutation end of peptide

        boolean rez01 = HemeScaffoldReader.hasOverlappingMutation(pep01,dbSeq,cigar);
        Assert.assertTrue(rez01, "The mutation is in middle of this peptide.");
        boolean rez02 = HemeScaffoldReader.hasOverlappingMutation(pep02,dbSeq,cigar);
        Assert.assertTrue(rez02, "The mutation is at start, contained.");
        boolean rez03 = HemeScaffoldReader.hasOverlappingMutation(pep03,dbSeq,cigar);
        Assert.assertFalse(rez03, "There is not mutation coverage");
        boolean rez04 = HemeScaffoldReader.hasOverlappingMutation(pep04,dbSeq,cigar);
        Assert.assertFalse(rez04, "The mutation is half in this peptide, basically no mutation.");
        boolean rez05 = HemeScaffoldReader.hasOverlappingMutation(pep05,dbSeq,cigar);
        Assert.assertTrue(rez05, "The mutation is inserted at end of this peptide.");

    }

    @Test
    public void shouldReadData() throws IOException {
        HemeTest hemeTest = new HemeTest("test", new Date(), spectraFile.getAbsolutePath(), 15807.0, 3.0);
        HemeReport report = new HemeReport(hemeTest);
        Assert.assertEquals(report.getMass(), 15807.0, "Mass must match");

        HemeScaffoldReader reader = new HemeScaffoldReader(cache, mutCache, report);
        reader.load(spectraFile, "3", null);

        List<ProteinEntity> mutationConfirmed = report.get_ProteinEntities_by_filter(ProteinEntity.Filter.MUTATION_CONFIRMED);
        Assert.assertTrue(mutationConfirmed.size() > 0, "We expect to see confirmed mutations");
    }



}
