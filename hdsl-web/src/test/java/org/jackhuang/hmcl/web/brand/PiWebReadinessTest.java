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
package org.jackhuang.hmcl.web.brand;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Pins the protocol the readiness probe speaks.
///
/// The regression was silent and looked like a dead instance: pi-web's Fastify
/// server answers a cleartext HTTP/2 upgrade (`Upgrade: h2c`) with 400, while
/// the same URL over HTTP/1.1 answers 200. The JDK client defaults to HTTP/2,
/// so every launch waited out the full 180-second readiness timeout even
/// though both halves were already serving.
class PiWebReadinessTest {

    @Test
    void theProbeUsesHttp11BecauseFastifyRejectsH2c() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/pi-web/health", exchange -> {
            boolean upgrade = exchange.getRequestHeaders().getFirst("Upgrade") != null;
            byte[] body = (upgrade ? "upgrade refused" : "{\"ok\":true}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(upgrade ? 400 : 200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort()
                    + "/api/pi-web/health";

            assertTrue(BrandRuntime.answers(BrandRuntime.loopbackClient(), url),
                    "the panel's probe must speak HTTP/1.1");
            assertFalse(BrandRuntime.answers(HttpClient.newHttpClient(), url),
                    "the default client still sends the h2c upgrade this server refuses");
        } finally {
            server.stop(0);
        }
    }
}
