package com.mojang.authlib.yggdrasil;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.HttpMinecraftSessionService;
import com.mojang.authlib.minecraft.InsecureTextureException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.authlib.yggdrasil.response.Response;
import com.mojang.util.UUIDTypeAdapter;

public class YggdrasilMinecraftSessionService extends HttpMinecraftSessionService {
	
	private static final String[] WHITELISTED_DOMAINS;
	private static final Logger LOGGER;
	private static final String BASE_URL;
	private static final URL JOIN_URL;
	private static final URL CHECK_URL;
	private static final URL PROFILE_URL;
	private final PublicKey publicKey;
	private final Gson gson;
	private final LoadingCache<GameProfile, GameProfile> insecureProfiles;
	
	static {
		WHITELISTED_DOMAINS = new String[] { ".mcnet.djelectro.me", ".mcnet.djelectro.me" };
		LOGGER = LogManager.getLogger();
		BASE_URL = "https://sessionserver.mcnet.djelectro.me/session/minecraft/";
		JOIN_URL = HttpAuthenticationService.constantURL(BASE_URL + "join");
		CHECK_URL = HttpAuthenticationService.constantURL(BASE_URL + "hasJoined");
		PROFILE_URL = HttpAuthenticationService.constantURL(BASE_URL + "profile/");
	}
	
	protected YggdrasilMinecraftSessionService(YggdrasilAuthenticationService authenticationService) {
		super(authenticationService);
		this.gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
		this.insecureProfiles = CacheBuilder.newBuilder().expireAfterWrite(6L, TimeUnit.HOURS).build(new CacheLoader<GameProfile, GameProfile>() {
			public GameProfile load(GameProfile key) throws Exception {
				return YggdrasilMinecraftSessionService.this.fillGameProfile(key, false);
			}
		});
		try {
			X509EncodedKeySpec spec = new X509EncodedKeySpec(IOUtils.toByteArray(YggdrasilMinecraftSessionService.class.getResourceAsStream("/yggdrasil_session_pubkey.der")));
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			this.publicKey = keyFactory.generatePublic(spec);
		} catch (Exception ex) {
			throw new Error("Missing/invaild yggdrasil public key!");
		}
	}
	
	@Override
	public void joinServer(GameProfile profile, String authenticationToken, String serverId) throws AuthenticationException {
		JoinMinecraftServerRequest request = new JoinMinecraftServerRequest();
		request.accessToken = authenticationToken;
		request.selectedProfile = profile.getId();
		request.serverId = serverId;
		this.getAuthenticationService().makeRequest(YggdrasilMinecraftSessionService.JOIN_URL, request, Response.class);
	}
	
	@Override
	public GameProfile hasJoinedServer(GameProfile user, String serverId, InetAddress address) throws AuthenticationUnavailableException {
		HashMap<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("username", user.getName());
		arguments.put("serverId", serverId);
		if (address != null) {
			arguments.put("ip", address.getHostAddress());
		}
		
		URL url = HttpAuthenticationService.concatenateURL(YggdrasilMinecraftSessionService.CHECK_URL, HttpAuthenticationService.buildQuery(arguments));
		try {
			HasJoinedMinecraftServerResponse response = this.getAuthenticationService().makeRequest(url, null, HasJoinedMinecraftServerResponse.class);
			if (response != null && response.getId() != null) {
				GameProfile result = new GameProfile(response.getId(), user.getName());
				if (response.getProperties() != null) {
					result.getProperties().putAll(response.getProperties());
				}
				return result;
			}
			return null;
		} catch (AuthenticationUnavailableException ex) {
			throw ex;
		} catch (AuthenticationException ex) {
			return null;
		}
	}
	
	@Override
	public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile profile, boolean requireSecure) {
		Property textureProperty = Iterables.getFirst(profile.getProperties().get("textures"), null);
		if (textureProperty == null) {
			return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
		}
		if (requireSecure) {
			if (!textureProperty.hasSignature()) {
				YggdrasilMinecraftSessionService.LOGGER.error("Signature is missing from textures payload");
				throw new InsecureTextureException("Signature is missing from texture payload");
			}
			if (!textureProperty.isSignatureValid(this.publicKey)) {
				YggdrasilMinecraftSessionService.LOGGER.error("Texture payload has been tampered with (signature invalid)");
				throw new InsecureTextureException("Texture payload has been tampered with (signature invalid)");
			}
		}
		MinecraftTexturesPayload result;
		try {
			String json = new String(Base64.decodeBase64(textureProperty.getValue()), StandardCharsets.UTF_8);
			result = this.gson.fromJson(json, MinecraftTexturesPayload.class);
		} catch (JsonParseException ex) {
			YggdrasilMinecraftSessionService.LOGGER.error("Could not decode textures payload", ex);
			return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
		}
		if (result == null || result.getTextures() == null) {
			return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
		}
		return result.getTextures();
	}
	
	@Override
	public GameProfile fillProfileProperties(GameProfile profile, boolean requireSecure) {
		if (profile.getId() == null) {
			return profile;
		}
		if (!requireSecure) {
			return this.insecureProfiles.getUnchecked(profile);
		}
		return this.fillGameProfile(profile, true);
	}
	
	protected GameProfile fillGameProfile(GameProfile profile, boolean requireSecure) {
		try {
			URL url = HttpAuthenticationService.constantURL(YggdrasilMinecraftSessionService.PROFILE_URL + UUIDTypeAdapter.fromUUID(profile.getId()));
			url = HttpAuthenticationService.concatenateURL(url, "unsigned=" + !requireSecure);
			MinecraftProfilePropertiesResponse response = this.getAuthenticationService().makeRequest(url, null, MinecraftProfilePropertiesResponse.class);
			if (response == null) {
				YggdrasilMinecraftSessionService.LOGGER.debug("Couldn't fetch profile properties for " + profile + " as the profile does not exist");
				return profile;
			}
			GameProfile result = new GameProfile(response.getId(), response.getName());
			result.getProperties().putAll(response.getProperties());
			profile.getProperties().putAll(response.getProperties());
			YggdrasilMinecraftSessionService.LOGGER.debug("Successfully fetched profile properties for " + profile);
			return result;
		} catch (AuthenticationException ex) {
			YggdrasilMinecraftSessionService.LOGGER.warn("Couldn't look up profile properties for " + profile, ex);
		}
		return profile;
	}
	
	@Override
	public YggdrasilAuthenticationService getAuthenticationService() {
		return (YggdrasilAuthenticationService) super.getAuthenticationService();
	}
	
	private static boolean isWhitelistedDomain(String url) {
		URI uri = null;
		try {
			uri = new URI(url);
		} catch(URISyntaxException ex) {
			throw new IllegalArgumentException("Invalid URL '" + url + "'");
		}
		String domain = uri.getHost();
		for (int i = 0; i < WHITELISTED_DOMAINS.length; i++) {
			if (domain.endsWith(WHITELISTED_DOMAINS[i])) {
				return true;
			}
		}
		return false;
	}
}