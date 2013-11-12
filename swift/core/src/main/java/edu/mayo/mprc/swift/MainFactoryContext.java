package edu.mayo.mprc.swift;

import edu.mayo.mprc.swift.commands.SwiftEnvironment;
import edu.mayo.mprc.swift.resources.WebUiHolder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * This is a utility class for centralizing access to the Spring ApplicationContext that has been set.  Ideally this
 * class would eventually go away as we wire more and more of Swift through Spring but in reality it will take a large
 * effort (too large?) to decouple Swift enough to make full Spring wiring possible.
 */
public final class MainFactoryContext {
	private static ApplicationContext context = null;

	private MainFactoryContext() {
	}

	public static ApplicationContext getContext() {
		synchronized (MainFactoryContext.class) {
			initialize();
			return context;
		}
	}

	public static void setContext(final ApplicationContext c) {
		synchronized (MainFactoryContext.class) {
			context = c;
		}
	}

	/**
	 * Initialize the context.
	 */
	public static void initialize() {
		synchronized (MainFactoryContext.class) {
			if (context == null) {
				context = getDefaultContext();
			}
		}
	}

	/**
	 * this is the default application context for Swift.  This should be used in production.  There is a testContext on
	 * that can be retreived from TestingUtilities which manages a test database.
	 */
	private static ApplicationContext getDefaultContext() {
		return new ClassPathXmlApplicationContext(new String[]{"/factories.xml"});
	}

	/**
	 * Returns a bean of a given id using the context returned by {@link #getContext()}.
	 *
	 * @param beanId Bean id we want.
	 * @return The bean for {@code beanId}.
	 */
	private static Object getBean(final String beanId) {
		return getContext().getBean(beanId);
	}

	public static SwiftEnvironment getSwiftEnvironment() {
		return (SwiftEnvironment) getBean("swiftEnvironment");
	}

	public static WebUiHolder getWebUiHolder() {
		return (WebUiHolder) getBean("webUiHolder");
	}
}
