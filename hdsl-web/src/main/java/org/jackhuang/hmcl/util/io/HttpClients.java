/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2025 huangyuhui <huanghongxun2008@126.com> and contributors
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
package org.jackhuang.hmcl.util.io;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.http.HttpClient;

/// Closes an [`HttpClient`] where the runtime supports it.
///
/// `HttpClient` became `AutoCloseable` in Java 21; the launcher's minimum runtime
/// is 17, where there is no `close()` to call and the connection pool is reclaimed
/// when the client becomes unreachable. Reflection lets one code path keep the
/// explicit shutdown on 21 and compile — and run — on 17.
@NotNullByDefault
public final class HttpClients {
    /// `HttpClient.close`, or `null` before Java 21.
    private static final @Nullable Method CLOSE = find();

    private HttpClients() {
    }

    /// Closes the client when the runtime has that method; otherwise does nothing.
    ///
    /// Failures are swallowed on purpose: this runs in `finally` blocks, where
    /// throwing would replace the answer the caller was about to give.
    ///
    /// @param client the client to close
    public static void closeQuietly(HttpClient client) {
        if (CLOSE == null) {
            return;
        }
        try {
            CLOSE.invoke(client);
        } catch (IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
            // Nothing useful to do in a finally block.
        }
    }

    private static @Nullable Method find() {
        try {
            return HttpClient.class.getMethod("close");
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
