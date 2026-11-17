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
package org.jackhuang.hmcl.web.minecraft;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McFeatureFilterTest {

    private static McConsent consent() {
        return new McConsent(3, "sha256:terms", "sha256:eula",
                "2026-10-30T00:00:00Z", "2026-10-30T01:00:00Z", "2026-10-30T01:00:00Z",
                "tester", "127.0.0.1", "scroll+checkbox", true, "1.4.0", "UA", "abc1234", null);
    }

    /// A Jetty with the filter in front of one servlet that answers 200 for
    /// every path, so the status tells us whether the filter let it through.
    private static Server server(McFeatureState feature) throws Exception {
        Server server = new Server(0);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setStatus(200);
                resp.getWriter().write("reached");
            }
        }), "/api/mc/*");
        context.addFilter(new FilterHolder(new McFeatureFilter(feature, "")), "/api/mc/*",
                EnumSet.of(DispatcherType.REQUEST));
        server.setHandler(context);
        server.start();
        return server;
    }

    private static HttpResponse<String> call(Server server, String method, String path) throws Exception {
        int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (!"GET".equals(method)) {
            builder = builder.method(method, HttpRequest.BodyPublishers.ofString("{}"));
        }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void disabledLooksLikeTheFeatureDoesNotExist(@TempDir Path dir) throws Exception {
        McFeatureState feature = new McFeatureState(dir.resolve("feature.json"));
        Server server = server(feature);
        try {
            assertEquals(404, call(server, "GET", "/api/mc/instances").statusCode());
            assertEquals(404, call(server, "POST", "/api/mc/instances").statusCode());
            assertEquals(404, call(server, "GET", "/api/mc/instances/x/logs").statusCode());

            // The enable flow itself has to stay reachable, or nothing could turn it on.
            assertEquals(200, call(server, "GET", "/api/mc/feature").statusCode());
            assertEquals(200, call(server, "GET", "/api/mc/eula").statusCode());
            assertEquals(200, call(server, "POST", "/api/mc/feature/enable").statusCode());
            assertEquals(200, call(server, "POST", "/api/mc/feature/decline").statusCode());
        } finally {
            server.stop();
        }
    }

    @Test
    void enabledLetsEverythingThrough(@TempDir Path dir) throws Exception {
        McFeatureState feature = new McFeatureState(dir.resolve("feature.json"));
        feature.enable(consent());
        Server server = server(feature);
        try {
            assertEquals(200, call(server, "GET", "/api/mc/instances").statusCode());
            assertEquals(200, call(server, "POST", "/api/mc/instances").statusCode());
        } finally {
            server.stop();
        }
    }

    @Test
    void theLockRefusesNewAndStartingServersButNotReads(@TempDir Path dir) throws Exception {
        AtomicLong clock = new AtomicLong();
        McFeatureState feature = new McFeatureState(dir.resolve("feature.json"), clock::get,
                McFeatureState.LOCK_DURATION);
        feature.enable(consent());
        feature.stopFeature();
        Server server = server(feature);
        try {
            HttpResponse<String> create = call(server, "POST", "/api/mc/instances");
            assertEquals(423, create.statusCode());
            assertTrue(create.body().contains("remainingSeconds"));

            assertEquals(423, call(server, "POST", "/api/mc/instances/abc/start").statusCode());
            assertEquals(200, call(server, "GET", "/api/mc/instances").statusCode());
            assertEquals(200, call(server, "POST", "/api/mc/instances/abc/stop").statusCode(),
                    "stopping is allowed: the servers are already down, and the hour is not a freeze on cleanup");
        } finally {
            server.stop();
        }

        clock.addAndGet(McFeatureState.LOCK_DURATION.toNanos() + Duration.ofSeconds(1).toNanos());
        Server after = server(feature);
        try {
            assertEquals(200, call(after, "POST", "/api/mc/instances").statusCode());
        } finally {
            after.stop();
        }
    }
}