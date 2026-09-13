package com.mojang.authlib.yggdrasil;

import com.mojang.authlib.Environment;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientException.ErrorType;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.minecraft.client.MinecraftClient.ServiceResponse;
import com.mojang.authlib.yggdrasil.request.FriendActionRequest;
import com.mojang.authlib.yggdrasil.request.JoinInfoUpdate;
import com.mojang.authlib.yggdrasil.request.PresenceRequest;
import com.mojang.authlib.yggdrasil.request.UpdateType;
import com.mojang.authlib.yggdrasil.request.UserAttributesRequest;
import com.mojang.authlib.yggdrasil.response.FriendData;
import com.mojang.authlib.yggdrasil.response.FriendsListResponse;
import com.mojang.authlib.yggdrasil.response.PresenceResponse;
import com.mojang.authlib.yggdrasil.response.PresenceStatus;
import com.mojang.authlib.yggdrasil.response.UserAttributesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jspecify.annotations.Nullable;
import java.net.Proxy;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class YggdrasilFriendsService implements FriendsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(YggdrasilFriendsService.class);
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final long REQUEST_COOLDOWN_SECONDS = 10;

    private final URL routeFriends;
    private final URL routePrivileges;
    private final URL routePresence;

    private final MinecraftClient minecraftClient;

    @Nullable
    private String friendsEtag;
    private FriendData friendsCache = FriendData.empty();

    @Nullable
    private String presenceEtag;
    private PresenceResponse presenceCache = PresenceResponse.empty();

    @Nullable
    private volatile Duration friendsPollInterval;
    @Nullable
    private volatile Duration presencePollInterval;

    @Override
    public Optional<Duration> getFriendsPollInterval() {
        return Optional.ofNullable(friendsPollInterval);
    }

    @Override
    public Optional<Duration> getPresencePollInterval() {
        return Optional.ofNullable(presencePollInterval);
    }

    private final AtomicBoolean requestPending = new AtomicBoolean();
    @Nullable
    private Instant requestCooldown;

    public YggdrasilFriendsService(final String accessToken, final Proxy proxy, final Environment env) {
        this.minecraftClient = new MinecraftClient(accessToken, proxy);
        this.routeFriends = HttpAuthenticationService.constantURL(env.servicesHost() + "/friends");
        this.routePrivileges = HttpAuthenticationService.constantURL(env.servicesHost() + "/player/attributes");
        this.routePresence = HttpAuthenticationService.constantURL(env.servicesHost() + "/presence");
    }

    /** 
     * Fetches friends, incoming requests, and outgoing requests in one call (GET /friends).
     * Implementations should send an If-None-Match header with the cached ETag when available.
     * Returns the cached {@link FriendData} unchanged when the server responds with 304 Not Modified.
     */
    @Override
    public ResultCode getFriendData(final Consumer<FriendData> friendData) {
        boolean isRequesting = requestPending.getAndSet(true);

        ResultCode resultCode;
        if (isRequesting) {
            while (isRequesting) {
                Thread.yield();
                isRequesting = requestPending.get();
            }

            friendData.accept(friendsCache);
            return ResultCode.SUCCESS;
        }

        if (canMakeRequest()) {
            requestCooldown = Instant.now().plusSeconds(REQUEST_COOLDOWN_SECONDS);
            resultCode = requestFriendData(friendData);
        } else {
            friendData.accept(friendsCache);
            resultCode = ResultCode.SUCCESS;
        }

        requestPending.set(false);
        return resultCode;
    }

    private ResultCode requestFriendData(final Consumer<FriendData> friendData) {
        try {
            final ServiceResponse<FriendsListResponse> response =
                minecraftClient.getWithEtag(routeFriends, FriendsListResponse.class, friendsEtag);

            final Duration retryAfter = response.retryAfter();
            if (retryAfter != null) {
                friendsPollInterval = retryAfter;
            }

            final FriendsListResponse body = response.body();
            if (body == null) {
                // 304 Not Modified, return the cached snapshot unchanged
                friendData.accept(friendsCache);
                return ResultCode.SUCCESS;
            }

            friendsCache = new FriendData(
                body.friends() != null ? List.copyOf(body.friends()) : List.of(),
                body.incomingRequests() != null ? List.copyOf(body.incomingRequests()) : List.of(),
                body.outgoingRequests() != null ? List.copyOf(body.outgoingRequests()) : List.of()
            );

            friendsEtag = response.etag();
            friendData.accept(friendsCache);
            return ResultCode.SUCCESS;
        } catch (final MinecraftClientHttpException e) {
            final ResultCode resultCode = handleHttpError(e);
            friendData.accept(friendsCache); // MST: TODO: should this happen in error cases?
            return resultCode;
        } catch (final MinecraftClientException e) {
            friendData.accept(friendsCache); // MST: TODO: should this happen in error cases?
            return ResultCode.ERROR;
        }
    }

    private boolean canMakeRequest() {
        return requestCooldown == null || Instant.now().isAfter(requestCooldown);
    }

    @Override
    public ResultCode removeFriend(final UUID playerID) {
        return putFriendAction(FriendActionRequest.byId(playerID, UpdateType.REMOVE));
    }

    @Override
    public ResultCode acceptIncomingFriendRequest(final UUID id) {
        return putFriendAction(FriendActionRequest.byId(id, UpdateType.ADD));
    }

    @Override
    public ResultCode declineIncomingFriendRequest(final UUID id) {
        return putFriendAction(FriendActionRequest.byId(id, UpdateType.REMOVE));
    }

    @Override
    public ResultCode sendFriendRequest(final String name) {
        return putFriendAction(FriendActionRequest.byName(name, UpdateType.ADD));
    }

    @Override
    public ResultCode sendFriendRequest(final UUID playerID) {
        return putFriendAction(FriendActionRequest.byId(playerID, UpdateType.ADD));
    }

    @Override
    public ResultCode revokeOutgoingFriendRequest(final UUID id) {
        return putFriendAction(FriendActionRequest.byId(id, UpdateType.REMOVE));
    }

    @Override
    public ResultCode updateFriendSettings(final boolean enableFriendlist, final boolean enableFriendInvites) {
        try {
            final UserAttributesRequest.FriendsPreferences friendsPreferences = new UserAttributesRequest.FriendsPreferences(
                enableFriendlist ? ToggleValue.ENABLED : ToggleValue.DISABLED,
                enableFriendInvites ? ToggleValue.ENABLED : ToggleValue.DISABLED
            );

            final UserAttributesRequest request = new UserAttributesRequest(null, friendsPreferences);

            // MST: for now we ignore the response from this request, we only care about potential HTTP error codes
            // TODO: figure out if we should parse the response later to update the client flags
            /*UserAttributesResponse response = */
            minecraftClient.post(routePrivileges, request, UserAttributesResponse.class);
        } catch (final MinecraftClientHttpException e) {
            return handleHttpError(e);
        } catch (final MinecraftClientException e) {
            // TODO: Handle errors appropriately
            if (e.getType() == ErrorType.SERVICE_UNAVAILABLE) {
                return ResultCode.SERVICE_NOT_AVAILABLE;
            }
            return ResultCode.ERROR;
        }
        return ResultCode.SUCCESS;
    }

    /** 
     * Sends a PUT /friends request and invalidates the local cache on success.
     */
    private ResultCode putFriendAction(final FriendActionRequest request) {
        try {
            final FriendsListResponse response = minecraftClient.put(routeFriends, request, FriendsListResponse.class);

            if (response != null) {
                requestCooldown = Instant.now().plusSeconds(REQUEST_COOLDOWN_SECONDS);
                friendsEtag = null;

                friendsCache = new FriendData(
                    response.friends() != null ? List.copyOf(response.friends()) : List.of(),
                    response.incomingRequests() != null ? List.copyOf(response.incomingRequests()) : List.of(),
                    response.outgoingRequests() != null ? List.copyOf(response.outgoingRequests()) : List.of()
                );
            }
        } catch (final MinecraftClientHttpException e) {
            return handleHttpError(e);
        } catch (final MinecraftClientException e) {
            // IO / service-unavailable
            if (e.getType() == ErrorType.SERVICE_UNAVAILABLE) {
                return ResultCode.SERVICE_NOT_AVAILABLE;
            }
            return ResultCode.ERROR;
        }
        return ResultCode.SUCCESS;
    }

    @Override
    public PresenceResponse presence(final String status) {
        return presence(status, null);
    }

    @Override
    public PresenceResponse presence(final String status, final @Nullable JoinInfoUpdate joinInfoUpdate) {
        try {
            final ServiceResponse<PresenceResponse> response = minecraftClient.postWithEtag(
                routePresence,
                new PresenceRequest(PresenceStatus.valueOf(status), joinInfoUpdate),
                PresenceResponse.class,
                presenceEtag
            );

            final Duration retryAfter = response.retryAfter();
            if (retryAfter != null) {
                presencePollInterval = retryAfter;
            }

            final PresenceResponse body = response.body();
            if (body == null) {
                presenceEtag = response.etag();
                return presenceCache;
            }
            final PresenceResponse responseToCache = body.presence() == null ? PresenceResponse.empty() : new PresenceResponse(List.copyOf(body.presence()));
            presenceEtag = response.etag();
            presenceCache = responseToCache;
            return responseToCache;
        } catch (final MinecraftClientHttpException e) {
            handleHttpError(e);
            return presenceCache;
        } catch (final MinecraftClientException e) {
            // IO / service-unavailable — ignore for now
            return presenceCache;
        }
    }

    /** 
     * Centralised HTTP error handler.
     * <ul>
     *   <li>400 – bad request with a body describing the error (logged)</li>
     *   <li>403 – user not entitled / forbidden</li>
     *   <li>429 – rate-limited (silently ignored; caller should back off)</li>
     *   <li>5xx – service unavailable (silently ignored; retry later)</li>
     * </ul>
     */
    /* 
     * FIXME: This is where we should implement:
     *  Switch service off if server tells us it's not available for this version
     */
    private ResultCode handleHttpError(final MinecraftClientHttpException e) {
        final int status = e.getStatus();
        if (status == HTTP_TOO_MANY_REQUESTS) {
            LOGGER.warn("Friends service rate-limited (429) — back off before retrying");
            return ResultCode.TOO_MANY_REQUESTS;
        } else if (status >= 500) {
            LOGGER.warn("Friends service unavailable ({}) — retry later", status);
            return ResultCode.SERVICE_NOT_AVAILABLE;
        } else if (status == MinecraftClientHttpException.FORBIDDEN) {
            LOGGER.warn("Friends service forbidden (403) — user may lack an active profile");
            return ResultCode.FORBIDDEN;
        } else if (status == 400) {
            LOGGER.warn("Friends service bad request (400) — Name or profile does not exist");
            return ResultCode.UNKNOWN_PROFILE;
        } else if (status == 401) {
            LOGGER.warn("Friends service unauthorized (401) — Invalid token");
            return ResultCode.UNAUTHORIZED;
        } else {
            LOGGER.debug("Friends service returned HTTP {} — {}", status, e.getMessage());
            return ResultCode.ERROR;
        }
    }
}