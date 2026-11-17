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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.Metadata;
import org.jackhuang.hmcl.web.audit.AuditLog;
import org.jackhuang.hmcl.web.auth.AuthFilter;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.config.ServerConfigWriter;
import org.jackhuang.hmcl.web.developer.DeveloperAuth;
import org.jackhuang.hmcl.web.developer.DeveloperElevation;
import org.jackhuang.hmcl.web.http.Json;
import org.jackhuang.hmcl.web.http.RequestSecurity;
import org.jackhuang.hmcl.web.transcode.media.MediaBrowser;
import org.jackhuang.hmcl.web.transcode.preset.PresetSchema;
import org.jackhuang.hmcl.web.transcode.preset.PresetStore;
import org.jackhuang.hmcl.web.transcode.preset.PresetTransfer;
import org.jackhuang.hmcl.web.transcode.preset.TranscodePreset;
import org.jackhuang.hmcl.web.transcode.runtime.FfmpegProbe;
import org.jackhuang.hmcl.web.transcode.runtime.HardwareProbe;
import org.jackhuang.hmcl.web.transcode.command.CommandBuilder;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

/// The transcode feature's own REST surface. At this stage it only owns the
/// enable flow; queue, presets, media and tools are added by their own commits.
///
/// The gate in front of this servlet is [TranscodeFeatureFilter]; while the
/// feature is disabled only the routes below are reachable, and everything else
/// is answered 404 by the filter.
@NotNullByDefault
public final class TranscodeApiServlet extends HttpServlet {

    private final TranscodeModule module;
    private final ServerConfig config;
    private final TranscodeFeatureState feature;
    private final AuthService auth;
    private final DeveloperAuth developerAuth;
    private final DeveloperElevation elevation;
    private final BooleanSupplier tlsActive;

    public TranscodeApiServlet(TranscodeModule module,
                               AuthService auth, DeveloperAuth developerAuth,
                               DeveloperElevation elevation, BooleanSupplier tlsActive) {
        this.module = module;
        this.config = module.config();
        this.feature = module.feature();
        this.auth = auth;
        this.developerAuth = developerAuth;
        this.elevation = elevation;
        this.tlsActive = tlsActive;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = path(request);
        if ("/feature".equals(path)) {
            featureJson(response);
            return;
        }
        if ("/terms".equals(path)) {
            termsJson(response);
            return;
        }
        if ("/capabilities".equals(path)) {
            capabilities(response);
            return;
        }
        if ("/media/list".equals(path)) {
            mediaList(request, response);
            return;
        }
        if ("/media/probe".equals(path)) {
            mediaProbe(request, response);
            return;
        }
        if ("/media/scan".equals(path)) {
            mediaScan(request, response);
            return;
        }
        if ("/presets".equals(path)) {
            presets(response);
            return;
        }
        if ("/presets/schema".equals(path)) {
            Json.write(response, PresetSchema.json(module.runtime().info().encoders()));
            return;
        }
        if (path.startsWith("/presets/")) {
            preset(request, response, path.substring("/presets/".length()));
            return;
        }
        if ("/queue".equals(path)) {
            Json.write(response, module.queue().snapshot());
            return;
        }
        if (path.startsWith("/queue/jobs/")) {
            queueJobGet(request, response, path.substring("/queue/jobs/".length()));
            return;
        }
        if (path.startsWith("/tools/quality/")) {
            qualityGet(request, response, path.substring("/tools/quality/".length()));
            return;
        }
        if (path.startsWith("/runtime/install/")) {
            runtimeGet(response, path.substring("/runtime/install/".length()));
            return;
        }
        Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = path(request);
        switch (path) {
            case "/feature/enable" -> enable(request, response);
            case "/feature/decline" -> decline(request, response);
            case "/feature/disable" -> disable(request, response);
            case "/presets" -> savePreset(request, response);
            case "/preview" -> preview(request, response);
            case "/plan" -> plan(request, response);
            case "/presets/import" -> importPreset(request, response);
            case "/queue/start" -> {
                module.queue().startQueue();
                Json.write(response, module.queue().snapshot());
            }
            case "/queue/pause" -> {
                module.queue().pauseQueue();
                Json.write(response, module.queue().snapshot());
            }
            case "/queue/stop" -> {
                module.queue().stopAll();
                Json.write(response, module.queue().snapshot());
            }
            case "/queue/clear" -> {
                int removed = module.queue().clearTerminal();
                audit("transcode.queue.clear", Integer.toString(removed), "success", request);
                Json.write(response, module.queue().snapshot());
            }
            case "/queue/action" -> queueBatchAction(request, response);
            case "/queue/reorder" -> queueReorder(request, response);
            case "/queue/sync-preset" -> queueSyncPreset(request, response);
            case "/queue/jobs" -> submitJob(request, response);
            case "/tools/merge" -> mergeTool(request, response);
            case "/tools/mux" -> muxTool(request, response);
            case "/tools/extract" -> extractTool(request, response);
            case "/tools/quality" -> qualityTool(request, response);
            case "/runtime/install" -> runtimeInstall(request, response);
            case "/capabilities/probe" -> capabilityProbe(request, response);
            default -> {
                if (path.startsWith("/queue/jobs/")) {
                    queueJobAction(request, response, path.substring("/queue/jobs/".length()));
                } else if (path.startsWith("/runtime/install/") && path.endsWith("/cancel")) {
                    String rest = path.substring("/runtime/install/".length(),
                            path.length() - "/cancel".length());
                    runtimeCancel(response, rest);
                } else if (path.startsWith("/tools/quality/") && path.endsWith("/cancel")) {
                    String rest = path.substring("/tools/quality/".length(),
                            path.length() - "/cancel".length());
                    qualityCancel(response, rest);
                } else {
                    Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
                }
            }
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = path(request);
        if (!"/settings".equals(path)) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        String policy = stringField(body, "failurePolicy");
        if (policy == null || !List.of("ask", "timeout-auto", "full-auto").contains(policy)) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "failurePolicy must be ask, timeout-auto or full-auto");
            return;
        }
        int timeout = intField(body, "failureTimeoutMinutes");
        if (body.has("failureTimeoutMinutes") && (timeout < 1 || timeout > 1440)) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "failureTimeoutMinutes must be between 1 and 1440");
            return;
        }
        try {
            ServerConfigWriter.setTranscodeFailurePolicy(config.dataDir, policy,
                    body.has("failureTimeoutMinutes") ? timeout : config.transcode.failureTimeoutMinutes);
            config.transcode.failurePolicy = policy;
            if (body.has("failureTimeoutMinutes")) {
                config.transcode.failureTimeoutMinutes = timeout;
            }
            audit("transcode.settings.update", policy + "/"
                    + config.transcode.failureTimeoutMinutes, "success", request);
            featureJson(response);
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = path(request);
        if (path.startsWith("/queue/jobs/")) {
            String id = path.substring("/queue/jobs/".length());
            if (id.isBlank() || id.contains("/")) {
                Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
                return;
            }
            if (!module.queue().remove(id)) {
                Json.error(response, HttpServletResponse.SC_CONFLICT,
                        "the job is not in a state that allows this action");
                return;
            }
            audit("transcode.queue.remove", id, "success", request);
            Json.write(response, module.queue().snapshot());
            return;
        }
        if (!path.startsWith("/presets/")) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }
        String id = path.substring("/presets/".length());
        try {
            if (!module.presets().delete(id)) {
                Json.error(response, HttpServletResponse.SC_NOT_FOUND, "preset not found or read-only");
                return;
            }
            JsonObject body = new JsonObject();
            body.addProperty("ok", true);
            Json.write(response, body);
        } catch (IllegalArgumentException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private static String path(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private void featureJson(HttpServletResponse response) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("state", feature.state().name());
        body.addProperty("enabled", feature.enabled());
        body.addProperty("termsVersion", TranscodeTerms.VERSION);
        body.addProperty("termsTextHash", TranscodeTerms.hash());
        Path media = TranscodePaths.mediaRoot(config);
        body.addProperty("mediaRoot", media.toString());
        body.addProperty("mediaRootExists", Files.isDirectory(media));
        body.addProperty("mediaRootWritable", Files.isDirectory(media) && Files.isWritable(media));
        body.addProperty("maxConcurrent", config.transcode.maxConcurrent);
        body.addProperty("maxQueue", config.transcode.maxQueue);
        body.addProperty("failurePolicy", config.transcode.failurePolicy);
        body.addProperty("failureTimeoutMinutes", config.transcode.failureTimeoutMinutes);
        body.addProperty("autoResume", config.transcode.autoResume);
        feature.consent().ifPresent(record -> {
            JsonObject consent = new JsonObject();
            consent.addProperty("termsVersion", record.termsVersion());
            consent.addProperty("termsTextHash", record.termsTextHash());
            consent.addProperty("acceptedAt", record.acceptedAt());
            consent.addProperty("acceptedBy", record.acceptedBy());
            consent.addProperty("acceptedFrom", record.acceptedFrom());
            consent.addProperty("ackMethod", record.ackMethod());
            consent.addProperty("uiVersion", record.uiVersion());
            consent.addProperty("clientUa", record.clientUa());
            consent.addProperty("buildCommit", record.buildCommit());
            consent.addProperty("sawDeclineOption", record.sawDeclineOption());
            body.add("consent", consent);
        });
        Json.write(response, body);
    }

    private void termsJson(HttpServletResponse response) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("version", TranscodeTerms.VERSION);
        body.addProperty("text", TranscodeTerms.text());
        body.addProperty("hash", TranscodeTerms.hash());
        Json.write(response, body);
    }

    private void capabilities(HttpServletResponse response) throws IOException {
        FfmpegProbe.FfmpegInfo info = module.runtime().info();
        JsonObject body = new JsonObject();
        body.addProperty("available", info.available());
        body.addProperty("version", info.version());
        body.addProperty("ffmpeg", info.ffmpeg() == null ? null : info.ffmpeg().toString());
        body.addProperty("ffprobe", info.ffprobe() == null ? null : info.ffprobe().toString());
        body.addProperty("error", info.error());
        body.addProperty("probedAt", info.probedAt());
        JsonArray encoders = new JsonArray();
        info.encoders().forEach(encoders::add);
        body.add("encoders", encoders);
        JsonArray hwaccels = new JsonArray();
        info.hwaccels().forEach(hwaccels::add);
        body.add("hwaccels", hwaccels);
        JsonArray filters = new JsonArray();
        info.filters().forEach(filters::add);
        body.add("filters", filters);
        Path media = module.mediaRoot();
        body.addProperty("mediaRoot", media.toString());
        body.addProperty("mediaRootExists", Files.isDirectory(media));
        body.addProperty("mediaRootWritable", Files.isDirectory(media) && Files.isWritable(media));
        body.addProperty("maxConcurrent", config.transcode.maxConcurrent);
        body.addProperty("maxQueue", config.transcode.maxQueue);
        Json.write(response, body);
    }

    private void mediaList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String relative = request.getParameter("path");
        int page = intParameter(request, "page", 0);
        int pageSize = intParameter(request, "pageSize", 200);
        try {
            MediaBrowser.Listing listing = module.media().list(module.mediaRoot(), relative, page, pageSize);
            Json.write(response, listing);
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void mediaProbe(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String relative = request.getParameter("path");
        if (relative == null || relative.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "path is required");
            return;
        }
        try {
            Json.write(response, module.media().probe(module.mediaRoot(), relative));
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "probe interrupted");
        }
    }

    private void mediaScan(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String relative = request.getParameter("path");
        boolean recursive = !"0".equals(request.getParameter("recursive"));
        int limit = intParameter(request, "limit", 2000);
        try {
            Json.write(response, module.media().scan(module.mediaRoot(), relative, recursive, limit));
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private static int intParameter(HttpServletRequest request, String name, int fallback) {
        String value = request.getParameter(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void presets(HttpServletResponse response) throws IOException {
        JsonArray array = new JsonArray();
        for (PresetStore.PresetEntry entry : module.presets().list()) {
            JsonObject item = new JsonObject();
            item.addProperty("id", entry.id());
            item.addProperty("builtin", entry.builtin());
            item.addProperty("name", entry.preset().name);
            item.addProperty("description", entry.preset().description);
            array.add(item);
        }
        JsonObject body = new JsonObject();
        body.add("presets", array);
        Json.write(response, body);
    }

    private void preset(HttpServletRequest request, HttpServletResponse response, String id) throws IOException {
        if (id.endsWith("/export")) {
            exportPreset(response, id.substring(0, id.length() - "/export".length()),
                    request.getParameter("format"));
            return;
        }
        var entry = module.presets().get(id);
        if (entry.isEmpty()) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "preset not found");
            return;
        }
        Json.write(response, entry.get());
    }

    private void exportPreset(HttpServletResponse response, String id, String format) throws IOException {
        var entry = module.presets().get(id);
        if (entry.isEmpty()) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "preset not found");
            return;
        }
        if ("3fui".equalsIgnoreCase(format)) {
            Json.write(response, PresetTransfer.to3fui(entry.get().preset()));
        } else {
            Json.write(response, entry.get().preset());
        }
    }

    private void importPreset(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        JsonObject data = body.has("data") && body.get("data").isJsonObject()
                ? body.getAsJsonObject("data") : body;
        String id = stringField(body, "id");
        if (id == null || id.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "id is required");
            return;
        }
        try {
            TranscodePreset preset = PresetTransfer.looksLike3fui(data)
                    ? PresetTransfer.from3fui(data)
                    : Json.GSON.fromJson(data, TranscodePreset.class);
            if (preset == null) {
                throw new IllegalArgumentException("preset is not an object");
            }
            String name = stringField(body, "name");
            if (name != null && !name.isBlank()) {
                preset.name = name;
            }
            PresetStore.PresetEntry saved = module.presets().save(id.trim(), preset);
            audit("transcode.preset.import", id.trim(), "success", request);
            Json.write(response, saved);
        } catch (IllegalArgumentException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void savePreset(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        String id = stringField(body, "id");
        if (id == null || id.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "id is required");
            return;
        }
        if (!body.has("preset") || !body.get("preset").isJsonObject()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "preset is required");
            return;
        }
        try {
            TranscodePreset preset = Json.GSON.fromJson(body.getAsJsonObject("preset"), TranscodePreset.class);
            PresetStore.PresetEntry saved = module.presets().save(id.trim(), preset);
            audit("transcode.preset.save", id.trim(), "success", request);
            Json.write(response, saved);
        } catch (IllegalArgumentException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private void preview(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        String inputPath = stringField(body, "input");
        boolean placeholder = inputPath == null || inputPath.isBlank();
        try {
            TranscodePreset preset = resolvePreset(body);
            preset.validate();
            Path input;
            Path output;
            Path chapters = null;
            Path subtitle = null;
            if (placeholder) {
                // The parameter panel asks for a command before a file is chosen: the
                // argv is what matters, so build it with the same placeholders 3FUI shows.
                input = Path.of("<输入文件>");
                output = Path.of("<输出文件>");
            } else {
                input = module.media().resolve(module.mediaRoot(), inputPath);
                String requestedDir = stringField(body, "outputDir");
                String presetDir = preset.outputDir;
                Path outputDir;
                if (requestedDir != null && !requestedDir.isBlank()) {
                    outputDir = module.media().resolveForWrite(module.mediaRoot(), requestedDir);
                } else if (presetDir != null && !presetDir.isBlank()) {
                    outputDir = module.media().resolveForWrite(module.mediaRoot(), presetDir);
                } else {
                    outputDir = input.getParent();
                }
                output = CommandBuilder.outputPath(preset, input, outputDir);
                chapters = preset.chaptersFile == null || preset.chaptersFile.isBlank()
                        ? null : module.media().resolve(module.mediaRoot(), preset.chaptersFile);
                subtitle = preset.subtitleFile == null || preset.subtitleFile.isBlank()
                        ? null : module.media().resolve(module.mediaRoot(), preset.subtitleFile);
            }
            List<List<String>> passes = CommandBuilder.buildPasses(preset, input, output, chapters, subtitle);
            List<String> args = passes.get(passes.size() - 1);
            JsonArray array = new JsonArray();
            args.forEach(array::add);
            JsonObject result = new JsonObject();
            result.addProperty("input", placeholder ? "<输入文件>" : input.toString());
            result.addProperty("output", placeholder ? "<输出文件>" : output.toString());
            result.add("args", array);
            result.addProperty("command", passes.size() == 1
                    ? CommandBuilder.display(args)
                    : "# 第一遍（分析）\n" + CommandBuilder.display(passes.get(0))
                            + "\n\n# 第二遍（出片）\n" + CommandBuilder.display(passes.get(1)));
            Json.write(response, result);
        } catch (IllegalArgumentException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    /// Plans output paths for a batch without submitting anything. The preparation list
    /// uses it to show the exact file the server will write, including collision suffixes.
    private void plan(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        try {
            TranscodePreset preset = resolvePreset(body);
            preset.validate();
            Path mediaRoot = module.mediaRoot();
            String requestedDir = stringField(body, "outputDir");
            Path outputDir = requestedDir == null || requestedDir.isBlank()
                    ? null : module.media().resolveForWrite(mediaRoot, requestedDir);
            Set<Path> reserved = new LinkedHashSet<>();
            JsonArray items = new JsonArray();
            JsonArray inputs = body.has("inputs") && body.get("inputs").isJsonArray()
                    ? body.getAsJsonArray("inputs") : new JsonArray();
            for (var element : inputs) {
                if (!element.isJsonPrimitive()) {
                    continue;
                }
                String relative = element.getAsString();
                JsonObject item = new JsonObject();
                item.addProperty("input", relative);
                try {
                    Path input = module.media().resolve(mediaRoot, relative);
                    Path directory = outputDir == null ? input.getParent() : outputDir;
                    Path output = CommandBuilder.outputPath(preset, input, directory, reserved);
                    reserved.add(output);
                    item.addProperty("inputAbsolute", input.toString());
                    item.addProperty("output", mediaRoot.relativize(output).toString().replace('\\', '/'));
                    item.addProperty("outputAbsolute", output.toString());
                    item.addProperty("exists", Files.exists(output));
                } catch (IOException | IllegalArgumentException e) {
                    item.addProperty("error", e.getMessage());
                }
                items.add(item);
            }
            JsonObject result = new JsonObject();
            result.addProperty("mediaRoot", mediaRoot.toString());
            result.addProperty("outputDir", outputDir == null ? "" : outputDir.toString());
            result.add("items", items);
            Json.write(response, result);
        } catch (IllegalArgumentException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private TranscodePreset resolvePreset(JsonObject body) {
        String presetId = stringField(body, "presetId");
        if (presetId != null && !presetId.isBlank()) {
            return module.presets().get(presetId)
                    .orElseThrow(() -> new IllegalArgumentException("preset not found: " + presetId))
                    .preset();
        }
        if (body.has("preset") && body.get("preset").isJsonObject()) {
            TranscodePreset preset = Json.GSON.fromJson(body.getAsJsonObject("preset"), TranscodePreset.class);
            if (preset == null) {
                throw new IllegalArgumentException("preset is not an object");
            }
            return preset;
        }
        throw new IllegalArgumentException("presetId or preset is required");
    }

    private void mergeTool(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        try {
            JsonObject job = module.tools().merge(
                    stringArray(body, "inputs"), stringField(body, "output"));
            audit("transcode.tool.merge", job.get("id").getAsString(), "success", request);
            Json.write(response, job);
        } catch (IOException | IllegalArgumentException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void muxTool(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        try {
            JsonObject job = module.tools().mux(
                    stringArray(body, "inputs"), stringArray(body, "maps"), stringField(body, "output"));
            audit("transcode.tool.mux", job.get("id").getAsString(), "success", request);
            Json.write(response, job);
        } catch (IOException | IllegalArgumentException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void extractTool(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        try {
            JsonObject job = module.tools().extract(
                    stringField(body, "input"),
                    stringField(body, "type"),
                    intField(body, "index"),
                    stringField(body, "output"));
            audit("transcode.tool.extract", job.get("id").getAsString(), "success", request);
            Json.write(response, job);
        } catch (IOException | IllegalArgumentException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private static java.util.List<String> stringArray(JsonObject body, String name) {
        if (!body.has(name) || !body.get(name).isJsonArray()) {
            return java.util.List.of();
        }
        java.util.List<String> result = new java.util.ArrayList<>();
        for (var element : body.getAsJsonArray(name)) {
            if (element.isJsonPrimitive()) {
                result.add(element.getAsString());
            }
        }
        return result;
    }

    private void capabilityProbe(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = readJson(request);
        String encoder = body == null ? null : stringField(body, "encoder");
        if (encoder == null || encoder.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "encoder is required");
            return;
        }
        Json.write(response, HardwareProbe.probe(module, encoder));
    }

    private void runtimeInstall(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        try {
            var job = module.installer().start(
                    stringField(body, "kind"),
                    stringField(body, "url"),
                    stringField(body, "sha256"),
                    stringField(body, "version"));
            audit("transcode.runtime.install", job.kind, "success", request);
            Json.write(response, job);
        } catch (IOException | IllegalArgumentException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void runtimeGet(HttpServletResponse response, String id) throws IOException {
        var job = module.installer().job(id);
        if (job.isEmpty()) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "install job not found");
            return;
        }
        Json.write(response, job.get());
    }

    private void runtimeCancel(HttpServletResponse response, String id) throws IOException {
        var job = module.installer().job(id);
        if (job.isEmpty()) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "install job not found");
            return;
        }
        module.installer().cancel(id);
        Json.write(response, job.get());
    }

    private void qualityTool(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        try {
            var job = module.quality().start(
                    stringField(body, "reference"),
                    stringField(body, "encoded"),
                    stringArray(body, "metrics"),
                    stringField(body, "start"),
                    stringField(body, "duration"));
            audit("transcode.tool.quality", job.id, "success", request);
            Json.write(response, job);
        } catch (IOException | IllegalArgumentException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void qualityGet(HttpServletRequest request, HttpServletResponse response, String rest)
            throws IOException {
        var job = module.quality().job(rest);
        if (job.isEmpty()) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "quality job not found");
            return;
        }
        Json.write(response, job.get());
    }

    private void qualityCancel(HttpServletResponse response, String id) throws IOException {
        var job = module.quality().job(id);
        if (job.isEmpty()) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "quality job not found");
            return;
        }
        module.quality().cancel(id);
        Json.write(response, job.get());
    }

    private void queueJobGet(HttpServletRequest request, HttpServletResponse response, String rest)
            throws IOException {
        String[] parts = rest.split("/");
        if (parts.length == 1) {
            var job = module.queue().jobJson(parts[0]);
            if (job.isEmpty()) {
                Json.error(response, HttpServletResponse.SC_NOT_FOUND, "job not found");
                return;
            }
            Json.write(response, job.get());
            return;
        }
        if (parts.length == 2 && "log".equals(parts[1])) {
            if (module.queue().jobJson(parts[0]).isEmpty()) {
                Json.error(response, HttpServletResponse.SC_NOT_FOUND, "job not found");
                return;
            }
            int tail = intParameter(request, "tail", 500);
            JsonObject body = new JsonObject();
            body.addProperty("id", parts[0]);
            body.addProperty("log", module.queue().log(parts[0], tail));
            Json.write(response, body);
            return;
        }
        Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
    }

    private void submitJob(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        String input = stringField(body, "input");
        if (input == null || input.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "input is required");
            return;
        }
        try {
            TranscodePreset preset = resolvePreset(body);
            String presetId = stringField(body, "presetId");
            String outputDir = stringField(body, "outputDir");
            JsonObject job = module.queue().submit(input, outputDir, presetId, preset);
            audit("transcode.queue.submit", job.get("id").getAsString(), "success", request);
            Json.write(response, job);
        } catch (IllegalArgumentException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void queueJobAction(HttpServletRequest request, HttpServletResponse response, String rest)
            throws IOException {
        String[] parts = rest.split("/");
        if (parts.length != 2) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }
        String id = parts[0];
        try {
            boolean changed;
            if ("decision".equals(parts[1])) {
                JsonObject body = readJson(request);
                String action = body == null ? null : stringField(body, "action");
                if (action == null || action.isBlank()) {
                    Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "action is required");
                    return;
                }
                changed = module.queue().decide(id, action);
            } else if ("reset-preset".equals(parts[1])) {
                JsonObject body = readJson(request);
                if (body == null) {
                    Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                            "request body must be a JSON object");
                    return;
                }
                TranscodePreset replacement = resolvePreset(body);
                replacement.validate();
                changed = module.queue().resetPreset(id, stringField(body, "presetId"), replacement);
            } else {
                changed = switch (parts[1]) {
                    case "start" -> module.queue().startJob(id);
                    case "cancel" -> module.queue().cancel(id);
                    case "retry" -> module.queue().retry(id);
                    case "pause" -> module.queue().pause(id);
                    case "resume" -> module.queue().resume(id);
                    default -> false;
                };
            }
            if (!changed) {
                Json.error(response, HttpServletResponse.SC_CONFLICT, "the job is not in a state that allows this action");
                return;
            }
            audit("transcode.queue." + parts[1], id, "success", request);
            Json.write(response, module.queue().snapshot());
        } catch (IllegalArgumentException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    /// One request, many jobs. Each id is handled on its own so a single refusal (a job
    /// that is still running) does not sink the batch; the response stays 200 and the
    /// caller reads `results` to explain what was skipped and why.
    private void queueBatchAction(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "request body must be a JSON object");
            return;
        }
        String action = stringField(body, "action");
        java.util.List<String> ids = stringArray(body, "ids");
        if (action == null || action.isBlank() || ids.isEmpty()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "action and ids are required");
            return;
        }
        JsonArray results = new JsonArray();
        int applied = 0;
        for (String id : ids) {
            JsonObject item = new JsonObject();
            item.addProperty("id", id);
            boolean changed;
            try {
                changed = switch (action) {
                    case "start" -> module.queue().startJob(id);
                    case "cancel", "stop" -> module.queue().cancel(id);
                    case "retry" -> module.queue().retry(id);
                    case "pause" -> module.queue().pause(id);
                    case "resume" -> module.queue().resume(id);
                    case "remove" -> module.queue().remove(id);
                    default -> false;
                };
            } catch (IOException | IllegalArgumentException e) {
                item.addProperty("ok", false);
                item.addProperty("reason", e.getMessage() == null
                        ? e.getClass().getSimpleName() : e.getMessage());
                results.add(item);
                continue;
            }
            item.addProperty("ok", changed);
            if (changed) {
                applied++;
            } else {
                item.addProperty("reason", "the job is not in a state that allows this action");
            }
            results.add(item);
        }
        audit("transcode.queue." + action, applied + "/" + ids.size(), "success", request);
        JsonObject answer = new JsonObject();
        answer.addProperty("ok", applied == ids.size());
        answer.addProperty("applied", applied);
        answer.add("results", results);
        answer.add("queue", module.queue().snapshot());
        Json.write(response, answer);
    }

    /// The whole order in one shot: partial reorders would be ambiguous while other
    /// clients keep submitting, so a list that does not cover every job is refused.
    private void queueReorder(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "request body must be a JSON object");
            return;
        }
        java.util.List<String> ids = stringArray(body, "ids");
        if (ids.isEmpty()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "ids is required");
            return;
        }
        if (!module.queue().reorder(ids)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "the order must list every job once and keep running jobs in place");
            return;
        }
        audit("transcode.queue.reorder", Integer.toString(ids.size()), "success", request);
        Json.write(response, module.queue().snapshot());
    }

    /// Batch form of `reset-preset`: only not-yet-started jobs are re-pointed, and the
    /// counts say how many were touched versus skipped (tool job / already running).
    private void queueSyncPreset(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "request body must be a JSON object");
            return;
        }
        try {
            TranscodePreset preset = resolvePreset(body);
            preset.validate();
            JsonObject result = module.queue().syncPreset(stringField(body, "presetId"), preset,
                    stringArray(body, "ids"));
            audit("transcode.queue.sync-preset", result.get("updated").getAsString(), "success", request);
            result.add("queue", module.queue().snapshot());
            Json.write(response, result);
        } catch (IllegalArgumentException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void enable(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireElevation(request, response)) {
            return;
        }
        JsonObject body = readJson(request);
        if (body == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return;
        }
        boolean termsAcknowledged = booleanField(body, "termsAcknowledged");
        boolean sawDecline = booleanField(body, "sawDeclineOption");
        int termsVersion = intField(body, "termsVersion");
        String termsTextHash = stringField(body, "termsTextHash");
        String uiVersion = stringField(body, "uiVersion");

        if (!termsAcknowledged) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "the transcode notice must be acknowledged");
            return;
        }
        if (!sawDecline) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "the decline option must have been visible (sawDeclineOption)");
            return;
        }
        if (termsVersion != TranscodeTerms.VERSION) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "the notice changed to v" + TranscodeTerms.VERSION + "; show it again");
            return;
        }
        if (termsTextHash == null || !TranscodeTerms.hash().equals(termsTextHash)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "the notice text does not match what this build offers");
            return;
        }
        if (uiVersion == null || uiVersion.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "uiVersion is required");
            return;
        }

        TranscodeConsent record = new TranscodeConsent(
                termsVersion,
                termsTextHash,
                Instant.now().toString(),
                actorOf(request) == null ? "unknown" : actorOf(request),
                request.getRemoteAddr(),
                "checkbox",
                uiVersion,
                request.getHeader("User-Agent"),
                Metadata.VERSION,
                true);
        feature.enable(record);
        module.queue().start();
        audit("transcode.feature.enable", "terms v" + termsVersion + " (" + termsTextHash + ")", "success", request);
        featureJson(response);
    }

    private void decline(HttpServletRequest request, HttpServletResponse response) throws IOException {
        audit("transcode.feature.decline", "transcode feature", "success", request);
        featureJson(response);
    }

    private void disable(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!requireElevation(request, response)) {
            return;
        }
        if (module.queue().activeCount() > 0 && !"1".equals(request.getParameter("force"))) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "transcode_busy: stop the queue or pass force=1 to stop it and disable");
            return;
        }
        module.queue().stop();
        feature.disable();
        audit("transcode.feature.disable", "transcode feature", "success", request);
        featureJson(response);
    }

    /// The same second-password check the Minecraft surface uses: only over TLS,
    /// only with a configured developer password, and satisfied by a session
    /// elevation or a bearer token. Agents are let through by
    /// [AuthFilter#elevationSatisfied], but the route itself is closed to them by
    /// [org.jackhuang.hmcl.web.developer.AgentScopes] (unmapped POSTs are denied).
    private boolean requireElevation(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!RequestSecurity.isSecure(request, tlsActive.getAsBoolean(), config)) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "tls_required: the developer elevation is only accepted over HTTPS");
            return false;
        }
        if (!developerAuth.isSet()) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "developer_password_not_set: set the developer password before enabling transcoding");
            return false;
        }
        if (AuthFilter.elevationSatisfied(request, elevation, keyOf(request), sessionAlive(request))) {
            return true;
        }
        Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                "developer_elevation_required: elevate with the developer password first");
        return false;
    }

    private String keyOf(HttpServletRequest request) {
        Object token = request.getAttribute(AuthFilter.SESSION_TOKEN_ATTRIBUTE);
        return DeveloperElevation.key(token instanceof String value ? value : null, request.getRemoteAddr());
    }

    private boolean sessionAlive(HttpServletRequest request) {
        Object token = request.getAttribute(AuthFilter.SESSION_TOKEN_ATTRIBUTE);
        return !(token instanceof String value) || value.isBlank() || auth.isAlive(value);
    }

    private static @Nullable String actorOf(HttpServletRequest request) {
        Object session = request.getAttribute(AuthFilter.SESSION_ATTRIBUTE);
        return session instanceof AuthService.Session s ? s.username() : null;
    }

    private void audit(String action, String object, String outcome, HttpServletRequest request) {
        AuditLog.log(AuditLog.Channel.WEB, action, object, outcome, actorOf(request), request.getRemoteAddr());
    }

    private static @Nullable JsonObject readJson(HttpServletRequest request) throws IOException {
        String text = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        try {
            return JsonParser.parseString(text).getAsJsonObject();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static @Nullable String stringField(JsonObject body, String name) {
        return body.has(name) && body.get(name).isJsonPrimitive() ? body.get(name).getAsString() : null;
    }

    private static boolean booleanField(JsonObject body, String name) {
        return body.has(name) && body.get(name).isJsonPrimitive() && body.get(name).getAsBoolean();
    }

    private static int intField(JsonObject body, String name) {
        try {
            return body.has(name) && body.get(name).isJsonPrimitive() ? body.get(name).getAsInt() : 0;
        } catch (RuntimeException e) {
            return 0;
        }
    }
}