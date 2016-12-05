package net.wouto.proxy.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.InsecureTextureException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.authlib.yggdrasil.response.Response;
import com.mojang.util.UUIDTypeAdapter;
import net.wouto.proxy.MojangProxyPlugin;
import net.wouto.proxy.util.Value;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ProxyMinecraftSessionService extends YggdrasilMinecraftSessionService {

    private static final Logger LOGGER = LogManager.getLogger();
    private Value<String> BASE_URL = new Value<>("https://sessionserver.mojang.com/session/minecraft/");
    private final PublicKey publicKey;
    private final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
    private final LoadingCache<GameProfile, GameProfile> insecureProfiles;
    private ProxyAuthenticationService authenticationService;

    public ProxyMinecraftSessionService(ProxyAuthenticationService authenticationService) {
        super(null);
        this.authenticationService = authenticationService;
        this.insecureProfiles = CacheBuilder.newBuilder()
                .expireAfterWrite(6L, TimeUnit.HOURS)
                .build(new CacheLoader<GameProfile, GameProfile>() {
                    public GameProfile load(GameProfile gameProfile) throws Exception {
                        return ProxyMinecraftSessionService.this.fillGameProfile(gameProfile, false);
                    }
                });
        try {
            X509EncodedKeySpec localX509EncodedKeySpec = new X509EncodedKeySpec(IOUtils.toByteArray(YggdrasilMinecraftSessionService.class.getResourceAsStream("/yggdrasil_session_pubkey.der")));
            KeyFactory localKeyFactory = KeyFactory.getInstance("RSA");
            this.publicKey = localKeyFactory.generatePublic(localX509EncodedKeySpec);
        } catch (Exception localException) {
            throw new Error("Missing/invalid yggdrasil public key!");
        }
    }

    public Value<String> getBaseUrl() {
        return BASE_URL;
    }

    private URL url(String url) {
        return HttpAuthenticationService.constantURL(url);
    }

    @Override
    public void joinServer(GameProfile gameProfile, String accessToken, String serverId) throws AuthenticationException {
        JoinMinecraftServerRequest localJoinMinecraftServerRequest = new JoinMinecraftServerRequest();
        localJoinMinecraftServerRequest.accessToken = accessToken;
        localJoinMinecraftServerRequest.selectedProfile = gameProfile.getId();
        localJoinMinecraftServerRequest.serverId = serverId;

        getAuthenticationService().makeRequest(url(BASE_URL.get() + "join"), localJoinMinecraftServerRequest, Response.class);
    }

    // sometimes @Override's
    public GameProfile hasJoinedServer(GameProfile gameProfile, String serverId, InetAddress inetAddress) throws AuthenticationUnavailableException {
        GameProfile result = null;
        HashMap<String, Object> localHashMap = new HashMap<>();

        localHashMap.put("username", gameProfile.getName());
        localHashMap.put("serverId", serverId);
        MojangProxyPlugin.get().getProxyUtil().appendProxyKeyQuery(localHashMap);
        URL localURL = HttpAuthenticationService.concatenateURL(url(BASE_URL.get() + "hasJoined"), HttpAuthenticationService.buildQuery(localHashMap));
        try {
            HasJoinedMinecraftServerResponse localHasJoinedMinecraftServerResponse = getAuthenticationService().makeRequest(localURL, null, HasJoinedMinecraftServerResponse.class);
            if ((localHasJoinedMinecraftServerResponse != null) && (localHasJoinedMinecraftServerResponse.getId() != null)) {
                GameProfile localGameProfile = new GameProfile(localHasJoinedMinecraftServerResponse.getId(), gameProfile.getName());
                if (localHasJoinedMinecraftServerResponse.getProperties() != null) {
                    localGameProfile.getProperties().putAll(localHasJoinedMinecraftServerResponse.getProperties());
                }
                result = localGameProfile;
            }
        } catch (AuthenticationException ignored) {
        }
        if (result == null) {
            MojangProxyPlugin.logger().warning("Unable to fetch profile from Proxy, fetching from Mojang");
            try {
                result = MojangProxyPlugin.get().getProxyUtil().getOriginalSessionService().invokeMethod("hasJoinedServer", null, gameProfile, serverId, inetAddress);
            } catch (Exception e) {
                try {
                    result = MojangProxyPlugin.get().getProxyUtil().getOriginalSessionService().invokeMethod("hasJoinedServer", null, gameProfile, serverId);
                } catch (Exception e2) {
                }
            }
        }
        return result;
    }

    // sometimes @Override's
    public GameProfile hasJoinedServer(GameProfile gameProfile, String serverId) throws AuthenticationUnavailableException {
        return this.hasJoinedServer(gameProfile, serverId, null);
    }

    @Override
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile gameProfile, boolean requireSigned) {
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = null;
        Property localProperty = Iterables.getFirst(gameProfile.getProperties().get("textures"), null);
        if (localProperty == null) {
            textures = new HashMap<>();
        } else {
            try {
                if (requireSigned) {
                    if (!localProperty.hasSignature()) {
                        LOGGER.error("Signature is missing from textures payload");
                        throw new InsecureTextureException("Signature is missing from textures payload");
                    }
                    if (!localProperty.isSignatureValid(this.publicKey)) {
                        LOGGER.error("Textures payload has been tampered with (signature invalid)");
                        throw new InsecureTextureException("Textures payload has been tampered with (signature invalid)");
                    }
                }
                MinecraftTexturesPayload localMinecraftTexturesPayload = null;
                try {
                    String str = new String(Base64.decodeBase64(localProperty.getValue()), Charsets.UTF_8);
                    localMinecraftTexturesPayload = this.gson.fromJson(str, MinecraftTexturesPayload.class);
                } catch (JsonParseException localJsonParseException) {
                    LOGGER.error("Could not decode textures payload", localJsonParseException);
                    textures = new HashMap<>();
                }
                if (localMinecraftTexturesPayload != null) {
                    if (localMinecraftTexturesPayload.getTextures() == null) {
                        textures = new HashMap<>();
                    }
                    textures = localMinecraftTexturesPayload.getTextures();
                }
            } catch (Exception ignored) {
            }
        }
        if (textures == null) {
            MojangProxyPlugin.logger().warning("Unable to fetch textures from Proxy, fetching from Mojang");
            return MojangProxyPlugin.get().getProxyUtil().getOriginalSessionService().invokeMethod("getTextures", null, gameProfile, requireSigned);
        }
        return textures;
    }

    @Override
    public GameProfile fillProfileProperties(GameProfile gameProfile, boolean forceChecked) {
        GameProfile result = null;
        if (gameProfile.getId() == null) {
            return gameProfile;
        }
        if (!forceChecked) {
            return this.insecureProfiles.getUnchecked(gameProfile);
        }
        result = fillGameProfile(gameProfile, true);
        if (result.getProperties().isEmpty()) {
            result = MojangProxyPlugin.get().getProxyUtil().getOriginalSessionService().invokeMethod("fillProfileProperties", null, gameProfile, forceChecked);
        }
        return result;
    }

    @Override
    public GameProfile fillGameProfile(GameProfile gameProfile, boolean signed) {
        GameProfile result = null;
        try {
            URL localURL = HttpAuthenticationService.constantURL(BASE_URL.get() + "profile/" + UUIDTypeAdapter.fromUUID(gameProfile.getId()));
            localURL = HttpAuthenticationService.concatenateURL(localURL, "unsigned=" + (!signed) + MojangProxyPlugin.get().getProxyUtil().getProxyKeyQuery(true));
            MinecraftProfilePropertiesResponse localMinecraftProfilePropertiesResponse = getAuthenticationService().makeRequest(localURL, null, MinecraftProfilePropertiesResponse.class);
            if (localMinecraftProfilePropertiesResponse == null) {
                LOGGER.debug("Couldn't fetch profile properties for " + gameProfile + " as the profile does not exist");
                result = gameProfile;
            }
            GameProfile localGameProfile = new GameProfile(localMinecraftProfilePropertiesResponse.getId(), localMinecraftProfilePropertiesResponse.getName());
            localGameProfile.getProperties().putAll(localMinecraftProfilePropertiesResponse.getProperties());
            gameProfile.getProperties().putAll(localMinecraftProfilePropertiesResponse.getProperties());
            LOGGER.debug("Successfully fetched profile properties for " + gameProfile);
            result = localGameProfile;
        } catch (AuthenticationException localAuthenticationException) {
            LOGGER.warn("Couldn't look up profile properties for " + gameProfile, localAuthenticationException);
        }
        if (result == null) {
            MojangProxyPlugin.logger().warning("Unable to fill a gameprofile from Proxy, fetching from Mojang");
            result = MojangProxyPlugin.get().getProxyUtil().getOriginalSessionService().invokeMethod("fillGameProfile", null, gameProfile, signed);
        }
        return result;
    }

    @Override
    public ProxyAuthenticationService getAuthenticationService() {
        return authenticationService;
    }

}
