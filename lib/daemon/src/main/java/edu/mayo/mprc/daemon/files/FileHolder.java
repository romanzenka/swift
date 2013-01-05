package edu.mayo.mprc.daemon.files;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.utilities.FileUtilities;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * A class implementing the {@link FileTokenHolder} protocol that does not actually hold any {@link FileToken} objects
 * itself.
 * <p/>
 * Reflection is used to harvest:
 * <ul>
 * <li>{@link File} properties</li>
 * <li>{@link FileTokenHolder} properties</li>
 * <li>{@code List&lt;File/FileTokenHolder>} properties</li>
 * <li>{@code Map&lt;Serializable, File/FileTokenHolder>} properties</li>
 * </ul>
 * <p/>
 * The files are translated to FileToken
 * objects and stored in {@link #tokenMap}. The other side then automatically translates the FileToken instances back
 * to Files and sets the matching properties.
 * <p/>
 * This way the user can send an object message with multiple references to files, and the files get transferred to the target
 * system if necessary.
 * <p/>
 * TODO: This is convoluted. Provide a cleaner implementation that could recursively sniff-out absolutely all files in the data structures.
 */
public class FileHolder implements FileTokenHolder {
	private static final long serialVersionUID = 20110418L;
	private HashMap<FieldIndex, FileToken> tokenMap;
	private transient ReceiverTokenTranslator translator;
	private transient FileTokenSynchronizer synchronizer;

	public FileHolder() {
	}

	public ReceiverTokenTranslator getTranslator() {
		return translator;
	}

	public FileTokenSynchronizer getSynchronizer() {
		return synchronizer;
	}

	@Override
	public void translateOnSender(final SenderTokenTranslator translator) {
		tokenMap = new HashMap<FieldIndex, FileToken>();
		for (final Field field : getFields()) {
			field.setAccessible(true);
			if (serializableFileField(field)) {
				addFileToken(translator, field);
			} else if (serializableFileListField(field)) {
				addFileTokenList(translator, field);
			} else if (serializableFileMapField(field)) {
				addFileTokenMap(translator, field);
			} else if (serializableFileTokenField(field)) {
				callTranslateOnSender(translator, field);
			}
		}
	}

	@Override
	public void translateOnReceiver(final ReceiverTokenTranslator translator, final FileTokenSynchronizer synchronizer, Set<File> filesThatShouldExist) {
		// Collect all files that were supposed to exist
		boolean checkFiles = false;

		if(filesThatShouldExist==null) {
			filesThatShouldExist = new HashSet<File>(tokenMap.size());
			checkFiles=true;
		}

		translateOnReceiverBody(translator, synchronizer, filesThatShouldExist);

		if(checkFiles) {
			FileUtilities.waitForFiles(filesThatShouldExist);
		}
	}

	private void translateOnReceiverBody(ReceiverTokenTranslator translator, FileTokenSynchronizer synchronizer, Set<File> filesThatShouldExist) {
		this.translator = translator;
		this.synchronizer = synchronizer;

		// Set all directly accessible fields from our token map
		for (final Map.Entry<FieldIndex, FileToken> entry : tokenMap.entrySet()) {
			translateEntry(translator, filesThatShouldExist, entry);
		}
		// Recursively set all the file token holder fields
		for (final Field field : getFields()) {
			translateTokenHolderField(translator, synchronizer, filesThatShouldExist, field);
		}
	}

	private void translateEntry(ReceiverTokenTranslator translator, Set<File> filesThatShouldExist, Map.Entry<FieldIndex, FileToken> entry) {
		final File file = translator.getFile(entry.getValue());
		if(entry.getValue()!=null && entry.getValue().existsOnSourceDaemon()) {
			filesThatShouldExist.add(file);
		}
		if (entry.getKey().getIndex() == null) {
			setFileField(entry.getKey().getField(), file);
		} else {
			setFileIndexedField(entry.getKey(), file);
		}
	}

	private void translateTokenHolderField(ReceiverTokenTranslator translator, FileTokenSynchronizer synchronizer, Set<File> filesThatShouldExist, Field field) {
		if (serializableFileTokenField(field)) {
			final FileTokenHolder fileTokenHolder = getFileTokenHolder(field);
			if (fileTokenHolder != null) {
				fileTokenHolder.translateOnReceiver(translator, synchronizer, filesThatShouldExist);
			}
		} else if (serializableFileListField(field)) {
			for (final Object o : getFieldList(field)) {
				if (o instanceof FileTokenHolder) {
					((FileTokenHolder) o).translateOnReceiver(translator, synchronizer, filesThatShouldExist);
				}
			}
		} else if (serializableFileMapField(field)) {
			for (final Object o : getFieldMap(field).entrySet()) {
				if (o instanceof Map.Entry) {
					Map.Entry e = (Map.Entry) o;
					if (e.getValue() instanceof FileTokenHolder) {
						((FileTokenHolder) e.getValue()).translateOnReceiver(translator, synchronizer, filesThatShouldExist);
					}
				}
			}
		}
	}

	/**
	 * @return A list of all fields to be considered. They are all set as accessible.
	 */
	private List<Field> getFields() {
		final List<Field> fields = new ArrayList<Field>();
		addFields(this.getClass(), fields);
		return fields;
	}

	private void addFields(final Class<?> clazz, final List<Field> fields) {
		final Field[] declaredFields = clazz.getDeclaredFields();
		for (final Field field : declaredFields) {
			field.setAccessible(true);
			fields.add(field);
		}
		if (!this.getClass().equals(FileHolder.class) && clazz.getSuperclass() != null) {
			addFields(clazz.getSuperclass(), fields);
		}
	}

	@Override
	public void synchronizeFileTokensOnReceiver() {
		// Nothing is being uploaded by default
	}

	/**
	 * Upload the file for a given field name. To be called from {@link #synchronizeFileTokensOnReceiver()} implementations.
	 *
	 * @param fieldName Name of the field to upload.
	 */
	public void uploadAndWait(final String fieldName) {
		final FileToken fileToken = tokenMap.get(new FieldIndex(fieldName, null));
		synchronizer.uploadAndWait(fileToken);
	}

	private boolean isSerializableField(final Field field) {
		return !Modifier.isTransient(field.getModifiers()) && !Modifier.isStatic(field.getModifiers());
	}

	private boolean serializableFileTokenField(final Field field) {
		return FileTokenHolder.class.isAssignableFrom(field.getType()) && isSerializableField(field);
	}

	private boolean serializableFileField(final Field field) {
		return File.class.isAssignableFrom(field.getType()) && isSerializableField(field);
	}

	/**
	 * @param field Object field.
	 * @return True if the field corresponds to a list of objects, at least one of which is a file or {@link FileTokenHolder}
	 */
	private boolean serializableFileListField(final Field field) {
		if (List.class.isAssignableFrom(field.getType()) && isSerializableField(field)) {
			final List list = getFieldList(field);
			if (list != null) {
				for (final Object o : list) {
					if (o instanceof File || o instanceof FileTokenHolder) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * @param field Object field.
	 * @return True if the field corresponds to a map from a Serializable(not File) to File. These are the only maps we support right now.
	 */
	private boolean serializableFileMapField(final Field field) {
		if (Map.class.isAssignableFrom(field.getType()) && isSerializableField(field)) {
			final Map map = getFieldMap(field);
			if (map != null) {
				for (final Object o : map.entrySet()) {
					if (o instanceof Map.Entry) {
						final Map.Entry entry = (Map.Entry) o;
						final Object key = entry.getKey();
						final Object value = entry.getValue();
						if (key instanceof Serializable && !(key instanceof File) &&
								(value instanceof File || value instanceof FileTokenHolder)) {
							return true;
						}
						if (key instanceof File || value instanceof File) {
							throw new MprcException("Cannot correctly serialize map entries that are not in <Serializable, File> format.");
						}
					}
				}
			}
		}
		return false;
	}

	private List getFieldList(final Field field) {
		List list = null;
		try {
			list = (List) field.get(this);
		} catch (IllegalAccessException e) {
			throwFieldAccess(field, e);
		}
		return list;
	}

	private Map getFieldMap(final Field field) {
		Map map = null;
		try {
			map = (Map) field.get(this);
		} catch (IllegalAccessException e) {
			throwFieldAccess(field, e);
		}
		return map;
	}

	private void setFileField(final String fieldName, final File file) {
		final Field field = getFieldForName(this.getClass(), fieldName);
		try {
			field.set(this, file);
		} catch (IllegalAccessException e) {
			throwFieldAccess(field, e);
		}
	}

	private void setFileIndexedField(final FieldIndex fieldIndex, final File file) {
		final Field field = getFieldForName(this.getClass(), fieldIndex.getField());
		if (List.class.isAssignableFrom(field.getType())) {
			final Serializable index = fieldIndex.getIndex();
			if (index instanceof Integer) {
				getFieldList(field).set((Integer) index, file);
			} else {
				throw new MprcException("The list index was not an integer: " + index);
			}
		} else if (Map.class.isAssignableFrom(field.getType())) {
			final Serializable index = fieldIndex.getIndex();
			getFieldMap(field).put(index, file);
		}
	}

	private Field getFieldForName(final Class<?> clazz, final String fieldName) {
		if (clazz == null) {
			return null;
		}
		Field field = null;
		try {
			field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
		} catch (NoSuchFieldException ignore) {
			// Failed to obtain, ask the parent
			field = getFieldForName(clazz.getSuperclass(), fieldName);
		}
		if (field == null) {
			throw new MprcException("Could not access field " + fieldName + " of " + this.getClass().getCanonicalName());
		}
		return field;
	}

	private void addFileToken(final SenderTokenTranslator translator, final Field field) {
		File file = null;
		try {
			file = (File) field.get(this);
		} catch (IllegalAccessException e) {
			throwFieldAccess(field, e);
		}
		tokenMap.put(new FieldIndex(field.getName(), null), token(translator, file));
	}

	private void addFileTokenList(final SenderTokenTranslator translator, final Field field) {
		int index = 0;
		for (final Object o : getFieldList(field)) {
			if (o instanceof File) {
				final File file = (File) o;
				final FileToken value = token(translator, file);
				tokenMap.put(new FieldIndex(field.getName(), index), value);
			} else if (o instanceof FileTokenHolder) {
				((FileTokenHolder) o).translateOnSender(translator);
			}
			index++;
		}
	}

	private void addFileTokenMap(final SenderTokenTranslator translator, final Field field) {
		for (final Object o : getFieldMap(field).entrySet()) {
			if (o instanceof Map.Entry) {
				final Map.Entry entry = (Map.Entry) o;
				if (entry.getKey() instanceof Serializable) {
					if (entry.getValue() instanceof File) {
						final File file = (File) entry.getValue();
						final FileToken value = token(translator, file);
						tokenMap.put(new FieldIndex(field.getName(), (Serializable) entry.getKey()), value);
					} else if (entry.getValue() instanceof FileTokenHolder) {
						((FileTokenHolder) entry.getValue()).translateOnSender(translator);
					}
				}
			}
		}
	}

	private FileToken token(final SenderTokenTranslator translator, final File file) {
		return translator.translateBeforeTransfer(file);
	}

	private void callTranslateOnSender(final SenderTokenTranslator translator, final Field field) {
		final FileTokenHolder fileTokenHolder = getFileTokenHolder(field);
		if (fileTokenHolder != null) {
			fileTokenHolder.translateOnSender(translator);
		}
	}

	private FileTokenHolder getFileTokenHolder(final Field field) {
		FileTokenHolder fileTokenHolder = null;
		try {
			fileTokenHolder = (FileTokenHolder) field.get(this);
		} catch (IllegalAccessException e) {
			throwFieldAccess(field, e);
		}
		return fileTokenHolder;
	}

	private void throwFieldAccess(final Field field, final IllegalAccessException e) {
		throw new MprcException("Could not serialize field " + field.getName() + " of " + this.getClass().getCanonicalName(), e);
	}
}
