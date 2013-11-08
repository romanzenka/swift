package edu.mayo.mprc.database;

import edu.mayo.mprc.utilities.exceptions.ExceptionUtilities;
import org.hibernate.HibernateException;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.usertype.UserType;

import java.io.File;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Storing {@link File} into database as its absolute path.
 */
public final class FileType implements UserType {

	public FileType() {
	}

	@Override
	public int[] sqlTypes() {
		return new int[]{StandardBasicTypes.STRING.sqlType()};
	}

	@Override
	public Class returnedClass() {
		return File.class;
	}

	@Override
	public boolean equals(final Object o, final Object o1) throws HibernateException {
		if (o == o1) {
			return true;
		}
		if (o == null || o1 == null) {
			return false;
		}
		return o.equals(o1);
	}

	@Override
	public int hashCode(final Object o) throws HibernateException {
		return o.hashCode();
	}

	@Override
	public Object nullSafeGet(final ResultSet resultSet, final String[] names, final Object owner) throws HibernateException, SQLException {
		final String uriString = resultSet.getString(names[0]);

		if (resultSet.wasNull()) {
			return null;
		}
		if (uriString == null) {
			return null;
		}

		try {
			return assemble(uriString, null);
		} catch (Exception t) {
			throw new HibernateException(t);
		}
	}

	@Override
	public void nullSafeSet(final PreparedStatement preparedStatement, final Object value, final int index) throws HibernateException, SQLException {
		if (null == value) {
			preparedStatement.setNull(index, Types.VARCHAR);
		} else {
			preparedStatement.setString(index, ((File) value).getAbsolutePath());
		}
	}

	@Override
	public Object deepCopy(final Object o) throws HibernateException {
		if (o == null) {
			return null;
		}
		return new File(((File) o).getAbsoluteFile().toURI());
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Serializable disassemble(final Object o) throws HibernateException {
		try {
			return ((File) o).getAbsolutePath();
		} catch (Exception t) {
			throw new HibernateException(t);
		}
	}

	@Override
	public Object assemble(final Serializable serializable, final Object o) throws HibernateException {
		try {
			if (!(serializable instanceof String)) {
				ExceptionUtilities.throwCastException(serializable, String.class);
				return null;
			}
			return new File((String) serializable);
		} catch (Exception t) {
			throw new HibernateException(t);
		}
	}

	@Override
	public Object replace(final Object original, final Object target, final Object owner) throws HibernateException {
		return original;
	}
}
