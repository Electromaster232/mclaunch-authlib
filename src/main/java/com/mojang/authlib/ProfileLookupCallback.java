package com.mojang.authlib;

public interface ProfileLookupCallback {
	
	public void onProfileLookupSucceeded(GameProfile paramGameProfile);
	public void onProfileLookupFailed(GameProfile paramGameProfile, Exception paramException);
}