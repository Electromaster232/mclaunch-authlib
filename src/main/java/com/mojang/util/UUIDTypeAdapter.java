package com.mojang.util;

import java.io.IOException;
import java.util.UUID;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class UUIDTypeAdapter extends TypeAdapter<UUID> {
	
	public void write(JsonWriter jsonWriter, UUID uuid) throws IOException {
		jsonWriter.value(fromUUID(uuid));
	}
	
	public UUID read(JsonReader jsonReader) throws IOException {
		return fromString(jsonReader.nextString());
	}
	
	public static String fromUUID(UUID uuid) {
		return uuid.toString().replace("-", "");
	}
	
	public static UUID fromString(String string) {
		return UUID.fromString(string.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
	}
}