package com.mojang.authlib;

import java.util.HashMap;
import java.util.Map;

public enum UserType {
	
	LEGACY("legacy"),
	MOJANG("mojang");
	
	private static final Map<String, UserType> BY_NAME;
	private final String name;
	
	static {
		BY_NAME = new HashMap<String, UserType>();
		for (UserType userType : values()) {
			UserType.BY_NAME.put(userType.name, userType);
		}
	}
	
	private UserType(String name) {
		this.name = name;
	}
	
	public static UserType byName(String string) {
		return UserType.BY_NAME.get(string.toLowerCase());
	}
	
	public String getName() {
		return this.name;
	}
}