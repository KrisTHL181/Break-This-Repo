/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2020  huangyuhui <huanghongxun2008@126.com> and contributors
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
package org.jackhuang.hmcl.util.platform;

import org.jackhuang.hmcl.util.Lang;
import org.jackhuang.hmcl.util.function.ExceptionalFunction;
import org.jackhuang.hmcl.util.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

public final class SystemUtils {
    /// Where the output-draining half of [#run] runs.
    ///
    /// The desktop build used the task package's I/O pool; HDSL-web has no task
    /// package, so each drain gets a thread of its own — virtual on Java 21+, a
    /// daemon platform thread on 17 (see [VirtualThreads]).
    private static final java.util.concurrent.ExecutorService IO_EXECUTOR =
            VirtualThreads.newPerTaskExecutor();

    private SystemUtils() {
    }

    /// Resolves a program on `PATH`.
    ///
    /// The name is completed by platform — on Windows `zstd` is `zstd.exe`, and the
    /// bare name names nothing — which is why this asks [Executables] rather than
    /// joining `PATH` entries itself.
    ///
    /// @param command the program name
    /// @return the resolved program, or `null` when it is not on `PATH`
    public static @Nullable Path which(String command) {
        return Executables.resolve(command).orElse(null);
    }

    public static int callExternalProcess(String... command) throws IOException, InterruptedException {
        return callExternalProcess(Arrays.asList(command));
    }

    public static int callExternalProcess(List<String> command) throws IOException, InterruptedException {
        return callExternalProcess(new ProcessBuilder(Executables.startable(command)));
    }

    public static int callExternalProcess(ProcessBuilder processBuilder) throws IOException, InterruptedException {
        ManagedProcess managedProcess = new ManagedProcess(processBuilder);
        managedProcess.pumpInputStream(SystemUtils::onLogLine);
        managedProcess.pumpErrorStream(SystemUtils::onLogLine);
        return managedProcess.getProcess().waitFor();
    }

    private static final Duration DEFAULT_MAX_WAIT_TIME = Duration.ofSeconds(15);

    public static String run(String... command) throws Exception {
        return run(List.of(command), DEFAULT_MAX_WAIT_TIME);
    }

    public static String run(List<String> command, Duration maxWaitTime) throws Exception {
        return run(command, inputStream -> IOUtils.readFullyAsString(inputStream, OperatingSystem.NATIVE_CHARSET), maxWaitTime);
    }

    public static <T> T run(List<String> command, ExceptionalFunction<InputStream, T, ?> convert) throws Exception {
        return run(command, convert, DEFAULT_MAX_WAIT_TIME);
    }

    public static <T> T run(List<String> command, ExceptionalFunction<InputStream, T, ?> convert, Duration maxWaitTime) throws Exception {
        Process process = new ProcessBuilder(Executables.startable(command))
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        try {
            InputStream inputStream = process.getInputStream();
            CompletableFuture<T> future = CompletableFuture.supplyAsync(
                    Lang.wrap(() -> convert.apply(inputStream)),
                    IO_EXECUTOR);

            if (!process.waitFor(maxWaitTime.toMillis(), TimeUnit.MILLISECONDS))
                throw new TimeoutException();

            if (process.exitValue() != 0)
                throw new IOException("Bad exit code: " + process.exitValue());

            return future.get();
        } finally {
            if (process.isAlive())
                process.destroy();
        }
    }

    public static boolean supportJVMAttachment() {
        return Thread.currentThread().getContextClassLoader().getResource("com/sun/tools/attach/VirtualMachine.class") != null;
    }

    public static void onLogLine(String log) {
        LOG.info(log);
    }
}
