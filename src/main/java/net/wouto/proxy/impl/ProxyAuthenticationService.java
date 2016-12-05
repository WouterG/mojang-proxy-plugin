package net.wouto.proxy.impl;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.response.Response;

import java.net.Proxy;
import java.net.URL;

public class ProxyAuthenticationService extends YggdrasilAuthenticationService {

	public ProxyAuthenticationService(Proxy proxy, String clientToken) {
		super(proxy, clientToken);
	}

	@Override
	public <T extends Response> T makeRequest(URL url, Object o, Class<T> aClass) throws AuthenticationException {
		return super.makeRequest(url, o, aClass);
	}
}
