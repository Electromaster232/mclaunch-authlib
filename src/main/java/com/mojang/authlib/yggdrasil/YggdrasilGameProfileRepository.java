package com.mojang.authlib.yggdrasil;

import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;

public class YggdrasilGameProfileRepository implements GameProfileRepository {
	
	private static final Logger LOGGER;
	private static final String BASE_URL;
	private static final String SEARCH_PAGE_URL;
	private static final int ENTRIES_PER_PAGE;
	private static final int MAX_FAIL_COUNT;
	private static final int DELAY_BETWEEN_PAGES;
	private static final int DELAY_BETWEEN_FAILURES;
	private final YggdrasilAuthenticationService authenticationService;
	
	static {
		LOGGER = LogManager.getLogger();
		BASE_URL = "https://api.mcnet.djelectro.me/";
		SEARCH_PAGE_URL = BASE_URL + "profiles/";
		ENTRIES_PER_PAGE = 2;
		MAX_FAIL_COUNT = 3;
		DELAY_BETWEEN_PAGES = 100;
		DELAY_BETWEEN_FAILURES = 750;
	}
	
	public YggdrasilGameProfileRepository(YggdrasilAuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}
	
	@Override
	public void findProfilesByNames(String[] names, Agent agent, ProfileLookupCallback callback) {
		HashSet<String> criteria = Sets.newHashSet();
		for (String name : names) {
			if (!Strings.isNullOrEmpty(name)) {
				criteria.add(name.toLowerCase());
			}
		}
		int page = 0;
		for (List<String> request : Iterables.partition(criteria, YggdrasilGameProfileRepository.ENTRIES_PER_PAGE)) {
			boolean failed;
			int failCount = 0;
			do {
				failed = false;
				try {
					ProfileSearchResultsResponse response = this.authenticationService.makeRequest(HttpAuthenticationService.constantURL(YggdrasilGameProfileRepository.SEARCH_PAGE_URL + agent.getName().toLowerCase()), request, ProfileSearchResultsResponse.class);
					failCount = 0;
					YggdrasilGameProfileRepository.LOGGER.debug("Page {} returned {} results, parsing", page, response.getProfiles().length);
					HashSet<String> missing = Sets.newHashSet(request);
					for (GameProfile profile : response.getProfiles()) {
						YggdrasilGameProfileRepository.LOGGER.debug("Successfully looked up profile {}", profile);
						missing.remove(profile.getName().toLowerCase());
						callback.onProfileLookupSucceeded(profile);
					}
					for (String name : missing) {
						YggdrasilGameProfileRepository.LOGGER.debug("Couldn't find profile {}", name);
						callback.onProfileLookupFailed(new GameProfile(null, name), new ProfileNotFoundException("Server did not find the requested profile"));
					} try {
						Thread.sleep(YggdrasilGameProfileRepository.DELAY_BETWEEN_PAGES);
					} catch (InterruptedException ignored) {
					}
				} catch (AuthenticationException ex) {
					if (++failCount >= YggdrasilGameProfileRepository.MAX_FAIL_COUNT) {
						for (String name : request) {
							YggdrasilGameProfileRepository.LOGGER.debug("Couldn't find profile {} beacause of a server error", name);
							callback.onProfileLookupFailed(new GameProfile(null, name), ex);
						}
					} else {
						try {
							Thread.sleep(YggdrasilGameProfileRepository.DELAY_BETWEEN_FAILURES);
						} catch (InterruptedException ignored) {
						}
						failed = true;
					}
				}
			} while (failed);
		}
	}
}