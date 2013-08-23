package edu.mayo.mprc.database;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

/**
 * A class for safely persisting a type safe Java Enum in a database using a VarChar field.
 * This was provided by http://www.hibernate.org/272.html maybe that link still works if you want more information.
 * <p/>
 * This class unfortunately cannot accept inner enums (inner classes) because of how Class.forName() works
 */
public final class EnumUserType implements UserType, ParameterizedType {

	private Class clazz = null;

	@Override
	public void setParameterValues(final Properties params) {
		final String enumClassName = params.getProperty("enumClassName");
		if (enumClassName == null) {
			throw new MappingException("enumClassName parameter not specified");
		}

		try {
			this.clazz = Class.forName(enumClassName);
		} catch (java.lang.ClassNotFoundException e) {
			//try to it as if it were an inner class (aka enum) before failing all together


			throw new MappingException("enumClass " + enumClassName + " not found", e);
		}
	}

	private static final int[] SQL_TYPES = {Types.VARCHAR};

	@Override
	public int[] sqlTypes() {
		return SQL_TYPES;
	}

	@Override
	public Class returnedClass() {
		return clazz;
	}

	@Override
	public Object nullSafeGet(final ResultSet resultSet, final String[] names, final Object owner)
			throws HibernateException, SQLException {
		final String name = resultSet.getString(names[0]);
		Object result = null;
		if (!resultSet.wasNull()) {
			result = Enum.valueOf(clazz, name);
		}
		return result;
	}

	@Override
	public void nullSafeSet(final PreparedStatement preparedStatement, final Object value, final int index)
			throws HibernateException, SQLException {
		if (null == value) {
			preparedStatement.setNull(index, Types.VARCHAR);
		} else {
			preparedStatement.setString(index, ((Enum) value).name());
		}
	}

	@Override
	public Object deepCopy(final Object value) throws HibernateException {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Object assemble(final Serializable cached, final Object owner) throws HibernateException {
		return cached;
	}

	@Override
	public Serializable disassemble(final Object value) throws HibernateException {
		return (Serializable) value;
	}

	@Override
	public Object replace(final Object original, final Object target, final Object owner) throws HibernateException {
		return original;
	}

	@Override
	public int hashCode(final Object x) throws HibernateException {
		return x.hashCode();
	}

	@Override
	public boolean equals(final Object x, final Object y) throws HibernateException {
		if (x == y) {
			return true;
		}
		if (null == x || null == y) {
			return false;
		}
		return x.equals(y);
	}
}