package com.mojang.authlib.yggdrasil.request;

import com.mojang.authlib.Agent;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;

public class AuthenticationRequest {
	
	private Agent agent;
	private String username;
	private String password;
	private String clientToken;
	private boolean requestUser;
	
	public AuthenticationRequest(YggdrasilUserAuthentication authenticationService, String username, String password) {
		this.agent = authenticationService.getAgent();
		this.username = username;
		this.clientToken = authenticationService.getAuthenticationService().getClientToken();
		this.password = password;
		this.requestUser = true;
	}
	
	protected Agent getAgent() {
		return this.agent;
	}
	
	protected String getUsername() {
		return this.username;
	}
	
	protected String getPassword() {
		return this.password;
	}
	
	protected String getClientToken() {
		return this.clientToken;
	}
	
	protected boolean isRequestUser() {
		return this.requestUser;
	}
}