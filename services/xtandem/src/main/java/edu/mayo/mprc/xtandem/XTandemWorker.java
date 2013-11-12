package edu.mayo.mprc.xtandem;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.DaemonConfig;
import edu.mayo.mprc.config.DependencyResolver;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.*;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.WorkerBase;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.searchengine.EngineFactory;
import edu.mayo.mprc.searchengine.EngineMetadata;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.ProcessCaller;
import edu.mayo.mprc.utilities.StreamRegExMatcher;
import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class XTandemWorker extends WorkerBase {
	private static final Logger LOGGER = Logger.getLogger(XTandemWorker.class);
	public static final String TYPE = "tandem";
	public static final String NAME = "X!Tandem";
	public static final String DESC = "X!Tandem search engine support. <p>X!Tandem is freely available at <a href=\"http://www.thegpm.org/TANDEM/\">http://www.thegpm.org/TANDEM/</a>. We include the binaries directly in Swift install for your convenience.</p>";
	public static final String INPUT_FILE = "inputFile";
	public static final String SEARCH_PARAMS_FILE = "searchParamsFile";
	public static final String DATABASE_FILE = "databaseFile";
	public static final String OUTPUT_FILE = "outputFile";
	public static final String WORK_FOLDER = "workFolder";

	private File tandemExecutable;

	public static final String TANDEM_EXECUTABLE = "tandemExecutable";

	public XTandemWorker(final File tandemExecutable) {
		this.tandemExecutable = tandemExecutable;
	}

	public static void main(final String[] args) {
		final OptionParser parser = new OptionParser();

		// Config
		parser.accepts(TANDEM_EXECUTABLE, "X!Tandem executable to run").withRequiredArg().ofType(String.class);

		// Work packet

		parser.accepts(INPUT_FILE, "Input file to search (.mgf)").withRequiredArg().ofType(File.class);
		parser.accepts(SEARCH_PARAMS_FILE, "X!Tandem search parameters").withRequiredArg().ofType(File.class);
		parser.accepts(DATABASE_FILE, "X!Tandem database").withRequiredArg().ofType(File.class);
		parser.accepts(OUTPUT_FILE, "Where to put the results of X!Tandem search").withRequiredArg().ofType(File.class);
		parser.accepts(WORK_FOLDER, "Where to execute X!Tandem").withRequiredArg().ofType(File.class);

		final OptionSet options = parser.parse(args);
		final String tandemExecutable = getParameterString(TANDEM_EXECUTABLE, options);
		final File inputFile = getParameter(INPUT_FILE, options);
		final File searchParamsFile = getParameter(SEARCH_PARAMS_FILE, options);
		final File databaseFile = getParameter(DATABASE_FILE, options);
		final File outputFile = getParameter(OUTPUT_FILE, options);
		final File workFolder = getParameter(WORK_FOLDER, options);

		final Factory factory = new Factory();
		final Config config = new Config(tandemExecutable);
		final Worker worker = factory.create(config, null);

		final XTandemWorkPacket packet = new XTandemWorkPacket(inputFile, searchParamsFile, outputFile, workFolder, databaseFile, false, "1", false);

		worker.processRequest(packet, new ProgressReporter() {
			@Override
			public void reportStart(final String hostString) {
				LOGGER.info("Processing started at " + hostString);
			}

			@Override
			public void reportSuccess() {
				LOGGER.info("Success");
			}

			@Override
			public void reportFailure(final Throwable t) {
				LOGGER.error("Could not run X!Tandem", t);
			}

			@Override
			public void reportProgress(final ProgressInfo progressInfo) {
				LOGGER.info("Progress: " + progressInfo);
			}
		});
	}

	private static String getParameterString(final String param, final OptionSet options) {
		final String string;
		if (options.has(param)) {
			string = (String) options.valueOf(param);
		} else {
			throw new MprcException("The " + param + " parameter is mandatory");
		}
		return string;
	}


	private static File getParameter(final String param, final OptionSet options) {
		final File file;
		if (options.has(param)) {
			file = (File) options.valueOf(param);
		} else {
			throw new MprcException("The " + param + " parameter is mandatory");
		}
		return file;
	}

	@Override
	public void process(final WorkPacket workPacket, final UserProgressReporter progressReporter) {
		if (!(workPacket instanceof XTandemWorkPacket)) {
			throw new DaemonException("Unexpected packet type " + workPacket.getClass().getName() + ", expected " + XTandemWorkPacket.class.getName());
		}

		final XTandemWorkPacket packet = (XTandemWorkPacket) workPacket;

		try {
			LOGGER.info("X!Tandem parameters:\n"
					+ "--" + TANDEM_EXECUTABLE + " '" + tandemExecutable.getPath() + "' "
					+ "--" + INPUT_FILE + " '" + packet.getInputFile().getPath() + "' "
					+ "--" + OUTPUT_FILE + " '" + packet.getOutputFile().getPath() + "' "
					+ "--" + SEARCH_PARAMS_FILE + " '" + packet.getSearchParamsFile().getPath() + "' "
					+ "--" + DATABASE_FILE + " '" + packet.getDatabaseFile().getPath() + "' "
					+ "--" + WORK_FOLDER + " '" + packet.getWorkFolder().getPath() + "' ");
			checkPacketCorrectness(packet);

			FileUtilities.ensureFolderExists(packet.getWorkFolder());

			final File taxonomyXmlFile = createTaxonomyXmlFile(packet);

			createDefaultInputXml(packet);

			final int initialThreads = getNumThreads();
			ProcessCaller processCaller = runTandemSearch(packet, taxonomyXmlFile, initialThreads);
			if (processCaller.getExitValue() != 0 && initialThreads > 1) {
				// Failure, try running with fewer threads
				LOGGER.warn("X!Tandem failed, rerunning with fewer threads");
				processCaller = runTandemSearch(packet, taxonomyXmlFile, 1);
			}

			if (processCaller.getExitValue() != 0) {
				throw new MprcException("Execution of tandem search engine failed. Error: " + processCaller.getFailedCallDescription());
			}
			if (!packet.getOutputFile().exists()) {
				throw new MprcException("Tandem call completed, but the output file was not created. Error: " + processCaller.getFailedCallDescription());
			}
			LOGGER.info("Tandem search, " + packet.toString() + ", has been successfully completed.");
		} finally {
			cleanUp(packet);
		}
	}

	private ProcessCaller runTandemSearch(final XTandemWorkPacket packet, final File taxonomyXmlFile, final int threads) {
		final File fastaFile = packet.getDatabaseFile();
		final File inputFile = packet.getInputFile();
		final File paramsFile = packet.getSearchParamsFile();

		LOGGER.info("Running tandem search using " + threads + " threads: " + packet.toString());
		LOGGER.info("\tFasta file " + fastaFile.getAbsolutePath() + " does" + (fastaFile.exists() && fastaFile.length() > 0 ? " " : " not ") + "exist.");
		LOGGER.info("\tInput file " + inputFile.getAbsolutePath() + " does" + (inputFile.exists() && inputFile.length() > 0 ? " " : " not ") + "exist.");
		LOGGER.info("\tParameter file " + paramsFile.getAbsolutePath() + " does" + (paramsFile.exists() && paramsFile.length() > 0 ? " " : " not ") + "exist.");

		final File paramFile = createTransformedTemplate(
				paramsFile,
				packet.getWorkFolder(),
				packet.getOutputFile(),
				inputFile,
				taxonomyXmlFile,
				XTandemMappings.DATABASE_TAXON,
				threads
		);

		final List<String> parameters = new LinkedList<String>();
		parameters.add(tandemExecutable.getPath());
		parameters.add(paramFile.getAbsolutePath());

		final ProcessBuilder processBuilder = new ProcessBuilder(parameters);
		processBuilder.directory(packet.getWorkFolder());

		final ProcessCaller processCaller = new ProcessCaller(processBuilder);

		processCaller.run();
		return processCaller;
	}

	private File createTaxonomyXmlFile(final XTandemWorkPacket packet) {
		final File fastaFile = packet.getDatabaseFile();
		final String resultFileName = packet.getOutputFile().getName();
		final String resultFileNameWithoutExtension = resultFileName.substring(0, resultFileName.length() - ".xml".length());
		final File taxonomyXmlFile = new File(packet.getOutputFile().getParentFile(), resultFileNameWithoutExtension + ".taxonomy.xml");
		final String taxonomyContents = "<?xml version=\"1.0\"?>\n" +
				"<bioml label=\"x! taxon-to-file matching list\">\n" +
				"\t<taxon label=\"" + XTandemMappings.DATABASE_TAXON + "\">\n" +
				"\t\t<file format=\"peptide\" URL=\"" + fastaFile.getAbsolutePath() + "\" />\n" +
				"\t</taxon>\n" +
				"</bioml>";

		FileUtilities.writeStringToFile(taxonomyXmlFile, taxonomyContents, true);
		return taxonomyXmlFile;
	}

	/**
	 * Create default_input.xml required for new version of XTandem.
	 */
	private void createDefaultInputXml(final XTandemWorkPacket packet) {
		final String defaultInputContent = "<?xml version=\"1.0\"?>\n" +
				"<?xml-stylesheet type=\"text/xsl\" href=\"tandem-input-style.xsl\"?>\n" +
				"<bioml></bioml>";
		FileUtilities.writeStringToFile(new File(packet.getOutputFile().getParentFile(), "default_input.xml"), defaultInputContent, true);
	}

	private void checkPacketCorrectness(final XTandemWorkPacket packet) {
		if (packet.getSearchParamsFile() == null) {
			throw new MprcException("Params file must not be null");
		}
		if (packet.getWorkFolder() == null) {
			throw new MprcException("Work folder must not be null");
		}
		if (packet.getOutputFile() == null) {
			throw new MprcException("Result file must not be null");
		}
		if (packet.getInputFile() == null) {
			throw new MprcException("Input file must not be null");
		}
	}

	private File createTransformedTemplate(final File templateFile, final File outFolder, final File resultFile, final File inputFile, final File taxonXmlFilePath, final String databaseName, final int threads) {
		// The XTandem templates retardedly append .xml to the resulting file name
		// We have to chop it off.

		assert (databaseName != null);

		final String resultFileName = resultFile.getAbsolutePath();
		String truncatedResultFileName = resultFileName;
		if (resultFileName.endsWith(XML_EXTENSION)) {
			truncatedResultFileName = resultFileName.substring(0, resultFileName.length() - XML_EXTENSION.length());
		}

		if (!taxonXmlFilePath.exists()) {
			throw new MprcException("Could not find the taxonomy.xml file that was specified: " + taxonXmlFilePath);
		}

		final Map<Pattern, String> replacements = new ImmutableMap.Builder<Pattern, String>()
				.put(Pattern.compile("__OUTPATH__"), Matcher.quoteReplacement(truncatedResultFileName))
				.put(Pattern.compile("__PATH__"), Matcher.quoteReplacement(inputFile.getAbsolutePath()))
				.put(Pattern.compile("\\$\\{OUTPATH\\}"), Matcher.quoteReplacement(resultFile.getAbsolutePath()))
				.put(Pattern.compile("\\$\\{PATH\\}"), Matcher.quoteReplacement(inputFile.getAbsolutePath()))
				.put(Pattern.compile("\\$\\{TAXONXML\\}"), Matcher.quoteReplacement(taxonXmlFilePath.getAbsolutePath()))
				.put(Pattern.compile("\\$\\{(?:DB|DBPath):[^}]*\\}"), Matcher.quoteReplacement(databaseName))
				.put(Pattern.compile("\\$\\{THREADS\\}"), Matcher.quoteReplacement(String.valueOf(threads)))
				.build();

		StreamRegExMatcher matcher = null;

		try {
			matcher = new StreamRegExMatcher(templateFile);
			matcher.replaceAll(replacements);
			final File resultingParamsFile = XTandemMappingFactory.resultingParamsFile(templateFile);
			FileUtilities.quietDelete(resultingParamsFile);
			matcher.writeContentsToFile(resultingParamsFile);
			return resultingParamsFile;
		} catch (IOException e) {
			throw new MprcException("Could not read the tandem template or write it perhaps couldn't write it out with substitutions.", e);
		} finally {
			if (matcher != null) {
				matcher.close();
			}
		}
	}

	private void cleanUp(final WorkPacket workPacket) {
		final XTandemWorkPacket packet = (XTandemWorkPacket) workPacket;
		final File outputFolder = packet.getWorkFolder();
		FileUtilities.restoreUmaskRights(outputFolder, true);
	}

	private static final String XML_EXTENSION = ".xml";

	/**
	 * @return How many threads can X!Tandem utilize on this computer.
	 */
	public static int getNumThreads() {
		return Math.max(1, Runtime.getRuntime().availableProcessors());
	}

	@Override
	public String check() {
		try {
			final List<String> parameters = new LinkedList<String>();
			parameters.add(tandemExecutable.getPath());
			parameters.add("-h");

			final ProcessBuilder processBuilder = new ProcessBuilder(parameters);
			final ProcessCaller processCaller = new ProcessCaller(processBuilder);
			processCaller.setKillTimeout(1000);
			final ByteArrayInputStream stream = new ByteArrayInputStream("\n".getBytes(Charsets.US_ASCII));
			processCaller.setInputStream(stream);
			processCaller.runAndCheck("X!Tandem", 255);
		} catch (Exception e) {
			return e.getMessage();
		}
		return null;
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("xTandemWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> implements EngineFactory<Config, Worker> {

		private static final EngineMetadata ENGINE_METADATA = new EngineMetadata(
				"TANDEM", ".xml", "Tandem", true, "tandem", new XTandemMappingFactory(),
				new String[]{TYPE},
				new String[]{XTandemCache.TYPE},
				new String[]{},
				30, false);


		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			XTandemWorker worker = null;
			try {
				worker = new XTandemWorker(FileUtilities.getAbsoluteFileForExecutables(new File(config.get(TANDEM_EXECUTABLE))));
			} catch (Exception e) {
				throw new MprcException("Tandem worker could not be created.", e);
			}
			return worker;
		}

		@Override
		public EngineMetadata getEngineMetadata() {
			return ENGINE_METADATA;
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config extends ResourceConfigBase {
		public Config() {
		}

		public Config(final String tandemExecutable) {
			put(TANDEM_EXECUTABLE, tandemExecutable);
		}
	}

	public static final class Ui implements ServiceUiFactory {
		private static final String WIN32 = "bin/tandem/win32_tandem/tandem.exe";
		private static final String WIN64 = "bin/tandem/win64_tandem/tandem.exe";
		private static final String LINUX_32 = "bin/tandem/linux_redhat_tandem/tandem.exe";
		private static final String LINUX_64 = "bin/tandem/ubuntu_64bit_tandem/tandem.exe";
		private static final String MAC_OSX = "bin/tandem/osx_intel_tandem/tandem.exe";

		@Override
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder.property(TANDEM_EXECUTABLE, "Executable Path", "Tandem executable path. Tandem executables can be " +
					"<br/>found at <a href=\"ftp://ftp.thegpm.org/projects/tandem/binaries/\"/>ftp://ftp.thegpm.org/projects/tandem/binaries</a>"
					+ "<p>Swift install contains following executables for your convenience:</p>"
					+ "<table>"
					+ "<tr><td><tt>" + WIN32 + "</tt></td><td>Windows 32 bit</td></tr>"
					+ "<tr><td><tt>bin/tandem/win32_core2_tandem/tandem.exe</tt></td><td>Windows 32 bit, specialized for Core2 processors</td></tr>"
					+ "<tr><td><tt>" + WIN64 + "</tt></td><td>Windows 64 bit</td></tr>"
					+ "<tr><td><tt>bin/tandem/win64_core2_tandem/tandem.exe</tt></td><td>Windows 64 bit, specialized for Core2 processors</td></tr>"
					+ "<tr><td><tt>" + LINUX_32 + "</tt></td><td>RedHat Linux</td></tr>"
					+ "<tr><td><tt>" + MAC_OSX + "</tt></td><td>Mac OS 10 on intel processor</td></tr>"
					+ "<tr><td><tt>" + LINUX_64 + "</tt></td><td>Ubuntu Linux, 64 bit</td></tr>"
					+ "</table>")
					.required()
					.executable(Arrays.asList("-v"))
					.defaultValue(getDefaultExecutable(daemon))
					.addDaemonChangeListener(new ExecutableChanger(resource, daemon));
		}

		private static final class ExecutableChanger implements PropertyChangeListener {
			private final ResourceConfig resource;
			private final DaemonConfig daemon;

			ExecutableChanger(final ResourceConfig resource, final DaemonConfig daemon) {
				this.resource = resource;
				this.daemon = daemon;
			}

			@Override
			public void propertyChanged(final ResourceConfig config, final String propertyName, final String newValue, final UiResponse response, final boolean validationRequested) {
				response.setProperty(resource, TANDEM_EXECUTABLE, getDefaultExecutable(daemon));
			}

			@Override
			public void fixError(final ResourceConfig config, final String propertyName, final String action) {
				// We never report an error, nothing to fix.
			}
		}

		private static String getDefaultExecutable(final DaemonConfig daemon) {
			final String osArch = daemon.getOsArch() == null ? "" : daemon.getOsArch().toLowerCase(Locale.ENGLISH);
			if (daemon.isWindows()) {
				if (osArch.contains("64")) {
					return WIN64;
				} else {
					return WIN32;
				}
			} else if (daemon.isLinux()) {
				if (osArch.contains("64")) {
					return LINUX_64;
				} else {
					return LINUX_32;
				}
			} else if (daemon.isMac()) {
				return MAC_OSX;
			}
			return "";
		}
	}
}
