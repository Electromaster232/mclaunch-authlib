package com.mojang.authlib;

public interface GameProfileRepository {
	
	public void findProfilesByNames(String[] paramArrayOfString, Agent paramAgent, ProfileLookupCallback paramProfileLookupCallback);
}