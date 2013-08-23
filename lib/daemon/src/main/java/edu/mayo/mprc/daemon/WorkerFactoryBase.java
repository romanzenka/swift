package edu.mayo.mprc.daemon;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.config.FactoryBase;
import edu.mayo.mprc.config.ResourceConfig;
import edu.mayo.mprc.config.ui.ServiceUiFactory;
import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;

import java.lang.reflect.Field;

/**
 * A worker factory. Can create {@link Worker} instances.
 * <p/>
 * The default implementation follows naming conventions for the workers to obtain the config, ui classes
 * as well as user-friendly name, type and long description.
 *
 * @param <C> The configuration the given worker needs.
 */
public abstract class WorkerFactoryBase<C extends ResourceConfig> extends FactoryBase<C, Worker> implements WorkerFactory<C, Worker> {
	protected WorkerFactoryBase() {
	}

	/**
	 * @return Type string acquired from the parent class, looking at the TYPE constant.
	 */
	@Override
	public String getType() {
		return getStaticString(getEnclosingClass(), "TYPE");
	}

	/**
	 * @return User-friendly name acquired from the parent class, looking at the NAME constant
	 */
	@Override
	public String getUserName() {
		return getStaticString(getEnclosingClass(), "NAME");
	}

	/**
	 * @return Long description acquired from the parent class, looking at the DESC constant
	 */
	@Override
	public String getDescription() {
		return getStaticString(getEnclosingClass(), "DESC");
	}

	/**
	 * @return Config class, acquired from the parent class, looking for Config subclass.
	 */
	@Override
	public Class<? extends ResourceConfig> getConfigClass() {
		return getSubclass(getEnclosingClass(), "Config");
	}

	/**
	 * @return A new instance of {@link ServiceUiFactory}.
	 */
	@Override
	public ServiceUiFactory getServiceUiFactory() {
		final Class<?> clazz = getSubclass(getEnclosingClass(), "Ui");
		final Object o;
		try {
			o = clazz.newInstance();
		} catch (InstantiationException e) {
			throw new MprcException(e);
		} catch (IllegalAccessException e) {
			throw new MprcException(e);
		}
		if (o instanceof ServiceUiFactory) {
			return (ServiceUiFactory) o;
		} else {
			ExceptionUtilities.throwCastException(o, ServiceUiFactory.class);
			return null;
		}
	}

	/**
	 * We expect the Factory class to be enclosed in the worker class, which also contains metadata + the UI class.
	 *
	 * @return The enclosing class, or throws an exception if none is defined.
	 */
	private Class<?> getEnclosingClass() {
		final Class<?> enclosingClass = getClass().getEnclosingClass();
		if (enclosingClass == null) {
			throw new MprcException("The class " + getClass().getName() + " is not enclosed in any class. Cannot use the canonical location for user name, type, description, etc.");
		}
		return enclosingClass;
	}

	private static String getStaticString(final Class<?> clazz, final String fieldName) {
		try {
			final Field field = clazz.getDeclaredField(fieldName);
			final Object value = field.get(null);
			if (value instanceof String) {
				return (String) value;
			} else {
				throw new MprcException("The value of the field '" + fieldName + "' is not a string.");
			}
		} catch (NoSuchFieldException e) {
			throw new MprcException("Cannot access field '" + fieldName + "'.", e);
		} catch (IllegalAccessException e) {
			throw new MprcException("Access to field '" + fieldName + "' is not allowed.", e);
		}
	}

	private static Class getSubclass(final Class parent, final String suffix) {
		final String className = parent.getName() + "$" + suffix;
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new MprcException("Subclass " + suffix + " does not exist in " + parent.getName(), e);
		}
	}
}
