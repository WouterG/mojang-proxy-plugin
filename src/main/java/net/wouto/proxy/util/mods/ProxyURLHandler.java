package net.wouto.proxy.util.mods;

import net.wouto.proxy.MojangProxyPlugin;
import net.wouto.proxy.util.DebugUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class ProxyURLHandler extends URLStreamHandler {
	private final String protocol;
	private final URLStreamHandler handler;
	private final Method openCon;
	private final Method openConProxy;

	public ProxyURLHandler(String protocol) {
		this.protocol = protocol;
		if (protocol.equals("http")) {
			handler = new sun.net.www.protocol.http.Handler();
		} else {
			handler = new sun.net.www.protocol.https.Handler();
		}
		try {
			openCon = handler.getClass().getDeclaredMethod("openConnection", URL.class);
			openCon.setAccessible(true);
			openConProxy = handler.getClass().getDeclaredMethod("openConnection", URL.class, Proxy.class);
			openConProxy.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected URLConnection openConnection(URL u) throws IOException {
		MojangProxyPlugin.logger().info("URL.openConnection(url): " + u.toString());
		DebugUtil.printStackTrace();
		return getDefaultConnection(u);
	}

	@Override
	protected URLConnection openConnection(URL u, Proxy p) throws IOException {
		MojangProxyPlugin.logger().info("URL.openConnection(url, proxy): " + u.toString());
		DebugUtil.printStackTrace();
		return getDefaultConnection(u, p);
	}

	public URLConnection getDefaultConnection(URL u) {
		try {
			return (URLConnection) openCon.invoke(handler, u);
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	public URLConnection getDefaultConnection(URL u, Proxy p) {
		try {
			return (URLConnection) openConProxy.invoke(handler, u, p);
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
}
