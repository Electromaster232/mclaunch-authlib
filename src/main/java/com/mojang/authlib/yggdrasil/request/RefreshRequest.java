package com.mojang.authlib.yggdrasil.request;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;

public class RefreshRequest {
	
	private String clientToken;
	private String accessToken;
	private GameProfile selectedProfile;
	private boolean requestUser;
	
	public RefreshRequest(YggdrasilUserAuthentication authenticationService) {
		this(authenticationService, null);
	}
	
	public RefreshRequest(YggdrasilUserAuthentication authenticationService, GameProfile profile) {
		this.clientToken = authenticationService.getAuthenticationService().getClientToken();
		this.accessToken = authenticationService.getAuthenticatedToken();
		this.selectedProfile = profile;
		this.requestUser = true;
	}
	
	protected String getClientToken() {
		return this.clientToken;
	}
	
	protected String getAccessToken() {
		return this.accessToken;
	}
	
	protected GameProfile getSelectedProfile() {
		return this.selectedProfile;
	}
	
	protected boolean isRequestUser() {
		return this.requestUser;
	}
}