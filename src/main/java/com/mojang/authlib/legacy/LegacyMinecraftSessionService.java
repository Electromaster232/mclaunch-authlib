package com.mojang.authlib.legacy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.HttpMinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

public class LegacyMinecraftSessionService extends HttpMinecraftSessionService {
	
	private static final String BASE_URL = "http://session.mcnet.djelectro.me/game/";
	private static final URL JOIN_URL;
	private static final URL CHECK_URL;
	
	static {
		JOIN_URL = HttpAuthenticationService.constantURL(LegacyMinecraftSessionService.BASE_URL + "joinserver.jsp");
		CHECK_URL = HttpAuthenticationService.constantURL(LegacyMinecraftSessionService.BASE_URL + "checkserver.jsp");
	}
	
	protected LegacyMinecraftSessionService(LegacyAuthenticationService authenticationService) {
		super(authenticationService);
	}
	
	@Override
	public void joinServer(GameProfile profile, String authenticationToken, String serverId) throws AuthenticationException {
		HashMap<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("user", profile.getName());
		arguments.put("sessionId", authenticationToken);
		arguments.put("serverId", serverId);
		URL url = HttpAuthenticationService.concatenateURL(LegacyMinecraftSessionService.JOIN_URL, HttpAuthenticationService.buildQuery(arguments));
		
		try {
			String response = this.getAuthenticationService().performGetRequest(url);
			if (response == null || !response.equals("OK")) {
				throw new AuthenticationException(response);
			}
		} catch (IOException ex) {
			throw new AuthenticationUnavailableException(ex);
		}
	}
	
	@Override
	public GameProfile hasJoinedServer(GameProfile user, String serverId, InetAddress address) throws AuthenticationUnavailableException {
		HashMap<String, Object> arguments = new HashMap<String, Object>();
		arguments.put("user", user.getName());
		arguments.put("serverId", serverId);
		URL url = HttpAuthenticationService.concatenateURL(LegacyMinecraftSessionService.CHECK_URL, HttpAuthenticationService.buildQuery(arguments));
		try {
			String response = this.getAuthenticationService().performGetRequest(url);
			if (response != null && response.equals("YES")) {
				return user;
			}
		} catch (IOException ex) {
			throw new AuthenticationUnavailableException(ex);
		}
		return null;
	}
	
	@Override
	public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile profile, boolean requireSecure) {
		return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
	}
	
	@Override
	public GameProfile fillProfileProperties(GameProfile profile, boolean requireSecure) {
		return profile;
	}
	
	@Override
	public LegacyAuthenticationService getAuthenticationService() {
		return (LegacyAuthenticationService) super.getAuthenticationService();
	}
}