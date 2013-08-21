package edu.mayo.mprc.mascot;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.*;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.config.ui.UiBuilder;
import edu.mayo.mprc.daemon.WorkPacket;
import edu.mayo.mprc.daemon.Worker;
import edu.mayo.mprc.daemon.WorkerBase;
import edu.mayo.mprc.daemon.WorkerFactoryBase;
import edu.mayo.mprc.daemon.exception.DaemonException;
import edu.mayo.mprc.searchengine.EngineFactory;
import edu.mayo.mprc.searchengine.EngineMetadata;
import edu.mayo.mprc.utilities.FileUtilities;
import edu.mayo.mprc.utilities.FormScraper;
import edu.mayo.mprc.utilities.StreamRegExMatcher;
import edu.mayo.mprc.utilities.progress.PercentDone;
import edu.mayo.mprc.utilities.progress.UserProgressReporter;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Daemon worker for Mascot.
 */
public final class MascotWorker extends WorkerBase {
	private static final Logger LOGGER = Logger.getLogger(MascotWorker.class);

	private static final int BUFFER_SIZE = 8192;
	private static final Pattern PERCENT_DONE = Pattern.compile("^\\.*(\\d+)\\% complete\\s*$");
	private static final String MASCOT_URL = "mascotUrl";
	private static final String MASCOT_PUBLIC_URL = "mascotPublicUrl";
	public static final String TYPE = "mascot";
	public static final String NAME = "Mascot";
	public static final String DESC = "<a href=\"http://www.matrixscience.com/\">Mascot search engine</a> support.<p>Swift was tested against Mascot 2.2 and 2.4 without enabled security.</p>";
	public static final int INPUT_FILE_TIMEOUT = 2 * 60 * 1000;

	private URL baseUrl;
	private URL datFileBaseUrl;

	/**
	 * An URL the users can use to access Mascot from their workstations. Typically identical to {@link #baseUrl}.
	 */
	private URL publicBaseUrl;
	private HttpURLConnection connection;
	private WritableByteChannel channel;
	private String boundary;
	private File mascotOutputFile;
	private final Charset charset = Charset.forName("UTF-8");

	private static final Pattern DB_TAG_PATTERN = Pattern.compile("\\$\\{(?:DB|DBPath):([^}]+)\\}");

	private static final Random RANDOM = new Random();
	/**
	 * The CGI that runs mascot search. Relative to mascot base.
	 */
	public static final String MASCOT_CGI = "cgi/nph-mascot.exe?1";

	public void process(final WorkPacket workPacket, final UserProgressReporter progressReporter) {
		final MascotWorkPacket mascotWorkPacket = (MascotWorkPacket) workPacket;
		assert mascotWorkPacket.getInputFile() != null : "Mascot search failed: mgf file not specified";
		assert mascotWorkPacket.getSearchParamsFile() != null : "Mascot search failed: param file not specified";
		assert mascotWorkPacket.getOutputFile() != null : "Mascot search failed: output file not specified";
		assert mascotWorkPacket.getShortDbName() != null : "Mascot search failed: short db name not specified";

		try {
			LOGGER.debug(MessageFormat.format("Mascot search starting:\n\t{0} -> {1}\n\t(db name: {2})",
					mascotWorkPacket.getInputFile().getAbsolutePath(),
					mascotWorkPacket.getOutputFile().getAbsolutePath(),
					mascotWorkPacket.getShortDbName()));
			this.setMascotOutputFile(mascotWorkPacket.getOutputFile());

			// We have to modify the mascot params file, replacing the ${DB:whatever} tag with the supplied short db name.
			final StreamRegExMatcher matcher = new StreamRegExMatcher(DB_TAG_PATTERN, mascotWorkPacket.getSearchParamsFile());
			matcher.replaceAll(Matcher.quoteReplacement(mascotWorkPacket.getShortDbName()));
			matcher.writeContentsToFile(mascotWorkPacket.getSearchParamsFile());
			matcher.close();

			// Now we can run the search
			this.search(
					mascotWorkPacket.getSearchParamsFile().getAbsolutePath(),
					mascotWorkPacket.getInputFile().getAbsolutePath(),
					progressReporter);
		} catch (Exception e) {
			throw new DaemonException("Mascot search failed", e);
		}
		// When the search completes, we are done.
	}

	public void setUrl(final URL mascotUrl) {
		if (mascotUrl == null) {
			throw new MprcException("The mascot url must not be null");
		}
		boundary = ("--------" + RANDOM.nextLong()) + Math.abs(RANDOM.nextLong() % Long.MAX_VALUE);
		baseUrl = mascotUrl;
		datFileBaseUrl = mascotCgiUrl(mascotUrl);
	}

	public void setPublicUrl(final URL publicBaseUrl) {
		this.publicBaseUrl = publicBaseUrl;
	}

	static URL mascotCgiUrl(final URL mascotBaseUrl) {
		try {
			return new URL(mascotBaseUrl, MASCOT_CGI);
		} catch (MalformedURLException e) {
			throw new MprcException("Cannot obtain mascot CGI url from base url " + mascotBaseUrl.toString(), e);
		}
	}

	private void setMascotOutputFile(final File file) {
		mascotOutputFile = file;
	}

	private ByteBuffer getFormBodyTop(final Map<String, String> hash) {
		final Iterator<Map.Entry<String, String>> iterator = hash.entrySet().iterator();

		final StringBuilder sb = new StringBuilder();
		while (iterator.hasNext()) {
			final Map.Entry<String, String> entry = iterator.next();
			sb.append("--").append(boundary).append("\r\n")
					.append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"\r\n\r\n")
					.append(entry.getValue())
					.append("\r\n");
		}

		return charset.encode(sb.toString());
	}

	private ByteBuffer getFormFilePreamble(final String filename) {
		String s = "--" + boundary + "\r\n";
		s += "Content-Disposition: form-data; name=\"FILE\";";
		s += " filename=\"" + filename + "\"\r\n";
		s += "Content-Type: application/octet-stream\r\n\r\n";
		return charset.encode(s);
	}

	private ByteBuffer getFormBodyBottom() {
		return charset.encode("\r\n--" + boundary + "--\r\n");
	}

	public void setupPOSTConnection(final int len) {
		try {
			connection = (HttpURLConnection) datFileBaseUrl.openConnection();
		} catch (Exception e) {
			die("Couldn't setup POST connection", e);
		}

		try {
			connection.setRequestMethod("POST");
			// We must not use _connection.setChunkedStreamingMode(BUFFER_SIZE); here. This seems to be not supported
			// by mascot 2.2 on IIS 6.0. However, if we do not specify content length at all, we run out of heap space.
			// See http://forum.java.sun.com/thread.jspa?threadID=418441&messageID=2816084 for mor information
			connection.setFixedLengthStreamingMode(len);
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" +
					boundary);
			connection.setRequestProperty("Content-Length", Integer.toString(len));
			connection.setRequestProperty("Connection", "Keep-Alive");
			connection.setRequestProperty("Keep-Alive", "300");
		} catch (Exception e) {
			die("POST error", e);
		}
	}

	/**
	 * @return Returns the path to the resulting Mascot .dat file
	 */
	private String getPOSTResponse(final UserProgressReporter progressReporter) {
		final StringBuilder completePage = new StringBuilder();
		BufferedReader rreader = null;
		try {
			final DataInputStream in = new DataInputStream(connection.getInputStream());
			rreader = new BufferedReader(new InputStreamReader(in));

			String str;
			String link;
			final boolean inTag = false;
			final String regex = "href=\".*file=(.*\\.dat)\"";
			final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			final Pattern whitespace = Pattern.compile("^\\s*$", Pattern.CASE_INSENSITIVE);
			String url = null;
			while ((str = rreader.readLine()) != null) {

				final Matcher matcher = pattern.matcher(str);
				if (matcher.find()) {
					url = matcher.group(1);
				}
				str = FormScraper.stripHtmlTags(str);
				if (!whitespace.matcher(str).matches()) {
					LOGGER.info(str);
					completePage.append(str).append("\n");
					extractReportPercentDone(progressReporter, str);
				}
			}
			in.close();
			if (url != null) {
				return url;
			}
		} catch (Exception t) {
			die("Exception while reading response: ", t);
		} finally {
			FileUtilities.closeQuietly(rreader);
		}
		throw new DaemonException("no data URL for the mascot result\nHere is the returned page source:\n" + completePage.toString());
	}

	private void extractReportPercentDone(final UserProgressReporter progressReporter, final String logLine) {
		final Matcher percentMatcher = PERCENT_DONE.matcher(logLine);
		if (percentMatcher.matches()) {
			try {
				final float value = Float.parseFloat(percentMatcher.group(1));
				progressReporter.reportProgress(new PercentDone(value));
			} catch (NumberFormatException ignore) {
				// SWALLOWED: Not a big deal
				LOGGER.debug("Cannot parse Mascot progress" + logLine);
			}
		}
	}

	private void getDatFile(final URL url, final File outputFile) {
		int pos = 0;
		final byte[] buffer = new byte[BUFFER_SIZE];

		InputStream in = null;
		OutputStream out = null;

		try {
			final URLConnection connection = url.openConnection();
			in = connection.getInputStream();
			out = new FileOutputStream(outputFile);
			final int len = connection.getContentLength();

			while (pos < len) {
				if (in.available() > 0) {
					final int read = in.read(buffer);
					pos += read;
					out.write(buffer, 0, read);
				}
			}
		} catch (Exception t) {
			throw new MprcException("Could not obtain Mascot .dat file from " + url.toString() + " as " + outputFile.getAbsolutePath(), t);
		} finally {
			FileUtilities.closeQuietly(out);
			FileUtilities.closeQuietly(in);
		}
	}

	private File getOutputFile() {
		return mascotOutputFile;
	}

	public void search(final String parameters, final String data, final UserProgressReporter progressReporter) {
		if (!new File(parameters.trim()).exists()) {
			throw new MprcException("parameters file does not exist: " + parameters.trim());
		}
		if (!new File(data.trim()).exists()) {
			throw new MprcException("data file does not exist: " + data.trim());
		}

		LOGGER.debug("Searching " + data + " with parameters " + parameters);
		BufferedReader reader = null;
		Map<String, String> hash = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(parameters.trim()), "UTF-8"));
			hash = readParameters(reader);
		} catch (FileNotFoundException e) {
			throw new MprcException("Cannot find Mascot parameters file " + parameters, e);
		} catch (UnsupportedEncodingException e) {
			throw new MprcException("Unsupported mascot parameter file encoding", e);
		} catch (IOException e) {
			throw new MprcException("Exception accessing mascot parameter file " + parameters, e);
		} finally {
			FileUtilities.closeQuietly(reader);
		}
		// append input parameters and data file names to the search title
		String title = hash.get("COM");
		if (title != null) {
			title = title.trim() + " ";
		} else {
			title = "";
		}
		title += data + " " + parameters;
		hash.put("COM", title);


		final ByteBuffer buffer = getFormBodyTop(hash);
		final ByteBuffer buffer2 = getFormFilePreamble(data);
		final ByteBuffer buffer3 = getFormBodyBottom();
		FileChannel mgfChannel = null;
		long dataLength = 0;
		try {
			mgfChannel = new FileInputStream(data).getChannel();
			dataLength = mgfChannel.size();
		} catch (Exception e) {
			throw new MprcException("Could not open .mgf file for Mascot search " + data, e);
		}
		final long length = dataLength + buffer.limit() + buffer2.limit() + buffer3.limit();

		setupPOSTConnection((int) length);

		// try the niochannel approach
		try {
			channel = Channels.newChannel(connection.getOutputStream());
			channel.write(buffer);
			channel.write(buffer2);

			// this only works if dealing with less than 20 Meg
			long position = 0L;
			long amount = 0L;
			try {
				while (position < dataLength) {
					amount = mgfChannel.transferTo(position, dataLength - position, channel);
					position += amount;
				}
			} catch (Exception t) {
				LOGGER.debug("transferred " + position + "bytes, last chunk was " + amount + " bytes.");
				throw new MprcException("nio approach failed, " + t.getMessage(), t);
			}

			channel.write(buffer3);
		} catch (Exception e) {
			throw new MprcException("nio channel write failed, " + e.getMessage(), e);
		} finally {
			FileUtilities.closeQuietly(mgfChannel);
			FileUtilities.closeQuietly(channel);
		}

		final String filePath = getPOSTResponse(progressReporter);
		try {
			progressReporter.reportProgress(
					new MascotResultUrl(
							new URL(publicBaseUrl, "cgi/master_results.pl?file=" + filePath).toString()));
			getDatFile(new URL(datFileBaseUrl, filePath), getOutputFile());
		} catch (Exception e) {
			throw new MprcException("Cannot obtain mascot result", e);
		}
	}

	private Map<String, String> readParameters(final BufferedReader reader)
			throws IOException, IllegalArgumentException {
		final Map<String, String> hash = new HashMap<String, String>();
		String line;
		int count = 0;

		while ((line = reader.readLine()) != null) {
			count++;
			line = line.trim();
			if ((line.length() == 0) || (line.charAt(0) == '#')) {
				continue;
			}
			final int pos = line.indexOf('=');
			// handle both the case where there is no equal sign or the key part is empty
			// (return values -1 and 0, respectively)
			if (pos <= 0) {
				throw (new IllegalArgumentException("Not in key=value format at line " + count));
			}
			hash.put(line.substring(0, pos), line.substring(pos + 1));
		}
		return hash;
	}

	private void die(final String message, final Throwable t) {
		LOGGER.error(t);
		throw new DaemonException(message, t);
	}

	public String toString() {
		return "Mascot worker for URL: " + datFileBaseUrl.toString();
	}

	/**
	 * A factory capable of creating the worker
	 */
	@Component("mascotWorkerFactory")
	public static final class Factory extends WorkerFactoryBase<Config> implements EngineFactory {
		private MascotMappingFactory mappingFactory;

		@Override
		public Worker create(final Config config, final DependencyResolver dependencies) {
			final MascotWorker worker = new MascotWorker();
			try {
				final URL mascotUrl = new URL(config.getMascotUrl());
				worker.setUrl(mascotUrl);
				if (config.getMascotPublicUrl() != null && config.getMascotPublicUrl().trim().length() > 0) {
					worker.setPublicUrl(new URL(config.getMascotPublicUrl()));
				} else {
					worker.setPublicUrl(mascotUrl);
				}
			} catch (MalformedURLException e) {
				throw new MprcException("Not a valid mascot url: " + config.getMascotUrl(), e);
			}
			return worker;
		}

		public MascotMappingFactory getMappingFactory() {
			return mappingFactory;
		}

		@Resource(name = "mascotMappingFactory")
		public void setMappingFactory(MascotMappingFactory mappingFactory) {
			this.mappingFactory = mappingFactory;
		}

		@Override
		public EngineMetadata getEngineMetadata() {
			return new EngineMetadata(
					"MASCOT", ".dat", "Mascot", true, "mascot", getMappingFactory(),
					new String[]{TYPE},
					new String[]{MascotCache.TYPE},
					new String[]{MascotDeploymentService.TYPE, MockMascotDeploymentService.TYPE},
					10, false);
		}
	}

	/**
	 * Configuration for the factory
	 */
	public static final class Config implements ResourceConfig {
		private String mascotUrl;
		private String mascotPublicUrl;

		public Config() {
		}

		public Config(final String mascotUrl) {
			this.mascotUrl = mascotUrl;
		}

		public String getMascotUrl() {
			return mascotUrl;
		}

		public void setMascotUrl(final String mascotUrl) {
			this.mascotUrl = mascotUrl;
		}

		public String getMascotPublicUrl() {
			return mascotPublicUrl;
		}

		public void setMascotPublicUrl(final String mascotPublicUrl) {
			this.mascotPublicUrl = mascotPublicUrl;
		}

		@Override
		public void save(final ConfigWriter writer) {
			writer.put(MASCOT_URL, getMascotUrl());
			writer.put(MASCOT_PUBLIC_URL, getMascotPublicUrl());
		}

		@Override
		public void load(final ConfigReader reader) {
			setMascotUrl(reader.get(MASCOT_URL));
			setMascotPublicUrl(reader.get(MASCOT_PUBLIC_URL));
		}

		@Override
		public int getPriority() {
			return 0;
		}
	}

	public static final class Ui implements ServiceUiFactory {
		public void createUI(final DaemonConfig daemon, final ResourceConfig resource, final UiBuilder builder) {
			builder
					.property(MASCOT_URL, "URL", "Mascot search engine URL.<p>This URL is used by Swift to give commands to Mascot.</p>").required()
					.property(MASCOT_PUBLIC_URL, "Public URL", "Mascot URL to be used when accessing Mascot by the users.<p>If not specified, the URL (above) will be used.");
		}
	}
}
