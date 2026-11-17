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
package org.jackhuang.hmcl.web.terminal.pty;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jackhuang.hmcl.util.platform.OSVersion;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jackhuang.hmcl.util.platform.VirtualThreads;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/// The Windows pseudo terminal: `CreatePseudoConsole` plus `CreateProcessW`,
/// glued with JNA.
///
/// Three decisions are load-bearing:
///
/// - **ConPTY, never a console window.** The pseudo console is part of Windows
///   since 10 1809 and needs no binary beside the jar. That is what makes the
///   terminal work in a session with no desktop — a service has nowhere to show
///   a console — and what lets the browser see the same bytes a local console
///   would have painted.
/// - **argv-only.** `lpApplicationName` is the program on its own and the
///   command line is built here; nothing goes through `cmd.exe`. That is also
///   why a `.cmd`/`.bat`/`.ps1`/`.com` program is refused: `CreateProcess`
///   silently hands those to `cmd.exe`, which would put a shell back in the
///   chain this class exists to keep out.
/// - **No JNA callbacks.** Everything is a blocking call made from a Java
///   thread (`ReadFile`, `WaitForSingleObject`), so the native side never calls
///   back into the JVM.
///
/// One detail is easy to miss and invisible when wrong: the child is started
/// with `STARTF_USESTDHANDLES` and all three handles NULL. Without that,
/// `CreateProcess` copies the *panel's* standard handles into the child, so an
/// agent's output goes into the panel's own log file while the console stays
/// empty (measured on Windows 10 19045). With it, the console subsystem gives
/// the child the pseudo console's own handles, which is what a terminal is.
///
/// The contract is [Pty]'s, not Windows': when the child exits, [output]
/// reaches EOF and [#waitFor] answers, and there is no prompt after it because
/// nothing else was ever started. The one place the two platforms differ is
/// [terminate]: Windows has no polite signal that reaches a process attached to
/// a pseudo console (`GenerateConsoleCtrlEvent` does not), so the terminal gets
/// a grace period to leave on its own and is then killed with its job object.
/// That is "end the session", not "SIGTERM", and it is documented as such.
///
/// Only 64-bit JVMs are promised. Handles are [Pointer]s throughout, so a
/// 32-bit runtime would not corrupt itself — it is just untested.
@NotNullByDefault
public final class ConPty implements Pty {

    /// The oldest Windows that has `CreatePseudoConsole`: Windows 10 1809.
    public static final int REQUIRED_BUILD = 17763;

    /// What a machine without ConPTY is told, verbatim, wherever the panel says
    /// it. The 409 pre-check in the terminal route reuses this so the two
    /// places cannot drift.
    public static final String UNSUPPORTED_MESSAGE =
            "the browser terminal needs Windows 10 1809 (build " + REQUIRED_BUILD + ") or later; "
                    + "this machine reports " + OperatingSystem.SYSTEM_VERSION
                    + " (build " + buildNumber() + ")";

    /// Programs Windows would run through `cmd.exe` behind `CreateProcess`'s
    /// back. Refused: the whole point of the argv-only spawn is that no shell
    /// enters the chain.
    private static final List<String> SHELL_SHIMS = List.of(".cmd", ".bat", ".ps1", ".com");

    /// How long the pump may keep draining after the child exited.
    ///
    /// A bound, not a wait: the pseudo console's output reaches EOF as soon as
    /// the last process attached to it is gone, and the drain ends there. The
    /// bound covers a console that lingers; the pseudo console is closed after
    /// it, which is what finally unblocks a read that is still waiting.
    private static final long DRAIN_MILLIS = 2000;

    /// How long [#close] waits for the pump before it gives up on the join. The
    /// handles are closed either way; the thread then ends on its own.
    private static final long PUMP_JOIN_MILLIS = 2000;

    /// The wait loop's slice. Used to wait for the child, and to wait for the
    /// session in [#terminate], so both notice a close promptly.
    private static final long WAIT_SLICE_MILLIS = 100;

    /// One read from the pseudo console.
    private static final int READ_BUFFER = 32 * 1024;

    /// The most bytes handed to one `WriteFile`. ConPTY is known to misbehave
    /// with large single writes; chunking is the community's answer and costs
    /// nothing here.
    private static final int WRITE_CHUNK = 1024;

    /// How much output the pump may hold for a slow reader before it stops
    /// reading, which in turn blocks the child on its own writes.
    private static final int REPLAY_CAPACITY = 1024 * 1024;

    private static final int EXTENDED_STARTUPINFO_PRESENT = 0x00080000;
    private static final int CREATE_SUSPENDED = 0x00000004;
    private static final int CREATE_UNICODE_ENVIRONMENT = 0x00000400;
    private static final int STARTF_USESTDHANDLES = 0x00000100;
    private static final long PROC_THREAD_ATTRIBUTE_PSEUDOCONSOLE = 0x00020016L;

    private static final int HANDLE_FLAG_INHERIT = 0x00000001;
    private static final int DUPLICATE_SAME_ACCESS = 0x00000002;
    private static final int JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE = 0x00002000;
    private static final int JOB_OBJECT_EXTENDED_LIMIT_INFORMATION_CLASS = 9;

    private static final int ERROR_INVALID_HANDLE = 6;
    private static final int ERROR_BROKEN_PIPE = 109;
    private static final int ERROR_NO_DATA = 232;
    private static final int ERROR_PIPE_NOT_CONNECTED = 233;

    private static final int WAIT_OBJECT_0 = 0;
    private static final int WAIT_FAILED = 0xFFFFFFFF;
    private static final int WAIT_TIMEOUT = 0x00000102;

    /// The command line of a terminal is one string; `CommandLineToArgvW`
    /// undoes what this class does, so the escaping lives with the spawn.
    private static final int MAX_COMMAND_LINE = 32 * 1024;

    private static final boolean SUPPORTED = detect();

    private final Leased input;
    private final Leased output;

    private final Object consoleLock = new Object();
    private final Object handleLock = new Object();
    private boolean closed;

    /// The pseudo console, `null` once it is closed. Guarded by [consoleLock]:
    /// `ClosePseudoConsole` and `ResizePseudoConsole` must not overlap.
    private @Nullable Pointer pseudoConsole;

    private final Pointer process;
    private final long pid;
    private final @Nullable Pointer job;
    private final @Nullable Pointer thread;

    private final Buffer buffer = new Buffer(REPLAY_CAPACITY);
    private final AtomicBoolean inputClosed = new AtomicBoolean();
    private final CountDownLatch finished = new CountDownLatch(1);
    private final CountDownLatch pumpEnded = new CountDownLatch(1);
    private final Thread pump;
    private volatile int exitCode = -1;

    private ConPty(Leased input, Leased output, Pointer pseudoConsole, Pointer process, long pid,
                   @Nullable Pointer job, @Nullable Pointer thread) {
        this.input = input;
        this.output = output;
        this.pseudoConsole = pseudoConsole;
        this.process = process;
        this.pid = pid;
        this.job = job;
        this.thread = thread;
        // Platform threads, not virtual ones: the pump blocks in a native read and
        // the reaper in a wait-for-the-process downcall, and a virtual thread cannot
        // unmount while one of those runs — two carriers per terminal, which is how
        // a second terminal starves on a small machine (see VirtualThreads#startPlatform).
        this.pump = VirtualThreads.startPlatform(this::pump, "hdsl-conpty-pump");
        VirtualThreads.startPlatform(this::reap, "hdsl-conpty-reap");
    }

    /// Whether this machine can run a browser terminal at all.
    ///
    /// Two facts, both required: the Windows build the API arrived in (the
    /// reason a person can act on) and the symbol itself (the truth — a build
    /// number can be lied to by a compatibility shim).
    public static boolean platformSupported() {
        return SUPPORTED;
    }

    /// The Windows build number, `0` on every other platform.
    public static int buildNumber() {
        OSVersion version = OperatingSystem.SYSTEM_VERSION;
        return version instanceof OSVersion.Windows windows ? windows.build() : 0;
    }

    /// One line for the startup banner: what the terminal will use, or why it
    /// will not.
    public static String describe() {
        if (!SUPPORTED) {
            return "UNAVAILABLE - " + UNSUPPORTED_MESSAGE;
        }
        return "ConPTY (" + windowsName() + " build " + buildNumber() + ")";
    }

    /// Starts `command` on a new pseudo console.
    ///
    /// @throws PtyException when the platform has no ConPTY, when the program is
    ///                      one Windows would hand to a shell, or when a system
    ///                      call fails
    static ConPty spawn(List<String> command, @Nullable Path cwd,
                        Map<String, String> environment, PtySize size) throws IOException {
        if (!SUPPORTED) {
            throw new PtyException(UNSUPPORTED_MESSAGE);
        }
        if (command.isEmpty()) {
            throw new PtyException("empty command");
        }
        validateProgram(command.get(0));

        Win32 api = Win32.INSTANCE;

        // Two pipes: our write end feeds the console's input, the console's
        // output feeds our read end. Both are created inheritable because
        // conhost inherits the console's own ends; our ends lose the flag right
        // after (`bInheritHandles` is FALSE below, so nothing else can pick
        // them up either way).
        SecurityAttributes inheritable = new SecurityAttributes();
        PointerByReference readEnd = new PointerByReference();
        PointerByReference writeEnd = new PointerByReference();
        if (!api.CreatePipe(readEnd, writeEnd, inheritable, 0)) {
            throw new PtyException("CreatePipe", Native.getLastError());
        }
        Pointer ptyIn = readEnd.getValue();
        Pointer ourIn = writeEnd.getValue();

        readEnd = new PointerByReference();
        writeEnd = new PointerByReference();
        if (!api.CreatePipe(readEnd, writeEnd, inheritable, 0)) {
            int error = Native.getLastError();
            close(api, ptyIn, ourIn);
            throw new PtyException("CreatePipe", error);
        }
        Pointer ourOut = readEnd.getValue();
        Pointer ptyOut = writeEnd.getValue();

        boolean running = false;
        Memory attributes = null;
        Pointer pseudoConsole = null;
        Pointer process = null;
        Pointer thread = null;
        Pointer job = null;
        try {
            if (!api.SetHandleInformation(ourIn, HANDLE_FLAG_INHERIT, 0)
                    || !api.SetHandleInformation(ourOut, HANDLE_FLAG_INHERIT, 0)) {
                throw new PtyException("SetHandleInformation", Native.getLastError());
            }

            PointerByReference consoleRef = new PointerByReference();
            int hr = api.CreatePseudoConsole(new Coord(size.cols(), size.rows()), ptyIn, ptyOut, 0, consoleRef);
            if (hr != 0) {
                throw new PtyException(String.format("CreatePseudoConsole failed (HRESULT 0x%08X)", hr));
            }
            pseudoConsole = consoleRef.getValue();

            attributes = attributeList(api, pseudoConsole);
            Memory environmentBlock = environmentBlock(environment);

            StartupInfoEx startup = new StartupInfoEx();
            startup.lpAttributeList = attributes;
            // The console gives the child its standard handles *only* when the
            // parent does not hand it any. Without this, `CreateProcess` copies
            // the parent's own std handles into the child — and the panel's
            // stdout is a log file or a pipe, so `cmd /c echo hello` would write
            // into the panel's log instead of the pseudo console while the
            // console stayed empty. `USESTDHANDLES` with all three NULL is how a
            // GUI parent starts a console program: the console subsystem then
            // gives the child the console's own handles, which is exactly what a
            // terminal wants. (Measured on Windows 10 19045: with the parent's
            // handles the output never reached the console; with NULL it does.)
            startup.dwFlags = STARTF_USESTDHANDLES;
            ProcessInformation information = new ProcessInformation();
            WString program = new WString(command.get(0));
            WString commandLine = new WString(toCommandLine(command));
            WString workDirectory = cwd == null ? null : new WString(cwd.toString());
            boolean created = api.CreateProcessW(program, commandLine, null, null, false,
                    EXTENDED_STARTUPINFO_PRESENT | CREATE_SUSPENDED | CREATE_UNICODE_ENVIRONMENT,
                    environmentBlock, workDirectory, startup, information);
            if (!created) {
                throw new PtyException("CreateProcessW " + command.get(0), Native.getLastError());
            }
            process = information.hProcess;
            thread = information.hThread;

            // The console owns the child's side from here on; a copy left in the
            // parent keeps the console alive past the session and the output
            // never reaches EOF.
            close(api, ptyIn, ptyOut);
            ptyIn = null;
            ptyOut = null;
            api.DeleteProcThreadAttributeList(attributes);
            attributes = null;

            // CREATE_SUSPENDED is what makes the job assignment atomic with the
            // start: the child cannot spawn anything before it is in the job
            // that will sweep it.
            job = api.CreateJobObjectW(null, null);
            if (job != null && (!setKillOnJobClose(api, job)
                    || !api.AssignProcessToJobObject(job, process))) {
                Logger.LOG.warning("Could not put the terminal's process in a job object (error "
                        + Native.getLastError() + "); terminating it will not reach its children");
                api.CloseHandle(job);
                job = null;
            } else if (job == null) {
                Logger.LOG.warning("Could not create the terminal's job object (error "
                        + Native.getLastError() + "); terminating it will not reach its children");
            }
            if (api.ResumeThread(thread) == WAIT_FAILED) {
                throw new PtyException("ResumeThread", Native.getLastError());
            }

            ConPty pty = new ConPty(new Leased(ourIn), new Leased(ourOut), pseudoConsole, process,
                    information.dwProcessId & 0xFFFFFFFFL, job, thread);
            running = true;
            return pty;
        } finally {
            close(api, ptyIn, ptyOut);
            if (attributes != null) {
                api.DeleteProcThreadAttributeList(attributes);
            }
            if (!running) {
                close(api, ourIn, ourOut);
                if (thread != null) {
                    api.CloseHandle(thread);
                }
                if (process != null) {
                    api.TerminateProcess(process, 1);
                    api.CloseHandle(process);
                }
                if (job != null) {
                    api.CloseHandle(job);
                }
                if (pseudoConsole != null) {
                    api.ClosePseudoConsole(pseudoConsole);
                }
            }
        }
    }

    /// Refuses anything `CreateProcess` would quietly run through `cmd.exe`, and
    /// anything that is not a file this process can see.
    ///
    /// The suffix test is a string test on purpose: these rules are exercised on
    /// Linux too, where `\` is not a separator and path parsing would turn the
    /// whole argument into a file name.
    static void validateProgram(String program) throws PtyException {
        if (program.isEmpty()) {
            throw new PtyException("empty command");
        }
        String lower = program.toLowerCase(Locale.ROOT);
        for (String shim : SHELL_SHIMS) {
            if (lower.endsWith(shim)) {
                throw new PtyException("refusing to start " + program + ": CreateProcess would hand a "
                        + shim + " file to cmd.exe, which puts a shell in the terminal chain");
            }
        }
        Path path;
        try {
            path = Path.of(program);
        } catch (InvalidPathException e) {
            throw new PtyException("not a path: " + program);
        }
        if (!path.isAbsolute()) {
            throw new PtyException("refusing to start " + program
                    + ": the PTY spawn needs an absolute path to the program");
        }
        if (!Files.exists(path)) {
            throw new PtyException("no such program: " + program);
        }
    }

    /// `lpCommandLine`, quoted the way `CommandLineToArgvW` reads it back.
    ///
    /// `CreateProcessW` takes one string, so the argv list has to be turned into
    /// one. The rule is MSVCRT's: quote an argument that has a space, a tab or a
    /// quote (or is empty), double the backslashes that precede a quote, double
    /// the trailing ones. `argv[0]` goes through the same rule: `lpApplicationName`
    /// is given separately, but the child's own `argv[0]` comes from here.
    static String toCommandLine(List<String> command) throws PtyException {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < command.size(); i++) {
            if (i > 0) {
                line.append(' ');
            }
            appendArgument(line, command.get(i));
        }
        if (line.length() > MAX_COMMAND_LINE) {
            throw new PtyException("the command line is longer than " + MAX_COMMAND_LINE + " characters");
        }
        return line.toString();
    }

    private static void appendArgument(StringBuilder line, String argument) {
        int backslashes = 0;
        boolean plain = !argument.isEmpty();
        for (int i = 0; plain && i < argument.length(); i++) {
            char c = argument.charAt(i);
            if (c == ' ' || c == '\t' || c == '"') {
                plain = false;
            }
        }
        if (plain) {
            line.append(argument);
            return;
        }
        line.append('"');
        for (int i = 0; i < argument.length(); i++) {
            char c = argument.charAt(i);
            if (c == '\\') {
                backslashes++;
                continue;
            }
            if (c == '"') {
                for (int k = 0; k < backslashes * 2 + 1; k++) {
                    line.append('\\');
                }
                line.append('"');
                backslashes = 0;
                continue;
            }
            for (int k = 0; k < backslashes; k++) {
                line.append('\\');
            }
            backslashes = 0;
            line.append(c);
        }
        for (int k = 0; k < backslashes * 2; k++) {
            line.append('\\');
        }
        line.append('"');
    }

    /// The child's complete environment, as `CreateProcessW` wants it:
    /// UTF-16LE, `KEY=VALUE` entries, two terminating zeros.
    ///
    /// `lpEnvironment` is the whole block, not a delta — `HOME`/`USERPROFILE`/
    /// `PATH` are made of it. Keys Windows uses for a drive's current directory
    /// (they start with `=`) are dropped: they are not variables and the block
    /// is not where they belong.
    static Memory environmentBlock(Map<String, String> environment) {
        List<String> entries = new ArrayList<>();
        environment.forEach((key, value) -> {
            if (key.isEmpty() || key.charAt(0) == '=' || value == null) {
                return;
            }
            entries.add(key + "=" + value);
        });
        entries.sort(String.CASE_INSENSITIVE_ORDER);
        long bytes = 2;
        for (String entry : entries) {
            bytes += (long) (entry.length() + 1) * 2;
        }
        Memory block = new Memory(bytes);
        block.clear();
        long offset = 0;
        for (String entry : entries) {
            byte[] encoded = (entry + '\0').getBytes(StandardCharsets.UTF_16LE);
            block.write(offset, encoded, 0, encoded.length);
            offset += encoded.length;
        }
        return block;
    }

    @Override
    public OutputStream input() {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                write(new byte[]{(byte) b}, 0, 1);
            }

            @Override
            public void write(byte[] bytes, int offset, int length) throws IOException {
                if (inputClosed.get()) {
                    throw new IOException("the terminal has ended");
                }
                if (length == 0) {
                    return;
                }
                int written = 0;
                while (written < length) {
                    int chunk = Math.min(length - written, WRITE_CHUNK);
                    // A lease, like the pump: the terminal can end between the
                    // check above and the write, and this must not type into
                    // whatever holds the handle by then.
                    Pointer lease = input.acquire();
                    if (lease == null) {
                        throw new IOException("the terminal has ended");
                    }
                    try {
                        byte[] slice = written == 0 && chunk == bytes.length
                                ? bytes
                                : Arrays.copyOfRange(bytes, offset + written, offset + written + chunk);
                        IntByReference count = new IntByReference();
                        if (!Win32.INSTANCE.WriteFile(lease, slice, chunk, count, null)) {
                            throw new PtyException("WriteFile", Native.getLastError());
                        }
                        if (count.getValue() <= 0) {
                            throw new IOException("the terminal accepted no bytes");
                        }
                        written += count.getValue();
                    } finally {
                        Win32.INSTANCE.CloseHandle(lease);
                    }
                }
            }
        };
    }

    @Override
    public InputStream output() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                byte[] one = new byte[1];
                int n = read(one, 0, 1);
                return n < 0 ? -1 : one[0] & 0xff;
            }

            @Override
            public int read(byte[] bytes, int offset, int length) throws IOException {
                try {
                    return buffer.read(bytes, offset, length);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("interrupted while reading the terminal", e);
                }
            }
        };
    }

    @Override
    public void resize(PtySize size) {
        synchronized (consoleLock) {
            if (pseudoConsole == null) {
                return;
            }
            // A failed resize is not fatal: the session keeps running at the old
            // size and the browser sends another one on the next fit.
            if (!Win32.INSTANCE.ResizePseudoConsole(pseudoConsole, new Coord(size.cols(), size.rows()))) {
                Logger.LOG.debug("ResizePseudoConsole failed (error " + Native.getLastError() + ")");
            }
        }
    }

    @Override
    public int waitFor() {
        try {
            finished.await();
            return exitCode;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    @Override
    public long pid() {
        return pid;
    }

    /// Gives the child `grace` to leave on its own, then kills its job object.
    ///
    /// Windows offers nothing like `SIGTERM` that reaches a process attached to a
    /// pseudo console, so this is "a window to exit, then force". The whole
    /// process tree goes with it, because every process started under the
    /// terminal is in the job.
    @Override
    public void terminate(Duration grace) {
        if (finished.getCount() == 0) {
            return;
        }
        long deadline = System.nanoTime() + Math.max(0, grace.toNanos());
        while (System.nanoTime() < deadline) {
            try {
                if (finished.await(WAIT_SLICE_MILLIS, TimeUnit.MILLISECONDS)) {
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        kill();
        try {
            finished.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        synchronized (handleLock) {
            if (closed) {
                return;
            }
            closed = true;
        }
        terminate(Duration.ofMillis(500));
        closePseudoConsole();
        // After the pseudo console, not before: closing the pipes first leaves a
        // read waiting on a console that will never write again.
        try {
            pump.join(PUMP_JOIN_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        input.close();
        output.close();
        Win32 api = Win32.INSTANCE;
        if (thread != null) {
            api.CloseHandle(thread);
        }
        api.CloseHandle(process);
        // Closing the job is what sweeps anything the child left behind
        // (JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE).
        if (job != null) {
            api.CloseHandle(job);
        }
        finished.countDown();
    }

    /// Waits for the pump to stop touching the terminal: by then every byte it
    /// read out of the console is in the replay buffer.
    boolean awaitPump(Duration timeout) throws InterruptedException {
        return pumpEnded.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void pump() {
        byte[] bytes = new byte[READ_BUFFER];
        IntByReference count = new IntByReference();
        try {
            while (true) {
                Pointer lease = output.acquire();
                if (lease == null) {
                    return;
                }
                try {
                    count.setValue(0);
                    if (!Win32.INSTANCE.ReadFile(lease, bytes, bytes.length, count, null)) {
                        int error = Native.getLastError();
                        if (!endOfOutput(error)) {
                            Logger.LOG.warning("Reading the terminal failed (error " + error
                                    + "); ending its output");
                        }
                        return;
                    }
                    int n = count.getValue();
                    if (n <= 0) {
                        return;
                    }
                    buffer.put(Arrays.copyOf(bytes, n));
                } finally {
                    Win32.INSTANCE.CloseHandle(lease);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            buffer.finish();
            pumpEnded.countDown();
        }
    }

    /// Waits for the child, then ends the session in the order the contract
    /// needs: input refused, pump drained, console closed (no further output can
    /// be observed), leftovers swept, and only then [#waitFor] answers.
    private void reap() {
        Win32 api = Win32.INSTANCE;
        try {
            while (true) {
                int wait = api.WaitForSingleObject(process, (int) WAIT_SLICE_MILLIS);
                if (wait == WAIT_OBJECT_0) {
                    break;
                }
                if (wait == WAIT_FAILED) {
                    Logger.LOG.warning("Waiting for the terminal's process failed (error "
                            + Native.getLastError() + ")");
                    break;
                }
                if (wait == WAIT_TIMEOUT) {
                    continue;
                }
                break;
            }
            IntByReference code = new IntByReference();
            if (api.GetExitCodeProcess(process, code)) {
                exitCode = code.getValue();
            }
        } finally {
            inputClosed.set(true);
            try {
                awaitPump(Duration.ofMillis(DRAIN_MILLIS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            closePseudoConsole();
            kill();
            buffer.finish();
            finished.countDown();
        }
    }

    private void kill() {
        if (job != null) {
            Win32.INSTANCE.TerminateJobObject(job, 1);
        } else {
            Win32.INSTANCE.TerminateProcess(process, 1);
        }
    }

    private void closePseudoConsole() {
        synchronized (consoleLock) {
            if (pseudoConsole == null) {
                return;
            }
            Win32.INSTANCE.ClosePseudoConsole(pseudoConsole);
            pseudoConsole = null;
        }
    }

    /// Whether a failed read means the console is gone rather than something
    /// transient: the conhost side closed (`109`), or the handle was closed
    /// under the pump by [#close] (`6`, `232`, `233`).
    private static boolean endOfOutput(int error) {
        return error == ERROR_BROKEN_PIPE || error == ERROR_INVALID_HANDLE
                || error == ERROR_NO_DATA || error == ERROR_PIPE_NOT_CONNECTED;
    }

    private static @Nullable Memory attributeList(Win32 api, Pointer pseudoConsole) throws PtyException {
        // The size query is expected to fail with ERROR_INSUFFICIENT_BUFFER;
        // succeeding would mean this machine's kernel disagrees with its own
        // documentation, and the returned size would be meaningless.
        Memory size = new Memory(8);
        api.InitializeProcThreadAttributeList(null, 1, 0, size);
        long bytes = Native.POINTER_SIZE == 8 ? size.getLong(0) : size.getInt(0);
        if (bytes <= 0) {
            throw new PtyException("InitializeProcThreadAttributeList returned no size (error "
                    + Native.getLastError() + ")");
        }
        Memory list = new Memory(bytes);
        if (!api.InitializeProcThreadAttributeList(list, 1, 0, size)) {
            throw new PtyException("InitializeProcThreadAttributeList", Native.getLastError());
        }
        if (!api.UpdateProcThreadAttribute(list, 0, PROC_THREAD_ATTRIBUTE_PSEUDOCONSOLE,
                pseudoConsole, Native.POINTER_SIZE, null, null)) {
            int error = Native.getLastError();
            api.DeleteProcThreadAttributeList(list);
            throw new PtyException("UpdateProcThreadAttribute", error);
        }
        return list;
    }

    private static boolean setKillOnJobClose(Win32 api, Pointer job) {
        JobObjectExtendedLimitInformation limits = new JobObjectExtendedLimitInformation();
        limits.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE;
        limits.write();
        return api.SetInformationJobObject(job, JOB_OBJECT_EXTENDED_LIMIT_INFORMATION_CLASS,
                limits.getPointer(), limits.size());
    }

    private static void close(Win32 api, @Nullable Pointer first, @Nullable Pointer second) {
        if (first != null) {
            api.CloseHandle(first);
        }
        if (second != null) {
            api.CloseHandle(second);
        }
    }

    private static boolean detect() {
        if (OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS) {
            return false;
        }
        if (buildNumber() < REQUIRED_BUILD) {
            return false;
        }
        try {
            // The version number is what a person can act on; the symbol is what
            // is actually there. Both are required, and `getFunction` reports a
            // missing symbol by throwing rather than by returning null.
            NativeLibrary.getInstance("kernel32").getFunction("CreatePseudoConsole");
            return true;
        } catch (UnsatisfiedLinkError | RuntimeException e) {
            return false;
        }
    }

    private static String windowsName() {
        OSVersion version = OperatingSystem.SYSTEM_VERSION;
        if (version instanceof OSVersion.Windows windows) {
            if (windows.build() >= 22000) {
                return "Windows 11";
            }
            return "Windows " + windows.major() + "." + windows.minor();
        }
        return String.valueOf(version);
    }

    /// A handle that callers use through duplicates, so `close` can free the
    /// terminal's own number at any moment.
    ///
    /// `CloseHandle` frees the handle value at once and the kernel hands it to
    /// the next pipe this JVM creates — and this JVM runs one terminal per
    /// instance. A read or a write that was already inside the kernel when the
    /// session ended would then be operating on the *next* session. Gating calls
    /// on a flag was not enough on POSIX (the check and the call are not atomic,
    /// and a blocking call loses that race reliably), and the same is true here.
    ///
    /// So the terminal's handle is only ever duplicated
    /// (`DuplicateHandle(..., DUPLICATE_SAME_ACCESS)`): the copy is the caller's
    /// for the duration of one call, and closing the terminal's handle cannot
    /// recycle it.
    private static final class Leased {

        private final Pointer handle;
        private boolean closed;

        Leased(Pointer handle) {
            this.handle = handle;
        }

        /// A duplicate for one call, or `null` when the terminal has ended. The
        /// caller must [release] it, and must not touch it after that.
        synchronized @Nullable Pointer acquire() {
            if (closed) {
                return null;
            }
            PointerByReference duplicate = new PointerByReference();
            Pointer self = Win32.INSTANCE.GetCurrentProcess();
            if (!Win32.INSTANCE.DuplicateHandle(self, handle, self, duplicate, 0, false,
                    DUPLICATE_SAME_ACCESS)) {
                return null;
            }
            return duplicate.getValue();
        }

        void release(Pointer lease) {
            Win32.INSTANCE.CloseHandle(lease);
        }

        /// Ends the terminal: no new lease, and the value goes back to the
        /// kernel now. A call that already holds a lease is unaffected.
        synchronized void close() {
            if (closed) {
                return;
            }
            closed = true;
            Win32.INSTANCE.CloseHandle(handle);
        }
    }

    /// The bytes the pump has read and the reader has not, with the end marked
    /// rather than thrown away: a chunk read just before the child exited is the
    /// tail of the screen.
    private static final class Buffer {
        private final ArrayDeque<byte[]> chunks = new ArrayDeque<>();
        private final long capacity;
        private byte[] head;
        private int headOffset;
        private long bytes;
        private boolean finished;

        Buffer(long capacity) {
            this.capacity = capacity;
        }

        synchronized void put(byte[] data) throws InterruptedException {
            while (bytes + data.length > capacity && !finished) {
                wait();
            }
            chunks.addLast(data);
            bytes += data.length;
            notifyAll();
        }

        synchronized int read(byte[] target, int offset, int length) throws InterruptedException {
            while (bytes == 0 && !finished) {
                wait();
            }
            if (bytes == 0) {
                return -1;
            }
            int copied = 0;
            while (copied < length) {
                if (head == null) {
                    head = chunks.pollFirst();
                    headOffset = 0;
                    if (head == null) {
                        break;
                    }
                }
                int n = Math.min(length - copied, head.length - headOffset);
                System.arraycopy(head, headOffset, target, offset + copied, n);
                headOffset += n;
                copied += n;
                bytes -= n;
                if (headOffset == head.length) {
                    head = null;
                }
            }
            notifyAll();
            return copied;
        }

        synchronized void finish() {
            finished = true;
            notifyAll();
        }
    }

    /// A terminal size, passed **by value** — the four bytes `CreatePseudoConsole`
    /// and `ResizePseudoConsole` expect. Passing it by reference fails silently,
    /// which is the easiest way to lose a day to this file.
    @Structure.FieldOrder({"X", "Y"})
    public static final class Coord extends Structure implements Structure.ByValue {
        public short X;
        public short Y;

        public Coord() {
        }

        public Coord(int columns, int rows) {
            this.X = (short) columns;
            this.Y = (short) rows;
        }
    }

    /// `SECURITY_ATTRIBUTES`, with `bInheritHandle` set: the pipe ends
    /// `CreatePseudoConsole` is given must be inheritable by conhost.
    @Structure.FieldOrder({"nLength", "lpSecurityDescriptor", "bInheritHandle"})
    public static final class SecurityAttributes extends Structure {
        public int nLength;
        public Pointer lpSecurityDescriptor;
        public int bInheritHandle;

        public SecurityAttributes() {
            nLength = size();
            bInheritHandle = 1;
        }
    }

    /// `STARTUPINFOEXW`: `STARTUPINFOW` plus the attribute list that carries the
    /// pseudo console. Field order is Win32's; `cb` is the whole structure's
    /// size (112 on 64-bit, 72 on 32-bit), which is what `CreateProcessW`
    /// checks.
    @Structure.FieldOrder({"cb", "lpReserved", "lpDesktop", "lpTitle", "dwX", "dwY", "dwXSize",
            "dwYSize", "dwXCountChars", "dwYCountChars", "dwFillAttribute", "dwFlags",
            "wShowWindow", "cbReserved2", "lpReserved2", "hStdInput", "hStdOutput", "hStdError",
            "lpAttributeList"})
    public static final class StartupInfoEx extends Structure {
        public int cb;
        public Pointer lpReserved;
        public Pointer lpDesktop;
        public Pointer lpTitle;
        public int dwX;
        public int dwY;
        public int dwXSize;
        public int dwYSize;
        public int dwXCountChars;
        public int dwYCountChars;
        public int dwFillAttribute;
        public int dwFlags;
        public short wShowWindow;
        public short cbReserved2;
        public Pointer lpReserved2;
        public Pointer hStdInput;
        public Pointer hStdOutput;
        public Pointer hStdError;
        public Pointer lpAttributeList;

        public StartupInfoEx() {
            cb = size();
        }
    }

    /// `PROCESS_INFORMATION`.
    @Structure.FieldOrder({"hProcess", "hThread", "dwProcessId", "dwThreadId"})
    public static final class ProcessInformation extends Structure {
        public Pointer hProcess;
        public Pointer hThread;
        public int dwProcessId;
        public int dwThreadId;
    }

    /// `JOBOBJECT_EXTENDED_LIMIT_INFORMATION`, flattened. Only `LimitFlags` is
    /// set here; the rest must still be present and in order, because Windows
    /// writes nothing back but reads the whole structure by size.
    @Structure.FieldOrder({"PerProcessUserTimeLimit", "PerJobUserTimeLimit", "LimitFlags",
            "MinimumWorkingSetSize", "MaximumWorkingSetSize", "ActiveProcessLimit", "Affinity",
            "PriorityClass", "SchedulingClass", "ReadOperationCount", "WriteOperationCount",
            "OtherOperationCount", "ReadTransferCount", "WriteTransferCount",
            "OtherTransferCount", "ProcessMemoryLimit", "JobMemoryLimit",
            "PeakProcessMemoryUsed", "PeakJobMemoryUsed"})
    public static final class JobObjectExtendedLimitInformation extends Structure {
        public long PerProcessUserTimeLimit;
        public long PerJobUserTimeLimit;
        public int LimitFlags;
        public long MinimumWorkingSetSize;
        public long MaximumWorkingSetSize;
        public int ActiveProcessLimit;
        public long Affinity;
        public int PriorityClass;
        public int SchedulingClass;
        public long ReadOperationCount;
        public long WriteOperationCount;
        public long OtherOperationCount;
        public long ReadTransferCount;
        public long WriteTransferCount;
        public long OtherTransferCount;
        public long ProcessMemoryLimit;
        public long JobMemoryLimit;
        public long PeakProcessMemoryUsed;
        public long PeakJobMemoryUsed;
    }

    /// `kernel32` as this file needs it.
    ///
    /// `SIZE_T` out-parameters are handed over as raw [Memory] rather than
    /// through JNA's fixed-width integer references: a `SIZE_T` is pointer-sized,
    /// and a four-byte reference would be written over by an eight-byte value.
    public interface Win32 extends Library {
        Win32 INSTANCE = Native.load("kernel32", Win32.class);

        int CreatePseudoConsole(Coord size, Pointer input, Pointer output, int flags,
                                PointerByReference pseudoConsole);

        boolean ResizePseudoConsole(Pointer pseudoConsole, Coord size);

        void ClosePseudoConsole(Pointer pseudoConsole);

        boolean InitializeProcThreadAttributeList(Pointer list, int count, int flags, Pointer size);

        boolean UpdateProcThreadAttribute(Pointer list, int flags, long attribute, Pointer value,
                                          int size, Pointer previous, IntByReference returned);

        void DeleteProcThreadAttributeList(Pointer list);

        boolean CreateProcessW(WString application, WString commandLine, Pointer processAttributes,
                               Pointer threadAttributes, boolean inheritHandles, int flags,
                               Pointer environment, WString currentDirectory, StartupInfoEx startup,
                               ProcessInformation information);

        Pointer CreateJobObjectW(Pointer attributes, WString name);

        boolean SetInformationJobObject(Pointer job, int informationClass, Pointer information, int length);

        boolean AssignProcessToJobObject(Pointer job, Pointer process);

        boolean TerminateJobObject(Pointer job, int exitCode);

        boolean TerminateProcess(Pointer process, int exitCode);

        boolean CreatePipe(PointerByReference read, PointerByReference write,
                           SecurityAttributes attributes, int size);

        boolean SetHandleInformation(Pointer handle, int mask, int flags);

        boolean DuplicateHandle(Pointer sourceProcess, Pointer source, Pointer targetProcess,
                                PointerByReference target, int access, boolean inherit, int options);

        Pointer GetCurrentProcess();

        boolean CloseHandle(Pointer handle);

        boolean ReadFile(Pointer handle, byte[] buffer, int toRead, IntByReference read, Pointer overlapped);

        boolean WriteFile(Pointer handle, byte[] buffer, int toWrite, IntByReference written,
                          Pointer overlapped);

        int WaitForSingleObject(Pointer handle, int milliseconds);

        boolean GetExitCodeProcess(Pointer process, IntByReference exitCode);

        int ResumeThread(Pointer thread);
    }
}
