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

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.web.task.TaskService;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.util.Optional;

/// The task half of the REST surface, mapped at `/api/tasks/*`:
///
/// - `GET  /api/tasks`             — every known task, newest first
/// - `GET  /api/tasks/{id}`        — one task: `{id, kind, state, message, fraction, error?, approval?}`
/// - `POST /api/tasks/{id}/cancel` — stop the work, `{ok: true}`; 404 when
///   unknown, 409 when the task is no longer running
/// - `POST /api/tasks/{id}/approve` — deliver an approval decision,
///   `{allow: true|false}`; 404 when unknown, 409 when the task is not
///   parked on an approval
///
/// Tasks are the asynchronous work behind installs and launches; their
/// progress also streams over the WebSocket gateway on the `tasks` topic.
/// Cancelling reaches the pnpm process behind an install through
/// [org.jackhuang.hmcl.dsh.DshCommand#stopRunning], scoped to the cancelled
/// task's own worker thread so a sibling install keeps running.
@NotNullByDefault
public final class TasksApiServlet extends HttpServlet {

    private final TaskService tasks;

    public TasksApiServlet(TaskService tasks) {
        this.tasks = tasks;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty() || pathInfo.equals("/")) {
            Json.write(response, java.util.Map.of("tasks", tasks.list()));
            return;
        }
        String taskId = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        Optional<TaskService.TaskInfo> task = tasks.get(taskId);
        if (task.isEmpty()) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "task not found");
            return;
        }
        Json.write(response, task.get());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        String trimmed = pathInfo == null ? "" : pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        String[] segments = trimmed.split("/");
        if (segments.length != 2 || !("cancel".equals(segments[1]) || "approve".equals(segments[1]))) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }
        String taskId = segments[0];
        if ("approve".equals(segments[1])) {
            approve(request, response, taskId);
            return;
        }
        if (tasks.get(taskId).isEmpty()) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "task not found");
            return;
        }
        if (!tasks.cancel(taskId)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT, "task is not running");
            return;
        }
        Json.write(response, java.util.Map.of("ok", true));
    }

    /// Delivers an approval decision: `{allow: true}` runs the pending scripts
    /// (the AUTO policy's answer), `{allow: false}` installs without them (the
    /// NEVER policy's answer).
    private void approve(HttpServletRequest request, HttpServletResponse response, String taskId)
            throws IOException {
        Boolean allow;
        try {
            var body = com.google.gson.JsonParser
                    .parseString(new String(request.getInputStream().readAllBytes(),
                            java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject();
            var element = body.get("allow");
            allow = element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()
                    ? element.getAsBoolean() : null;
        } catch (com.google.gson.JsonParseException | IllegalStateException e) {
            allow = null;
        }
        if (allow == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "request body must be a JSON object with a boolean `allow`");
            return;
        }
        switch (tasks.approve(taskId, allow)) {
            case OK -> Json.write(response, java.util.Map.of("ok", true));
            case UNKNOWN_TASK -> Json.error(response, HttpServletResponse.SC_NOT_FOUND, "task not found");
            case NOT_WAITING -> Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "task is not waiting for an approval");
        }
    }
}
