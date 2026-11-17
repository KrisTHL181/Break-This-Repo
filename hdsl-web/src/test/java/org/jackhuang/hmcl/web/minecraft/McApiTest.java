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
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.emergency.EmergencyStop;
import org.jackhuang.hmcl.web.event.EventBus;
import org.jackhuang.hmcl.web.task.TaskService;
import org.jackhuang.hmcl.web.terminal.TerminalManager;
import org.jackhuang.hmcl.web.terminal.TerminalTickets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The servlet behind the gate: the reads the enable flow needs, and the 404
/// the rest of the surface gives while the feature is off.
class McApiTest {

    private record Wired(Server server, McFeatureState feature, int port) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            server.stop();
        }
    }

    private static McConsent consent() {
        return new McConsent(3, "sha256:terms", "sha256:eula",
                "2026-10-30T00:00:00Z", "2026-10-30T01:00:00Z", "2026-10-30T01:00:00Z",
                "tester", "127.0.0.1", "scroll+checkbox", true, "1.4.0", "UA", "abc1234", null);
    }

    private static Wired wired(Path dir, boolean enabled) throws Exception {
        ServerConfig config = new ServerConfig();
        McFeatureState feature = new McFeatureState(dir.resolve("feature.json"));
        if (enabled) {
            feature.enable(consent());
        }
        McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
        TerminalManager terminals = new TerminalManager();
        McRuntime runtime = new McRuntime(manager, terminals, feature, "java", null);
        McVersions versions = new McVersions(dir.resolve("versions"),
                URI.create("http://127.0.0.1:1/manifest.json"));
        McBackups backups = new McBackups(dir.resolve("backups"));
        McApiServlet servlet = new McApiServlet(config, feature, manager, runtime, versions,
                new McCores(versions, dir.resolve("cores")),
                new McJavaRuntimes(dir.resolve("java")), backups,
                new TaskService(new EventBus()), new TerminalTickets(), null, null, null,
                () -> true, new EmergencyStop());

        Server server = new Server(0);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new ServletHolder(servlet), "/api/mc/*");
        context.addFilter(new FilterHolder(new McFeatureFilter(feature, "")), "/api/mc/*",
                EnumSet.of(DispatcherType.REQUEST));
        server.setHandler(context);
        server.start();
        int port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        return new Wired(server, feature, port);
    }

    private static HttpResponse<String> get(int port, String path) throws Exception {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void disabledStillServesTheEnableFlowAnd404sTheRest(@TempDir Path dir) throws Exception {
        try (Wired wired = wired(dir, false)) {
            HttpResponse<String> feature = get(wired.port(), "/api/mc/feature");
            assertEquals(200, feature.statusCode());
            assertTrue(feature.body().contains("DISABLED"));
            // The card has to be able to show where servers will live and
            // which Java will run them before the user enables anything.
            assertTrue(feature.body().contains("\"java\""));
            assertTrue(feature.body().contains("\"storage\""));
            assertTrue(feature.body().contains("\"source\":\"panel\""));
            assertTrue(feature.body().contains("\"root\""));
            // The advanced panel validates a manual heap value from these; the
            // SPA must not hardcode a ceiling the server would reject.
            assertTrue(feature.body().contains("\"limits\""));
            assertTrue(feature.body().contains("\"minMemoryMb\":256"));
            assertTrue(feature.body().contains("\"maxMemoryMb\":8192"));

            HttpResponse<String> terms = get(wired.port(), "/api/mc/terms");
            assertEquals(200, terms.statusCode());
            assertTrue(terms.body().contains("\"version\":3"));

            assertEquals(404, get(wired.port(), "/api/mc/instances").statusCode());
            assertEquals(404, get(wired.port(), "/api/mc/versions").statusCode());
        }
    }

    @Test
    void enabledListsInstancesAndKeepsTheTerms(@TempDir Path dir) throws Exception {
        try (Wired wired = wired(dir, true)) {
            HttpResponse<String> instances = get(wired.port(), "/api/mc/instances");
            assertEquals(200, instances.statusCode());
            assertTrue(instances.body().contains("\"instances\""));

            assertEquals(200, get(wired.port(), "/api/mc/terms").statusCode());
        }
    }

    /// 向导起步的两问：服务器能放哪、默认端口是什么。
    @Test
    void theCreateWizardCanAskForStorageAndPorts(@TempDir Path dir) throws Exception {
        try (Wired wired = wired(dir, true)) {
            HttpResponse<String> storage = get(wired.port(), "/api/mc/storage");
            assertEquals(200, storage.statusCode());
            assertTrue(storage.body().contains("\"external\""));
            assertTrue(storage.body().contains("\"writable\""));
            assertTrue(storage.body().contains("\"default\":\"external\""));

            HttpResponse<String> ports = get(wired.port(), "/api/mc/ports");
            assertEquals(200, ports.statusCode());
            assertTrue(ports.body().contains("\"port\":"));
            assertTrue(ports.body().contains("\"rconPort\":"));
        }
    }
}