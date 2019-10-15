package com.mojang.authlib.yggdrasil;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Proxy;
import java.net.URL;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.UserMigratedException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import com.mojang.authlib.yggdrasil.response.Response;
import com.mojang.util.UUIDTypeAdapter;

public class YggdrasilAuthenticationService extends HttpAuthenticationService {
	
	private final String clientToken;
	private final Gson gson;
	
	public YggdrasilAuthenticationService(Proxy proxy, String clientToken) {
		super(proxy);
		this.clientToken = clientToken;
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(GameProfile.class, new GameProfileSerializer());
		builder.registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer());
		builder.registerTypeAdapter(UUID.class, new UUIDTypeAdapter());
		builder.registerTypeAdapter(ProfileSearchResultsResponse.class, new ProfileSearchResultsResponse.Serializer());
		this.gson = builder.create();
	}
	
	@Override
	public UserAuthentication createUserAuthentication(Agent agent) {
		return new YggdrasilUserAuthentication(this, agent);
	}
	
	@Override
	public MinecraftSessionService createMinecraftSessionService() {
		return new YggdrasilMinecraftSessionService(this);
	}
	
	@Override
	public GameProfileRepository createProfileRepository() {
		return new YggdrasilGameProfileRepository(this);
	}
	
	protected <T extends Response> T makeRequest(URL url, Object object, Class<T> type) throws AuthenticationException {
		try {
			String jsonResult;
			if (object == null) {
				jsonResult = performGetRequest(url);
			} else {
				jsonResult = performPostRequest(url, this.gson.toJson(object), "application/json");
			}
			T result = (T) this.gson.fromJson(jsonResult, type);
			if (result == null) {
				return null;
			} else if (StringUtils.isBlank(result.getError())) {
				return result;
			} else if (StringUtils.isNotBlank(result.getCause()) && result.getCause().equals("UserMigratedException")) {
				throw new UserMigratedException(result.getErrorMessage());
			} else if (StringUtils.isNotBlank(result.getError()) && result.getError().equals("ForbiddenOperationException")) {
				throw new InvalidCredentialsException(result.getErrorMessage());
			}
			throw new AuthenticationException(result.getErrorMessage());
		} catch (IOException | IllegalStateException | JsonParseException ex) {
			throw new AuthenticationUnavailableException("Cannot contact authentication server", ex);
		}
	}
	
	public String getClientToken() {
		return this.clientToken;
	}
	
	private static class GameProfileSerializer implements JsonSerializer<GameProfile>, JsonDeserializer<GameProfile> {
		
		public GameProfile deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
			JsonObject object = (JsonObject) jsonElement;
			UUID id = null;
			if (object.has("id")) {
				id = jsonDeserializationContext.deserialize(object.get("id"), UUID.class);
			}
			String name = null;
			if (object.has("name")) {
				name = object.getAsJsonPrimitive("name").getAsString();
			}
			return new GameProfile(id, name);
		}
		
		public JsonElement serialize(GameProfile gameProfile, Type type, JsonSerializationContext jsonSerializationContext) {
			JsonObject result = new JsonObject();
			if (gameProfile.getId() != null) {
				result.add("id", jsonSerializationContext.serialize(gameProfile.getId()));
			}
			if (gameProfile.getName() != null) {
				result.addProperty("name", gameProfile.getName());
			}
			return result;
		}
	}
}