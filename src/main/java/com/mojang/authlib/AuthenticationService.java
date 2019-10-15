package com.mojang.authlib;

import com.mojang.authlib.minecraft.MinecraftSessionService;

public interface AuthenticationService {
	
	public UserAuthentication createUserAuthentication(Agent paramAgent);
	public MinecraftSessionService createMinecraftSessionService();
	public GameProfileRepository createProfileRepository();
}