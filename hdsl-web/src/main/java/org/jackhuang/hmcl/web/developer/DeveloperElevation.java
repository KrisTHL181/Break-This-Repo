/*
 * HDSL-web
 * Copyright (C) 2026  HDSL-web contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.web.developer;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/// The thirty-minute developer-mode elevation, shared by every door that
/// needs it: the developer routes and the web terminal.
///
/// The key is the **session token**, never the username: two browser sessions
/// of the same account must not share an elevation, and logging one out must
/// end its elevation in the same instant. A key whose session is gone is
/// treated as not elevated (the caller passes that in, because only
/// [org.jackhuang.hmcl.web.auth.AuthService] knows).
///
/// Each elevation is a **ticket with one extension coupon**: the holder may
/// stretch it to [EXTENDED_TTL] at most once; stretching it again takes a
/// fresh password proof ([#elevate]). Re-proving mints a new ticket that keeps
/// whatever time the old one had left — re-verifying mid-extension must not
/// cut the window back down to [TTL].
///
/// Failures are counted per key with the same shape the sign-in gate uses:
/// [#MAX_FAILURES] wrong passwords inside [#FAILURE_WINDOW] lock the key for
/// [#LOCK]. A correct password clears both.
@NotNullByDefault
public final class DeveloperElevation {

    /// How long one elevation lasts, sliding on every use. Thirty minutes is
    /// deliberately longer than a terminal session's usual idle gaps: a user
    /// watching an agent should not be asked for the password mid-task, and
    /// the elevation only gates new opens/attach, not the live WebSocket.
    public static final Duration TTL = Duration.ofMinutes(30);
    /// The ceiling a session may give itself on demand (the developer page's
    /// 「延长提权」): one long stretch — a hands-on maintenance window — beats
    /// being re-asked hourly, and it is still a fraction of a day.
    public static final Duration EXTENDED_TTL = Duration.ofHours(3);
    /// How many wrong developer passwords are allowed inside the window.
    public static final int MAX_FAILURES = 3;
    /// How long a locked key waits before it may guess again.
    public static final Duration LOCK = Duration.ofMinutes(10);
    /// How far back failures are counted.
    public static final Duration FAILURE_WINDOW = Duration.ofMinutes(10);

    private static final String SESSION_PREFIX = "token:";

    /// The key prefix of an elevation held by a bearer token minted through
    /// [#mintToken]. A token is not tied to a browser session: it exists for
    /// callers that cannot carry the session cookie (a host script, an
    /// inspection agent, CI), and it dies the way any elevation does — TTL,
    /// password change, or [clearAll].
    private static final String BEARER_PREFIX = "bearer:";

    private static final SecureRandom RANDOM = new SecureRandom();

    /// One agent grant as the panel presents it: who holds it, what it may do,
    /// which instance it belongs to, and when it dies.
    ///
    /// The token itself never appears in this record — [#id] is the handle the
    /// operator clicks to revoke, so a page that lists grants never carries a
    /// credential it could hand to a screenshot or a bug report.
    ///
    /// How a grant came to exist.
    ///
    /// The difference decides what a remote caller still has to prove when
    /// [org.jackhuang.hmcl.web.config.ServerConfig.AgentConfig#remoteNeedsSession]
    /// is on: a browser mint (`SESSION`) already means a person opened the
    /// panel and copied the token out, while a password mint (`PASSWORD`)
    /// means only that the long-lived agent password was at hand.
    public enum MintedBy {
        PASSWORD,
        SESSION
    }

    /// @param id         short handle, unique among live grants
    /// @param label      what the operator typed on the settings page
    /// @param scopes     the capability classes of [AgentScopes]
    /// @param instanceId the instance the agent runs in, or `null`
    /// @param expiresAt  when the grant dies
    /// @param mintedBy   which door minted it
    public record AgentGrant(String id, String label, Set<String> scopes,
                             @Nullable String instanceId, Instant expiresAt, MintedBy mintedBy) {
    }

    /// What the panel remembers about an agent bearer token beyond its expiry.
    private record AgentInfo(String id, String label, Set<String> scopes,
                             @Nullable String instanceId, MintedBy mintedBy) {
    }


    private final ConcurrentHashMap<String, Elevation> elevations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Failures> failures = new ConcurrentHashMap<>();

    /// The elevation key for one request: the session token when there is
    /// one, otherwise the remote address (authentication disabled is
    /// loopback-only by config validation).
    public static String key(@Nullable String sessionToken, @Nullable String remoteAddress) {
        if (sessionToken != null && !sessionToken.isBlank()) {
            return SESSION_PREFIX + sessionToken;
        }
        return "anonymous:" + (remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress);
    }

    /// Whether `key` is elevated right now, given whether its session is
    /// still alive.
    public boolean isElevated(String key, boolean sessionAlive) {
        if (!sessionAlive) {
            elevations.remove(key);
            return false;
        }
        Elevation elevation = elevations.get(key);
        return elevation != null && elevation.expiry.isAfter(Instant.now());
    }

    /// Passes and slides an elevation, or answers `false`.
    public boolean require(String key, boolean sessionAlive) {
        Instant now = Instant.now();
        prune(now);
        if (!sessionAlive) {
            elevations.remove(key);
            return false;
        }
        Elevation elevation = elevations.get(key);
        if (elevation != null && elevation.expiry.isAfter(now)) {
            // An agent grant is a promise with a date on it: the operator was
            // shown "剩 2:47" when they approved it. Sliding that would let a
            // chatty agent renew itself forever, so agent keys keep the expiry
            // they were minted with and simply die.
            if (elevation.agent == null) {
                synchronized (elevation) {
                    // 只往前滑、绝不缩短：一次常规使用（开终端、开关页…）不能把
                    // 已经延长到 3 小时的提权砍回 30 分钟。
                    Instant slid = now.plus(TTL);
                    if (slid.isAfter(elevation.expiry)) {
                        elevation.expiry = slid;
                    }
                }
            }
            return true;
        }
        if (elevation != null) {
            elevations.remove(key, elevation);
        }
        return false;
    }

    /// Marks `key` elevated for a fresh [TTL]. When the key already holds an
    /// unexpired elevation, the fresh proof replaces it but keeps the later
    /// expiry: someone re-verifying inside a 3-hour extension keeps the
    /// remaining time and simply earns a new extension coupon.
    public void elevate(String key) {
        Instant fresh = Instant.now().plus(TTL);
        elevations.merge(key, new Elevation(fresh, false, null), (existing, unused) -> {
            Elevation next = new Elevation(fresh, false, null);
            if (existing.expiry.isAfter(next.expiry)) {
                next.expiry = existing.expiry;
            }
            return next;
        });
    }

    /// Stretches `key`'s elevation to `duration` from now, answering the new
    /// expiry, or `null` when there is nothing to stretch: no elevation, an
    /// expired one, or one whose single extension coupon is already spent.
    /// The caller must have established elevation first (see [#require]);
    /// extending a key that has none would mint authority out of nothing.
    public @Nullable Instant extend(String key, Duration duration) {
        Elevation elevation = elevations.get(key);
        if (elevation == null) {
            return null;
        }
        Instant now = Instant.now();
        synchronized (elevation) {
            if (!elevation.expiry.isAfter(now) || elevation.extended) {
                return null;
            }
            elevation.expiry = now.plus(duration);
            elevation.extended = true;
            return elevation.expiry;
        }
    }

    /// When `key`'s elevation expires, if it is elevated and its session
    /// alive — what [#status] answers as `expiresAt`.
    public @Nullable Instant expiryOf(String key, boolean sessionAlive) {
        if (!sessionAlive) {
            elevations.remove(key);
            return null;
        }
        Elevation elevation = elevations.get(key);
        return elevation != null && elevation.expiry.isAfter(Instant.now()) ? elevation.expiry : null;
    }

    /// Ends one key's elevation.
    public void clear(String key) {
        elevations.remove(key);
        failures.remove(key);
    }

    /// Ends every elevation: a changed or cleared developer password makes
    /// every proof of the old one worthless.
    public void clearAll() {
        elevations.clear();
        failures.clear();
    }

    /// Mints a bearer token with its own elevation window.
    ///
    /// The caller must have proved the password already; this method never
    /// checks one. The token is a URL-safe 32-byte random string, stored only
    /// in memory and never logged — treat it like the password itself.
    ///
    /// @return the token, to be handed to the caller and to nobody else
    public String mintToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        elevate(BEARER_PREFIX + token);
        return token;
    }

    /// Passes a bearer token's elevation, or answers `false`.
    ///
    /// **Agent grants are not elevation tokens.** Both live under `bearer:`,
    /// but a grant is scoped authority handed to a model, and letting
    /// `requireToken` accept one would make every agent token a full developer
    /// proof in the guard filter and the WebSocket upgrade gate — the scope
    /// check would be the only thing standing between a `read` grant and an
    /// instance page. Refusing here keeps the two credentials apart.
    ///
    /// Unknown, expired and revoked tokens are all just `false`: the caller
    /// answers one code (`developer_elevation_required`) so a probe cannot tell
    /// "wrong token" from "expired token".
    public boolean requireToken(String token) {
        if (token.isBlank()) {
            return false;
        }
        String key = BEARER_PREFIX + token;
        Elevation elevation = elevations.get(key);
        if (elevation == null || elevation.agent != null) {
            return false;
        }
        return require(key, true);
    }

    /// Mints a bearer token **for an agent**, carrying its label, its
    /// capability classes and the instance it belongs to.
    ///
    /// The caller must have proved the developer password already — this method
    /// mints authority without checking any, exactly like [#mintToken]. What it
    /// adds is who: a token minted here can be listed on the panel, told apart
    /// from the operator's own elevation, and revoked on its own.
    ///
    /// The token never slides. `ttl` is capped at [EXTENDED_TTL]: the operator
    /// is shown the expiry when they approve, and an agent that used its
    /// credential every few minutes must not be able to renew it forever.
    ///
    /// @param label      what the operator typed on the settings page
    /// @param scopes     the capability classes of [AgentScopes]
    /// @param instanceId the instance the agent runs in, or `null`
    /// @param ttl        how long it lives, clamped to `[1s, EXTENDED_TTL]`
    /// @param mintedBy   which door minted it
    /// @return the token, to be handed to that agent and to nobody else
    public String mintAgentToken(String label, Set<String> scopes, @Nullable String instanceId,
                                 Duration ttl, @Nullable Instant notAfter, MintedBy mintedBy) {
        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        byte[] idBytes = new byte[6];
        RANDOM.nextBytes(idBytes);
        String id = Base64.getUrlEncoder().withoutPadding().encodeToString(idBytes).toLowerCase(Locale.ROOT);
        Duration life = ttl.isNegative() || ttl.isZero() ? Duration.ofSeconds(1) : ttl;
        if (life.compareTo(EXTENDED_TTL) > 0) {
            life = EXTENDED_TTL;
        }
        Instant expiry = Instant.now().plus(life);
        // A grant is borrowed power: it never outlives the elevation that
        // authorized it. Without this an operator's 30-minute window could mint
        // a 3-hour credential and walk away.
        if (notAfter != null && expiry.isAfter(notAfter)) {
            expiry = notAfter;
        }
        elevations.put(BEARER_PREFIX + token,
                new Elevation(expiry, false,
                        new AgentInfo(id, label == null || label.isBlank() ? "agent" : label,
                                Set.copyOf(scopes), instanceId, mintedBy)));
        return token;
    }

    /// The grant behind a bearer token, or `null` when it is unknown, expired,
    /// not an agent token, or **nobody is elevated any more**.
    ///
    /// Reading does **not** slide the expiry: an agent that keeps calling must
    /// still lose its credential at the moment the operator was promised.
    ///
    /// The live-elevation check is what makes "borrowed power" literal. Minting
    /// already caps the expiry at the human window, but that window can also end
    /// early — logout clears it, and 「停止提权」 clears it. Without this check a
    /// grant would stay usable until its own date even though the loan was
    /// returned; with it, the grant dies the moment the last elevated session
    /// ends.
    ///
    /// @param token the bearer token from the `Authorization` header
    /// @return the grant, or `null`
    public @Nullable AgentGrant agentGrant(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Elevation elevation = elevations.get(BEARER_PREFIX + token);
        if (elevation == null || elevation.agent == null || !elevation.expiry.isAfter(Instant.now())) {
            return null;
        }
        if (latestHumanExpiry() == null) {
            return null;
        }
        AgentInfo agent = elevation.agent;
        return new AgentGrant(agent.id(), agent.label(), agent.scopes(), agent.instanceId(),
                elevation.expiry, agent.mintedBy());
    }

    /// Every live agent grant, newest expiry last — what the panel's list draws.
    ///
    /// "Live" includes the loan: when nobody is elevated the grants are already
    /// unusable ([#agentGrant] refuses them), so the list must not keep showing
    /// agents as if they were still working. The tokens stay in memory — a
    /// later elevation does not revive them, because the operator is expected
    /// to mint a fresh one — but they are not presented as active.
    ///
    /// @return the grants, never `null`
    public List<AgentGrant> agentGrants() {
        if (latestHumanExpiry() == null) {
            return List.of();
        }
        Instant now = Instant.now();
        List<AgentGrant> grants = new ArrayList<>();
        for (Elevation elevation : elevations.values()) {
            AgentInfo agent = elevation.agent;
            if (agent != null && elevation.expiry.isAfter(now)) {
                grants.add(new AgentGrant(agent.id(), agent.label(), agent.scopes(),
                        agent.instanceId(), elevation.expiry, agent.mintedBy()));
            }
        }
        grants.sort((a, b) -> a.expiresAt().compareTo(b.expiresAt()));
        return grants;
    }

    /// The handles of every agent grant still in the registry, without the
    /// "is anybody elevated" filter [#agentGrants] applies.
    ///
    /// Used when dropping grants to also close the console sockets they opened:
    /// the socket is not re-checked per byte, so the panel has to find it by
    /// the grant's handle. A grant already unusable because nobody is elevated
    /// must still be listed here, or its socket would outlive the loan.
    ///
    /// @return the grant ids, never `null`
    public List<String> agentGrantIds() {
        List<String> ids = new ArrayList<>();
        for (Elevation elevation : elevations.values()) {
            if (elevation.agent != null) {
                ids.add(elevation.agent.id());
            }
        }
        return ids;
    }

    /// Ends one agent grant: the token stops working on the next request.
    ///
    /// @param id the grant's handle from [AgentGrant#id]
    /// @return whether a live grant was found and removed
    public boolean revokeAgent(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        boolean removed = false;
        for (var entry : elevations.entrySet()) {
            AgentInfo agent = entry.getValue().agent;
            if (agent != null && agent.id().equals(id)) {
                removed |= elevations.remove(entry.getKey(), entry.getValue());
            }
        }
        return removed;
    }

    /// Ends every agent grant, leaving the operator's own elevation alone.
    ///
    /// Called when the agent password is set or cleared: rotating the secret
    /// that minted a grant must not leave its holders running, and it must not
    /// sign the operator out either.
    ///
    /// @return how many grants were dropped
    public int revokeAllAgents() {
        int before = 0;
        for (Elevation elevation : elevations.values()) {
            if (elevation.agent != null) {
                before++;
            }
        }
        elevations.entrySet().removeIf(entry -> entry.getValue().agent != null);
        return before;
    }

    /// The latest expiry among the **human** elevations still running, or
    /// `null` when nobody is elevated.
    ///
    /// This is what makes an agent grant *borrowed* authority: the agent
    /// password is worthless until a person has proved the developer password,
    /// and a grant never outlives the window that authorized it. The panel
    /// answers `agent_password_not_elevated` when this returns `null`, so the
    /// operator learns the precondition instead of seeing a mysterious 401.
    ///
    /// @return the latest live human expiry, or `null`
    public @Nullable Instant latestHumanExpiry() {
        Instant now = Instant.now();
        Instant latest = null;
        for (var entry : elevations.entrySet()) {
            // Only a **session** elevation counts. Every `/elevate` also mints
            // an anonymous bearer token for scripts; that one is keyed by
            // `bearer:` and belongs to nobody, so counting it would let a
            // forgotten token keep every agent password alive.
            if (!entry.getKey().startsWith(SESSION_PREFIX)) {
                continue;
            }
            Elevation elevation = entry.getValue();
            if (elevation.agent == null && elevation.expiry.isAfter(now)
                    && (latest == null || elevation.expiry.isAfter(latest))) {
                latest = elevation.expiry;
            }
        }
        return latest;
    }

    /// Whether wrong passwords locked this key.
    public boolean isLocked(String key, Instant now) {
        Failures record = failures.computeIfAbsent(key, k -> new Failures());
        synchronized (record) {
            return record.lockedUntil != null && now.isBefore(record.lockedUntil);
        }
    }

    /// Records a wrong developer password.
    ///
    /// @return whether this failure tripped the lock
    public boolean recordFailure(String key, Instant now) {
        Failures record = failures.computeIfAbsent(key, k -> new Failures());
        synchronized (record) {
            if (record.lockedUntil != null) {
                if (now.isBefore(record.lockedUntil)) {
                    return true;
                }
                record.lockedUntil = null;
                record.count = 0;
            }
            if (record.windowStart == null || now.isAfter(record.windowStart.plus(FAILURE_WINDOW))) {
                record.count = 0;
                record.windowStart = now;
            }
            record.count++;
            if (record.count >= MAX_FAILURES) {
                record.lockedUntil = now.plus(LOCK);
                record.count = 0;
                return true;
            }
            return false;
        }
    }

    /// Forgets a key's failures, which a correct password does.
    public void clearFailures(String key) {
        failures.remove(key);
    }

    /// Drops elevations whose fifteen minutes are up. Sessions come and go,
    /// so the map is only bounded if the expired entries actually leave.
    public void prune(Instant now) {
        if (elevations.size() < 64) {
            return;
        }
        elevations.entrySet().removeIf(entry -> !entry.getValue().expiry.isAfter(now));
    }

    private static final class Elevation {
        private Instant expiry;
        private boolean extended;
        /// Non-null only for a grant minted for an agent: those never slide,
        /// and their scope decides what the bearer may reach.
        private final @Nullable AgentInfo agent;

        private Elevation(Instant expiry, boolean extended, @Nullable AgentInfo agent) {
            this.expiry = expiry;
            this.extended = extended;
            this.agent = agent;
        }
    }

    private static final class Failures {
        private int count;
        private @Nullable Instant windowStart;
        private @Nullable Instant lockedUntil;
    }
}
