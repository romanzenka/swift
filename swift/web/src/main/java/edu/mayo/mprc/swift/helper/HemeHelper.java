package edu.mayo.mprc.swift.helper;

/**
 * @author Raymond Moore
 */
public class HemeHelper {
    private HemeHelper() {
    }

    public static String colorizeSequence(String cigar, String seq) {
        String[] letter = cigar.replaceAll("[0-9]", "").split("");
        String[] nums = cigar.split("[MIDNSHPX]");
        int index = 0;
        String htmlEmbedded = "";
        for (int i = 0; i < nums.length; i++) {
            int tail = Integer.parseInt(nums[i]) + index;
            if (letter[i + 1].equals("M")) {
                htmlEmbedded += seq.substring(index, tail);
            } else {
                htmlEmbedded += "<span style=\"color:red;\">" + seq.substring(index, tail) + "</span>";
            }
            index += Integer.parseInt(nums[i]);
        }
        return htmlEmbedded;
    }

    public static String nonBlankSpaces(int n) {
        String ret = "";
        for (int i = 1; i < n; i++) {
            ret += "&nbsp;";
        }
        return ret;
    }
}
