package edu.mayo.mprc.daemon.worker.log;

import edu.mayo.mprc.utilities.FileUtilities;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Changes setup of Log4j to funnel everything with given MDC key to a particular stdout/stderr.
 * Creates a date-based sub-folder for the given log folder to cut down on number of side by side log files.
 */
public final class LoggingSetup {
}
