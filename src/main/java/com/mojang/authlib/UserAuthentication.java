package com.mojang.authlib;

import java.util.Map;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.properties.PropertyMap;

public interface UserAuthentication {
	
	public boolean canLogIn();
	public void logIn() throws AuthenticationException;
	public void logOut();
	public boolean isLoggedIn();
	public boolean canPlayOnline();
	public GameProfile[] getAvailableProfiles();
	public GameProfile getSelectedProfile();
	public void selectGameProfile(GameProfile paramGameProfile) throws AuthenticationException;
	public void loadFromStorage(Map<String, Object> paramMap);
	public Map<String, Object> saveForStorage();
	public void setUsername(String paramString);
	public void setPassword(String paramString);
	public String getAuthenticatedToken();
	public String getUserID();
	public PropertyMap getUserProperties();
	public UserType getUserType();
}