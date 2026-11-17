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
package org.jackhuang.hmcl.web.terminal;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// One-use tickets that bridge the authenticated POST and the WebSocket
/// handshake.
///
/// The WebSocket URL ends up in the browser's address bar history, proxy logs
/// and `Referer`-able places; putting the session cookie there would leak it.
/// A ticket is good for one attach, for one instance, for one session, and
/// for [DEFAULT_TTL] seconds — enough to open the socket, useless afterwards.
@NotNullByDefault
public final class TerminalTickets {

    public static final Duration DEFAULT_TTL = Duration.ofSeconds(30);

    /// What a ticket remembers.
    ///
    /// @param instanceId   the instance the terminal was opened for
    /// @param brand        the brand id (`dsh`, `kimi`, …) the terminal serves
    /// @param sessionToken the panel session that asked for it, or `null` when
    ///                     authentication is disabled or the asker was an agent
    /// @param agentGrantId the agent grant that asked for it, or `null` for a
    ///                     browser ticket; the handshake requires that grant's
    ///                     bearer token, so the URL alone cannot attach
    /// @param expiresAt    when the ticket stops being valid
    public record Ticket(String instanceId, String brand, @Nullable String sessionToken,
                         @Nullable String agentGrantId, Instant expiresAt) {
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();
    private final Duration ttl;

    public TerminalTickets() {
        this(DEFAULT_TTL);
    }

    public TerminalTickets(Duration ttl) {
        this.ttl = ttl;
    }

    /// Issues a ticket for one attach.
    ///
    /// @param instanceId the instance the terminal was opened for
    /// @param brand      the brand id the terminal serves
    /// @param sessionToken the session that asked, or `null` without auth
    public String issue(String instanceId, String brand, @Nullable String sessionToken) {
        return issue(instanceId, brand, sessionToken, null);
    }

    /// Issues a ticket for an **agent's** attach: no session cookie exists on
    /// that side, so the handshake proves the holder with the grant's bearer
    /// token instead. The id travels in the ticket (never the token — this
    /// string ends up in a URL) and is compared against the bearer at
    /// handshake time.
    ///
    /// @param instanceId   the instance the terminal was opened for
    /// @param brand        the brand id the terminal serves
    /// @param agentGrantId the grant that asked for it
    /// @return the ticket
    public String issueForAgent(String instanceId, String brand, String agentGrantId) {
        return issue(instanceId, brand, null, agentGrantId);
    }

    private String issue(String instanceId, String brand, @Nullable String sessionToken,
                         @Nullable String agentGrantId) {
        prune();
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tickets.put(ticket, new Ticket(instanceId, brand, sessionToken, agentGrantId,
                Instant.now().plus(ttl)));
        return ticket;
    }

    /// Consumes a ticket, or answers `null` when it is unknown, already used or
    /// expired. A ticket is removed on the first successful consume.
    public @Nullable Ticket consume(@Nullable String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return null;
        }
        Ticket found = tickets.remove(ticket);
        if (found == null || !found.expiresAt().isAfter(Instant.now())) {
            return null;
        }
        return found;
    }

    /// How many tickets are waiting for their attach (tests and diagnostics).
    public int size() {
        return tickets.size();
    }

    private void prune() {
        Instant now = Instant.now();
        tickets.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }
}
