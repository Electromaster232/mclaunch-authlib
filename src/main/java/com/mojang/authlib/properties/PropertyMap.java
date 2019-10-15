package com.mojang.authlib.properties;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.common.collect.ForwardingMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class PropertyMap extends ForwardingMultimap<String, Property> {
	
	private final Multimap<String, Property> properties;
	
	public PropertyMap() {
		this.properties = LinkedHashMultimap.create();
	}
	
	protected Multimap<String, Property> delegate() {
		return this.properties;
	}
	
	public static class Serializer implements JsonSerializer<PropertyMap>, JsonDeserializer<PropertyMap> {
		public PropertyMap deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
			PropertyMap result = new PropertyMap();
			if (jsonElement instanceof JsonObject) {
				JsonObject object = (JsonObject) jsonElement;
				for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
					if (!(entry.getValue() instanceof JsonArray)) {
						continue;
					}
					for (JsonElement element : (JsonArray) entry.getValue()) {
						result.put(entry.getKey(), new Property(entry.getKey(), element.getAsString()));
					}
				}
			} else if (jsonElement instanceof JsonArray) {
				for (JsonElement element : (JsonArray) jsonElement) {
					if (!(element instanceof JsonObject)) {
						continue;
					}
					JsonObject object = (JsonObject) element;
					String name = object.getAsJsonPrimitive("name").getAsString();
					String value = object.getAsJsonPrimitive("value").getAsString();
					if (object.has("signature")) {
						result.put(name, new Property(name, value, object.getAsJsonPrimitive("signature").getAsString()));
					} else {
						result.put(name, new Property(name, value));
					}
				}
			}
			return result;
		}
		
		public JsonElement serialize(PropertyMap propertyMap, Type type, JsonSerializationContext jsonDeserializationContext) {
			JsonArray result = new JsonArray();
			for (Property property : propertyMap.values()) {
				JsonObject object = new JsonObject();
				object.addProperty("name", property.getName());
				object.addProperty("value", property.getValue());
				if (property.hasSignature()) {
					object.addProperty("signature", property.getSignature());
				}
				result.add(object);
			}
			return result;
		}
	}
}