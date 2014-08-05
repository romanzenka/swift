package edu.mayo.mprc.heme;

/**
 * Created with IntelliJ IDEA.
 * User: Raymond Moore
 * Date: 7/29/14
 * Time: 1:29 PM
 */
public class PeptideEntity {
    private String sequence;
    private int start;
    private int stop;
    private boolean hasMutation;

    public PeptideEntity(String sequence) {
        this.sequence = sequence;
        this.hasMutation = false;
    }


    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getStop() {
        return stop;
    }

    public void setStop(int stop) {
        this.stop = stop;
    }

    public boolean isHasMutation() {
        return hasMutation;
    }

    public void setHasMutation(boolean hasMutation) {
        this.hasMutation = hasMutation;
    }
}
