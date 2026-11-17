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
package org.jackhuang.hmcl.web.terminal;

import org.jackhuang.hmcl.util.platform.VirtualThreads;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jackhuang.hmcl.web.terminal.pty.Pty;
import org.jackhuang.hmcl.web.terminal.pty.PtySize;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/// One running agent TUI, detached from any viewer.
///
/// The PTY process belongs to the session, not to a WebSocket: closing the
/// browser only [detach]es. The session keeps the last [replayBytes] of output
/// and hands it to the next viewer, so a refresh comes back to the same screen
/// — while nothing about the agent is restarted.
///
/// When the child exits, the pump drains the last bytes, marks the session
/// [State#EXITED], tells every attached sink the exit code, and answers
/// [#waitFor]. Input is refused from that moment on: the terminal can never be
/// handed to anything else. A child that exits non-zero without writing a byte
/// is reported in the app log and in the exit frame, because that failure leaves
/// no other trace ([#silentFailure]). An exit this panel caused is not one:
/// stopping a terminal is supposed to produce no output.
@NotNullByDefault
public final class TerminalSession {

    public enum State {
        RUNNING,
        EXITED
    }

    private final String instanceId;
    private final Pty pty;
    private final byte[] replay;
    private final Object lock = new Object();
    private final List<TerminalSink> sinks = new ArrayList<>();
    /// Only ever read and written under [lock], like [sinks]: the exit path
    /// takes its snapshot inside that lock.
    private final List<Runnable> exitListeners = new ArrayList<>();
    private final CountDownLatch exited = new CountDownLatch(1);
    private final Thread pump;
    private int replayCount;
    private volatile State state = State.RUNNING;
    private volatile int exitCode = Integer.MIN_VALUE;
    /// Everything the child ever wrote, replay buffer or not. The pump reads it
    /// only after the output stream reached its end, so no lock is needed for
    /// that reading thread; it is volatile so a viewer that is told about the
    /// exit sees the final count.
    private volatile long outputBytes;
    /// Set when this panel is the one ending the session. A stop we asked for
    /// reports the signal it took as the exit code and prints nothing on the way
    /// out, so it has the same shape as a failure without being one.
    private volatile boolean endedByPanel;

    /// @param replayBytes how much output is kept for a reattach
    public TerminalSession(String instanceId, Pty pty, int replayBytes) {
        if (replayBytes < 0) {
            throw new IllegalArgumentException("negative replay size");
        }
        this.instanceId = instanceId;
        this.pty = pty;
        this.replay = new byte[replayBytes];
        // A platform thread for the same reason the PTY's own pump is one: this
        // loop blocks inside a native read, which pins whatever thread runs it.
        // Two virtual threads per terminal (this one and the PTY's reaper) were
        // enough to take every carrier of a 4-core box and leave the second
        // terminal's pump unscheduled — a running child behind an empty screen.
        this.pump = VirtualThreads.startPlatform(this::pump, "hdsl-terminal-" + instanceId);
    }

    public String instanceId() {
        return instanceId;
    }

    /// The pid of the terminal's root process — the instance's live root, as
    /// [org.jackhuang.hmcl.web.auth.PeerIdentity] sees it.
    ///
    /// @return the pid, or `-1` when the platform does not name one
    public long pid() {
        return pty.pid();
    }

    public State state() {
        return state;
    }

    /// The child's exit code once [State#EXITED], `Integer.MIN_VALUE` before.
    public int exitCode() {
        return exitCode;
    }

    /// Whether the session failed without ever writing a byte.
    ///
    /// A child that dies instantly — a missing plugin, a rejected argument, a
    /// crash before the first paint — leaves the same empty screen as a viewer
    /// that missed the output, and only this counter tells the two apart. It is
    /// what lets the exit frame say the terminal never printed anything instead
    /// of showing a bare non-zero code.
    ///
    /// A session this panel ended does not count, whatever its exit code: a stop
    /// we asked for takes a signal and prints nothing, so both halves of this
    /// predicate hold for it — and reporting that as a failure would put a
    /// diagnosis in the log and a hint in the browser every time somebody
    /// pressed 停止, deleted an instance, or let the lifecycle close a terminal
    /// behind them.
    public boolean silentFailure() {
        return exitCode != 0 && outputBytes == 0 && !endedByPanel;
    }

    /// Bytes typed at the terminal.
    public OutputStream input() {
        return pty.input();
    }

    /// Tells the child the viewer's terminal changed size.
    public void resize(PtySize size) {
        pty.resize(size);
    }

    /// Attaches a viewer, replaying what it missed first.
    ///
    /// The snapshot and the registration happen under the same lock the pump
    /// broadcasts under, so a viewer sees every byte exactly once: either in
    /// the snapshot or live, never both and never neither.
    public void attach(TerminalSink sink) {
        synchronized (lock) {
            if (state == State.RUNNING) {
                Logger.LOG.info("Terminal reattach for " + instanceId + ": replaying " + replayCount + " bytes");
                if (replayCount > 0) {
                    byte[] snapshot = Arrays.copyOf(replay, replayCount);
                    sink.data(snapshot, 0, snapshot.length);
                }
                sinks.add(sink);
                return;
            }
        }
        sink.exited(exitCode);
    }

    /// Removes a viewer. The agent keeps running.
    public void detach(TerminalSink sink) {
        synchronized (lock) {
            sinks.remove(sink);
        }
    }

    /// Ends the child's process group, waiting up to `grace` for the exit.
    public void terminate(Duration grace) {
        // Before the signal: the exit it causes is not a failure to diagnose, and
        // the pump may read the flag the moment the child is gone.
        endedByPanel = true;
        pty.terminate(grace);
        try {
            // The pump reports the exit; give it the same grace again so a
            // caller that terminates and closes sees a settled session.
            exited.await(Math.max(1, grace.toMillis()) + 500, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /// Runs `listener` once the child is gone. Used by the manager to drop the
    /// instance → session mapping without polling.
    ///
    /// The state check and the registration happen under the same lock in which
    /// the pump marks the session exited and takes its snapshot of this list: a
    /// listener is either in that snapshot or run here, never both. Running it
    /// here while the pump still held the lock would mean a second run.
    public void onExit(Runnable listener) {
        synchronized (lock) {
            if (state == State.RUNNING) {
                exitListeners.add(listener);
                return;
            }
        }
        listener.run();
    }

    /// Waits for the child and returns its exit code.
    public int waitFor() {
        try {
            exited.await();
            return exitCode;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    /// Terminates the child and releases the terminal.
    public void close() {
        terminate(Duration.ofSeconds(2));
        pty.close();
    }

    private void pump() {
        byte[] buffer = new byte[16 * 1024];
        try (InputStream output = pty.output()) {
            int n;
            while ((n = output.read(buffer, 0, buffer.length)) >= 0) {
                broadcast(buffer, 0, n);
            }
        } catch (IOException ignored) {
            // The PTY was closed under us (shutdown or a broken child); the
            // exit path below still runs and reports it, and the byte count it
            // reads is the one this pump actually saw.
        } finally {
            int code = pty.waitFor();
            exitCode = code;
            if (silentFailure()) {
                // A terminal that dies without a single byte of output leaves
                // nothing to diagnose: the audit line has an exit code and the
                // app log has a closed socket, and neither says the child never
                // spoke. Say that one fact — it is what the session knows — so
                // the line leads somewhere instead of reading like every other
                // exit.
                Logger.LOG.warning("Terminal " + instanceId + " exited with code " + code
                        + " without producing any output");
            }
            List<Runnable> listeners;
            synchronized (lock) {
                // The state write and the snapshot are one step under this lock,
                // which is what makes onExit's listener run exactly once: a
                // caller that lost the race against this block finds EXITED and
                // runs its listener itself.
                state = State.EXITED;
                for (TerminalSink sink : sinks) {
                    sink.exited(code);
                }
                sinks.clear();
                listeners = new ArrayList<>(exitListeners);
            }
            exited.countDown();
            for (Runnable listener : listeners) {
                listener.run();
            }
        }
    }

    private void broadcast(byte[] data, int offset, int length) {
        synchronized (lock) {
            outputBytes += length;
            appendReplay(data, offset, length);
            for (TerminalSink sink : sinks) {
                sink.data(data, offset, length);
            }
        }
    }

    /// Keeps the most recent bytes, dropping the oldest when full.
    private void appendReplay(byte[] data, int offset, int length) {
        if (replay.length == 0) {
            return;
        }
        if (length >= replay.length) {
            System.arraycopy(data, offset + length - replay.length, replay, 0, replay.length);
            replayCount = replay.length;
            return;
        }
        if (replayCount + length > replay.length) {
            int drop = replayCount + length - replay.length;
            System.arraycopy(replay, drop, replay, 0, replayCount - drop);
            replayCount -= drop;
        }
        System.arraycopy(data, offset, replay, replayCount, length);
        replayCount += length;
    }
}
