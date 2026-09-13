package com.mojang.authlib.yggdrasil;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.Environment;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.authlib.yggdrasil.response.ProfileAction;
import com.mojang.util.UUIDTypeAdapter;
import com.mojang.util.UndashedUuid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class YggdrasilMinecraftSessionService implements MinecraftSessionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(YggdrasilMinecraftSessionService.class);
    private final MinecraftClient client;
    private final ServicesKeySet servicesKeySet;
    private final String baseUrl;
    private final URL joinUrl;
    private final URL checkUrl;

    private final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
    private final LoadingCache<UUID, Optional<ProfileResult>> insecureProfiles = CacheBuilder
        .newBuilder()
        .expireAfterWrite(6, TimeUnit.HOURS)
        .build(new CacheLoader<>() {
            @Override
            public Optional<ProfileResult> load(final UUID key) {
                return Optional.ofNullable(fetchProfileUncached(key, false));
            }
        });

    protected YggdrasilMinecraftSessionService(final ServicesKeySet servicesKeySet, final Proxy proxy, final Environment env) {
        client = MinecraftClient.unauthenticated(proxy);
        this.servicesKeySet = servicesKeySet;
        baseUrl = env.sessionHost() + "/session/minecraft/";

        joinUrl = HttpAuthenticationService.constantURL(baseUrl + "join");
        checkUrl = HttpAuthenticationService.constantURL(baseUrl + "hasJoined");
    }

    @Override
    public void joinServer(final UUID profileId, final String authenticationToken, final String serverId) throws AuthenticationException {
        final JoinMinecraftServerRequest request = new JoinMinecraftServerRequest(authenticationToken, profileId, serverId);
        try {
            client.post(joinUrl, request, Void.class);
        } catch (final MinecraftClientException e) {
            throw e.toAuthenticationException();
        }
    }

    @Override
    @Nullable
    public ProfileResult hasJoinedServer(final String profileName, final String serverId, @Nullable final InetAddress address) throws AuthenticationUnavailableException {
        final Map<String, Object> arguments = new HashMap<>();

        arguments.put("username", profileName);
        arguments.put("serverId", serverId);

        if (address != null) {
            arguments.put("ip", address.getHostAddress());
        }

        final URL url = HttpAuthenticationService.concatenateURL(checkUrl, HttpAuthenticationService.buildQuery(arguments));

        try {
            final HasJoinedMinecraftServerResponse response = client.get(url, HasJoinedMinecraftServerResponse.class);
            if (response != null && response.id() != null) {
                final GameProfile result = new GameProfile(response.id(), profileName, Objects.requireNonNullElse(response.properties(), PropertyMap.EMPTY));

                final Set<ProfileActionType> profileActions = extractProfileActionTypes(response.profileActions());
                return new ProfileResult(result, profileActions);
            } else {
                return null;
            }
        } catch (final MinecraftClientException e) {
            if (e.toAuthenticationException() instanceof final AuthenticationUnavailableException unavailable) {
                throw unavailable;
            }
            return null;
        }
    }

    @Nullable
    @Override
    public Property getPackedTextures(final GameProfile profile) {
        return Iterables.getFirst(profile.properties().get("textures"), null);
    }

    @Override
    public MinecraftProfileTextures unpackTextures(final Property packedTextures) {
        final String value = packedTextures.value();
        final SignatureState signatureState = getPropertySignatureState(packedTextures);

        final MinecraftTexturesPayload result;
        try {
            final String json = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            result = gson.fromJson(json, MinecraftTexturesPayload.class);
        } catch (final JsonParseException | IllegalArgumentException e) {
            LOGGER.error("Could not decode textures payload", e);
            return MinecraftProfileTextures.EMPTY;
        }

        if (result == null || result.textures() == null || result.textures().isEmpty()) {
            return MinecraftProfileTextures.EMPTY;
        }

        final Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = result.textures();
        for (final Map.Entry<MinecraftProfileTexture.Type, MinecraftProfileTexture> entry : textures.entrySet()) {
            final String url = entry.getValue().getUrl();
            if (url == null || !TextureUrlChecker.isAllowedTextureDomain(url)) {
                LOGGER.error("Textures payload url is invalid: {}", url);
                return MinecraftProfileTextures.EMPTY;
            }
        }

        return new MinecraftProfileTextures(
            textures.get(MinecraftProfileTexture.Type.SKIN),
            textures.get(MinecraftProfileTexture.Type.CAPE),
            textures.get(MinecraftProfileTexture.Type.ELYTRA),
            signatureState
        );
    }

    @Nullable
    @Override
    public ProfileResult fetchProfile(final UUID profileId, final boolean requireSecure) {
        if (!requireSecure) {
            return insecureProfiles.getUnchecked(profileId).orElse(null);
        }

        return fetchProfileUncached(profileId, true);
    }

    @Override
    public String getSecurePropertyValue(final Property property) throws InsecurePublicKeyException {
        return switch (getPropertySignatureState(property)) {
            case UNSIGNED ->
                throw new InsecurePublicKeyException.MissingException("Missing signature from \"" + property.name() + "\"");
            case INVALID ->
                throw new InsecurePublicKeyException.InvalidException("Property \"" + property.name() + "\" has been tampered with (signature invalid)");
            case SIGNED -> property.value();
        };
    }

    private SignatureState getPropertySignatureState(final Property property) {
        if (!property.hasSignature()) {
            return SignatureState.UNSIGNED;
        }
        if (servicesKeySet.keys(ServicesKeyType.PROFILE_PROPERTY).stream().noneMatch(key -> key.validateProperty(property))) {
            return SignatureState.INVALID;
        }
        return SignatureState.SIGNED;
    }

    @Nullable
    private ProfileResult fetchProfileUncached(final UUID profileId, final boolean requireSecure) {
        try {
            URL url = HttpAuthenticationService.constantURL(baseUrl + "profile/" + UndashedUuid.toString(profileId));
            url = HttpAuthenticationService.concatenateURL(url, "unsigned=" + !requireSecure);

            final MinecraftProfilePropertiesResponse response = client.get(url, MinecraftProfilePropertiesResponse.class);
            if (response == null) {
                LOGGER.debug("Couldn't fetch profile properties for {} as the profile does not exist", profileId);
                return null;
            }

            final GameProfile profile = response.profile();
            final Set<ProfileActionType> profileActions = extractProfileActionTypes(response.profileActions());

            LOGGER.debug("Successfully fetched profile properties for {}", profile);
            return new ProfileResult(profile, profileActions);
        } catch (final MinecraftClientException | IllegalArgumentException e) {
            LOGGER.warn("Couldn't look up profile properties for {}", profileId, e);
            return null;
        }
    }

    private static Set<ProfileActionType> extractProfileActionTypes(final Set<ProfileAction> response) {
        return response.stream()
            .map(ProfileAction::type)
            .collect(Collectors.toSet());
    }
}
