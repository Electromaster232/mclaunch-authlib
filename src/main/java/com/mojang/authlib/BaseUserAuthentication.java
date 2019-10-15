package com.mojang.authlib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.util.UUIDTypeAdapter;

public abstract class BaseUserAuthentication implements UserAuthentication {
	
	private static final Logger LOGGER;
	protected static final String STORAGE_KEY_PROFILE_NAME;
	protected static final String STORAGE_KEY_PROFILE_ID;
	protected static final String STORAGE_KEY_PROFILE_PROPERTIES;
	protected static final String STORAGE_KEY_USER_NAME;
	protected static final String STORAGE_KEY_USER_ID;
	protected static final String STORAGE_KEY_USER_PROPERTIES;
	private final AuthenticationService authenticationService;
	private final PropertyMap userProperties;
	private String userId;
	private String username;
	private String password;
	private GameProfile selectedProfile;
	private UserType userType;
	
	static {
		LOGGER = LogManager.getLogger();
		STORAGE_KEY_PROFILE_NAME = "displayName";
		STORAGE_KEY_PROFILE_ID = "uuid";
		STORAGE_KEY_PROFILE_PROPERTIES = "profileProperties";
		STORAGE_KEY_USER_NAME = "username";
		STORAGE_KEY_USER_ID = "userid";
		STORAGE_KEY_USER_PROPERTIES = "userProperties";
	}
	
	protected BaseUserAuthentication(AuthenticationService authenticationService) {
		this.userProperties = new PropertyMap();
		Validate.notNull(authenticationService);
		this.authenticationService = authenticationService;
	}
	
	@Override
	public boolean canLogIn() {
		if (!this.canPlayOnline() && StringUtils.isNotBlank(this.getUsername()) && StringUtils.isNotBlank(this.getPassword())) {
			return true;
		}
		return false;
	}
	
	@Override
	public void logOut() {
		this.password = null;
		this.userId = null;
		this.setSelectedProfile(null);
		this.getModifiableUserProperties().clear();
		this.setUserType(null);
	}
	
	@Override
	public boolean isLoggedIn() {
		if (this.getSelectedProfile() != null) {
			return true;
		}
		return false;
	}
	
	@Override
	public void setUsername(String username) {
		if (this.isLoggedIn() && this.canPlayOnline()) {
			throw new IllegalStateException("Cannot change username whilst logged in & online");
		}
		this.username = username;
	}
	
	@Override
	public void setPassword(String password) {
		if (this.isLoggedIn() && this.canPlayOnline() && StringUtils.isNotBlank(password)) {
			throw new IllegalStateException("Cannot set password whilst logged in & online");
		}
		this.password = password;
	}
	
	protected String getUsername() {
		return this.username;
	}
	
	protected String getPassword() {
		return this.password;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void loadFromStorage(Map<String, Object> credentials) {
		this.logOut();
		this.setUsername(String.valueOf(credentials.get(BaseUserAuthentication.STORAGE_KEY_USER_NAME)));
		if (credentials.containsKey(BaseUserAuthentication.STORAGE_KEY_USER_ID)) {
			this.userId = String.valueOf(credentials.get(BaseUserAuthentication.STORAGE_KEY_USER_ID));
		} else {
			this.userId = this.username;
		}
		if (credentials.containsKey(BaseUserAuthentication.STORAGE_KEY_USER_PROPERTIES)) {
			try {
				List<Map<String, String>> list = (List<Map<String, String>>) credentials.get(BaseUserAuthentication.STORAGE_KEY_USER_PROPERTIES);
				for (Map<String, String> propertyMap : list) {
					String name = propertyMap.get("name");
					String value = propertyMap.get("value");
					String signature = propertyMap.get("signature");
					if (signature == null) {
						this.getModifiableUserProperties().put(name, new Property(name, value));
					} else {
						this.getModifiableUserProperties().put(name, new Property(name, value, signature));
					}
				}
			} catch (Throwable t) {
				BaseUserAuthentication.LOGGER.warn("Couldn't deserialize user properties", t);
			}
		}
		if (credentials.containsKey(BaseUserAuthentication.STORAGE_KEY_PROFILE_NAME) && credentials.containsKey(BaseUserAuthentication.STORAGE_KEY_PROFILE_ID)) {
			GameProfile profile = new GameProfile(UUIDTypeAdapter.fromString(String.valueOf(credentials.get(BaseUserAuthentication.STORAGE_KEY_PROFILE_ID))), String.valueOf(credentials.get(BaseUserAuthentication.STORAGE_KEY_PROFILE_NAME)));
			if (credentials.containsKey(BaseUserAuthentication.STORAGE_KEY_PROFILE_PROPERTIES)) {
				try {
					List<Map<String, String>> list = (List<Map<String, String>>) credentials.get(BaseUserAuthentication.STORAGE_KEY_PROFILE_PROPERTIES);
					for (Map<String, String> propertyMap : list) {
						String name = propertyMap.get("name");
						String value = propertyMap.get("value");
						String signature = propertyMap.get("signature");
						if (signature == null) {
							profile.getProperties().put(name, new Property(name, value));
						} else {
							profile.getProperties().put(name, new Property(name, value, signature));
						}
					}
				} catch (Throwable t) {
					BaseUserAuthentication.LOGGER.warn("Couldn't deserialize profile properties", t);
				}
			}
			this.setSelectedProfile(profile);
		}
	}
	
	@Override
	public Map<String, Object> saveForStorage() {
		Map<String, Object> result = new HashMap<String, Object>();
		if (this.getUsername() != null) {
			result.put(BaseUserAuthentication.STORAGE_KEY_USER_NAME, this.getUsername());
		}
		if (this.getUserID() != null) {
			result.put(BaseUserAuthentication.STORAGE_KEY_USER_ID, this.getUserID());
		}
		if (!this.getUserProperties().isEmpty()) {
			List<Map<String, String>> properties = new ArrayList<Map<String, String>>();
			for (Property userProperty : this.getUserProperties().values()) {
				Map<String, String> property = new HashMap<String, String>();
				property.put("name", userProperty.getName());
				property.put("value", userProperty.getValue());
				property.put("signature", userProperty.getSignature());
				properties.add(property);
			}
			result.put(BaseUserAuthentication.STORAGE_KEY_USER_PROPERTIES, properties);
		}
		GameProfile selectedProfile = this.getSelectedProfile();
		if (selectedProfile != null) {
			result.put(BaseUserAuthentication.STORAGE_KEY_PROFILE_NAME, selectedProfile.getName());
			result.put(BaseUserAuthentication.STORAGE_KEY_PROFILE_ID, selectedProfile.getId());
			List<Map<String, String>> properties = new ArrayList<Map<String, String>>();
			for (Property profileProperty : selectedProfile.getProperties().values()) {
				Map<String, String> property = new HashMap<String, String>();
				property.put("name", profileProperty.getName());
				property.put("value", profileProperty.getValue());
				property.put("signature", profileProperty.getSignature());
				properties.add(property);
			}
			if (!properties.isEmpty()) {
				result.put(BaseUserAuthentication.STORAGE_KEY_PROFILE_PROPERTIES, properties);
			}
		}
		return result;
	}
	
	protected void setSelectedProfile(GameProfile selectedProfile) {
		this.selectedProfile = selectedProfile;
	}
	
	@Override
	public GameProfile getSelectedProfile() {
		return this.selectedProfile;
	}
	
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(this.getClass().getSimpleName());
		result.append("{");
		if (this.isLoggedIn()) {
			result.append("Logged in as ");
			result.append(this.getUsername());
			if (this.getSelectedProfile() != null) {
				result.append(" / ");
				result.append(this.getSelectedProfile());
				result.append(" - ");
				if (this.canPlayOnline()) {
					result.append("Online");
				} else {
					result.append("Offline");
				}
			}
		} else {
			result.append("Not logged in");
		}
		result.append("}");
		return result.toString();
	}
	
	public AuthenticationService getAuthenticationService() {
		return this.authenticationService;
	}
	
	@Override
	public String getUserID() {
		return this.userId;
	}
	
	@Override
	public PropertyMap getUserProperties() {
		if (this.isLoggedIn()) {
			PropertyMap result = new PropertyMap();
			result.putAll(this.getModifiableUserProperties());
			return result;
		}
		return new PropertyMap();
	}
	
	protected PropertyMap getModifiableUserProperties() {
		return this.userProperties;
	}
	
	@Override
	public UserType getUserType() {
		if (this.isLoggedIn()) {
			if (this.userType != null) {
				return this.userType;
			}
			return UserType.LEGACY;
		}
		return null;
	}
	
	protected void setUserType(UserType userType) {
		this.userType = userType;
	}
	
	protected void setUserid(String userId) {
		this.userId = userId;
	}
}