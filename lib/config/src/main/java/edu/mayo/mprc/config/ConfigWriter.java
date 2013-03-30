package edu.mayo.mprc.config;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.StringUtilities;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Roman Zenka
 */
public final class ConfigWriter implements java.io.Closeable {
	private PrintWriter writer = null;
	private boolean inSection = false;
	private String section = null;
	private final static String INDENT = "        ";
	private List<Triplet> contents = new ArrayList<Triplet>(10);
	private MultiFactory multiFactory;
	private DependencyResolver dependencyResolver;

	public ConfigWriter(final File file, final MultiFactory multiFactory) {
		try {
			init(new FileWriter(file, false), multiFactory);
		} catch (IOException e) {
			throw new MprcException("Cannot create config writer for file " + file.getAbsolutePath(), e);
		}
	}

	public ConfigWriter(final Writer writer, final MultiFactory multiFactory) {
		init(writer, multiFactory);
	}

	private void init(final Writer writer, final MultiFactory multiFactory) {
		this.writer = new PrintWriter(writer);
		dependencyResolver = new DependencyResolver(multiFactory);
		this.multiFactory = multiFactory;
	}

	public String getResourceId(final Class<? extends ResourceConfig> clazz) {
		return multiFactory.getId(clazz);
	}

	public void openSection(final ResourceConfig config) {
		if (inSection) {
			throw new MprcException("Cannot nest sections");
		}
		writer.println();
		section = multiFactory.getId(config.getClass());
		final String name = getDependencyResolver().getIdFromConfig(config);
		writer.println("<" + section + " " + name + ">");
		inSection = true;
		contents.clear();
	}

	public void addConfig(final String key, final String value, final String comment) {
		contents.add(new Triplet(key, value, comment));
	}

	public void closeSection() {
		if (!inSection) {
			throw new MprcException("Not in section");
		}
		int maxKey = 0;
		int maxVal = 0;
		for (final Triplet triplet : contents) {
			final int keyLength = triplet.getKey().length();
			if (keyLength > maxKey) {
				maxKey = keyLength;
			}
			final int valueLength = triplet.getValue().length();
			if (valueLength > maxVal && valueLength < 50) {
				maxVal = valueLength;
			}
		}

		for (final Triplet triplet : contents) {
			writer.print(INDENT);
			final String key = triplet.getKey();
			writer.print(key);
			writer.print(StringUtilities.repeat(' ', maxKey - key.length() + 2));

			final String value = triplet.getValue();
			writer.print(value);
			writer.print(StringUtilities.repeat(' ', Math.max(maxVal - value.length() + 2, 2)));

			final String comment = triplet.getComment();
			if (comment != null) {
				writer.print("# ");
				writer.print(comment);
			}
			writer.println();
		}

		writer.println("</" + section + ">");
		inSection = false;
	}


	@Override
	public void close() throws IOException {
		writer.close();
	}

	public DependencyResolver getDependencyResolver() {
		return dependencyResolver;
	}

	public void register(final ResourceConfig config) {
		getDependencyResolver().addConfig(getNewId(config), config);
	}

	public void register(final ResourceConfig config, final String name) {
		getDependencyResolver().addConfig(name, config);
	}

	private String getNewId(ResourceConfig config) {
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

		private Triplet(final String key, final String value, final String comment) {
			this.key = key.trim();

			String val = value == null ? "" : value.trim();
			val = val.replaceAll("\\\\", Matcher.quoteReplacement("\\\\"));
			val = val.replaceAll(Pattern.quote("#"), Matcher.quoteReplacement("\\#"));
			val = val.replaceAll(Pattern.quote("\n"), Matcher.quoteReplacement("\\n"));
			val = val.replaceAll(Pattern.quote("\r"), Matcher.quoteReplacement("\\r"));
			this.value = val;
			this.comment = comment == null ? null : comment.trim();
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
	}
}
