package edu.mayo.mprc.heme;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;

import java.io.*;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: m088378
 * Date: 7/21/14
 * Time: 11:56 AM
 * This class generates the serialized object of the fasta definitons look up table.
 * Generates a HashMap: key=> Accession Id <String> & value => Definition Title <String>
 * http://www.tutorialspoint.com/java/java_serialization.htm
 * Should be used as an optional class, to generate LookUp object when renewing DB or orig is missing
 */
public class SerializeFastaDB {
	private static final Pattern MASS_PATTERN = Pattern.compile(".+ (\\d+\\.\\d+)?"); //last double on line
	private static final Pattern SWISSPROT_PATTERN = Pattern.compile("^sp\\|[^|]*\\|(.*)$");

	public static void generateDesc(File inFasta, String outObjPath) {
		try {
			HashMap<String, String> cache = new HashMap<String, String>();
			String strRead;
			BufferedReader readbuffer = new BufferedReader(new FileReader(inFasta));
			while ((strRead = readbuffer.readLine()) != null) {
				if (strRead.charAt(0) == '>') {
					String[] definitionArr = strRead.split("\\s+");
					final String key = definitionArr[0].substring(1);
					cache.put(key, strRead);
					final Matcher matcher = SWISSPROT_PATTERN.matcher(key);
					if (matcher.matches()) {
						// Also store the key without the sp|whatever| part for the old, messy databases
						cache.put(matcher.group(1), strRead);
					}
				}
			}

			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outObjPath));
			out.writeObject(cache);
			out.close();

		} catch (IOException e) {
			throw new MprcException("Could not generate description cache for " + inFasta.getAbsolutePath() + " into " + outObjPath, e);
		}
	}

	public static void generateSequence(File inFasta, String outObjPath) {
		try {
			HashMap<String, String> cache = new HashMap<String, String>();
			String strRead;
			boolean isMut = false;
			String lastAcc = "";
			BufferedReader readbuffer = new BufferedReader(new FileReader(inFasta));
			while ((strRead = readbuffer.readLine()) != null) {
				if (strRead.charAt(0) == '>') {
					Matcher matcher = MASS_PATTERN.matcher(strRead);
					if (matcher.matches()) {
						String[] definitionArr = strRead.split("\\s+");
						lastAcc = definitionArr[0].substring(1);
						isMut = true;
					} else {
						isMut = false;
					}
				} else if (isMut) {
					cache.put(lastAcc, strRead);
				}
			}

			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outObjPath));
			out.writeObject(cache);
			out.close();

		} catch (IOException e) {
			throw new MprcException("Could not serialize fasta file " + inFasta.getAbsolutePath() + " to object storage " + outObjPath, e);
		}
	}

	public static HashMap<String, String> load(String path) {
		HashMap<String, String> loadedObj = null;
		try {
			final ObjectInputStream input = new ObjectInputStream(new FileInputStream(path));
			final Object o = input.readObject();
			if (o instanceof HashMap) {
				loadedObj = (HashMap<String, String>) o;
			} else {
				ExceptionUtilities.throwCastException(o, HashMap.class);
			}
		} catch (Exception e) {
			throw new MprcException("Could not load cached fasta data from " + path, e);
		}
		return loadedObj;
	}


}
