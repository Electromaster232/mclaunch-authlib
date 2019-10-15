package com.mojang.authlib;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.mojang.authlib.properties.PropertyMap;

public class GameProfile {
	
	private final UUID id;
	private final String name;
	private final PropertyMap properties;
	private boolean legacy;
	
	public GameProfile(UUID id, String name) {
		this.properties = new PropertyMap();
		if (id == null && StringUtils.isBlank(name)) {
			throw new IllegalArgumentException("Name and ID cannot both be blank");
		}
		this.id = id;
		this.name = name;
	}
	
	public UUID getId() {
		return this.id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public PropertyMap getProperties() {
		return this.properties;
	}
	
	public boolean isLegacy() {
		return this.legacy;
	}
	
	public boolean isComplete() {
		if (this.getId() != null && StringUtils.isNotBlank(this.getName())) {
			return true;
		}
		return false;
	}
	
	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null || this.getClass() != object.getClass()) {
			return false;
		}
		GameProfile gameProfile = (GameProfile) object;
		if (this.getId() != null && gameProfile.getId() != null && this.getId().equals(gameProfile.getId())) {
			if (this.getName() != null && gameProfile.getName() != null && this.getName().equals(gameProfile.getName())) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		if (this.getId() != null) {
			if (this.getName() != null) {
				return 31 * this.getId().hashCode() + this.getName().hashCode();
			}
			return 31 * this.getId().hashCode();
		}
		return 0;
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this).append("id", this.getId()).append("name", this.getName()).append("properties", this.getProperties()).append("legacy", this.isLegacy()).toString();
	}
}