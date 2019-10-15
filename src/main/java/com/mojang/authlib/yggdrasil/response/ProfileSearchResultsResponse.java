package com.mojang.authlib.yggdrasil.response;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;

public class ProfileSearchResultsResponse extends Response {
	
	private GameProfile[] profiles;
	
	public GameProfile[] getProfiles() {
		return this.profiles;
	}
	
	public static class Serializer implements JsonDeserializer<ProfileSearchResultsResponse> {
		
		public ProfileSearchResultsResponse deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
			ProfileSearchResultsResponse result = new ProfileSearchResultsResponse();
			if (jsonElement instanceof JsonObject) {
				JsonObject object = (JsonObject) jsonElement;
				if (object.has("error")) {
					result.setError(object.getAsJsonPrimitive("error").getAsString());
				}
				if (object.has("errorMessage")) {
					result.setError(object.getAsJsonPrimitive("errorMessage").getAsString());
				}
				if (object.has("cause")) {
					result.setError(object.getAsJsonPrimitive("cause").getAsString());
				}
			} else {
				result.profiles = (GameProfile[]) jsonDeserializationContext.deserialize(jsonElement, GameProfile[].class);
			}
			return result;
		}
	}
}