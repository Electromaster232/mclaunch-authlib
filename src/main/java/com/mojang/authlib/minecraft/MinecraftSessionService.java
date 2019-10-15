package com.mojang.authlib.minecraft;

import java.net.InetAddress;
import java.util.Map;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;

public interface MinecraftSessionService {
	
	public void joinServer(GameProfile paramGameProfile, String paramString1, String paramString2) throws AuthenticationException;
	public GameProfile hasJoinedServer(GameProfile paramGameProfile, String paramString, InetAddress paramInetAddress) throws AuthenticationUnavailableException;
	public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile paramGameProfile, boolean paramBoolean);
	public GameProfile fillProfileProperties(GameProfile paramGameProfile, boolean paramBoolean);
}