package edu.mayo.mprc.config;

import com.google.common.collect.Lists;
import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.StringUtilities;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes out the configuration to a text file, similar to Apache's httpd.conf
 * <p/>
 * Format:
 * <c><pre>
 * &lt;type name>
 * key value # comment
 * &lt/type>
 * </pre></c>
 * <p/>
 * This format is sadly quite verbose, because each service -> runner -> worker triplet gets saved as
 * three separate objects. Therefore the service and runner saving is hijacked and these three objects are embedded into one
 * service object like so:
 * <c><pre>
 *     &lt;service name>
 *     type type_of_worker # Embedded worker type
 * <p/>
 *     runner.type localRunner/sgeRunner # Embedded runner config starts here
 *     runner.runner_key value # comment
 * <p/>
 *     worker_key value # Embedded worker config starts here
 *     worker_key value # comment
 *     &lt;service>
 * </pre></c>
 * <p/>
 * In other words, the config starts with service configuration (contains only the worker type field),
 * continues with runner configuration prefixed with <c>runner.</c> and finally
 * goes to the worker configuration itself.
 * <p/>
 * Since the services and daemons are aware of their own name, when saving these, the given names are honored instead
 * of auto-generating the names. Auto-generated names start with an underscore.
 *
 * @author Roman Zenka
 */
public final class AppConfigWriter implements Closeable {
	public static final String RUNNER_WORKER_TYPE = "runner.workerType";
	public static final String RUNNER_TYPE = "runner.type";
	public static final String RUNNER_PREFIX = "runner.";
	private PrintWriter writer;
	private boolean inSection;
	private String section;
	private final static String INDENT = "        ";
	private MultiFactory multiFactory;
	private DependencyResolver dependencyResolver;
	private InnerConfigWriter rootWriter = new SectionConfigWriter(ApplicationConfig.class);

	public AppConfigWriter(final File file, final MultiFactory multiFactory) {
		try {
			init(new FileWriter(file, false), multiFactory);
		} catch (IOException e) {
			throw new MprcException("Cannot create config writer for file " + file.getAbsolutePath(), e);
		}
	}

	public AppConfigWriter(final Writer writer, final MultiFactory multiFactory) {
		init(writer, multiFactory);
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

	private void init(final Writer writer, final MultiFactory multiFactory) {
		this.writer = new PrintWriter(writer);
		dependencyResolver = new DependencyResolver(multiFactory);
		this.multiFactory = multiFactory;
	}

	public void save(final ApplicationConfig config) {
		writeHeader();
		rootWriter.save(config);
	}

	private void writeHeader() {
		final List<Triplet> comments = Lists.newArrayList();
		comments.add(new Triplet("Application configuration"));
		comments.add(new Triplet("Supported types:"));
		final List<Class> supportedConfigClasses = new ArrayList<Class>(multiFactory.getConfigClasses().values());
		Collections.sort(supportedConfigClasses, new Comparator<Class>() {
			@Override
			public int compare(final Class o1, final Class o2) {
				return multiFactory.getId(o1).compareTo(multiFactory.getId(o2));
			}
		});
		int maxClassLen = getMaxTypeLength(supportedConfigClasses);
		for (final Class clazz : supportedConfigClasses) {
			final String id = multiFactory.getId(clazz);
			comments.add(new Triplet("    " + id + StringUtilities.repeat(' ', maxClassLen - id.length()) + "  " + multiFactory.getUserName(clazz)));
		}
		writeTriplets(comments, "");
	}

	private int getMaxTypeLength(List<Class> supportedConfigClasses) {
		int maxClassLen = 0;
		for (final Class clazz : supportedConfigClasses) {
			final String id = multiFactory.getId(clazz);
			if (id.length() > maxClassLen) {
				maxClassLen = id.length();
			}
		}
		return maxClassLen;
	}

	private void writeTriplets(final Collection<Triplet> contents, final String indent) {
		int maxKey = 0;
		int maxVal = 0;
		for (final Triplet triplet : contents) {
			final String key = triplet.getKey();
			if (key != null) {
				final int keyLength = key.length();
				if (keyLength > maxKey) {
					maxKey = keyLength;
				}
				final int valueLength = triplet.getValue().length();
				if (valueLength > maxVal && valueLength < 50) {
					maxVal = valueLength;
				}
			}
		}

		for (final Triplet triplet : contents) {
			writer.print(indent);
			final String key = triplet.getKey();
			if (key != null) {
				writer.print(key);
				writer.print(StringUtilities.repeat(' ', maxKey - key.length() + 2));

				final String value = triplet.getValue();
				writer.print(value);
				writer.print(StringUtilities.repeat(' ', Math.max(maxVal - value.length() + 2, 2)));
			}

			final String comment = triplet.getComment();
			if (comment != null && !comment.isEmpty()) {
				writer.print("# ");
				writer.print(comment);
			}
			writer.println();
		}
	}

	private DependencyResolver getDependencyResolver() {
		return dependencyResolver;
	}

	private String getNewName(final ResourceConfig config) {
		final String nameBase = multiFactory.getId(config.getClass());
		int i = 1;
		while (true) {
			if (dependencyResolver.getConfigFromId("_" + nameBase + "_" + i) == null) {
				break;
			}
			i++;
		}
		return "_" + nameBase + "_" + i;
	}

	private static final class Triplet {
		private final String key;
		private final String value;
		private final String comment;

		public Triplet(final String comment) {
			this(null, null, comment);
		}

		public Triplet(final String key, final String value, final String comment) {
			this.key = key == null ? null : key.trim();

			this.value = escapeValue(value);
			this.comment = comment;
		}

		public String getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}

		public String getComment() {
			return comment;
		}

		private static String escapeValue(final String value) {
			String val = value == null ? "" : value.trim();
			val = val.replaceAll("\\\\", Matcher.quoteReplacement("\\\\"));
			val = val.replaceAll(Pattern.quote("#"), Matcher.quoteReplacement("\\#"));
			val = val.replaceAll(Pattern.quote("\n"), Matcher.quoteReplacement("\\n"));
			val = val.replaceAll(Pattern.quote("\r"), Matcher.quoteReplacement("\\r"));
			return val;
		}
	}

	/**
	 * A sub-writer for a particular object. Collects all of its key-value pairs,
	 * opens a section, writes it out, closes the section.
	 */
	private abstract class InnerConfigWriter extends ConfigWriterBase {
		private List<Triplet> contents = new ArrayList<Triplet>(10);
		private Class currentClass;

		public final Class getCurrentClass() {
			return currentClass;
		}

		public final List<Triplet> getContents() {
			return contents;
		}

		private InnerConfigWriter(final Class currentClass) {
			this.currentClass = currentClass;
		}

		@Override
		public void put(final String key, final String value, final String comment) {
			if (!NamedResource.class.isAssignableFrom(currentClass) || !"name".equals(key)) {
				contents.add(new Triplet(key, value, comment));
			}
			// Else do not save the name. It is already used in the section
		}

		// Only output the value if it is not the same as default.
		@Override
		public void put(final String key, final int value, final int defaultValue, final String comment) {
			if (value != defaultValue) {
				put(key, value, comment);
			}
		}

		@Override
		public final void comment(final String comment) {
			put(null, null, comment);
		}

		private boolean register(final ResourceConfig config) {
			if (getDependencyResolver().getIdFromConfig(config) == null) {
				final String name = getItemName(config);
				getDependencyResolver().addConfig(name == null ? getNewName(config) : name, config);
				return true;
			}
			return false;
		}

		private String getItemName(final ResourceConfig config) {
			if (config instanceof NamedResource) {
				return ((NamedResource) config).getName();
			}
			return null;
		}

		@Override
		public final String save(final ResourceConfig resourceConfig) {
			if (resourceConfig != null) {
				if (register(resourceConfig)) {
					saveStrategy(resourceConfig);
				}
				return getDependencyResolver().getIdFromConfig(resourceConfig);
			} else {
				return "";
			}
		}

		abstract void saveStrategy(final ResourceConfig resourceConfig);
	}

	/**
	 * Writes all children as sections.
	 */
	private final class SectionConfigWriter extends InnerConfigWriter {
		private SectionConfigWriter(final Class currentClass) {
			super(currentClass);
		}

		@Override
		public void put(final String key, final String value, final String comment) {
			if (getCurrentClass().equals(ServiceConfig.class) && ServiceConfig.RUNNER.equals(key)) {
				// Do nothing. The values are embedded

			} else if (!(getCurrentClass().equals(ApplicationConfig.class) && ApplicationConfig.DAEMONS.equals(key))) {
				super.put(key, value, comment);
			}
			// else: We do not save the list of daemons. All daemons contained within are used
		}

		@Override
		void saveStrategy(final ResourceConfig resourceConfig) {
			final InnerConfigWriter innerWriter;
			if (resourceConfig instanceof RunnerConfig) {
				if (!"localRunner".equals(multiFactory.getId(resourceConfig.getClass()))) {
					put(RUNNER_TYPE, multiFactory.getId(resourceConfig.getClass()), "Type of the runner (localRunner/sgeRunner)");
				}
				innerWriter = new RunnerConfigWriter(resourceConfig.getClass(), this);
				resourceConfig.save(innerWriter);
			} else if (resourceConfig instanceof ApplicationConfig) {
				innerWriter = new SectionConfigWriter(resourceConfig.getClass());
				resourceConfig.save(innerWriter);
				writeTriplets(innerWriter.getContents(), "");
			} else {
				innerWriter = new SectionConfigWriter(resourceConfig.getClass());
				resourceConfig.save(innerWriter);
				openSection(resourceConfig);
				closeSection(innerWriter.getContents());
			}
		}

		private void openSection(final ResourceConfig config) {
			if (inSection) {
				throw new MprcException("Cannot nest sections");
			}
			writer.println();
			section = multiFactory.getId(config.getClass());
			final String name = getDependencyResolver().getIdFromConfig(config);
			if (name == null) {
				throw new MprcException("The config has to be registered before saving");
			}
			writer.println("<" + section + " " + name + ">");
			inSection = true;
		}

		private void closeSection(final Collection<Triplet> contents) {
			if (!inSection) {
				throw new MprcException("Not in section");
			}
			writeTriplets(contents, INDENT);

			writer.println("</" + section + ">");
			inSection = false;
		}
	}

	private final class RunnerConfigWriter extends InnerConfigWriter {
		InnerConfigWriter embedIntoWriter;

		public RunnerConfigWriter(final Class currentClass, final InnerConfigWriter embedIntoWriter) {
			super(currentClass);
			this.embedIntoWriter = embedIntoWriter;
		}

		@Override
		public void put(final String key, final String value, final String comment) {
			// Skip the worker value, we embed the worker directly
			if (!RunnerConfig.WORKER.equals(key)) {
				embedIntoWriter.put(RUNNER_PREFIX + key, value, comment);
			}
		}

		@Override
		void saveStrategy(final ResourceConfig resourceConfig) {
			// Links to services save as sections
			if (resourceConfig instanceof ServiceConfig) {
				final SectionConfigWriter writer = new SectionConfigWriter(resourceConfig.getClass());
				writer.saveStrategy(resourceConfig);
			} else {
				// Worker data embed directly
				embedIntoWriter.put(RUNNER_WORKER_TYPE, multiFactory.getId(resourceConfig.getClass()), "Type of the worker");
				resourceConfig.save(embedIntoWriter);
			}
		}
	}
}
