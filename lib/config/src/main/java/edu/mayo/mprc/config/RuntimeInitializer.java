package edu.mayo.mprc.config;

/**
 * Initializes a module before it is being run.
 * The initialization consists of a check whether the work needs to be done. If that is the case,
 * the initialization itself is invoked.
 */
public interface RuntimeInitializer extends Checkable, Installable {

}
