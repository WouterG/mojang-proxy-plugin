package net.wouto.proxy;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.wouto.proxy.impl.ProxyAuthenticationService;
import net.wouto.proxy.impl.ProxyGameProfileRepository;
import net.wouto.proxy.impl.ProxyMinecraftSessionService;
import net.wouto.proxy.util.ObjProxy;
import net.wouto.proxy.util.ReflectionUtil;
import net.wouto.proxy.util.Value;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;

public class ProxyUtil {

	ObjProxy originalAuthenticationService;
	ObjProxy originalGameProfileRepository;
	ObjProxy originalSessionService;

	private ProxyAuthenticationService authenticationService;
	private ProxyGameProfileRepository gameProfileRepository;
	private ProxyMinecraftSessionService sessionService;

	private Class craftServerClass;
	private Class minecraftServerClass;
	private Class userCacheClass;

	private Object craftServer;
	private Object minecraftServer;
	private Object userCache;

	private Value<String> gameProfilesUrl;
	private Value<String> sessionServiceUrl;

	private Value<String> proxyKey;

	private FileConfiguration config;

	public ProxyUtil(FileConfiguration config) {
		this.config = config;

		this.proxyKey = new Value<>(null);
		this.craftServer = Bukkit.getServer();
		this.craftServerClass = this.craftServer.getClass();
		try {
			Method m = ReflectionUtil.findMethod(this.craftServerClass, "getServer");
			this.minecraftServer = m.invoke(this.craftServer);
			this.minecraftServerClass = this.minecraftServer.getClass();
			m = ReflectionUtil.findMethod(this.minecraftServerClass, "getUserCache");
			this.userCache = m.invoke(this.minecraftServer);
			this.userCacheClass = this.userCache.getClass();
		} catch (Exception e) {
			MojangProxyPlugin.logger().log(Level.SEVERE, "Unable to initialize ProxyUtil", e);
		}
	}

	public ObjProxy getOriginalAuthenticationService() {
		return originalAuthenticationService;
	}

	public ObjProxy getOriginalGameProfileRepository() {
		return originalGameProfileRepository;
	}

	public ObjProxy getOriginalSessionService() {
		return originalSessionService;
	}

	public void install() throws Exception {
		this.loadConfig();
		this.installProxy();
		this.applyConfig();
		this.validateInstallation();
	}

	private void validateInstallation() throws Exception {
		Field authenticationServiceField = ReflectionUtil.findField(this.minecraftServerClass, YggdrasilAuthenticationService.class);
		Field sessionServiceField = ReflectionUtil.findField(this.minecraftServerClass, MinecraftSessionService.class);
		Field gameProfileRepositoryField = ReflectionUtil.findField(this.minecraftServerClass, GameProfileRepository.class);

		if (ReflectionUtil.getValue(authenticationServiceField, this.minecraftServer).getClass() != ProxyAuthenticationService.class) {
			throw new Exception("Failed to install " + ProxyAuthenticationService.class.getName());
		}
		if (ReflectionUtil.getValue(sessionServiceField, this.minecraftServer).getClass() != ProxyMinecraftSessionService.class) {
			throw new Exception("Failed to install " + ProxyMinecraftSessionService.class.getName());
		}
		if (ReflectionUtil.getValue(gameProfileRepositoryField, this.minecraftServer).getClass() != ProxyGameProfileRepository.class) {
			throw new Exception("Failed to install " + ProxyGameProfileRepository.class.getName());
		}
	}

	private void loadConfig() {
		this.config.addDefault("gameprofiles", "https://api.mojang.com/profiles/");
		this.config.addDefault("sessionservice", "https://sessionserver.mojang.com/session/minecraft/");
		this.config.addDefault("proxyKey", null);
	}

	private void applyConfig() {
		String gameProfilesUrlReplacement = this.config.getString("gameprofiles");
		if (gameProfilesUrlReplacement != null && !gameProfilesUrlReplacement.equals(this.gameProfilesUrl.getOriginal())) {
			if (isUrlValid(gameProfilesUrlReplacement)) {
				this.gameProfilesUrl.setReplacement(gameProfilesUrlReplacement);
				MojangProxyPlugin.logger().info("Changed the gameprofiles url to: " + this.gameProfilesUrl.get());
			} else {
				MojangProxyPlugin.logger().severe("Invalid gameprofiles url: " + gameProfilesUrlReplacement);
			}
		}

		String sessionServiceUrlReplacement = this.config.getString("sessionservice");
		if (sessionServiceUrlReplacement != null && !sessionServiceUrlReplacement.equals(this.sessionServiceUrl.getOriginal())) {
			if (isUrlValid(sessionServiceUrlReplacement)) {
				this.sessionServiceUrl.setReplacement(sessionServiceUrlReplacement);
				MojangProxyPlugin.logger().info("Changed the sessionserver url to: " + this.sessionServiceUrl.get());
			} else {
				MojangProxyPlugin.logger().severe("Invalid sessionservice url: " + sessionServiceUrlReplacement);
			}
		}

		String proxykey = this.config.getString("proxyKey", null);
		if (proxykey != null && !proxykey.isEmpty()) {
			this.proxyKey.setReplacement(proxykey);
		}
	}

	private void installProxy() throws Exception {
		// to start we need to initialize the AuthenticationService as it's used in the GameProfileRepository and
		// the MinecraftSessionService

		// first we need to get the client-token from the original AuthenticationService
		Field authenticationServiceField = ReflectionUtil.findField(this.minecraftServerClass, YggdrasilAuthenticationService.class);
		YggdrasilAuthenticationService authenticationService = ReflectionUtil.getValue(authenticationServiceField, this.minecraftServer);
		this.originalAuthenticationService = new ObjProxy(authenticationService);
		String authenticationServiceClientToken = authenticationService.getClientToken();
		// now we create a new proxied authentication service
		this.authenticationService = new ProxyAuthenticationService(Proxy.NO_PROXY, authenticationServiceClientToken);
		// and with that the others
		this.gameProfileRepository = new ProxyGameProfileRepository(this.authenticationService);
		this.sessionService = new ProxyMinecraftSessionService(this.authenticationService);

		// now we install the services
		Field sessionServiceField = ReflectionUtil.findField(this.minecraftServerClass, MinecraftSessionService.class);
		Field gameProfileRepositoryField = ReflectionUtil.findField(this.minecraftServerClass, GameProfileRepository.class);

		this.originalSessionService = new ObjProxy(ReflectionUtil.getValue(sessionServiceField, this.minecraftServer));
		this.originalGameProfileRepository = new ObjProxy(ReflectionUtil.getValue(gameProfileRepositoryField, this.minecraftServer));

		MojangProxyPlugin.logger().info("patching AuthenticationService with " + this.authenticationService.getClass().getName());
		ReflectionUtil.setPrivateFinal(authenticationServiceField, this.minecraftServer, this.authenticationService);
		MojangProxyPlugin.logger().info("patching SessionService with " + this.sessionService.getClass().getName());
		ReflectionUtil.setPrivateFinal(sessionServiceField, this.minecraftServer, this.sessionService);
		MojangProxyPlugin.logger().info("patching GameProfileRepository with " + this.gameProfileRepository.getClass().getName());
		ReflectionUtil.setPrivateFinal(gameProfileRepositoryField, this.minecraftServer, this.gameProfileRepository);

		// Now we also need to update the UserCache as it also has a reference to the GameProfileRepository
		// Note that this won't work < 1.9
		try {
			Field userCacheGameProfileRepositoryField = ReflectionUtil.findField(this.userCacheClass, GameProfileRepository.class);
			ReflectionUtil.setPrivateFinal(userCacheGameProfileRepositoryField, this.userCache, this.gameProfileRepository);
		} catch (Exception e) {
			MojangProxyPlugin.logger().log(Level.INFO, "Using <= 1.8 spigot version(?) - UserCache has not been patched.");
		}

		this.gameProfilesUrl = this.gameProfileRepository.getSearchPageUrl();
		this.sessionServiceUrl = this.sessionService.getBaseUrl();
	}

	public String getProxyKeyQuery(boolean appendQuery) {
		if (this.proxyKey.get() == null) {
			return "";
		}
		return (appendQuery ? "&" : "?") + "proxyKey=" + this.proxyKey.get();
	}

	public void appendProxyKeyQuery(Map<String, Object> map) {
		if (this.proxyKey.get() != null) {
			map.put("proxyKey", this.proxyKey.get());
		}
	}

	private boolean isUrlValid(String url) {
		try {
			new URL(url);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
