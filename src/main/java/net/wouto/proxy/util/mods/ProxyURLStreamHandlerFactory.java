package net.wouto.proxy.util.mods;

import net.wouto.proxy.MojangProxyPlugin;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class ProxyURLStreamHandlerFactory implements URLStreamHandlerFactory {

	private URLStreamHandlerFactory base;

	public ProxyURLStreamHandlerFactory(URLStreamHandlerFactory base) {
		this.base = base;
	}

	@Override
	public URLStreamHandler createURLStreamHandler(String protocol) {
		if (protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("https")) {
			return new ProxyURLHandler(protocol);
		}
		MojangProxyPlugin.logger().info("Not logging request with protocol: " + protocol);
		return null;
	}

}
