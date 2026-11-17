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

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// A ticket is one attach, one instance, one session, and short-lived.
class TerminalTicketsTest {

    @Test
    void aTicketIsConsumedOnceAndCarriesItsInstance() {
        TerminalTickets tickets = new TerminalTickets();
        String issued = tickets.issue("kimi-1", "kimi", "session-token");

        TerminalTickets.Ticket granted = tickets.consume(issued);
        assertNotNull(granted);
        assertEquals("kimi-1", granted.instanceId());
        assertEquals("kimi", granted.brand(), "the ready frame reports the brand from the ticket");
        assertEquals("session-token", granted.sessionToken());
        assertNull(tickets.consume(issued), "the second consume must not find it");
    }

    @Test
    void anAgentTicketCarriesItsGrantInsteadOfASession() {
        TerminalTickets tickets = new TerminalTickets();
        String issued = tickets.issueForAgent("mc-1", "minecraft", "grant-1");

        TerminalTickets.Ticket granted = tickets.consume(issued);
        assertNotNull(granted);
        assertEquals("grant-1", granted.agentGrantId());
        assertNull(granted.sessionToken(), "an agent has no session cookie to bind");
        assertNull(tickets.consume(issued), "the second consume must not find it");
    }

    @Test
    void aBrowserTicketNamesNoGrant() {
        TerminalTickets tickets = new TerminalTickets();
        TerminalTickets.Ticket granted = tickets.consume(tickets.issue("kimi-1", "kimi", "session-token"));
        assertNotNull(granted);
        assertNull(granted.agentGrantId());
    }

    @Test
    void anExpiredTicketIsRefused() throws Exception {
        TerminalTickets tickets = new TerminalTickets(Duration.ofMillis(20));
        String issued = tickets.issue("kimi-1", "kimi", null);
        Thread.sleep(60);
        assertNull(tickets.consume(issued));
    }

    @Test
    void blankTicketsAreRefused() {
        TerminalTickets tickets = new TerminalTickets();
        assertNull(tickets.consume(null));
        assertNull(tickets.consume(""));
        assertNull(tickets.consume("never-issued"));
    }
}
