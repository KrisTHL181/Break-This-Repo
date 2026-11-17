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

import org.jackhuang.hmcl.web.TestSupport;
import org.jackhuang.hmcl.web.brand.Brand;
import org.jackhuang.hmcl.web.brand.BrandRuntime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What the proxy does with a brand client's asset when the instance does not
/// say how long it is.
///
/// The `/assets/…` references OpenCode's bundle builds at runtime (`new
/// Worker("/assets/markdown.worker-*.js")`) are what the asset patcher exists
/// for, and the patcher used to run only when the instance announced a
/// `content-length`. OpenCode 1.19.5 announces none — its assets come back
/// `Transfer-Encoding: chunked` — so the bundle was forwarded untouched, the
/// worker was requested at the panel root, and the app reported `Markdown
/// highlighting worker failed`. A size the instance did not state is not a size
/// over the cap, and these tests are what keeps the two from being confused
/// again: the whole path is driven through the panel's own server, with a stub
/// upstream writing the bytes of the response head itself, so the missing
/// length is the test's own doing rather than something a container added.
///
/// A body past the cap takes the other road (see
/// [InstanceProxyServlet#canPatchAsset]): it is not patched, and it is still
/// sent whole — the pieces are joined in the test for the reason the servlet
/// joins them, because a truncated script is a script that runs nothing.
class BrandAssetChunkedPatchTest {

    private static final String INSTANCE = "brand-chunked-asset-test";

    private static final String MOUNT = "/i/" + INSTANCE + "/";

    /// The shape that broke: a root-absolute asset reference the bundle keeps as
    /// a literal, plus the entry point's own end marker so a body that lost its
    /// tail can be told from one that did not.
    private static final String BUNDLE = "const TRe=\"/assets/worker-x.js\";\n"
            + "new Worker(TRe,{type:\"module\"});\n"
            + "/*bundle-end*/";

    @TempDir
    Path dataDir;

    private TestSupport.RunningServer running;

    @AfterEach
    void tearDown() throws Exception {
        forgetRunningBrand(INSTANCE);
        if (running != null) {
            running.close();
            running = null;
        }
    }

    /// The regression: no `content-length` at all, and the bundle still has to
    /// come back pointing at the mount — and uncached, because a cached copy
    /// without the patch would send the worker to the panel root on every later
    /// load.
    @Test
    void aBundleWithoutAContentLengthIsStillPatched() throws Exception {
        byte[] bundle = BUNDLE.getBytes(StandardCharsets.UTF_8);
        try (ChunkedUpstream upstream = new ChunkedUpstream(bundle, null)) {
            registerRunningBrand(INSTANCE, Brand.OPENCODE, upstream.port());
            HttpResponse<byte[]> asset = fetch(startServer(), "assets/index-CjbuCoME.js");

            assertEquals(200, asset.statusCode(), body(asset) + " — " + upstream.log());
            String body = body(asset);
            assertTrue(body.contains("\"" + MOUNT + "assets/worker-x.js\""),
                    "the asset reference must be given the mount it is served under: " + body);
            assertFalse(body.contains("\"/assets/worker-x.js\""),
                    "a root-absolute reference would be fetched from the panel root: " + body);
            assertTrue(body.contains("/*bundle-end*/"), "the patch must not lose the file's tail: " + body);
            assertEquals("no-store", asset.headers().firstValue("cache-control").orElse(null),
                    "a patched body is not the instance's own and must not be cached under its address");
        }
    }

    /// The same asset compressed, which is how a real instance sends it. The
    /// patch is matched against what the browser would run, so the body has to be
    /// decoded first — and the response goes out decoded, because `content-encoding`
    /// is dropped with the `content-length` it described.
    @Test
    void aCompressedBundleWithoutAContentLengthIsDecodedBeforeItIsPatched() throws Exception {
        byte[] gzipped = gzip(BUNDLE.getBytes(StandardCharsets.UTF_8));
        try (ChunkedUpstream upstream = new ChunkedUpstream(gzipped, "gzip")) {
            registerRunningBrand(INSTANCE, Brand.OPENCODE, upstream.port());
            HttpResponse<byte[]> asset = fetch(startServer(), "assets/index-CjbuCoME.js");

            assertEquals(200, asset.statusCode(), body(asset));
            String body = body(asset);
            assertTrue(body.contains("\"" + MOUNT + "assets/worker-x.js\""), body);
            assertNull(asset.headers().firstValue("content-encoding").orElse(null),
                    "the body went out decoded, so it must not be labelled with the instance's encoding");
        }
    }

    /// Past the cap the asset is not patched — and it is sent **whole**. The body
    /// is compressed on the wire and larger than the cap once decoded, so this
    /// also pins that the tail comes out of the same decompression: a tail read
    /// from the raw stream would be gzip bytes appended to JS.
    @Test
    void anAssetPastTheCapIsSentWhole() throws Exception {
        byte[] decoded = new byte[(int) InstanceProxyServlet.MAX_SCRIPT_BYTES + 1024 * 1024];
        byte[] anchor = "const ade=\"/assets/worker-x.js\";".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(anchor, 0, decoded, 0, anchor.length);
        for (int i = anchor.length; i < decoded.length; i++) {
            decoded[i] = (byte) ('a' + i % 26);
        }
        byte[] tail = "/*tail-of-the-bundle*/".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(tail, 0, decoded, decoded.length - tail.length, tail.length);

        try (ChunkedUpstream upstream = new ChunkedUpstream(gzip(decoded), "gzip")) {
            registerRunningBrand(INSTANCE, Brand.OPENCODE, upstream.port());
            HttpResponse<byte[]> asset = fetch(startServer(), "assets/index-CjbuCoME.js");

            assertEquals(200, asset.statusCode());
            assertArrayEquals(decoded, asset.body(),
                    "an asset too big to patch is sent as it is, decoded and complete — never truncated");
        }
    }

    // ------------------------------------------------- the rule, on its own --

    /// The same rule the endpoint tests exercise, named at the boundary: a length
    /// nobody stated is patchable, and only a *stated* length over the cap is not.
    @Test
    void aLengthNobodyStatedIsPatchable() {
        assertTrue(InstanceProxyServlet.canPatchAsset(-1),
                "chunked: the instance stated no length, which is not the same as one over the cap");
        assertTrue(InstanceProxyServlet.canPatchAsset(0));
        assertTrue(InstanceProxyServlet.canPatchAsset(InstanceProxyServlet.MAX_SCRIPT_BYTES));
        assertFalse(InstanceProxyServlet.canPatchAsset(InstanceProxyServlet.MAX_SCRIPT_BYTES + 1),
                "a stated length over the cap is what lets the proxy skip the read entirely");
    }

    /// The split the cap makes, on its own: the two pieces must join back into
    /// exactly the stream they came from, or the caller would send a body with a
    /// hole in it.
    @Test
    void aStreamPastTheCapIsSplitWithoutLosingAByte() throws Exception {
        byte[] body = new byte[100_000];
        for (int i = 0; i < body.length; i++) {
            body[i] = (byte) (i % 251);
        }

        try (InputStream in = new ByteArrayInputStream(body)) {
            InstanceProxyServlet.AssetRead read = InstanceProxyServlet.readUpTo(in, 40_000);
            assertNotNull(read.rest(), "a stream past the cap has to come back with the rest of itself");
            ByteArrayOutputStream joined = new ByteArrayOutputStream();
            joined.write(read.bytes());
            read.rest().transferTo(joined);
            assertArrayEquals(body, joined.toByteArray(),
                    "the prefix and the rest must join into the body the instance sent");
            assertTrue(read.bytes().length >= 40_000, "the prefix has to reach the cap");
        }
    }

    @Test
    void aStreamUnderTheCapComesBackWhole() throws Exception {
        byte[] body = "const a=\"/assets/x.js\";".getBytes(StandardCharsets.UTF_8);
        try (InputStream in = new ByteArrayInputStream(body)) {
            InstanceProxyServlet.AssetRead read = InstanceProxyServlet.readUpTo(in, 40_000);
            assertNull(read.rest(), "nothing is left over, so there is no stream to carry on with");
            assertArrayEquals(body, read.bytes());
        }
    }

    // ---------------------------------------------------------------- helpers --

    private String startServer() throws Exception {
        running = TestSupport.start(dataDir, Map.of(), config -> {
            config.bindHost = "127.0.0.1";
            config.auth.disabled = true;
        });
        return running.baseUrl();
    }

    private static HttpResponse<byte[]> fetch(String baseUrl, String path) throws Exception {
        return TestSupport.client().send(
                HttpRequest.newBuilder(URI.create(baseUrl + MOUNT + path)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
    }

    private static String body(HttpResponse<byte[]> response) {
        return new String(response.body(), StandardCharsets.UTF_8);
    }

    /// Makes `/i/<id>/` answer for a brand instance that is running on `port`.
    ///
    /// A brand instance that is really running is a real `kimi web` / `opencode
    /// web` process, which a test cannot have. What the mount asks the runtime is
    /// two things — whether the handle is alive, and which port it announced — and
    /// that state is in memory by design (a panel restart forgets every brand
    /// process, see [BrandRuntime]). So the state is what this seeds: a handle
    /// that is simply alive, the port of the stub below, and the brand the mount
    /// dispatches on. Everything downstream of that — the request, the servlet,
    /// its pump, the patch — is the real thing.
    @SuppressWarnings("unchecked")
    private static void registerRunningBrand(String id, Brand brand, int port) throws Exception {
        ((Map<String, Process>) brandState("PROCESSES")).put(id, aliveProcess());
        ((Map<String, Integer>) brandState("PORTS")).put(id, port);
        ((Map<String, Brand>) brandState("BRANDS")).put(id, brand);
        ((Map<String, BrandRuntime.State>) brandState("STATES")).put(id, BrandRuntime.State.RUNNING);
    }

    @SuppressWarnings("unchecked")
    private static void forgetRunningBrand(String id) throws Exception {
        for (String name : List.of("PROCESSES", "PORTS", "BRANDS", "STATES")) {
            ((Map<String, Object>) brandState(name)).remove(id);
        }
    }

    private static Object brandState(String name) throws Exception {
        Field field = BrandRuntime.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    /// A process handle that is alive and does nothing else: the runtime asks the
    /// handle, and only the handle, before it calls an instance running.
    private static Process aliveProcess() {
        return new Process() {
            @Override
            public OutputStream getOutputStream() {
                return OutputStream.nullOutputStream();
            }

            @Override
            public InputStream getInputStream() {
                return InputStream.nullInputStream();
            }

            @Override
            public InputStream getErrorStream() {
                return InputStream.nullInputStream();
            }

            @Override
            public boolean isAlive() {
                return true;
            }

            @Override
            public int waitFor() {
                throw new UnsupportedOperationException("the test's handle is always alive");
            }

            @Override
            public int exitValue() {
                throw new IllegalThreadStateException("the test's handle is always alive");
            }

            @Override
            public void destroy() {
            }
        };
    }

    private static byte[] gzip(byte[] body) throws IOException {
        ByteArrayOutputStream gzipped = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(gzipped)) {
            gzip.write(body);
        }
        return gzipped.toByteArray();
    }

    /// A stand-in for the instance's own server, answering the way OpenCode
    /// 1.19.5 answers its assets: `Transfer-Encoding: chunked`, **no
    /// content-length**. The head is written by hand so the absence of a length
    /// is the test's own doing, and the body goes out in two chunks so the proxy's
    /// read sees a body that arrives in pieces.
    private static final class ChunkedUpstream implements AutoCloseable {

        private final ServerSocket listen;
        private final byte[] body;
        private final String encoding;
        private final List<String> requests = new CopyOnWriteArrayList<>();

        ChunkedUpstream(byte[] body, String encoding) throws IOException {
            this.body = body;
            this.encoding = encoding;
            this.listen = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"));
            Thread accept = new Thread(this::acceptLoop, "chunked-upstream");
            accept.setDaemon(true);
            accept.start();
        }

        int port() {
            return listen.getLocalPort();
        }

        /// The request lines that arrived, for an assertion message that says what
        /// the instance was asked for.
        String log() {
            return String.join(" | ", requests);
        }

        private void acceptLoop() {
            try {
                while (true) {
                    Socket socket = listen.accept();
                    Thread answer = new Thread(() -> answer(socket), "chunked-upstream-answer");
                    answer.setDaemon(true);
                    answer.start();
                }
            } catch (IOException e) {
                // The test closed the socket; the accept loop is over.
            }
        }

        private void answer(Socket socket) {
            try (socket) {
                socket.setSoTimeout(30_000);
                InputStream in = socket.getInputStream();
                StringBuilder head = new StringBuilder();
                int read;
                while ((read = in.read()) >= 0) {
                    head.append((char) read);
                    if (head.length() >= 4 && head.substring(head.length() - 4).equals("\r\n\r\n")) {
                        break;
                    }
                }
                if (head.isEmpty()) {
                    return;
                }
                requests.add(head.substring(0, head.indexOf("\r\n")));

                OutputStream out = socket.getOutputStream();
                out.write(("HTTP/1.1 200 OK\r\n"
                        + "Content-Type: text/javascript\r\n"
                        + (encoding == null ? "" : "Content-Encoding: " + encoding + "\r\n")
                        // No Content-Length, deliberately: this is the shape that
                        // used to switch the asset patch off.
                        + "Transfer-Encoding: chunked\r\n"
                        + "Connection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                int half = body.length / 2;
                writeChunk(out, 0, half);
                writeChunk(out, half, body.length - half);
                out.write("0\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                out.flush();
            } catch (IOException e) {
                // The proxy hung up; there is nothing left to answer.
            }
        }

        private void writeChunk(OutputStream out, int at, int length) throws IOException {
            if (length <= 0) {
                return;
            }
            out.write((Integer.toHexString(length) + "\r\n").getBytes(StandardCharsets.US_ASCII));
            out.write(body, at, length);
            out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
        }

        @Override
        public void close() throws IOException {
            listen.close();
        }
    }
}
