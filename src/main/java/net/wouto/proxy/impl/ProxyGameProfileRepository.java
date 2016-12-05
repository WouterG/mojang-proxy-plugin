package net.wouto.proxy.impl;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilGameProfileRepository;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import net.wouto.proxy.MojangProxyPlugin;
import net.wouto.proxy.util.Value;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Set;

public class ProxyGameProfileRepository extends YggdrasilGameProfileRepository {

	private static final Logger LOGGER = LogManager.getLogger();
	private Value<String> SEARCH_PAGE_URL = new Value<>("https://api.mojang.com/profiles/");
	private ProxyAuthenticationService authenticationService;

	public ProxyGameProfileRepository(ProxyAuthenticationService authenticationService) {
		super(authenticationService);
		this.authenticationService = authenticationService;
	}

	public Value<String> getSearchPageUrl() {
		return this.SEARCH_PAGE_URL;
	}

	@Override
	public void findProfilesByNames(final String[] names, final Agent agent, final ProfileLookupCallback callback) {
		final Set<String> criteria = Sets.newHashSet();
		for (final String name : names) {
			if (!Strings.isNullOrEmpty(name)) {
				criteria.add(name.toLowerCase());
			}
		}
		final int page = 0;
		for (final List<String> request : Iterables.partition(criteria, 100)) {
			int failCount = 0;
			boolean failed;
			do {
				failed = false;
				try {
					final ProfileSearchResultsResponse response = this.authenticationService.makeRequest(HttpAuthenticationService.constantURL(SEARCH_PAGE_URL.get() + agent.getName().toLowerCase() + MojangProxyPlugin.get().getProxyUtil().getProxyKeyQuery(false)), request, ProfileSearchResultsResponse.class);
					failCount = 0;
					int length = 0;
					if (response != null && response.getProfiles() != null) {
						length = response.getProfiles().length;
					}
					LOGGER.debug("Page {} returned {} results, parsing", page, length);
					final Set<String> missing = Sets.newHashSet(request);
					if (response == null) {
						throw new AuthenticationException("Response was null");
					}
                    if (response.getProfiles() != null) {
                        for (final GameProfile profile : response.getProfiles()) {
                            LOGGER.debug("Successfully looked up profile {}", profile);
                            missing.remove(profile.getName().toLowerCase());
                            callback.onProfileLookupSucceeded(profile);
                        }
                    }
					for (final String name2 : missing) {
						LOGGER.debug("Couldn't find profile {}", name2);
						MojangProxyPlugin.logger().warning("Unable to find profiles from Proxy, fetching from Mojang");
						MojangProxyPlugin.get().getProxyUtil().getOriginalGameProfileRepository().invokeMethod("findProfilesByNames", null, new String[] { name2 }, Agent.MINECRAFT, callback);
					}
					try {
						Thread.sleep(100L);
					}
					catch (InterruptedException ignored) {}
				}
				catch (AuthenticationException e) {
					if (++failCount == 3) {
						for (final String name3 : request) {
							LOGGER.debug("Couldn't find profile {} because of a server error", name3);
							MojangProxyPlugin.logger().warning("Unable to find profiles from Proxy, fetching from Mojang");
							MojangProxyPlugin.get().getProxyUtil().getOriginalGameProfileRepository().invokeMethod("findProfilesByNames", null, new String[] { name3 }, Agent.MINECRAFT, callback);
						}
					}
					else {
						try {
							Thread.sleep(750L);
						}
						catch (InterruptedException ignored) {}
						failed = true;
					}
				}
			} while (failed);
		}
	}
}
