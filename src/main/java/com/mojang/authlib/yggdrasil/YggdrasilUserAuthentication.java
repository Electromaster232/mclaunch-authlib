package com.mojang.authlib.yggdrasil;

import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.HttpUserAuthentication;
import com.mojang.authlib.UserType;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.yggdrasil.request.AuthenticationRequest;
import com.mojang.authlib.yggdrasil.request.RefreshRequest;
import com.mojang.authlib.yggdrasil.request.ValidateRequest;
import com.mojang.authlib.yggdrasil.response.AuthenticationResponse;
import com.mojang.authlib.yggdrasil.response.RefreshResponse;
import com.mojang.authlib.yggdrasil.response.Response;
import com.mojang.authlib.yggdrasil.response.User;

public class YggdrasilUserAuthentication extends HttpUserAuthentication {
	
	private static final Logger LOGGER;
	private static final String BASE_URL = "https://authserver.mcnet.djelectro.me/";
	private static final URL ROUTE_AUTHENTICATE;
	private static final URL ROUTE_REFRESH;
	private static final URL ROUTE_VALIDATE;
	private static final URL ROUTE_INVALIDATE;
	private static final URL ROUTE_SIGNOUT;
	private static final String STORAGE_KEY_ACCESS_TOKEN;
	private final Agent agent;
	private GameProfile[] profiles;
	private String accessToken;
	private boolean isOnline;
	
	static {
		LOGGER = LogManager.getLogger();
		ROUTE_AUTHENTICATE = HttpAuthenticationService.constantURL(BASE_URL + "authenticate");
		ROUTE_REFRESH = HttpAuthenticationService.constantURL(BASE_URL + "refresh");
		ROUTE_VALIDATE = HttpAuthenticationService.constantURL(BASE_URL + "validate");
		ROUTE_INVALIDATE = HttpAuthenticationService.constantURL(BASE_URL + "invalidate");
		ROUTE_SIGNOUT = HttpAuthenticationService.constantURL(BASE_URL + "signout");
		STORAGE_KEY_ACCESS_TOKEN = "accessToken";
	}
	
	public YggdrasilUserAuthentication(YggdrasilAuthenticationService authenticationService, Agent agent) {
		super(authenticationService);
		this.agent = agent;
	}
	
	@Override
	public boolean canLogIn() {
		if (!this.canPlayOnline() && StringUtils.isNotBlank(this.getUsername()) && (StringUtils.isNotBlank(this.getPassword()) || StringUtils.isNotBlank(this.getAuthenticatedToken()))) {
			return true;
		}
		return false;
	}
	
	@Override
	public void logIn() throws AuthenticationException {
		if (StringUtils.isBlank(this.getUsername())) {
			throw new InvalidCredentialsException("Invalid username");
		}
		if (StringUtils.isNotBlank(this.getAuthenticatedToken())) {
			this.logInWithToken();
		} else if (StringUtils.isNotBlank(this.getPassword())) {
			this.logInWithPassword();
		} else {
			throw new InvalidCredentialsException("Invalid password");
		}
	}
	
	protected void logInWithPassword() throws AuthenticationException {
		if (StringUtils.isBlank(this.getUsername())) {
			throw new InvalidCredentialsException("Invalid username");
		}
		if (StringUtils.isBlank(this.getPassword())) {
			throw new InvalidCredentialsException("Invalid password");
		}
		YggdrasilUserAuthentication.LOGGER.info("Logging in with username & password");
		AuthenticationRequest request = new AuthenticationRequest(this, this.getUsername(), this.getPassword());
		AuthenticationResponse response = this.getAuthenticationService().makeRequest(YggdrasilUserAuthentication.ROUTE_AUTHENTICATE, request, AuthenticationResponse.class);
		if (!response.getClientToken().equals(this.getAuthenticationService().getClientToken())) {
			throw new AuthenticationException("Server requested we change our client token, Don't know how to handle this!");
		}
		if (response.getSelectedProfile() != null) {
			this.setUserType(response.getSelectedProfile().isLegacy() ? UserType.LEGACY : UserType.MOJANG);
		} else if (ArrayUtils.isNotEmpty(response.getAvailableProfiles())) {
			this.setUserType(response.getAvailableProfiles()[0].isLegacy() ? UserType.LEGACY : UserType.MOJANG);
		}
		User user = response.getUser();
		if (user != null && user.getId() != null) {
			this.setUserid(user.getId());
		} else {
			this.setUserid(this.getUsername());
		}
		this.isOnline = true;
		this.accessToken = response.getAccessToken();
		this.profiles = response.getAvailableProfiles();
		this.setSelectedProfile(response.getSelectedProfile());
		this.getModifiableUserProperties().clear();
		this.updateUserProperties(user);
	}
	
	protected void updateUserProperties(User user) {
		if (user == null) {
			return;
		}
		if (user.getProperties() != null) {
			this.getModifiableUserProperties().putAll(user.getProperties());
		}
	}
	
	protected void logInWithToken() throws AuthenticationException {
		if (StringUtils.isBlank(this.getUserID())) {
			if (StringUtils.isBlank(this.getUsername())) {
				this.setUserid(this.getUsername());
			} else {
				throw new InvalidCredentialsException("Invalid access token");
			}
		}
		if (StringUtils.isBlank(this.getAuthenticatedToken())) {
			throw new InvalidCredentialsException("Invalid access token");
		}
		YggdrasilUserAuthentication.LOGGER.info("Logging in with access token");
		if (this.checkTokenValidity()) {
			YggdrasilUserAuthentication.LOGGER.debug("Skipping refresh call as we're safely logged in.");
			this.isOnline = true;
			return;
		}
		RefreshRequest request = new RefreshRequest(this);
		RefreshResponse response = this.getAuthenticationService().makeRequest(YggdrasilUserAuthentication.ROUTE_REFRESH, request, RefreshResponse.class);
		if (!response.getClientToken().equals(this.getAuthenticationService().getClientToken())) {
			throw new AuthenticationException("Server requested we change out client token. Don't know how to handle this!");
		}
		if (response.getSelectedProfile() != null) {
			this.setUserType(response.getSelectedProfile().isLegacy() ? UserType.LEGACY : UserType.MOJANG);
		} else if (ArrayUtils.isNotEmpty(response.getAvailableProfiles())) {
			this.setUserType(response.getAvailableProfiles()[0].isLegacy() ? UserType.LEGACY : UserType.MOJANG);
		}
		if (response.getUser() != null && response.getUser().getId() != null) {
			this.setUserid(response.getUser().getId());
		} else {
			this.setUserid(this.getUsername());
		}
		this.isOnline = true;
		this.accessToken = response.getAccessToken();
		this.profiles = response.getAvailableProfiles();
		this.setSelectedProfile(response.getSelectedProfile());
		this.getModifiableUserProperties().clear();
		this.updateUserProperties(response.getUser());
	}
	
	protected boolean checkTokenValidity() throws AuthenticationException {
		ValidateRequest request = new ValidateRequest(this);
		try {
			this.getAuthenticationService().makeRequest(YggdrasilUserAuthentication.ROUTE_VALIDATE, request, Response.class);
			return true;
		} catch (AuthenticationException ex) {
		}
		return false;
	}
	
	@Override
	public void logOut() {
		super.logOut();
		this.accessToken = null;
		this.profiles = null;
		this.isOnline = false;
	}
	
	@Override
	public GameProfile[] getAvailableProfiles() {
		return this.profiles;
	}
	
	@Override
	public boolean isLoggedIn() {
		return StringUtils.isNotBlank(this.accessToken);
	}
	
	@Override
	public boolean canPlayOnline() {
		return this.isLoggedIn() && this.getSelectedProfile() != null && this.isOnline;
	}
	
	@Override
	public void selectGameProfile(GameProfile profile) throws AuthenticationException {
		if (!this.isLoggedIn()) {
			throw new AuthenticationException("Cannot change game profile whilst not logged in");
		}
		if (this.getSelectedProfile() != null) {
			throw new AuthenticationException("Cannot change game profile. You must log out and back in");
		}
		if (profile == null || !ArrayUtils.contains(this.profiles, profile)) {
			throw new IllegalArgumentException("Invalid profile '" + profile + "'");
		}
		RefreshRequest request = new RefreshRequest(this, profile);
		RefreshResponse response = this.getAuthenticationService().makeRequest(YggdrasilUserAuthentication.ROUTE_REFRESH, request, RefreshResponse.class);
		if (!response.getClientToken().equals(this.getAuthenticationService().getClientToken())) {
			throw new AuthenticationException("Server requested we change our client token. Don't know how to handler this!");
		}
		this.isOnline = true;
		this.accessToken = response.getAccessToken();
		this.setSelectedProfile(response.getSelectedProfile());
	}
	
	@Override
	public void loadFromStorage(Map<String, Object> credentials) {
		super.loadFromStorage(credentials);
		this.accessToken = String.valueOf(credentials.get(YggdrasilUserAuthentication.STORAGE_KEY_ACCESS_TOKEN));
	}
	
	@Override
	public Map<String, Object> saveForStorage() {
		Map<String, Object> result = super.saveForStorage();
		if (StringUtils.isNotBlank(this.getAuthenticatedToken())) {
			result.put(YggdrasilUserAuthentication.STORAGE_KEY_ACCESS_TOKEN, this.getAuthenticationService());
		}
		return result;
	}
	
	@Deprecated
	public String getSessonToken() {
		if (this.isLoggedIn() && this.getSelectedProfile() != null && this.canPlayOnline()) {
			return String.format("token:%s:%s", new Object[] {this.getAuthenticatedToken(), this.getSelectedProfile().getId()});
		}
		return null;
	}
	
	@Override
	public String getAuthenticatedToken() {
		return this.accessToken;
	}
	
	public Agent getAgent() {
		return this.agent;
	}
	
	@Override
	public String toString() {
		return "YggdrasilAuthenticationService{agent=" + this.getAgent() + ", profiles=" + Arrays.toString(this.getAvailableProfiles()) + ", selectedProfile=" + this.getSelectedProfile() + ", username='" + this.getUsername() + '\'' + ", isLoggedIn=" + this.isLoggedIn() + ", userType=" + this.getUserType() + ", canPlayOnline=" + this.canPlayOnline() + ", accessToken='" + this.getAuthenticatedToken() + '\'' + ", clientToken='" + this.getAuthenticationService().getClientToken() + '\'' + '}';
    }
	
	@Override
	public YggdrasilAuthenticationService getAuthenticationService() {
		return (YggdrasilAuthenticationService) super.getAuthenticationService();
	}
	
	public URL getInvalidateUrl() {
		return YggdrasilUserAuthentication.ROUTE_INVALIDATE;
	}
	
	public URL getSignoutUrl() {
		return YggdrasilUserAuthentication.ROUTE_SIGNOUT;
	}
}