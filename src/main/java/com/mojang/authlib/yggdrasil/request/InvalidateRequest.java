package com.mojang.authlib.yggdrasil.request;

import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;

public class InvalidateRequest {
	
	private String accessToken;
	private String clientToken;
	
	public InvalidateRequest(YggdrasilUserAuthentication authenticationService) {
		this.accessToken = authenticationService.getAuthenticatedToken();
		this.clientToken = authenticationService.getAuthenticationService().getClientToken();
	}
	
	protected String getAccessToken() {
		return this.accessToken;
	}
	
	protected String getClientToken() {
		return this.clientToken;
	}
}