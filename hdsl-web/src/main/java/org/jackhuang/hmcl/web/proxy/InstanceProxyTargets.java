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

import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.dsh.DshProcess;
import org.jackhuang.hmcl.dsh.DshProcessManager;
import org.jackhuang.hmcl.web.brand.BrandRuntime;
import org.jackhuang.hmcl.web.zcode.ZcodeRuntime;
import org.jetbrains.annotations.NotNullByDefault;

import java.net.URI;

/// Where `/i/<id>/` forwards to — the one place both the HTTP proxy
/// ([InstanceProxyServlet]) and its WebSocket relay ([InstanceProxyWebSocket])
/// ask, so the two cannot disagree about what an id means.
///
/// Two categories answer here now:
///
/// - a **dsh** instance, whose loopback port is the one its readiness line
///   reported (`DshProcess.webUrl`, falling back to the manifest's port);
/// - an **experimental ZCode** instance, whose process binds loopback and whose
///   port [ZcodeRuntime] remembers from its own readiness line.
@NotNullByDefault
final class InstanceProxyTargets {

    /// The id names no instance of either category: answer 404.
    static final int UNKNOWN = -2;

    /// The instance exists but holds no live process: answer 502.
    static final int NOT_RUNNING = -1;

    private InstanceProxyTargets() {
    }

    /// Resolves the loopback port an instance is reachable on.
    ///
    /// @param instanceId the id from the `/i/<id>/` path
    /// @return a positive port, or [UNKNOWN] / [NOT_RUNNING]
    static int resolve(String instanceId) {
        String target = BrandRuntime.targetForMount(instanceId);
        if (!target.equals(instanceId)) {
            int port = BrandRuntime.isRunning(target) ? BrandRuntime.portOf(target) : 0;
            return port > 0 ? port : NOT_RUNNING;
        }
        DshInstance instance = DshInstanceManager.find(instanceId);
        if (instance != null) {
            URI webUrl = DshProcessManager.find(instanceId).flatMap(DshProcess::webUrl).orElse(null);
            if (webUrl == null) {
                return NOT_RUNNING;
            }
            int port = webUrl.getPort();
            return port > 0 ? port : instance.portOrDefault();
        }
        if (!ZcodeRuntime.known(instanceId)) {
            // Third-party brand categories (Kimi Code, OpenCode): same
            // contract as ZCode — known ids answer with their live port or
            // NOT_RUNNING, unknown ids fall through to UNKNOWN.
            if (!BrandRuntime.known(instanceId)) {
                return UNKNOWN;
            }
            int port = BrandRuntime.isRunning(instanceId) ? BrandRuntime.portOf(instanceId) : 0;
            return port > 0 ? port : NOT_RUNNING;
        }
        int port = ZcodeRuntime.isRunning(instanceId) ? ZcodeRuntime.portOf(instanceId) : 0;
        return port > 0 ? port : NOT_RUNNING;
    }

    /// The same answer for the WebSocket relay, which asks before any HTTP
    /// request does: a live dsh process is enough there — the instance manifest
    /// is not required — with the experimental ZCode category as the fallback.
    ///
    /// @param instanceId the id from the `/i/<id>/` path
    /// @return a positive port, or [NOT_RUNNING]
    static int resolveForRelay(String instanceId) {
        String target = BrandRuntime.targetForMount(instanceId);
        if (!target.equals(instanceId)) {
            int port = BrandRuntime.isRunning(target) ? BrandRuntime.portOf(target) : 0;
            return port > 0 ? port : NOT_RUNNING;
        }
        URI webUrl = DshProcessManager.find(instanceId).flatMap(DshProcess::webUrl).orElse(null);
        if (webUrl != null && webUrl.getPort() > 0) {
            return webUrl.getPort();
        }
        if (BrandRuntime.known(instanceId)) {
            int port = BrandRuntime.isRunning(instanceId) ? BrandRuntime.portOf(instanceId) : 0;
            return port > 0 ? port : NOT_RUNNING;
        }
        int port = ZcodeRuntime.isRunning(instanceId) ? ZcodeRuntime.portOf(instanceId) : 0;
        return port > 0 ? port : NOT_RUNNING;
    }
}
