package net.wouto.proxy;

import net.wouto.proxy.util.Mods;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MojangProxyPlugin extends JavaPlugin {

	public static final Level LOG_LEVEL = Level.ALL;

	private static MojangProxyPlugin instance;

	private Logger LOG;
	private FileConfiguration config = getConfig();
	private ProxyUtil proxyUtil;

	@Override
	public void onEnable() {
		MojangProxyPlugin.instance = this;
		this.LOG = getLogger();
		LOG.setLevel(LOG_LEVEL);
		this.proxyUtil = new ProxyUtil(this.config);
		try {
			this.proxyUtil.install();
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Plugin initialization failed - A restart may be needed to prevent unwanted behaviour", e);
		}

		this.config.options().copyDefaults(true);
		saveConfig();
		Mods.apply(this);
	}

	public ProxyUtil getProxyUtil() {
		return proxyUtil;
	}

	public static MojangProxyPlugin get() {
		return MojangProxyPlugin.instance;
	}

	public static Logger logger() {
		return instance.LOG;
	}

}
