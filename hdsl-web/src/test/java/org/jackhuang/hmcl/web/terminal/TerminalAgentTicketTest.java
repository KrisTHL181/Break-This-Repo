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

import org.jackhuang.hmcl.web.developer.AgentScopes;
import org.jackhuang.hmcl.web.developer.DeveloperElevation;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// The agent half of the terminal handshake: a ticket is useless without the
/// bearer that asked for it, and even that bearer must still hold
/// `mc-console`.
class TerminalAgentTicketTest {

    private static TerminalTickets.Ticket ticket(String brand, String grantId) {
        return new TerminalTickets.Ticket("mc-1", brand, null, grantId, Instant.now().plusSeconds(30));
    }

    private static DeveloperElevation elevated() {
        DeveloperElevation store = new DeveloperElevation();
        store.elevate(DeveloperElevation.key("session-1", null));
        return store;
    }

    private static String mint(DeveloperElevation store, Set<String> scopes) {
        return store.mintAgentToken("mc-bot", scopes, "mc/one", Duration.ofMinutes(10), null,
                DeveloperElevation.MintedBy.SESSION);
    }

    @Test
    void theTicketAndTheBearerMustNameTheSameGrant() {
        DeveloperElevation store = elevated();
        String token = mint(store, Set.of(AgentScopes.MC, AgentScopes.MC_CONSOLE));
        DeveloperElevation.AgentGrant grant = store.agentGrant(token);
        assertNotNull(grant);

        assertNotNull(TerminalWebSocket.authorizeAgentTicket(store, token, ticket("minecraft", grant.id())));
        assertNull(TerminalWebSocket.authorizeAgentTicket(store, token, ticket("minecraft", "somebody-else")),
                "the URL alone must not open a console");
        assertNull(TerminalWebSocket.authorizeAgentTicket(store, null, ticket("minecraft", grant.id())),
                "a ticket without the bearer is a string in a URL");
        assertNull(TerminalWebSocket.authorizeAgentTicket(store, "", ticket("minecraft", grant.id())));
        assertNull(TerminalWebSocket.authorizeAgentTicket(store, "never-issued", ticket("minecraft", grant.id())));
    }

    @Test
    void mcAloneDoesNotAttach() {
        DeveloperElevation store = elevated();
        String token = mint(store, Set.of(AgentScopes.MC));
        DeveloperElevation.AgentGrant grant = store.agentGrant(token);
        assertNotNull(grant);

        assertNull(TerminalWebSocket.authorizeAgentTicket(store, token, ticket("minecraft", grant.id())),
                "start/stop and one-line input stay in mc; the PTY is a separate class");
    }

    @Test
    void onlyTheMinecraftConsoleIsReachableThisWay() {
        DeveloperElevation store = elevated();
        String token = mint(store, Set.of(AgentScopes.MC_CONSOLE));
        DeveloperElevation.AgentGrant grant = store.agentGrant(token);
        assertNotNull(grant);

        assertNull(TerminalWebSocket.authorizeAgentTicket(store, token, ticket("dsh", grant.id())),
                "an mc-console grant must not attach an instance shell");
    }

    @Test
    void aDeadGrantCannotAttach() {
        DeveloperElevation store = elevated();
        String token = mint(store, Set.of(AgentScopes.MC_CONSOLE));
        DeveloperElevation.AgentGrant grant = store.agentGrant(token);
        assertNotNull(grant);

        store.revokeAgent(grant.id());
        assertNull(TerminalWebSocket.authorizeAgentTicket(store, token, ticket("minecraft", grant.id())));
    }

    @Test
    void whenTheLenderLeavesTheConsoleStopsAttaching() {
        DeveloperElevation store = elevated();
        String token = mint(store, Set.of(AgentScopes.MC_CONSOLE));
        DeveloperElevation.AgentGrant grant = store.agentGrant(token);
        assertNotNull(grant);

        store.clear(DeveloperElevation.key("session-1", null));
        assertNull(TerminalWebSocket.authorizeAgentTicket(store, token, ticket("minecraft", grant.id())),
                "the loan ended; the grant may not attach anything");
    }
}
