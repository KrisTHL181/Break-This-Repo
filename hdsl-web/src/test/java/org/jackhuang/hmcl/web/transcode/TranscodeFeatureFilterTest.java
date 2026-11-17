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
package org.jackhuang.hmcl.web.transcode;

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
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranscodeFeatureFilterTest {

    private static TranscodeConsent consent() {
        return new TranscodeConsent(TranscodeTerms.VERSION, TranscodeTerms.hash(),
                "2026-11-10T00:00:00Z", "tester", "127.0.0.1",
                "checkbox", "0.6.82", "UA", "abc1234", true);
    }

    /// A Jetty with the filter in front of one servlet that answers 200 for
    /// every path, so the status tells us whether the filter let it through.
    private static Server server(TranscodeFeatureState feature) throws Exception {
        Server server = new Server(0);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setStatus(200);
                resp.getWriter().write("reached");
            }
        }), "/api/transcode/*");
        context.addFilter(new FilterHolder(new TranscodeFeatureFilter(feature, "")), "/api/transcode/*",
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
        TranscodeFeatureState feature = new TranscodeFeatureState(dir.resolve("feature.json"));
        Server server = server(feature);
        try {
            assertEquals(404, call(server, "GET", "/api/transcode/queue").statusCode());
            assertEquals(404, call(server, "POST", "/api/transcode/queue").statusCode());
            assertEquals(404, call(server, "GET", "/api/transcode/presets").statusCode());
            assertEquals(404, call(server, "POST", "/api/transcode/feature/disable").statusCode());

            // The enable flow itself has to stay reachable, or nothing could turn it on.
            assertEquals(200, call(server, "GET", "/api/transcode/feature").statusCode());
            assertEquals(200, call(server, "GET", "/api/transcode/terms").statusCode());
            assertEquals(200, call(server, "POST", "/api/transcode/feature/enable").statusCode());
            assertEquals(200, call(server, "POST", "/api/transcode/feature/decline").statusCode());
        } finally {
            server.stop();
        }
    }

    @Test
    void enabledLetsEverythingThrough(@TempDir Path dir) throws Exception {
        TranscodeFeatureState feature = new TranscodeFeatureState(dir.resolve("feature.json"));
        feature.enable(consent());
        Server server = server(feature);
        try {
            assertEquals(200, call(server, "GET", "/api/transcode/queue").statusCode());
            assertEquals(200, call(server, "POST", "/api/transcode/queue").statusCode());
            assertEquals(200, call(server, "POST", "/api/transcode/feature/disable").statusCode());
        } finally {
            server.stop();
        }
    }
}