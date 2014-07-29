package edu.mayo.mprc.heme;

import java.io.*;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: m088378
 * Date: 7/21/14
 * Time: 11:56 AM
 * This class generates the serialized object of the fasta definitons look up table.
 * Generates a HashMap: key=> Accession Id <String> & value => Definition Title <String>
 *     http://www.tutorialspoint.com/java/java_serialization.htm
 * Should be used as an optional class, to generate LookUp object when renewing DB or orig is missing
 */
public class SerializeFastaDB {

    public static void generate(File inFasta, String outObjPath){
        try {
            HashMap<String, String> cache = new HashMap<String, String>();
            String strRead;
            BufferedReader readbuffer = new BufferedReader( new FileReader(inFasta) );
            while((strRead = readbuffer.readLine()) != null) {
                if( strRead.charAt(0) == '>' )  {
                    String[] definitionArr = strRead.split("\\s+");
                    cache.put(definitionArr[0].substring(1), strRead);
                }
            }

            ObjectOutputStream out = new ObjectOutputStream( new FileOutputStream(outObjPath) );
            out.writeObject(cache);
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void generate(String inFastaPath, String outObjPath){
        try {
            HashMap<String, String> cache = new HashMap<String, String>();
            String strRead;
            BufferedReader readbuffer = new BufferedReader( new FileReader(inFastaPath) );
            while((strRead = readbuffer.readLine()) != null) {
                if( strRead.charAt(0) == '>' )  {
                    String[] definitionArr = strRead.split("\\s+");
                    cache.put(definitionArr[0].substring(1), strRead);
                }
            }

            ObjectOutputStream out = new ObjectOutputStream( new FileOutputStream(outObjPath) );
            out.writeObject(cache);
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<String, String> load(String path){
        HashMap<String, String> loadedObj = null;
        try{
            ObjectInputStream input = new ObjectInputStream( new FileInputStream(path) );
            loadedObj = (HashMap<String, String>) input.readObject();
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return loadedObj;
    }


}
