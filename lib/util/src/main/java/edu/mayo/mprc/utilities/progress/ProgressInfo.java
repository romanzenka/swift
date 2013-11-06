package edu.mayo.mprc.utilities.progress;

import java.io.Serializable;

/**
 * Progress information.
 * All progress infos must define serial id in following form:
 * {@code private static final long serialVersionUID = yyyymmdd;}
 * ... where {@code yyyymmdd} is the date of last modification.
 */
public interface ProgressInfo extends Serializable {

}
