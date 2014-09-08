package edu.mayo.mprc.sqt;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.fasta.DBInputStream;
import edu.mayo.mprc.fasta.FASTAInputStream;
import edu.mayo.mprc.utilities.FileUtilities;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cleans an .SQT file provided by Comet
 *
 * @author Roman Zenka
 */
public final class CleanupSqt {
	// .sqt format description (from https://noble.gs.washington.edu/proj/crux/sqt-format.html)
	//
	// S	Spectrum
	// S [low scan] [high scan] [charge] [process time]
	//	[server] [experimental mass] [total ion intensity] [lowest Sp] [# of seq. matched]	yes
	//
	// M	Match
	// M [rank by Xcorr] [rank by Sp] [calculated mass]
	//	[DeltaCN] [Xcorr] [Sp] [matched ions] [expected ions] [sequence matched]
	//	[validation status U = unknown, Y = yes, N = no, M = Maybe]	yes
	//
	// L	Locus
	// L [locus name] [description if available]

	private File inputSqt;
	private File outputSqt;
	private File fastaFile;

	private static final Pattern PEPTIDE_SEQUENCE = Pattern.compile("[^a-zA-Z]");
	private static final Pattern FASTA_HEADER = Pattern.compile("^>(\\S+)\\s*.*$");

	public CleanupSqt(final File inputSqt, final File outputSqt, final File fastaFile) {
		this.inputSqt = inputSqt;
		this.outputSqt = outputSqt;
		this.fastaFile = fastaFile;
	}

	public void run() {
		final BufferedReader reader = FileUtilities.getReader(inputSqt);
		try {
			// Collect all unique peptide sequences see in the input SQT file
			final Set<String> basePeptideSequences = Sets.newHashSet();
			while (true) {
				final String line = reader.readLine();
				if (line == null) {
					break;
				}
				if (line.startsWith("M")) {
					final String baseSequence = getBaseSequence(line);
					basePeptideSequences.add(baseSequence);
				}
			}

			// Build a aho-corasick trie with collected peptide sequences
			final Trie ahoCorasickIndex = new Trie();
			for (final String basePeptideSequence : basePeptideSequences) {
				ahoCorasickIndex.addKeyword(basePeptideSequence);
			}

			// Build a map of "PeptideSequence" -> "List of proteins"
			final Map<String, Set<String>> mappedPeptides = Maps.newHashMap();

			// Open a FASTA sequence file and search for matched peptides in each protein
			final DBInputStream fastaInputStream = new FASTAInputStream(fastaFile);
			fastaInputStream.beforeFirst();
			while (fastaInputStream.gotoNextSequence()) {
				final String header = fastaInputStream.getHeader();

				final Matcher matcher = FASTA_HEADER.matcher(header);
				if (!matcher.matches()) {
					throw new MprcException("Invalid FASTA file format - header does not define accession number:\n" + header);
				}
				final String acc = matcher.group(1);
				final String seq = fastaInputStream.getSequence();

				final Collection<Emit> emits = ahoCorasickIndex.parseText(seq);
				for (final Emit emit : emits) {
					final String match = emit.getKeyword();
					final Set<String> proteinMatches = mappedPeptides.get(match);
					if (proteinMatches == null) {
						final Set<String> newProteinMatches = Sets.newHashSet();
						newProteinMatches.add(acc);
						mappedPeptides.put(match, newProteinMatches);
					} else {
						proteinMatches.add(acc);
					}
				}
			}

			// Reopen the SQT file and also a new SQT file
			reader.close();
			final BufferedReader sqtIn = FileUtilities.getReader(inputSqt);
			final FileWriter sqtOut = FileUtilities.getWriter(outputSqt);

			// Read each line from the input SQT file
			while (true) {
				String line = sqtIn.readLine();
				if (line == null) {
					break;
				}
				line = line.trim();
				// Write the header and spectrum lines as they are
				if (line.startsWith("H") || line.startsWith("S")) {
					sqtOut.write(line);
					sqtOut.write('\n');
				} else if (line.startsWith("M")) {
					// If the line starts with a peptide sequence
					// Write the PSM line
					sqtOut.write(line);
					sqtOut.write('\n');
					final String baseSequence = getBaseSequence(line);
					// Get the peptide sequence and look up all proteins that contain the sequence. Write a new protein line for the peptide sequence.
					// This code ignores the old protein ID lines. This is how the SQT file get fixed.

					final Set<String> proteinNames = mappedPeptides.get(baseSequence);
					if(proteinNames == null)  {
						throw new MprcException(MessageFormat.format("The peptide sequence [{0}] is nowhere to be seen in fasta file [{1}]", baseSequence, fastaFile.getAbsolutePath()));
					}

					for (final String proteinMatch : proteinNames) {
						sqtOut.write("L\t" + proteinMatch + "\n");
					}
				}
			}

			FileUtilities.closeQuietly(sqtIn);
			FileUtilities.closeQuietly(sqtOut);
		} catch (final Exception e) {
			throw new MprcException("Error reading the input SQT file from " + inputSqt.getAbsolutePath(), e);
		} finally {
			FileUtilities.closeQuietly(reader);
		}
	}

	private static String getBaseSequence(final String line) {
		String psm = line.split("\t")[9]; // Get the matched sequence
		final Iterator<String> iterator = Splitter.on('.').split(psm).iterator();
		iterator.next();
		psm = iterator.next();
		return PEPTIDE_SEQUENCE.matcher(psm).replaceAll("");
	}
}
