package com.mojang.authlib.legacy;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.HttpUserAuthentication;
import com.mojang.authlib.UserType;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.util.UUIDTypeAdapter;

public class LegacyUserAuthentication extends HttpUserAuthentication {
	
	private static final URL AUTHENTICATION_URL;
	private static final int AUTHENTICATION_VERSION;
	private static final int RESPONSE_PART_PROFILE_NAME;
	private static final int RESPONSE_PART_SESSION_TOKEN;
	private static final int RESPONSE_PART_PROFILE_ID;
	private String sessionToken;
	
	static {
		AUTHENTICATION_URL = HttpAuthenticationService.constantURL("https://login.mcnet.djelectro.me");
		AUTHENTICATION_VERSION = 14;
		RESPONSE_PART_PROFILE_NAME = 2;
		RESPONSE_PART_SESSION_TOKEN = 3;
		RESPONSE_PART_PROFILE_ID = 4;
	}
	
	protected LegacyUserAuthentication(LegacyAuthenticationService authenticationService) {
		super(authenticationService);
	}
	
	@Override
	public void logIn() throws AuthenticationException {
		if (StringUtils.isBlank(this.getUsername())) {
			throw new InvalidCredentialsException("Invalid username");
		}
		
		if (StringUtils.isBlank(this.getPassword())) {
			throw new InvalidCredentialsException("Invalid password");
		}
		
		HashMap<String, Object> args = new HashMap<String, Object>();
		args.put("user", this.getUsername());
		args.put("password", this.getPassword());
		args.put("version", LegacyUserAuthentication.AUTHENTICATION_VERSION);
		
		String response;
		try {
			response = this.getAuthenticationService().performPostRequest(LegacyUserAuthentication.AUTHENTICATION_URL, HttpAuthenticationService.buildQuery(args), "application/x-www-form-urlencoded").trim();
		} catch (IOException ex) {
			throw new AuthenticationException("Authentication service is not responding", ex);
		}
		
		if (StringUtils.isBlank(response)) {
			throw new AuthenticationException("Invalid response from authentication server");
		}
		
		String[] split = response.split(":");
		if (split.length != 5) {
			throw new InvalidCredentialsException(response);
		}
		
		String profileId = split[LegacyUserAuthentication.RESPONSE_PART_PROFILE_ID];
		String profileName = split[LegacyUserAuthentication.RESPONSE_PART_PROFILE_NAME];
		String sessionToken = split[LegacyUserAuthentication.RESPONSE_PART_SESSION_TOKEN];
		
		if (StringUtils.isBlank(profileId) || StringUtils.isBlank(profileName) || StringUtils.isBlank(sessionToken)) {
			throw new AuthenticationException("Unknown response from authentication server: " + response);
		}
		
		this.setSelectedProfile(new GameProfile(UUIDTypeAdapter.fromString(profileId), profileName));
		this.sessionToken = sessionToken;
		this.setUserType(UserType.LEGACY);
	}
	
	@Override
	public void logOut() {
		super.logOut();
		this.sessionToken = null;
	}
	
	@Override
	public boolean canPlayOnline() {
		if (this.isLoggedIn() && this.getSelectedProfile() != null && this.getAuthenticatedToken() != null) {
			return true;
		}
		return false;
	}
	
	@Override
	public GameProfile[] getAvailableProfiles() {
		if (this.getSelectedProfile() != null) {
			return new GameProfile[] { this.getSelectedProfile() };
		}
		return new GameProfile[0];
	}
	
	@Override
	public void selectGameProfile(GameProfile profile) throws AuthenticationException {
		throw new UnsupportedOperationException("Game profiles cannot be changed in the legacy authentication service");
	}
	
	@Override
	public String getAuthenticatedToken() {
		return this.sessionToken;
	}
	
	@Override
	public String getUserID() {
		return this.getUsername();
	}
	
	@Override
	public LegacyAuthenticationService getAuthenticationService() {
		return (LegacyAuthenticationService) super.getAuthenticationService();
	}
}