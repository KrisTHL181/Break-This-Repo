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
package org.jackhuang.hmcl.web.plain;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.util.platform.Executables;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/// Server-side markdown for the legacy surface, rendered by the **panel's own
/// frontend renderer**.
///
/// `web/scripts/render-markdown.mjs` is bundled at build time into
/// `/web-legacy/ssr/render-markdown.cjs` and runs react-markdown with the same
/// plugin chain and the same sanitize schema as the SPA's `Markdown.tsx`, so a
/// transcript read without JavaScript carries exactly the markup a modern
/// browser shows. Rendering is expensive (a Node process), so it happens on a
/// bounded background pool and the HTML is cached under `<data>/plain-md/`,
/// keyed by message id and the hash of the content — a regenerate or an edit
/// changes the hash and therefore misses the cache.
///
/// Rendering never blocks a request: [render] answers the cache and schedules a
/// miss, and [prefetch] is called when a turn finishes so the transcript is
/// usually ready before anyone opens it.
///
/// Every failure mode degrades to `null`, and the caller then renders the
/// escaped `<pre>` the surface always had: no Node on a bare `java -jar`
/// install, a missing bundle, a renderer crash, a timeout. The legacy surface
/// must never lose the message because its pretty rendering failed.
@NotNullByDefault
public final class MarkdownSsr {

    private static final org.jackhuang.hmcl.util.logging.Logger LOG =
            org.jackhuang.hmcl.util.logging.Logger.LOG;

    /// The bundled renderer, as shipped inside the jar.
    private static final String RESOURCE = "/web-legacy/ssr/render-markdown.cjs";

    /// A renderer that cannot answer in this time is killed and treated as absent.
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /// A rendered fragment larger than this is not a chat message.
    private static final long MAX_OUTPUT_BYTES = 4L * 1024 * 1024;

    /// Cached fragments beyond this count prune the oldest files.
    private static final int MAX_CACHE_FILES = 2000;

    /// A few Node processes at a time: enough to fill a transcript without
    /// letting one long conversation start a process per message.
    private static final int DEFAULT_WORKERS = 4;
    private static final int MAX_WORKERS = 16;

    /// Running + queued render tasks. A request that cannot get a slot skips
    /// the prefetch; the next poll tries again, so a burst never grows an
    /// unbounded queue of message contents.
    private static final int MAX_TASKS = 32;

    /// After a renderer failure (crash, timeout, oversize), skip the same
    /// message for this long. Without it every 500 ms poll would respawn a
    /// process for a permanently broken renderer.
    private static final Duration FAILURE_COOLDOWN = Duration.ofMinutes(5);

    private final Path dataDir;
    private final Path cacheDir;
    private final int workerCount;
    private final AtomicReference<Optional<Path>> node = new AtomicReference<>();
    private final AtomicReference<Optional<Path>> script = new AtomicReference<>();
    private final Set<String> pending = ConcurrentHashMap.newKeySet();
    private final java.util.Map<String, Long> failed = new ConcurrentHashMap<>();
    private final Semaphore slots = new Semaphore(MAX_TASKS);
    private final AtomicBoolean nodeLogged = new AtomicBoolean();
    /// Created on first use: most servers never enable SSR, and an eager pair of
    /// pools per HdslServer would leave threads behind in every test and reload.
    private volatile @Nullable ExecutorService workers;
    private volatile @Nullable ScheduledExecutorService watchdog;

    public MarkdownSsr(Path dataDir, Path cacheDir) {
        this(dataDir, cacheDir, DEFAULT_WORKERS);
    }

    public MarkdownSsr(Path dataDir, Path cacheDir, int workerCount) {
        this.dataDir = dataDir;
        this.cacheDir = cacheDir;
        this.workerCount = Math.max(1, Math.min(MAX_WORKERS, workerCount));
    }

    /// Whether server-side rendering can run at all: a Node executable and the
    /// bundled renderer. The settings page exposes this so a silent `<pre>`
    /// fallback is visible instead of surprising.
    public boolean available() {
        return nodeExecutable() != null && scriptFile() != null;
    }

    /// The rendered HTML for one finalized message if it is already cached, or
    /// `null` after scheduling the miss. Never blocks on Node.
    ///
    /// @param messageId  the stored message id, used in the cache file name
    /// @param content    the stored markdown text
    /// @param sourcesJson the message's search hits (`[{index,url,…}]`), or
    ///                    `null`; without them the renderer cannot turn
    ///                    `[citation:N]` markers into links
    /// @return the rendered fragment, or `null`
    public @Nullable String render(String messageId, String content,
                                   @Nullable String sourcesJson) {
        if (content == null || content.isBlank()) {
            return null;
        }
        Path cache = cacheFile(messageId, content, sourcesJson);
        if (cache == null) {
            return null;
        }
        try {
            if (Files.isRegularFile(cache)) {
                return Files.readString(cache, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            LOG.warning("Plain markdown cache failed for " + messageId, e);
        }
        prefetch(messageId, content, sourcesJson);
        return null;
    }

    /// Renders in the background unless the fragment is cached or already being
    /// rendered. Safe to call from a turn-finish callback and from a request.
    public void prefetch(String messageId, String content, @Nullable String sourcesJson) {
        if (content == null || content.isBlank() || !available()) {
            return;
        }
        Path cache = cacheFile(messageId, content, sourcesJson);
        if (cache == null || Files.isRegularFile(cache)) {
            return;
        }
        String key = cache.getFileName().toString();
        Long failedAt = failed.get(key);
        if (failedAt != null && System.currentTimeMillis() - failedAt < FAILURE_COOLDOWN.toMillis()) {
            return;
        }
        if (!slots.tryAcquire()) {
            return;
        }
        if (!pending.add(key)) {
            slots.release();
            return;
        }
        workers().submit(() -> {
            try {
                if (Files.isRegularFile(cache)) {
                    return;
                }
                String rendered = invoke(content, sourcesJson);
                if (rendered != null) {
                    writeCache(cache, rendered);
                    failed.remove(key);
                } else {
                    failed.put(key, System.currentTimeMillis());
                }
            } catch (IOException e) {
                failed.put(key, System.currentTimeMillis());
                LOG.warning("Plain markdown cache failed for " + messageId, e);
            } finally {
                pending.remove(key);
                slots.release();
            }
        });
    }

    /// `<cacheDir>/<messageId>-<sha256(content)>.html`, or `null` when the id
    /// carries characters that do not belong in a file name.
    private @Nullable Path cacheFile(String messageId, String content,
                                    @Nullable String sourcesJson) {
        if (messageId == null || !messageId.matches("[A-Za-z0-9_-]+")) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Sources are part of the key: the same text with citations and
            // without them renders differently.
            String material = content + "\u0000" + (sourcesJson == null ? "" : sourcesJson);
            String hash = HexFormat.of().formatHex(digest.digest(material.getBytes(StandardCharsets.UTF_8)));
            return cacheDir.resolve(messageId + "-" + hash + ".html");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /// Runs one renderer process with a hard deadline.
    ///
    /// The watchdog kills the process if it is still alive after [TIMEOUT]; that
    /// closes both pipes, so a renderer that hangs before writing anything can
    /// no longer pin the worker (or the request that scheduled it).
    private @Nullable String invoke(String content, @Nullable String sourcesJson) {
        Path nodeExecutable = nodeExecutable();
        Path scriptFile = scriptFile();
        if (nodeExecutable == null || scriptFile == null) {
            return null;
        }
        Process process = null;
        AtomicBoolean timedOut = new AtomicBoolean();
        try {
            process = new ProcessBuilder(nodeExecutable.toString(), scriptFile.toString())
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            Process running = process;
            ScheduledFuture<?> killer = watchdog().schedule(() -> {
                timedOut.set(true);
                running.destroyForcibly();
            }, TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            try {
                try (OutputStream stdin = process.getOutputStream()) {
                    stdin.write(envelope(content, sourcesJson).getBytes(StandardCharsets.UTF_8));
                }
                byte[] stdout;
                try (InputStream out = process.getInputStream()) {
                    stdout = out.readNBytes((int) (MAX_OUTPUT_BYTES + 1));
                }
                int exit = process.waitFor();
                if (timedOut.get()) {
                    LOG.warning("Plain markdown renderer timed out");
                    return null;
                }
                if (exit != 0 || stdout.length > MAX_OUTPUT_BYTES) {
                    LOG.warning("Plain markdown renderer answered " + exit);
                    return null;
                }
                String rendered = new String(stdout, StandardCharsets.UTF_8);
                return rendered.isBlank() ? null : rendered;
            } finally {
                killer.cancel(false);
            }
        } catch (IOException e) {
            LOG.warning("Plain markdown renderer could not run", e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /// The renderer's stdin: one JSON object, because citations need the hits
    /// the SPA receives as `message.sources`.
    static String envelope(String content, @Nullable String sourcesJson) {
        JsonObject envelope = new JsonObject();
        envelope.addProperty("content", content);
        if (sourcesJson != null && !sourcesJson.isBlank()) {
            try {
                JsonElement parsed = JsonParser.parseString(sourcesJson);
                if (parsed.isJsonArray()) {
                    envelope.add("sources", parsed);
                }
            } catch (RuntimeException ignored) {
                // A broken sources blob must not cost the message its render.
            }
        }
        return envelope.toString();
    }

    private ExecutorService workers() {
        ExecutorService current = workers;
        if (current == null) {
            synchronized (this) {
                current = workers;
                if (current == null) {
                    current = Executors.newFixedThreadPool(workerCount, runnable -> {
                        Thread thread = new Thread(runnable, "plain-markdown");
                        thread.setDaemon(true);
                        return thread;
                    });
                    workers = current;
                }
            }
        }
        return current;
    }

    private ScheduledExecutorService watchdog() {
        ScheduledExecutorService current = watchdog;
        if (current == null) {
            synchronized (this) {
                current = watchdog;
                if (current == null) {
                    current = Executors.newSingleThreadScheduledExecutor(runnable -> {
                        Thread thread = new Thread(runnable, "plain-markdown-watchdog");
                        thread.setDaemon(true);
                        return thread;
                    });
                    watchdog = current;
                }
            }
        }
        return current;
    }

    /// The panel's managed Node first: a bare Windows deployment has one under
    /// `<data>/hdsl/runtimes` and no `node` on PATH. PATH is the fallback.
    private @Nullable Path nodeExecutable() {
        Optional<Path> resolved = node.get();
        if (resolved == null) {
            resolved = Optional.ofNullable(resolveNode());
            node.set(resolved);
        }
        return resolved.orElse(null);
    }

    private @Nullable Path resolveNode() {
        Path managed = managedNode(dataDir);
        if (managed != null) {
            logNodeOnce("managed runtime " + managed);
            return managed;
        }
        Optional<Path> onPath = Executables.resolve("node");
        if (onPath.isPresent()) {
            logNodeOnce("PATH " + onPath.get());
            return onPath.get();
        }
        logNodeOnce("none");
        return null;
    }

    private void logNodeOnce(String source) {
        if (nodeLogged.compareAndSet(false, true)) {
            LOG.info("Plain markdown Node: " + source);
        }
    }

    /// The newest managed Node under `<data>/hdsl/runtimes`, or `null`.
    ///
    /// The directory is the panel's own (`NodeRuntimeManager`), so this is the
    /// executable the dsh instances already use. Newest by mtime: version
    /// directory names are not a reliable sort with prereleases.
    static @Nullable Path managedNode(Path dataDir) {
        Path runtimes = dataDir.resolve("hdsl").resolve("runtimes");
        if (!Files.isDirectory(runtimes)) {
            return null;
        }
        String executable = File.separatorChar == '\\' ? "node.exe" : "node";
        try (Stream<Path> versions = Files.list(runtimes)) {
            return versions
                    .map(version -> version.resolve(executable))
                    .filter(Files::isRegularFile)
                    .filter(path -> File.separatorChar == '\\' || Files.isExecutable(path))
                    .max(Comparator.comparingLong(MarkdownSsr::modified))
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private static long modified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    /// The bundled renderer unpacked beside the cache, refreshed when the jar
    /// ships a different build of it.
    private @Nullable Path scriptFile() {
        Optional<Path> current = script.get();
        if (current != null) {
            return current.orElse(null);
        }
        Optional<Path> unpacked = Optional.ofNullable(unpackScript());
        script.set(unpacked);
        return unpacked.orElse(null);
    }

    private @Nullable Path unpackScript() {
        try (InputStream resource = MarkdownSsr.class.getResourceAsStream(RESOURCE)) {
            if (resource == null) {
                LOG.warning("Plain markdown renderer is not in this jar");
                return null;
            }
            byte[] bundle = resource.readAllBytes();
            Files.createDirectories(cacheDir);
            Path target = cacheDir.resolve("render-markdown.cjs");
            if (Files.isRegularFile(target) && Files.size(target) == bundle.length) {
                return target;
            }
            Path staging = cacheDir.resolve("render-markdown.cjs.tmp");
            Files.write(staging, bundle);
            try {
                Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        } catch (IOException e) {
            LOG.warning("Plain markdown renderer could not be unpacked", e);
            return null;
        }
    }

    private void writeCache(Path cache, String rendered) throws IOException {
        Files.createDirectories(cacheDir);
        Path staging = cacheDir.resolve(cache.getFileName() + ".tmp");
        Files.write(staging, rendered.getBytes(StandardCharsets.UTF_8));
        try {
            Files.move(staging, cache, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(staging, cache, StandardCopyOption.REPLACE_EXISTING);
        }
        prune();
    }

    /// Bounds the cache: beyond [MAX_CACHE_FILES] the oldest fragments go.
    private void prune() {
        try (Stream<Path> files = Files.list(cacheDir)) {
            var cached = files.filter(path -> path.getFileName().toString().endsWith(".html"))
                    .sorted(Comparator.comparingLong(MarkdownSsr::modified))
                    .toList();
            for (int i = 0; i + MAX_CACHE_FILES < cached.size(); i++) {
                Files.deleteIfExists(cached.get(i));
            }
        } catch (IOException e) {
            // A cache that cannot be pruned is still a cache; the next write tries again.
        }
    }

}