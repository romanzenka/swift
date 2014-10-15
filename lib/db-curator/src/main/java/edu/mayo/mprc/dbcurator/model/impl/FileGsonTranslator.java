package edu.mayo.mprc.dbcurator.model.impl;

import com.google.gson.*;
import edu.mayo.mprc.database.FileTokenToDatabaseTranslator;

import java.io.File;
import java.lang.reflect.Type;

/**
 * @author Raymond Moore
 */
public class FileGsonTranslator implements JsonSerializer<File>, JsonDeserializer<File> {
    private FileTokenToDatabaseTranslator translator;

    public FileGsonTranslator(FileTokenToDatabaseTranslator translator) {
        this.translator = translator;
    }

    @Override
    public JsonElement serialize(File file, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(translator.fileToDatabaseToken(file));
    }

    @Override
    public File deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return translator.databaseTokenToFile( jsonElement.getAsString() );
    }
}
