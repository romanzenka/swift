package edu.mayo.mprc.config;

import com.google.common.base.Splitter;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads configs written with {@link AppConfigWriter}.
 *
 * @author Roman Zenka
 */
public final class AppConfigReader implements Closeable {
	private BufferedReader reader;
	private MultiFactory multiFactory;
	private DependencyResolver dependencyResolver;

	public AppConfigReader(final File configFile, final MultiFactory multiFactory) {
		try {
			init(new FileReader(configFile), multiFactory);
		} catch (FileNotFoundException e) {
			throw new MprcException("Cannot read config file " + configFile.getAbsolutePath(), e);
		}
	}

	public AppConfigReader(final Reader reader, final MultiFactory multiFactory) {
		init(reader, multiFactory);
	}

	private void init(final Reader reader, final MultiFactory multiFactory) {
		this.reader = new BufferedReader(reader);
		this.multiFactory = multiFactory;
		this.dependencyResolver = new DependencyResolver(multiFactory);
	}

	public ApplicationConfig load() {
		final ApplicationConfig config = new ApplicationConfig();
		int lineNum = 0;
		final Map<String, String> values = new LinkedHashMap<String, String>(10);
		try {
			boolean inSection = false;
			String name = null;
			String type = null;
			while (true) {
				final String line = reader.readLine();
				lineNum++;
				if (line == null) {
					break;
				}
				final String unescapedLine = unescapeLine(line);
				if (unescapedLine.isEmpty()) {
					continue;
				}
				if (unescapedLine.startsWith("<")) {
					if (inSection) {
						if (!unescapedLine.startsWith("</")) {
							throw new MprcException("Nested sections are not allowed");
						}
						inSection = false;
						final ResourceConfig resourceConfig;
						if ("service".equals(type)) {
							// Special treatment #1
							resourceConfig = createService(name, values);
						} else {
							resourceConfig = createResource(name, type, values);
						}
						// Special treatment #2 - add daemons to the enclosing app
						if ("daemon".equals(type)) {
							config.addDaemon((DaemonConfig) resourceConfig);
						}
						if (resourceConfig instanceof NamedResource) {
							((NamedResource) resourceConfig).setName(name);
						}
						values.clear();
					} else {
						inSection = true;
						final Iterable<String> typeName = Splitter.on(' ').omitEmptyStrings().trimResults().split(
								unescapedLine.subSequence(1, unescapedLine.length() - 1));

						final Iterator<String> iterator = typeName.iterator();
						if (!iterator.hasNext()) {
							throw new MprcException("Missing type declaration: " + unescapedLine);
						}
						type = iterator.next();
						if (!iterator.hasNext()) {
							throw new MprcException("Missing name of the item: " + unescapedLine);
						}
						name = iterator.next();

						if (!values.isEmpty()) {
							// Special treatment #3 - all the configs declared OUTSIDE the sections configure the app itself
							final Map<String, String> save = MapConfigWriter.save(config, dependencyResolver);
							for (final Map.Entry<String, String> entry : values.entrySet()) {
								save.put(entry.getKey(), entry.getValue());
							}
							MapConfigReader.load(config, save, dependencyResolver);
						}

						values.clear();
					}
				} else {
					final int firstSpace = unescapedLine.indexOf(' ');
					if (firstSpace < 0) {
						values.put(unescapedLine, "");
					} else {
						final String key = unescapedLine.substring(0, firstSpace);
						final String value = unescapedLine.substring(firstSpace).trim();
						values.put(key, value);
					}
				}
			}
		} catch (Exception e) {
			throw new MprcException("Error reading line: " + lineNum, e);
		} finally {
			FileUtilities.closeQuietly(this);
		}
		return config;
	}

	/**
	 * Services are created special.
	 * Each service is basically three tangled objects:
	 * service - runner - worker
	 * <p/>
	 * These are lumped together in the config file as this pattern is ubiquitous and
	 * it saves user some typing.
	 *
	 * @param name   Name of the service.
	 * @param values key-value pairs defining the service.
	 */
	private ServiceConfig createService(final String name, final Map<String, String> values) {
		final Map<String, String> workerParams = new HashMap<String, String>(10);
		final Map<String, String> runnerParams = new HashMap<String, String>(5);
		final Map<String, String> serviceParams = new HashMap<String, String>(2);
		String workerType = null;
		String runnerType = null;
		for (final Map.Entry<String, String> entry : values.entrySet()) {
			final String key = entry.getKey();
			final String value = entry.getValue();
			if (AppConfigWriter.RUNNER_TYPE.equals(key)) {
				runnerType = value;
			} else if (AppConfigWriter.RUNNER_WORKER_TYPE.equals(key)) {
				workerType = value;
			} else if (key.startsWith(AppConfigWriter.RUNNER_PREFIX)) {
				runnerParams.put(key.substring(AppConfigWriter.RUNNER_PREFIX.length()), value);
			} else {
				workerParams.put(key, value);
			}
		}
		if (workerType == null) {
			throw new MprcException("Worker type was not specified. Use " + AppConfigWriter.RUNNER_WORKER_TYPE + " property.");
		}
		if (runnerType == null) {
			throw new MprcException("Runner type was not specified. Use " + AppConfigWriter.RUNNER_TYPE + " property.");
		}

		final String workerId = "_service_" + name + "_worker";
		createResource(workerId, workerType, workerParams);

		runnerParams.put(RunnerConfig.WORKER, workerId);
		final String runnerId = "_service_" + name + "_runner";
		createResource(runnerId, runnerType, runnerParams);

		serviceParams.put(ServiceConfig.RUNNER, runnerId);
		return (ServiceConfig) createResource(name, "service", serviceParams);
	}

	private ResourceConfig createResource(final String name, final String type, final Map<String, String> values) {
		final ResourceConfig resourceConfig;
		final Class<? extends ResourceConfig> configClass = multiFactory.getConfigClass(type);
		try {
			resourceConfig = configClass.newInstance();
		} catch (Exception e) {
			throw new MprcException("Could not create instance of config: " + configClass.getName(), e);
		}
		resourceConfig.load(new MapConfigReader(dependencyResolver, values));
		dependencyResolver.addConfig(name, resourceConfig);
		return resourceConfig;
	}

	static String unescapeLine(final String line) {
		if (line == null) {
			return null;
		}
		int lastIndex = 0;
		int state = 0;
		final StringBuilder result = new StringBuilder(line.length());
		for (int i = 0; i < line.length(); i++) {
			final char c = line.charAt(i);
			switch (state) {
				case 0: // Start
					if ((' ' == c) || (c == '\t')) {
						// Keep eating spaces and tabs
					} else {
						if ('\\' == c) {
							state = 2; // Escape
						} else if ('#' == c) {
							state = 3; // Comment
						} else {
							state = 1; // Collecting characters
							result.append(c);
							lastIndex++;
						}
					}
					break;
				case 1: // Collect characters
					if ('\\' == c) {
						state = 2; // Escape
					} else if ('#' == c) {
						state = 3; // Comment
					} else {
						result.append(c);
						if (c != ' ' && c != '\t') {
							lastIndex = result.length();
						}
					}
					break;
				case 2: // Escape
					switch (c) {
						case 'n':
							result.append('\n');
							lastIndex++;
							break;
						case 'r':
							result.append('\r');
							lastIndex++;
							break;
						default:
							result.append(c);
							if (c != ' ' && c != '\t') {
								lastIndex = result.length();
							}
							break;
					}
					state = 1;
				case 3: // Comment - we never escape this case
					break;

			}
		}
		return result.substring(0, lastIndex);
	}

	@Override
	public void close() throws IOException {
		FileUtilities.closeQuietly(reader);
	}
}
