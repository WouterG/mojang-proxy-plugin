package net.wouto.proxy.util;

import net.wouto.proxy.MojangProxyPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

public class Mods {

	private static Collection<Mod> mods = new ArrayList<>();

	public static void apply(MojangProxyPlugin plugin) {
		if (mods.isEmpty()) {
			return;
		}
		MojangProxyPlugin.logger().log(Level.INFO, "Applying " + mods.size() + " mods");
		for (Mod mod : mods) {
			try {
				mod.apply(plugin);
			} catch (Exception e) {
				MojangProxyPlugin.logger().log(Level.WARNING, "Failed to install mod", e);
			}
		}
	}

	static {
//
//      This mod installs a new factory in Java's own URL class to see all outgoing http/https requests - not needed for now
//
//		mods.add(plugin -> {
//			Field f = URL.class.getDeclaredField("factory");
//			f.setAccessible(true);
//			URLStreamHandlerFactory baseFactory = (URLStreamHandlerFactory) f.get(null);
//			f.set(null, null);
//			URL.setURLStreamHandlerFactory(null);
//			URL.setURLStreamHandlerFactory(new ProxyURLStreamHandlerFactory(baseFactory));
//		});
	}

	private interface Mod {
		void apply(MojangProxyPlugin plugin) throws Exception;
	}

}
