package edu.mayo.mprc.swift.commands;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import edu.mayo.mprc.MprcException;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;

/**
 * @author Roman Zenka
 */
public final class JodaTimeTypeAdapter extends TypeAdapter<DateTime> {

	@Override
	public void write(JsonWriter out, DateTime value) throws IOException {
		out.value(value.toString(ISODateTimeFormat.dateTime()));
	}

	@Override
	public DateTime read(JsonReader in) throws IOException {
		throw new MprcException("Not implemented");
	}
}
