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
package org.jackhuang.hmcl.web.proxy;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.jackhuang.hmcl.dsh.DshHomeMode;
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.dsh.DshProcess;
import org.jackhuang.hmcl.dsh.DshProcessManager;
import org.jackhuang.hmcl.setting.GameDirectory;
import org.jackhuang.hmcl.web.NodeDshStub;
import org.jackhuang.hmcl.web.TestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What the proxy asks the instance for, against what it can do with the answer.
///
/// The two sides have to agree on one thing: the body rewriting knows gzip and
/// deflate and nothing else (there is no brotli or zstd decoder in this build,
/// and no dependency that would bring one). A browser sends `gzip, deflate, br,
/// zstd`, so a request that passes that header through unchanged invites an
/// instance to answer brotli — and the rewriter then decoded that body as UTF-8
/// text, so the browser was handed the mangled result with the mount missing
/// from it entirely. The instance here answers exactly what it was asked for,
/// which is how the disagreement becomes visible.
class ProxyBodyEncodingTest {

    private static final String INSTANCE = "proxy-body-encoding-test";

    /// The browser's own header, as Chrome sends it.
    private static final String BROWSER_ENCODINGS = "gzip, deflate, br, zstd";

    /// A page shaped like the one dsh generates: the rewrite has something to do
    /// on it, and its `<base>` is what says whether the rewrite ran at all.
    private static final String PAGE = "<!doctype html><html><head><base href=\"/\">"
            + "<script src=\"/plugins/two/client.js\"></script></head><body>the dsh page</body></html>";

    private final List<AutoCloseable> cleanup = new ArrayList<>();

    @TempDir
    Path dataDir;

    @AfterEach
    void tearDown() throws Exception {
        DshProcessManager.stop(INSTANCE);
        if (DshInstanceManager.exists(INSTANCE)) {
            DshInstanceManager.delete(INSTANCE);
        }
        for (AutoCloseable task : cleanup) {
            task.close();
        }
        cleanup.clear();
    }

    /// The instance must not be asked for an encoding the rewriter cannot undo —
    /// and the page must arrive rewritten, which is the half of this the browser
    /// can see.
    ///
    /// Both halves are asserted deliberately: "the instance was not asked" alone
    /// would be satisfied by not asking for anything at all, and "the page came
    /// back mounted" alone would be satisfied by an instance that answers
    /// identity to everything. Together they say the request carried the one
    /// encoding the rewriter was asked for (`gzip`), and the answer came back
    /// through it.
    @Test
    void theInstanceIsNotAskedForAnEncodingTheRewriterCannotDecode() throws Exception {
        NodeDshStub.assumeNode();
        try (TestSupport.RunningServer running = startServer()) {
            DshInstance instance = createInstance();
            // The passive stub announces the loopback port and idles, so the
            // upstream can bind that same port — see ProxyUpstreamFailureTest.
            NodeDshStub.install(instance, false);
            DshProcess process = DshProcessManager.launch(instance);
            cleanup.add(process::stop);
            cleanup.add(() -> DshProcessManager.stop(INSTANCE));
            awaitReady(process);

            try (NegotiatingUpstream upstream = new NegotiatingUpstream(instance.portOrDefault())) {
                HttpResponse<String> page = TestSupport.client().send(
                        HttpRequest.newBuilder(URI.create(running.baseUrl() + "/i/" + INSTANCE + "/"))
                                .header("Accept-Encoding", BROWSER_ENCODINGS)
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());

                assertEquals(200, page.statusCode(), page.body());

                String asked = upstream.asked();
                assertTrue(asked.contains("gzip"),
                        "the rewriter can undo gzip, so it may still be asked for: " + asked);
                assertFalse(asked.contains("br"),
                        "the rewriter has no brotli decoder, so the instance must not be asked for one —"
                                + " the browser is handed the UTF-8 decode of it instead: " + asked);
                assertFalse(asked.contains("zstd"),
                        "the rewriter has no zstd decoder either: " + asked);

                // And the answer came back through the rewriting, not gutted by it:
                // a gzipped page the rewriter undid, mounted, with nothing of the
                // decode left in it.
                assertTrue(page.body().contains("<base href=\"/i/" + INSTANCE + "/\">"), page.body());
                assertTrue(page.body().contains("src=\"/i/" + INSTANCE + "/plugins/two/client.js\""), page.body());
                assertFalse(page.body().contains("\uFFFD"),
                        "the browser was handed text decoded out of a body the rewriter cannot read: " + page.body());
            }
        }
    }

    // ---------------------------------------------------------------- helpers --

    private TestSupport.RunningServer startServer() throws Exception {
        TestSupport.RunningServer running = TestSupport.start(dataDir, Map.of(), config -> {
            config.bindHost = "127.0.0.1";
            config.auth.disabled = true;
        });
        cleanup.add(running::close);
        return running;
    }

    private static DshInstance createInstance() throws Exception {
        return DshInstanceManager.create(INSTANCE, "0.1.7-rc.1", "web",
                GameDirectory.defaultDirectory().directory(), DshHomeMode.ISOLATED, null, List.of(), Map.of());
    }

    private static void awaitReady(DshProcess process) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (process.webUrl().isPresent()) {
                return;
            }
            Thread.sleep(50);
        }
        assertNotNull(process.webUrl().orElse(null), "the stub never printed its readiness line");
    }

    /// The instance side, on the loopback port the readiness line announced: it
    /// answers what it was asked for, the way a real server does.
    ///
    /// Asked for brotli it answers brotli — bytes no UTF-8 decode survives, and
    /// they do not have to be real brotli to prove that, because the rewriter has
    /// no decoder for the name either way. Asked for gzip it answers the same page
    /// gzipped, which is how dsh sends its page; asked for nothing it answers the
    /// page as it is.
    private static final class NegotiatingUpstream implements AutoCloseable {

        private final Server server;
        private final List<String> asked = new CopyOnWriteArrayList<>();

        NegotiatingUpstream(int port) throws Exception {
            this.server = new Server();
            ServerConnector connector = new ServerConnector(server);
            connector.setHost("127.0.0.1");
            connector.setPort(port);
            server.addConnector(connector);
            server.setHandler(new Handler.Abstract() {
                @Override
                public boolean handle(Request request, Response response, Callback callback) throws Exception {
                    String encodings = request.getHeaders().get("Accept-Encoding");
                    asked.add(encodings == null ? "" : encodings);
                    byte[] body;
                    String encoding = null;
                    if (encodings != null && (encodings.contains("br") || encodings.contains("zstd"))) {
                        body = new byte[]{0x0b, 0x00, (byte) 0x80, (byte) 0xfe, (byte) 0xff, 0x1b, 0x00};
                        encoding = "br";
                    } else if (encodings != null && encodings.contains("gzip")) {
                        body = gzip(PAGE);
                        encoding = "gzip";
                    } else {
                        body = PAGE.getBytes(StandardCharsets.UTF_8);
                    }
                    response.setStatus(200);
                    response.getHeaders().put(HttpHeader.CONTENT_TYPE, "text/html; charset=utf-8");
                    if (encoding != null) {
                        response.getHeaders().put("Content-Encoding", encoding);
                    }
                    response.getHeaders().put(HttpHeader.CONTENT_LENGTH, body.length);
                    response.write(true, ByteBuffer.wrap(body), callback);
                    return true;
                }
            });
            server.start();
        }

        /// The `Accept-Encoding` of every request that arrived, as it arrived.
        String asked() {
            return String.join(" | ", asked);
        }

        @Override
        public void close() throws Exception {
            server.stop();
        }

        private static byte[] gzip(String text) throws IOException {
            ByteArrayOutputStream gzipped = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(gzipped)) {
                gzip.write(text.getBytes(StandardCharsets.UTF_8));
            }
            return gzipped.toByteArray();
        }
    }
}