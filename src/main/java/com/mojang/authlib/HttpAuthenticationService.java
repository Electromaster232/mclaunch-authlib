package com.mojang.authlib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class HttpAuthenticationService extends BaseAuthenticationService {
	
	private static final Logger LOGGER;
	private final Proxy proxy;
	
	static {
		LOGGER = LogManager.getLogger();
	}
	
	protected HttpAuthenticationService(Proxy proxy) {
		Validate.notNull(proxy);
		this.proxy = proxy;
	}
	
	public Proxy getProxy() {
		return this.proxy;
	}
	
	protected HttpURLConnection createUrlConnection(URL url) throws IOException {
		Validate.notNull(url);
		HttpAuthenticationService.LOGGER.debug("Opening connection to " + url);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection(getProxy());
		connection.setConnectTimeout(15000);
		connection.setReadTimeout(15000);
		connection.setUseCaches(false);
		return connection;
	}
	
	public String performPostRequest(URL url, String post, String contentType) throws IOException {
		Validate.notNull(url);
		Validate.notNull(post);
		Validate.notNull(contentType);
		HttpURLConnection connection = this.createUrlConnection(url);
		byte[] postAsBytes = post.getBytes(StandardCharsets.UTF_8);
		connection.setRequestProperty("Content-Type", contentType + "; charset=utf-8");
		connection.setRequestProperty("Content-Length", "" + postAsBytes.length);
		connection.setDoOutput(true);
		HttpAuthenticationService.LOGGER.debug("Writing POST data to " + url + ": " + post);
		OutputStream outputStream = null;
		try {
			outputStream = connection.getOutputStream();
			IOUtils.write(postAsBytes, outputStream);
		} finally {
			IOUtils.closeQuietly(outputStream);
		}
		HttpAuthenticationService.LOGGER.debug("Reading data from " + url);
		InputStream inputStream = null;
		try {
			inputStream = connection.getInputStream();
			String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
			HttpAuthenticationService.LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
			HttpAuthenticationService.LOGGER.debug("Response: " + result);
			return result;
		} catch (IOException ex) {
			IOUtils.closeQuietly(inputStream);
			inputStream = connection.getErrorStream();
			if (inputStream != null) {
				HttpAuthenticationService.LOGGER.debug("Reading error page from " + url);
				String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
				HttpAuthenticationService.LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
				HttpAuthenticationService.LOGGER.debug("Response: " + result);
				return result;
			}
			HttpAuthenticationService.LOGGER.debug("Request failed", ex);
			throw ex;
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}
	
	public String performGetRequest(URL url) throws IOException {
		Validate.notNull(url);
		HttpURLConnection connection = this.createUrlConnection(url);
		HttpAuthenticationService.LOGGER.debug("Reading data from " + url);
		InputStream inputStream = null;
		try {
			inputStream = connection.getInputStream();
			String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
			HttpAuthenticationService.LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
			HttpAuthenticationService.LOGGER.debug("Response: " + result);
			return result;
		} catch (IOException ex) {
			IOUtils.closeQuietly(inputStream);
			inputStream = connection.getErrorStream();
			if (inputStream != null) {
				HttpAuthenticationService.LOGGER.debug("Reading error page from " + url);
				String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
				HttpAuthenticationService.LOGGER.debug("Successful read, server response was " + connection.getResponseCode());
				HttpAuthenticationService.LOGGER.debug("Response: " + result);
				return result;
			}
			HttpAuthenticationService.LOGGER.debug("Request failed", ex);
			throw ex;
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}
	
	public static URL constantURL(String url) {
		try {
			return new URL(url);
		} catch (MalformedURLException ex) {
			throw new Error("Couldn't create constant for " + url, ex);
		}
	}
	
	public static String buildQuery(Map<String, Object> query) {
		if (query == null) {
			return "";
		}
		StringBuilder stringBuilder = new StringBuilder();
		for (Map.Entry<String, Object> entry : query.entrySet()) {
			if (stringBuilder.length() > 0) {
				stringBuilder.append("&");
			}
			try {
				stringBuilder.append(URLEncoder.encode((String) entry.getKey(), "UTF-8"));
			} catch (UnsupportedEncodingException ex) {
				HttpAuthenticationService.LOGGER.error("Unexpected exception building query", ex);
			}
			if (entry.getValue() != null) {
				stringBuilder.append("=");
				try {
					stringBuilder.append(URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
				} catch (UnsupportedEncodingException ex) {
					HttpAuthenticationService.LOGGER.error("Unexpected exception building query", ex);
				}
			}
		}
		return stringBuilder.toString();
	}
	
	public static URL concatenateURL(URL url, String query) {
		try {
			if (url.getQuery() != null && url.getQuery().length() > 0) {
				return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "&" + query);
			}
			return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "?" + query);
		} catch (MalformedURLException ex) {
			throw new IllegalArgumentException("Could not concatenate given URL with GET arguments!", ex);
		}
	}
}