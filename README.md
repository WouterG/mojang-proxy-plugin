## Mojang Proxy Plugin

Configure the Mojang Proxy plugin to point to a [mojang-proxy-server](https://github.com/WouterG/mojang-proxy-server). If your server is running on api.example.com you can configure your `./plugins/MojangProxyPlugin/config.yml` like so:

```yml
# default https://api.mojang.com/profiles/
gameprofiles: http://api.example.com/profiles/
# default https://sessionserver.mojang.com/session/minecraft/
sessionservice: http://api.example.com/session/minecraft/
# default <empty>
proxyKey:
```

The proxyKey can be used to configure a private proxy setup. Just match the key with the server's proxyKey.

You can also decide to route one type of requests through Mojang's API and the other through the proxy, although this prevents the proxy from being able to cache items.
