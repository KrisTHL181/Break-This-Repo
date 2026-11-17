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
package org.jackhuang.hmcl.web.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/// The download half of the export flow, mapped at `/api/exports/*` (behind
/// the same auth gate as every `/api/*` route):
///
/// - `GET /api/exports`            — every artifact in `<HDSL_DATA>/exports`,
///   newest first: `{filename, sizeBytes, createdAt}`
/// - `GET /api/exports/{filename}` — the artifact itself, as an attachment.
///   The name is normalized and confined to the exports directory
///   ([PackExports#resolveDownload]), so a crafted path is a 400, not a file
///   from somewhere else.
@NotNullByDefault
public final class ExportsApiServlet extends HttpServlet {

    private final ServerConfig config;

    public ExportsApiServlet(ServerConfig config) {
        this.config = config;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty() || pathInfo.equals("/")) {
            list(response);
            return;
        }
        String name = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        download(response, name);
    }

    private void list(HttpServletResponse response) throws IOException {
        Path dir = config.dataDir.resolve("exports");
        JsonArray exports = new JsonArray();
        if (Files.isDirectory(dir)) {
            List<Path> files = new ArrayList<>();
            try (Stream<Path> children = Files.list(dir)) {
                children.filter(Files::isRegularFile).forEach(files::add);
            }
            files.sort(Comparator.comparingLong(ExportsApiServlet::createdAt).reversed());
            for (Path file : files) {
                JsonObject entry = new JsonObject();
                entry.addProperty("filename", file.getFileName().toString());
                entry.addProperty("sizeBytes", Files.size(file));
                entry.addProperty("createdAt", createdAt(file));
                exports.add(entry);
            }
        }
        JsonObject body = new JsonObject();
        body.add("exports", exports);
        Json.write(response, body);
    }

    private void download(HttpServletResponse response, String name) throws IOException {
        Path dir = PackExports.ensureDirectory(config.dataDir.resolve("exports"));
        Path file = PackExports.resolveDownload(dir, name);
        if (file == null) {
            if (name == null || name.isBlank() || name.contains("/") || name.contains("\\")
                    || name.equals(".") || name.equals("..") || name.startsWith(".")) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "not a plain file name");
                return;
            }
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "export not found");
            return;
        }
        long size = Files.size(file);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/octet-stream");
        response.setContentLengthLong(size);
        // RFC 5987: the ASCII fallback strips what a header cannot carry, the
        // encoded form keeps the real name for clients that read it.
        String fallback = file.getFileName().toString().replaceAll("[^A-Za-z0-9._-]", "_");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fallback
                + "\"; filename*=UTF-8''" + URLEncoder.encode(file.getFileName().toString(),
                StandardCharsets.UTF_8).replace("+", "%20"));
        try (InputStream in = Files.newInputStream(file)) {
            OutputStream out = response.getOutputStream();
            in.transferTo(out);
            out.flush();
        }
    }

    /// The creation time of a file, falling back to its modification time
    /// where the filesystem records none.
    private static long createdAt(Path file) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
            long created = attributes.creationTime().toMillis();
            return created > 0 ? created : attributes.lastModifiedTime().toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }
}
