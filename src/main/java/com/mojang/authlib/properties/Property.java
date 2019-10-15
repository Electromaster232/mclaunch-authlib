package com.mojang.authlib.properties;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

public class Property {
	
	private final String name;
	private final String value;
	private final String signature;
	
	public Property(String name, String value) {
		this(name, value, null);
	}
	
	public Property(String name, String value, String signature) {
		this.name = name;
		this.value = value;
		this.signature = signature;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getValue() {
		return this.value;
	}
	
	public String getSignature() {
		return this.signature;
	}
	
	public boolean hasSignature() {
		if (StringUtils.isNotBlank(getSignature())) {
			return true;
		}
		return false;
	}
	
	public boolean isSignatureValid(PublicKey publicKey) {
		try {
			Signature signature = Signature.getInstance("SHA1withRSA");
			signature.initVerify(publicKey);
			signature.update(getValue().getBytes());
			return signature.verify(Base64.decodeBase64(getSignature()));
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException ex) {
			ex.printStackTrace();
		}
		return false;
	}
}