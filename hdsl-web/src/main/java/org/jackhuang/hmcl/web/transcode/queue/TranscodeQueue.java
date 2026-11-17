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
package org.jackhuang.hmcl.web.transcode.queue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.jackhuang.hmcl.util.ProcessTree;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jackhuang.hmcl.util.platform.Privileges;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jackhuang.hmcl.web.event.EventBus;
import org.jackhuang.hmcl.web.http.Json;
import org.jackhuang.hmcl.web.transcode.TranscodeModule;
import org.jackhuang.hmcl.web.transcode.command.CommandBuilder;
import org.jackhuang.hmcl.web.transcode.preset.TranscodePreset;
import org.jackhuang.hmcl.web.transcode.runtime.FfmpegProbe;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/// The server-side encode queue.
///
/// Everything that matters happens here, not in the browser: the scheduler, the child
/// processes, progress parsing, logs and the persisted queue. Closing the page or an
/// agent disconnecting only stops the observer; the jobs keep running.
///
/// **First start is explicit.** A queue in [RunState#IDLE] accepts jobs but starts none;
/// [startQueue] moves it to [RunState#RUNNING], and from then on completed jobs free a
/// slot for the next queued one and newly submitted jobs start automatically. [RunState#PAUSED]
/// stops dispatch but lets already-running jobs finish.
@NotNullByDefault
public final class TranscodeQueue {

    public enum RunState {
        @SerializedName("idle") IDLE,
        @SerializedName("running") RUNNING,
        @SerializedName("paused") PAUSED
    }

    /// A deliberate pause (`队列暂停`): nothing lifts it except the operator.
    private static final String PAUSE_OPERATOR = "operator";
    /// A pause that exists only because a job is parked for a decision. It is lifted
    /// automatically when the last parked job leaves the queue.
    private static final String PAUSE_DECISION = "decision";

    private final TranscodeModule module;
    private final EventBus bus;
    private final Path dataDir;
    private final Path mediaRoot;
    private final Path logsDir;
    private final Path queueFile;
    private final Object lock = new Object();
    private final List<TranscodeJob> jobs = new ArrayList<>();
    /// Outputs promised to queued/running jobs. `Files.exists()` cannot see a file
    /// that has not been written yet, so the queue reserves the name at submit time.
    private final Set<Path> reservedOutputs = new HashSet<>();

    private final ExecutorService workers = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "transcode-worker");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService scheduler = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "transcode-scheduler");
        thread.setDaemon(true);
        return thread;
    });
    private final ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "transcode-decision-timeout");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, ScheduledFuture<?>> decisionTimeouts = new ConcurrentHashMap<>();

    private volatile RunState runState = RunState.IDLE;
    /// Why the queue is paused. `decision` means a job is parked waiting for the
    /// operator; once the last such job is gone (removed, cancelled, re-pointed) the
    /// pause has no reason left and dispatch must resume on its own. `operator` is a
    /// deliberate pause and is never lifted implicitly. Persisted with the queue so a
    /// restart cannot turn a parked pause into a silent manual one.
    private String pauseReason = PAUSE_OPERATOR;
    private boolean started;

    public TranscodeQueue(TranscodeModule module, EventBus bus) {
        this.module = module;
        this.bus = bus;
        this.dataDir = module.dataDir();
        this.mediaRoot = module.mediaRoot();
        this.logsDir = dataDir.resolve("logs");
        this.queueFile = dataDir.resolve("queue.json");
    }

    /// Opens the queue (and resumes if the saved state says RUNNING and auto-resume is on).
    public void start() {
        synchronized (lock) {
            if (started) {
                return;
            }
            try {
                Files.createDirectories(dataDir);
                Files.createDirectories(logsDir);
            } catch (IOException e) {
                Logger.LOG.warning("Could not create the transcode data directory " + dataDir, e);
            }
            loadLocked();
            started = true;
            if (runState == RunState.RUNNING && !module.config().transcode.autoResume) {
                runState = RunState.PAUSED;
                pauseReason = PAUSE_OPERATOR;
            }
            saveLocked();
        }
        publishQueue();
        if (runState == RunState.RUNNING) {
            dispatch();
        }
    }

    /// Stops the queue and its children. Used when the feature is disabled.
    public void stop() {
        List<Process> processes = new ArrayList<>();
        synchronized (lock) {
            if (!started) {
                return;
            }
            started = false;
            runState = RunState.IDLE;
            cancelAllDecisionTimeouts();
            long now = System.currentTimeMillis();
            for (TranscodeJob job : jobs) {
                if (!job.state.terminal()) {
                    job.state = TranscodeJob.State.CANCELLED;
                    job.error = "transcode feature disabled";
                    job.endedAt = now;
                    releaseReservation(job);
                    if (job.process != null) {
                        processes.add(job.process);
                        job.process = null;
                    }
                }
            }
            saveLocked();
        }
        for (Process process : processes) {
            ProcessTree.terminate(process, 2);
        }
        publishQueue();
    }

    /// Adds a job. `input` and `outputDir` are relative to the media root.
    public JsonObject submit(String input, String outputDir, String presetId, TranscodePreset preset)
            throws IOException {
        start();
        Path inputPath = module.media().resolve(mediaRoot, input);
        Path directory;
        if (outputDir != null && !outputDir.isBlank()) {
            directory = module.media().resolveForWrite(mediaRoot, outputDir);
        } else if (preset.outputDir != null && !preset.outputDir.isBlank()) {
            directory = module.media().resolveForWrite(mediaRoot, preset.outputDir);
        } else {
            directory = inputPath.getParent();
        }
        // Build once before accepting: a preset that cannot produce a command should
        // fail at submit, not after the job has already taken a queue slot.
        Path chapters = preset.chaptersFile == null || preset.chaptersFile.isBlank()
                ? null : module.media().resolve(mediaRoot, preset.chaptersFile);
        Path subtitle = preset.subtitleFile == null || preset.subtitleFile.isBlank()
                ? null : module.media().resolve(mediaRoot, preset.subtitleFile);
        CommandBuilder.buildPasses(preset, inputPath, directory.resolve("preview.mkv"), chapters, subtitle);

        TranscodeJob job = new TranscodeJob();
        job.id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        job.input = relative(inputPath);
        job.preset = preset.copy();
        job.presetId = presetId == null ? "" : presetId;
        job.presetName = preset.name == null ? "" : preset.name;
        job.encoder = preset.videoEncoder;
        job.logFile = logsDir.resolve(job.id + ".log").toString();
        job.createdAt = System.currentTimeMillis();

        synchronized (lock) {
            long active = jobs.stream().filter(candidate -> !candidate.state.terminal()).count();
            if (active >= module.config().transcode.maxQueue) {
                throw new IOException("the transcode queue is full (" + module.config().transcode.maxQueue + ")");
            }
            Path outputPath = CommandBuilder.outputPath(preset, inputPath, directory, reservedOutputs);
            reservedOutputs.add(outputPath);
            job.output = relative(outputPath);
            jobs.add(job);
            saveLocked();
        }
        publishQueue();
        if (runState == RunState.RUNNING) {
            dispatch();
        }
        return jobJson(job.id).orElseThrow();
    }

    /// Adds a job whose argv is already decided by a tool. The queue still owns the
    /// process, log, progress, cancellation and the server-side lifetime; only the
    /// command construction is different.
    public JsonObject submitCommand(String label, String outputRel, List<String> args, String tool)
            throws IOException {
        start();
        Path output = module.media().resolveForWrite(mediaRoot, outputRel);
        if (Files.isDirectory(output)) {
            throw new IOException("output must be a file path, not a directory: " + outputRel);
        }
        TranscodeJob job = new TranscodeJob();
        job.id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        job.input = label;
        job.preset = new TranscodePreset();
        job.preset.container = extension(output);
        job.presetName = tool;
        job.tool = tool;
        job.encoder = "copy";
        job.command = List.copyOf(args);
        job.logFile = logsDir.resolve(job.id + ".log").toString();
        job.createdAt = System.currentTimeMillis();
        synchronized (lock) {
            long active = jobs.stream().filter(candidate -> !candidate.state.terminal()).count();
            if (active >= module.config().transcode.maxQueue) {
                throw new IOException("the transcode queue is full (" + module.config().transcode.maxQueue + ")");
            }
            Path unique = uniqueOutput(output);
            reservedOutputs.add(unique);
            job.output = relative(unique);
            jobs.add(job);
            saveLocked();
        }
        publishQueue();
        if (runState == RunState.RUNNING) {
            dispatch();
        }
        return jobJson(job.id).orElseThrow();
    }

    /// First explicit start (or resume after a pause).
    public void startQueue() {
        start();
        synchronized (lock) {
            runState = RunState.RUNNING;
            pauseReason = PAUSE_OPERATOR;
            saveLocked();
        }
        publishQueue();
        dispatch();
    }

    /// Stop dispatching; running jobs continue.
    public void pauseQueue() {
        synchronized (lock) {
            runState = RunState.PAUSED;
            pauseReason = PAUSE_OPERATOR;
            saveLocked();
        }
        publishQueue();
    }

    /// Lifts a decision-pause once no job is waiting for a decision anymore. Without
    /// this, removing (or re-pointing) the parked job leaves the whole queue paused and
    /// the jobs behind it never start — the operator did not press 暂停, so the pause is
    /// not theirs to clear. Called while holding [lock]; returns true when the queue
    /// moved back to RUNNING and the caller should dispatch.
    private boolean resumeAfterDecisionLocked() {
        if (runState != RunState.PAUSED || !PAUSE_DECISION.equals(pauseReason)) {
            return false;
        }
        for (TranscodeJob job : jobs) {
            if (job.state == TranscodeJob.State.WAITING_DECISION) {
                return false;
            }
        }
        runState = RunState.RUNNING;
        pauseReason = PAUSE_OPERATOR;
        return true;
    }

    /// Cancel everything that is not finished. The queue goes back to IDLE.
    public void stopAll() {
        List<Process> processes = new ArrayList<>();
        synchronized (lock) {
            runState = RunState.IDLE;
            pauseReason = PAUSE_OPERATOR;
            cancelAllDecisionTimeouts();
            long now = System.currentTimeMillis();
            for (TranscodeJob job : jobs) {
                if (!job.state.terminal()) {
                    job.state = TranscodeJob.State.CANCELLED;
                    job.error = "stopped by the operator";
                    job.endedAt = now;
                    releaseReservation(job);
                    if (job.process != null) {
                        processes.add(job.process);
                        job.process = null;
                    }
                }
            }
            saveLocked();
        }
        for (Process process : processes) {
            ProcessTree.terminate(process, 2);
        }
        publishQueue();
    }

    public boolean cancel(String id) {
        Process process = null;
        synchronized (lock) {
            TranscodeJob job = find(id);
            if (job == null || job.state.terminal()) {
                return false;
            }
            job.state = TranscodeJob.State.CANCELLED;
            job.error = "cancelled by the operator";
            job.endedAt = System.currentTimeMillis();
            // A parked job owes a decision; cancelling it means nobody has to answer.
            cancelDecisionTimeout(id);
            job.decisionDeadline = -1;
            releaseReservation(job);
            resumeAfterDecisionLocked();
            if (job.process != null) {
                process = job.process;
                job.process = null;
            }
            saveLocked();
        }
        if (process != null) {
            ProcessTree.terminate(process, 2);
        }
        publishQueue();
        dispatch();
        return true;
    }

    public boolean retry(String id) {
        synchronized (lock) {
            TranscodeJob job = find(id);
            if (job == null || !(job.state == TranscodeJob.State.FAILED
                    || job.state == TranscodeJob.State.CANCELLED)) {
                return false;
            }
            // A tool job keeps its argv in `command` and a label in `input`, and
            // resetForRetry() clears both. Put them back, or the retry would try to
            // transcode a file named like "提取 0:v:0 · input.mp4".
            String tool = job.tool;
            List<String> command = job.command;
            String encoder = job.encoder;
            job.resetForRetry();
            job.tool = tool == null ? "" : tool;
            job.command = command == null ? List.of() : command;
            job.encoder = encoder;
            // Cancel/remove released the name, so a job submitted in between may have
            // taken it. Keep the old name when it is still free, otherwise pick `_1`
            // instead of letting two jobs write the same file.
            Path output = uniqueOutput(mediaRoot.resolve(job.output));
            reservedOutputs.add(output);
            job.output = relative(output);
            saveLocked();
        }
        publishQueue();
        if (runState == RunState.RUNNING) {
            dispatch();
        }
        return true;
    }

    /// Removes one job from the queue. Running/paused jobs must be stopped first:
    /// the UI greys the button out and the API refuses with 409. Only the queue
    /// record goes away — media files already produced are never deleted, which
    /// matches 3FUI's `remove` contract.
    public boolean remove(String id) {
        synchronized (lock) {
            TranscodeJob job = find(id);
            if (job == null || !removable(job)) {
                return false;
            }
            cleanupLocked(job);
            jobs.remove(job);
            resumeAfterDecisionLocked();
            saveLocked();
        }
        publishQueue();
        if (runState == RunState.RUNNING) {
            dispatch();
        }
        return true;
    }

    /// Drops every terminal record (done / failed / cancelled). Queued, running and
    /// waiting-for-decision jobs stay: a decision is still owed to the operator.
    public int clearTerminal() {
        List<TranscodeJob> dropped = new ArrayList<>();
        synchronized (lock) {
            for (TranscodeJob job : jobs) {
                if (job.state.terminal()) {
                    cleanupLocked(job);
                    dropped.add(job);
                }
            }
            if (dropped.isEmpty()) {
                return 0;
            }
            jobs.removeAll(dropped);
            resumeAfterDecisionLocked();
            saveLocked();
        }
        publishQueue();
        if (runState == RunState.RUNNING) {
            dispatch();
        }
        return dropped.size();
    }

    /// Starts one queued job now: it jumps to the front of the queued segment and the
    /// queue is switched to RUNNING, so `dispatch()` picks it at the first free slot.
    ///
    /// 3FUI's `开始任务` launches the job without looking at the concurrency limit; that
    /// lets the operator silently break the limit they configured, so this one still
    /// goes through `dispatchNow()`.
    public boolean startJob(String id) {
        synchronized (lock) {
            TranscodeJob job = find(id);
            if (job == null || job.state != TranscodeJob.State.QUEUED || job.process != null) {
                return false;
            }
            TranscodeJob firstQueued = null;
            for (TranscodeJob candidate : jobs) {
                if (candidate != job && candidate.state == TranscodeJob.State.QUEUED) {
                    firstQueued = candidate;
                    break;
                }
            }
            if (firstQueued != null) {
                jobs.remove(job);
                jobs.add(jobs.indexOf(firstQueued), job);
            }
            runState = RunState.RUNNING;
            saveLocked();
        }
        publishQueue();
        dispatch();
        return true;
    }

    /// Reorders the whole queue. `ids` must name every job exactly once and no job that
    /// owns a slot (running / paused / stopping / waiting for a decision) may move —
    /// same rule as 3FUI's `可排序`; violating either refuses the whole request instead
    /// of applying half of it.
    public boolean reorder(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        synchronized (lock) {
            if (ids.size() != jobs.size()) {
                return false;
            }
            Map<String, TranscodeJob> byId = new HashMap<>();
            for (TranscodeJob job : jobs) {
                byId.put(job.id, job);
            }
            if (new HashSet<>(ids).size() != ids.size() || byId.size() != ids.size()
                    || !byId.keySet().containsAll(ids)) {
                return false;
            }
            for (int i = 0; i < jobs.size(); i++) {
                TranscodeJob current = jobs.get(i);
                if (!reorderable(current) && !ids.get(i).equals(current.id)) {
                    return false;
                }
            }
            List<TranscodeJob> next = new ArrayList<>(jobs.size());
            for (String id : ids) {
                next.add(byId.get(id));
            }
            jobs.clear();
            jobs.addAll(next);
            saveLocked();
        }
        publishQueue();
        return true;
    }

    /// Re-queues a stopped/finished job with a different parameter preset.
    ///
    /// A job owns a snapshot of the preset it was submitted with, so editing a
    /// preset later never changes an existing encode. This is the explicit
    /// "use this preset instead" action: replace the snapshot, clear progress,
    /// log and decision state, and re-plan the output (the container may have
    /// changed). An already produced file is kept — the fresh encode gets a `_1`
    /// name instead of overwriting it. Tool jobs (merge/mux/extract) have no
    /// preset of their own and are refused.
    public boolean resetPreset(String id, String presetId, TranscodePreset preset) throws IOException {
        boolean changed = applyPreset(id, presetId, preset, false);
        if (changed) {
            publishQueue();
            if (runState == RunState.RUNNING) {
                dispatch();
            }
        }
        return changed;
    }

    /// Re-points queued jobs at another preset, the batch form of [resetPreset]. Only
    /// not-yet-started jobs are touched: rewriting the preset of a running encode would
    /// swap its argv mid-flight. Tool jobs have no preset, terminal records are left to
    /// `clearTerminal`. Returns the counts the UI shows in a toast.
    public JsonObject syncPreset(String presetId, TranscodePreset preset, @Nullable List<String> ids) {
        List<String> targets = new ArrayList<>();
        int skippedTool = 0;
        int skippedActive = 0;
        int skippedTerminal = 0;
        synchronized (lock) {
            boolean filtered = ids != null && !ids.isEmpty();
            for (TranscodeJob job : jobs) {
                if (filtered && !ids.contains(job.id)) {
                    continue;
                }
                if (job.state.terminal()) {
                    skippedTerminal++;
                } else if (job.tool != null && !job.tool.isBlank()) {
                    skippedTool++;
                } else if (!presetSwappable(job, true)) {
                    skippedActive++;
                } else {
                    targets.add(job.id);
                }
            }
        }
        int updated = 0;
        int failed = 0;
        for (String id : targets) {
            try {
                if (applyPreset(id, presetId, preset, true)) {
                    updated++;
                } else {
                    failed++;
                }
            } catch (IOException | IllegalArgumentException e) {
                failed++;
            }
        }
        JsonObject result = new JsonObject();
        result.addProperty("updated", updated);
        result.addProperty("failed", failed);
        result.addProperty("skippedTool", skippedTool);
        result.addProperty("skippedActive", skippedActive);
        result.addProperty("skippedTerminal", skippedTerminal);
        if (updated > 0) {
            publishQueue();
            if (runState == RunState.RUNNING) {
                dispatch();
            }
        }
        return result;
    }

    /// The shared "point this job at another preset" flow. Validation runs outside the
    /// lock so a slow media resolve cannot stall every other API call; the mutation then
    /// re-checks the state, because the job may have started in between.
    private boolean applyPreset(String id, String presetId, TranscodePreset preset, boolean queuedOnly)
            throws IOException {
        Path input;
        Path directory;
        synchronized (lock) {
            TranscodeJob job = find(id);
            if (job == null || !presetSwappable(job, queuedOnly)) {
                return false;
            }
            input = module.media().resolve(mediaRoot, job.input);
            Path parent = Path.of(job.output).getParent();
            directory = module.media().resolveForWrite(mediaRoot,
                    parent == null ? "." : parent.toString());
        }
        // Validate before mutating: a preset that cannot produce a command must
        // fail here, not after the job has gone back to QUEUED.
        Path chapters = preset.chaptersFile == null || preset.chaptersFile.isBlank()
                ? null : module.media().resolve(mediaRoot, preset.chaptersFile);
        Path subtitle = preset.subtitleFile == null || preset.subtitleFile.isBlank()
                ? null : module.media().resolve(mediaRoot, preset.subtitleFile);
        CommandBuilder.buildPasses(preset, input, directory.resolve("preview.mkv"), chapters, subtitle);

        synchronized (lock) {
            TranscodeJob job = find(id);
            if (job == null || !presetSwappable(job, queuedOnly)) {
                return false;
            }
            releaseReservation(job);
            Path output = CommandBuilder.outputPath(preset, input, directory, reservedOutputs);
            reservedOutputs.add(output);
            job.output = relative(output);
            cancelDecisionTimeout(id);
            deleteLog(job);
            job.resetForRetry();
            // The encoder history belongs to the old preset; a new preset starts clean.
            job.switchRecords = new ArrayList<>();
            job.preset = preset.copy();
            job.presetId = presetId == null ? "" : presetId;
            job.presetName = preset.name == null ? "" : preset.name;
            job.encoder = preset.videoEncoder;
            resumeAfterDecisionLocked();
            saveLocked();
        }
        return true;
    }

    private static boolean presetSwappable(TranscodeJob job, boolean queuedOnly) {
        if (queuedOnly) {
            return job.state == TranscodeJob.State.QUEUED
                    && job.process == null
                    && (job.tool == null || job.tool.isBlank());
        }
        return resettable(job);
    }

    private static boolean removable(TranscodeJob job) {
        return job.process == null
                && job.state != TranscodeJob.State.RUNNING
                && job.state != TranscodeJob.State.PAUSED
                && job.state != TranscodeJob.State.STOPPING;
    }

    private static boolean resettable(TranscodeJob job) {
        return removable(job) && (job.tool == null || job.tool.isBlank());
    }

    /// Position may only change for jobs that own no slot: a running / paused /
    /// judging job must keep its place or the "当前 / 下一个" reading becomes a lie.
    private static boolean reorderable(TranscodeJob job) {
        return job.process == null
                && job.state != TranscodeJob.State.RUNNING
                && job.state != TranscodeJob.State.PAUSED
                && job.state != TranscodeJob.State.STOPPING
                && job.state != TranscodeJob.State.WAITING_DECISION;
    }

    /// Shared cleanup for remove / clear: cancel a pending decision timer, release
    /// the promised output name and drop the job's log file. Called while holding lock.
    private void cleanupLocked(TranscodeJob job) {
        cancelDecisionTimeout(job.id);
        releaseReservation(job);
        deleteLog(job);
    }

    private void deleteLog(TranscodeJob job) {
        if (job.logFile == null || job.logFile.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(job.logFile));
        } catch (IOException e) {
            Logger.LOG.warning("Could not remove the transcode log " + job.logFile, e);
        }
    }

    /// SIGSTOP/SIGCONT on a POSIX host. Windows has no equivalent Java API; the
    /// caller gets a clear refusal instead of a silent no-op.
    public boolean pause(String id) throws IOException {
        return signal(id, "STOP", TranscodeJob.State.RUNNING, TranscodeJob.State.PAUSED);
    }

    public boolean resume(String id) throws IOException {
        return signal(id, "CONT", TranscodeJob.State.PAUSED, TranscodeJob.State.RUNNING);
    }

    private boolean signal(String id, String signal, TranscodeJob.State expected,
                           TranscodeJob.State target) throws IOException {
        Process process;
        synchronized (lock) {
            TranscodeJob job = find(id);
            if (job == null || job.state != expected || job.process == null) {
                return false;
            }
            if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS) {
                throw new IOException("pause/resume is not supported on Windows in this build");
            }
            process = job.process;
            job.state = target;
            saveLocked();
        }
        try {
            Process kill = new ProcessBuilder("kill", "-" + signal, Long.toString(process.pid()))
                    .redirectErrorStream(true)
                    .start();
            if (kill.waitFor() != 0) {
                throw new IOException("kill -" + signal + " failed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("pause/resume interrupted", e);
        }
        publishQueue();
        return true;
    }

    public Optional<JsonObject> jobJson(String id) {
        synchronized (lock) {
            TranscodeJob job = find(id);
            return job == null ? Optional.empty() : Optional.of(jobView(job));
        }
    }

    public JsonObject snapshot() {
        synchronized (lock) {
            JsonObject body = new JsonObject();
            body.addProperty("runState", runState.name().toLowerCase(Locale.ROOT));
            body.addProperty("maxConcurrent", module.config().transcode.maxConcurrent);
            body.addProperty("maxQueue", module.config().transcode.maxQueue);
            JsonArray array = new JsonArray();
            for (TranscodeJob job : jobs) {
                array.add(jobView(job));
            }
            body.add("jobs", array);
            return body;
        }
    }

    /// The job as the UI sees it: the stored fields plus a `caps` block saying which
    /// actions are legal right now. The frontend must not re-derive this from `state`
    /// — the two copies drifted apart once already (`resettable` is wider than 3FUI's).
    /// `caps` is derived, never persisted.
    private JsonObject jobView(TranscodeJob job) {
        JsonObject view = Json.GSON.toJsonTree(job).getAsJsonObject();
        JsonObject caps = new JsonObject();
        caps.addProperty("start", job.state == TranscodeJob.State.QUEUED && job.process == null);
        caps.addProperty("pause", job.state == TranscodeJob.State.RUNNING
                && job.process != null && pausesNatively());
        caps.addProperty("resume", job.state == TranscodeJob.State.PAUSED && job.process != null);
        caps.addProperty("stop", !job.state.terminal() && job.state != TranscodeJob.State.STOPPING);
        caps.addProperty("remove", removable(job));
        caps.addProperty("reset", resettable(job));
        caps.addProperty("reorder", reorderable(job));
        view.add("caps", caps);
        return view;
    }

    /// `pause`/`resume` shell out to `kill -STOP/CONT`; Windows has no equivalent, so
    /// the button must not be offered there at all instead of failing after the click.
    private static boolean pausesNatively() {
        return OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS;
    }

    public long activeCount() {
        synchronized (lock) {
            return jobs.stream().filter(job -> !job.state.terminal()).count();
        }
    }

    /// The tail of one job's ffmpeg log, newest last.
    public String log(String id, int tail) {
        String logFile;
        synchronized (lock) {
            TranscodeJob job = find(id);
            if (job == null || job.logFile == null) {
                return "";
            }
            logFile = job.logFile;
        }
        Path file = Path.of(logFile);
        if (!Files.isRegularFile(file)) {
            return "";
        }
        try {
            return String.join("\n", readTail(file, Math.max(1, Math.min(2000, tail))));
        } catch (IOException e) {
            return "";
        }
    }

    /// Reads only the tail of a log instead of `readAllLines`: an encode that has been
    /// running for hours leaves a file far larger than the 80–600 lines the UI shows,
    /// and the queue page polls this every 1.5s.
    private static List<String> readTail(Path file, int limit) throws IOException {
        long size = Files.size(file);
        int window = (int) Math.min(size, 256 * 1024);
        if (window <= 0) {
            return List.of();
        }
        byte[] buffer = new byte[window];
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            raf.seek(size - window);
            raf.readFully(buffer);
        }
        String[] lines = new String(buffer, StandardCharsets.UTF_8).split("\n", -1);
        int from = Math.max(0, lines.length - limit);
        List<String> result = new ArrayList<>(Math.min(limit, lines.length));
        for (int i = from; i < lines.length; i++) {
            // The window may begin mid-line; that fragment is not a real log line.
            if (i == 0 && window < size) {
                continue;
            }
            String line = lines[i].endsWith("\r") ? lines[i].substring(0, lines[i].length() - 1) : lines[i];
            result.add(line);
        }
        return result;
    }

    private void dispatch() {
        scheduler.execute(this::dispatchNow);
    }

    private void dispatchNow() {
        List<TranscodeJob> start = new ArrayList<>();
        synchronized (lock) {
            if (!started || runState != RunState.RUNNING) {
                return;
            }
            long active = jobs.stream()
                    .filter(job -> job.state == TranscodeJob.State.RUNNING
                            || job.state == TranscodeJob.State.PAUSED
                            || job.state == TranscodeJob.State.WAITING_DECISION
                            || job.state == TranscodeJob.State.STOPPING)
                    .count();
            int slots = module.config().transcode.maxConcurrent - (int) active;
            for (TranscodeJob job : jobs) {
                if (slots <= 0) {
                    break;
                }
                if (job.state == TranscodeJob.State.QUEUED) {
                    job.state = TranscodeJob.State.RUNNING;
                    job.startedAt = System.currentTimeMillis();
                    job.message = "starting";
                    start.add(job);
                    slots--;
                }
            }
            if (!start.isEmpty()) {
                saveLocked();
            }
        }
        if (!start.isEmpty()) {
            publishQueue();
        }
        for (TranscodeJob job : start) {
            workers.execute(() -> runJob(job));
        }
    }

    private void runJob(TranscodeJob job) {
        Path part = null;
        try {
            if (job.state != TranscodeJob.State.RUNNING) {
                return;
            }
            FfmpegProbe.FfmpegInfo info = module.runtime().info();
            if (!info.available()) {
                fail(job, info.error() == null ? "ffmpeg is unavailable" : info.error(), null);
                return;
            }
            Path input = mediaRoot.resolve(job.input);
            Path output = mediaRoot.resolve(job.output);
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            part = partPath(output, job.id);
            Files.deleteIfExists(part);
            job.durationMs = job.tool.isBlank() ? durationMs(input) : -1;
            Path chapters = job.preset.chaptersFile == null || job.preset.chaptersFile.isBlank()
                    ? null : module.media().resolve(mediaRoot, job.preset.chaptersFile);
            Path subtitle = job.preset.subtitleFile == null || job.preset.subtitleFile.isBlank()
                    ? null : module.media().resolve(mediaRoot, job.preset.subtitleFile);
            List<List<String>> passes = job.tool.isBlank()
                    ? CommandBuilder.buildPasses(job.preset, input, part, chapters, subtitle)
                    : List.of(toolArgs(job.command, part));

            Path log = Path.of(job.logFile == null ? logsDir.resolve(job.id + ".log").toString() : job.logFile);
            Files.createDirectories(log.getParent());
            Files.writeString(log, "", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            job.steps = new ArrayList<>();
            for (int passIndex = 0; passIndex < passes.size(); passIndex++) {
                List<String> pass = passes.get(passIndex);
                List<String> command = new ArrayList<>(pass.size() + 1);
                command.add(info.ffmpeg().toString());
                command.addAll(pass);
                job.command = List.copyOf(command);
                String passLabel = passes.size() > 1 ? "pass " + (passIndex + 1) + "/" + passes.size() + ": " : "";
                TranscodeJob.Step step = new TranscodeJob.Step();
                step.name = stepName(job, passes.size(), passIndex);
                step.state = "running";
                step.args = List.copyOf(command);
                job.steps.add(step);
                Files.writeString(log, "# " + passLabel + CommandBuilder.display(command) + "\n",
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                ProcessBuilder builder = new ProcessBuilder(Privileges.asAgent(command));
                builder.redirectError(ProcessBuilder.Redirect.appendTo(log.toFile()));
                Process process = builder.start();
                job.process = process;
                publishJob(job);

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        updateProgress(job, line, passIndex, passes.size());
                    }
                }
                int code = process.waitFor();
                job.process = null;
                if (job.state == TranscodeJob.State.CANCELLED) {
                    step.state = "cancelled";
                    Files.deleteIfExists(part);
                    cleanupPassLog(part);
                    return;
                }
                if (code != 0) {
                    step.state = "failed";
                    handleFailure(job, "ffmpeg exited with code " + code + tail(log), part);
                    return;
                }
                step.state = "done";
            }
            cleanupPassLog(part);
            String invalid = job.tool.isBlank()
                    ? invalidOutputReason(part, job.preset, input)
                    : invalidAnyOutputReason(part);
            if (invalid != null) {
                handleFailure(job, invalid + tail(log), part);
                return;
            }
            move(part, output);
            job.fraction = 1;
            job.message = "done";
            job.state = TranscodeJob.State.DONE;
            job.endedAt = System.currentTimeMillis();
            job.sizeBytes = Files.size(output);
            part = null;
            releaseReservation(job);
            publishJob(job);
        } catch (Exception e) {
            if (job.state != TranscodeJob.State.CANCELLED) {
                fail(job, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), part);
            }
        } finally {
            job.process = null;
            synchronized (lock) {
                saveLocked();
            }
            publishQueue();
            dispatch();
        }
    }

    private static void cleanupPassLog(Path part) {
        Path parent = part.getParent();
        if (parent == null) {
            return;
        }
        String prefix = part.getFileName().toString() + ".pass";
        try (var stream = Files.newDirectoryStream(parent, prefix + "*")) {
            for (Path file : stream) {
                Files.deleteIfExists(file);
            }
        } catch (IOException ignored) {
            // A leftover pass log is harmless; the next job overwrites it.
        }
    }

    private void updateProgress(TranscodeJob job, String line, int passIndex, int passCount) {
        int equals = line.indexOf('=');
        if (equals <= 0) {
            return;
        }
        String key = line.substring(0, equals).trim();
        String value = line.substring(equals + 1).trim();
        try {
            switch (key) {
                case "out_time_us" -> {
                    job.outTimeMs = Long.parseLong(value) / 1000;
                    if (job.durationMs > 0) {
                        double passFraction = Math.min(1, (double) job.outTimeMs / job.durationMs);
                        job.fraction = Math.min(1, (passIndex + passFraction) / Math.max(1, passCount));
                    }
                }
                case "speed" -> job.speed = parseSpeed(value);
                case "fps" -> job.fps = parseDouble(value);
                case "bitrate" -> job.bitrate = value;
                case "total_size" -> job.sizeBytes = Long.parseLong(value);
                case "frame" -> job.frame = Integer.parseInt(value);
                case "progress" -> {
                    if ("end".equals(value)) {
                        job.fraction = Math.min(1, (double) (passIndex + 1) / Math.max(1, passCount));
                        job.message = "finalizing";
                    }
                }
                default -> {
                }
            }
        } catch (RuntimeException ignored) {
            // A malformed progress line is not a reason to fail a running job.
        }
        long now = System.currentTimeMillis();
        if (now - job.lastPublishAt >= 500) {
            job.lastPublishAt = now;
            publishJob(job);
        }
    }

    private void publishQueue() {
        JsonObject event = new JsonObject();
        event.addProperty("type", "transcode-queue");
        event.add("queue", snapshot());
        bus.publish("transcode:queue", event);
    }

    private void publishJob(TranscodeJob job) {
        JsonObject event = new JsonObject();
        event.addProperty("type", "transcode-job");
        event.add("job", jobView(job));
        bus.publish("transcode:queue", event);
        bus.publish("transcode:job:" + job.id, event);
    }

    /// A hardware failure does not go straight to FAILED: the operator gets a decision
    /// (or the policy decides). `full-auto` degrades immediately; the other two tiers
    /// park the job and pause the queue.
    /// Replaces the output placeholder in a tool command with the real `.part` path.
    /// Tool args are stored with the final output last; this keeps the queue's atomic
    /// rename contract without every tool knowing about it.
    private static List<String> toolArgs(List<String> stored, Path part) {
        List<String> args = new ArrayList<>(stored);
        if (!args.isEmpty()) {
            args.set(args.size() - 1, part.toString());
        }
        return args;
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1) : "mkv";
    }

    private static String stepName(TranscodeJob job, int passCount, int passIndex) {
        if (job.tool != null && !job.tool.isBlank()) {
            return job.tool;
        }
        if (passCount > 1) {
            return passIndex == 0 ? "分析（第一遍）" : "出片（第二遍）";
        }
        return "编码";
    }

    private void handleFailure(TranscodeJob job, String message, @Nullable Path part) {
        if (job.state == TranscodeJob.State.CANCELLED || job.state == TranscodeJob.State.FAILED) {
            return;
        }
        if (part != null) {
            try {
                Files.deleteIfExists(part);
            } catch (IOException ignored) {
            }
            cleanupPassLog(part);
        }
        String fallback = fallbackEncoder(job.preset.videoEncoder);
        if (fallback != null && !job.fallbackAttempted) {
            if ("full-auto".equals(policy())) {
                applyFallback(job);
                publishJob(job);
                dispatch();
                return;
            }
            synchronized (lock) {
                job.state = TranscodeJob.State.WAITING_DECISION;
                job.error = message;
                job.message = "waiting for decision";
                job.decisionDeadline = System.currentTimeMillis() + timeoutMinutes() * 60_000L;
                runState = RunState.PAUSED;
                pauseReason = PAUSE_DECISION;
                saveLocked();
            }
            appendLog(job, "\n# WAITING FOR DECISION: " + message + "\n");
            publishJob(job);
            publishQueue();
            scheduleDecisionTimeout(job.id);
            return;
        }
        fail(job, message, null);
    }

    private boolean applyFallback(TranscodeJob job) {
        return applyFallback(job, "policy");
    }

    /// Applies the next step of the hardware chain. Called under [lock] or with a job
    /// that is not running; it also persists, so callers do not have to remember to.
    /// `trigger` records who asked for the hop so the log dialog can explain a chain of
    /// fallbacks later (`operator`, `timeout`, `policy`, `queue`).
    private boolean applyFallback(TranscodeJob job, String trigger) {
        synchronized (lock) {
            if (job.preset == null || job.fallbackAttempted) {
                return false;
            }
            String from = job.preset.videoEncoder;
            String to = fallbackEncoder(from);
            if (to == null) {
                return false;
            }
            TranscodeJob.SwitchRecord record = new TranscodeJob.SwitchRecord();
            record.at = System.currentTimeMillis();
            record.from = from == null ? "" : from;
            record.to = to;
            record.trigger = trigger;
            record.reason = job.error == null ? "" : job.error.strip();
            if (record.reason.length() > 400) {
                record.reason = record.reason.substring(0, 400) + "…";
            }
            job.switchRecords.add(record);
            job.fallbackFrom = from;
            job.fallbackTo = to;
            job.fallbackAttempted = true;
            job.preset.videoEncoder = to;
            job.preset.qualityMode = to.endsWith("_vaapi") ? "cqp" : "crf";
            if (job.preset.qualityValue == null || job.preset.qualityValue.isBlank()) {
                job.preset.qualityValue = "23";
            }
            job.preset.preset = switch (to) {
                case "libvpx-vp9", "libaom-av1", "libsvtav1" -> "8";
                case "librav1e" -> "6";
                case "h264_vaapi", "hevc_vaapi", "vp9_vaapi" -> "";
                default -> "medium";
            };
            job.encoder = to;
            job.state = TranscodeJob.State.QUEUED;
            job.fraction = -1;
            job.message = "queued after fallback";
            job.error = null;
            job.outTimeMs = -1;
            job.speed = -1;
            job.fps = -1;
            job.bitrate = "";
            job.sizeBytes = -1;
            job.frame = -1;
            job.startedAt = -1;
            job.endedAt = -1;
            job.decisionDeadline = -1;
            job.command = List.of();
            saveLocked();
            return true;
        }
    }

    private void applyFallbackToQueued(TranscodeJob failed) {
        synchronized (lock) {
            String original = failed.fallbackFrom != null ? failed.fallbackFrom : failed.encoder;
            for (TranscodeJob job : jobs) {
                if (job == failed || job.state != TranscodeJob.State.QUEUED) {
                    continue;
                }
                if (original != null && original.equals(job.encoder) && !job.fallbackAttempted) {
                    applyFallback(job, "queue");
                }
            }
            saveLocked();
        }
    }

    /// Resolves a parked job. `stop` fails it and leaves the queue paused; the three
    /// fallback actions degrade it (and optionally the queued jobs of the same
    /// hardware family) and resume dispatch.
    public boolean decide(String id, String action) {
        synchronized (lock) {
            TranscodeJob job = find(id);
            if (job == null || job.state != TranscodeJob.State.WAITING_DECISION) {
                return false;
            }
            cancelDecisionTimeout(id);
            switch (action) {
                case "stop" -> {
                    job.state = TranscodeJob.State.FAILED;
                    job.error = "stopped by the operator";
                    job.message = "failed";
                    job.endedAt = System.currentTimeMillis();
                    releaseReservation(job);
                    runState = RunState.PAUSED;
                    pauseReason = PAUSE_OPERATOR;
                }
                case "fallback" -> {
                    applyFallback(job, "operator");
                    runState = RunState.RUNNING;
                    pauseReason = PAUSE_OPERATOR;
                }
                case "fallback-queue", "auto" -> {
                    applyFallback(job, "operator");
                    applyFallbackToQueued(job);
                    runState = RunState.RUNNING;
                    pauseReason = PAUSE_OPERATOR;
                }
                default -> {
                    return false;
                }
            }
            saveLocked();
        }
        publishQueue();
        dispatch();
        return true;
    }

    private void scheduleDecisionTimeout(String id) {
        long minutes = timeoutMinutes();
        ScheduledFuture<?> future = timeoutScheduler.schedule(
                () -> onDecisionTimeout(id), Math.max(1, minutes), TimeUnit.MINUTES);
        ScheduledFuture<?> previous = decisionTimeouts.put(id, future);
        if (previous != null) {
            previous.cancel(false);
        }
    }

    // Package-private so a test can exercise the timeout branch without waiting a minute.
    void onDecisionTimeout(String id) {
        synchronized (lock) {
            TranscodeJob job = find(id);
            decisionTimeouts.remove(id);
            if (job == null || job.state != TranscodeJob.State.WAITING_DECISION) {
                return;
            }
            if ("timeout-auto".equals(policy())) {
                applyFallback(job, "timeout");
                applyFallbackToQueued(job);
                runState = RunState.RUNNING;
                pauseReason = PAUSE_OPERATOR;
            } else {
                job.state = TranscodeJob.State.FAILED;
                job.error = "decision timed out after " + timeoutMinutes() + " minutes";
                job.message = "failed";
                job.endedAt = System.currentTimeMillis();
                releaseReservation(job);
                runState = RunState.PAUSED;
                pauseReason = PAUSE_OPERATOR;
            }
            saveLocked();
        }
        publishQueue();
        dispatch();
    }

    private void cancelDecisionTimeout(String id) {
        ScheduledFuture<?> future = decisionTimeouts.remove(id);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void cancelAllDecisionTimeouts() {
        for (ScheduledFuture<?> future : decisionTimeouts.values()) {
            future.cancel(false);
        }
        decisionTimeouts.clear();
    }

    private String policy() {
        String value = module.config().transcode.failurePolicy;
        return value == null ? "ask" : value;
    }

    private int timeoutMinutes() {
        return Math.max(1, module.config().transcode.failureTimeoutMinutes);
    }

    private static @Nullable String fallbackEncoder(@Nullable String encoder) {
        if (encoder == null) {
            return null;
        }
        return switch (encoder) {
            case "h264_qsv" -> "h264_vaapi";
            case "hevc_qsv" -> "hevc_vaapi";
            case "vp9_qsv" -> "vp9_vaapi";
            case "av1_qsv" -> "libaom-av1";
            case "h264_vaapi" -> "libx264";
            case "hevc_vaapi" -> "libx265";
            case "vp9_vaapi" -> "libvpx-vp9";
            case "av1_vaapi" -> "libsvtav1";
            case "h264_nvenc" -> "libx264";
            case "hevc_nvenc" -> "libx265";
            case "av1_nvenc" -> "libsvtav1";
            case "h264_amf", "h264_d3d12va", "h264_vulkan" -> "libx264";
            case "hevc_amf", "hevc_d3d12va", "hevc_vulkan" -> "libx265";
            case "av1_amf", "av1_d3d12va", "av1_vulkan" -> "libsvtav1";
            default -> null;
        };
    }

    private void fail(TranscodeJob job, String message, @Nullable Path part) {
        if (job.state == TranscodeJob.State.CANCELLED) {
            return;
        }
        job.state = TranscodeJob.State.FAILED;
        job.error = message;
        job.message = "failed";
        job.endedAt = System.currentTimeMillis();
        releaseReservation(job);
        if (part != null) {
            try {
                Files.deleteIfExists(part);
            } catch (IOException ignored) {
            }
            cleanupPassLog(part);
        }
        appendLog(job, "\n# FAILED: " + message + "\n");
        publishJob(job);
    }

    private void appendLog(TranscodeJob job, String text) {
        if (job.logFile == null) {
            return;
        }
        try {
            Files.writeString(Path.of(job.logFile), text, StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    private long durationMs(Path input) {
        try {
            JsonObject probe = module.runtime().probeMedia(input);
            JsonObject format = probe.has("format") ? probe.getAsJsonObject("format") : null;
            if (format != null && format.has("duration")) {
                double seconds = Double.parseDouble(format.get("duration").getAsString());
                return Math.max(0, Math.round(seconds * 1000));
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private @Nullable String invalidAnyOutputReason(Path output) {
        if (!Files.isRegularFile(output) || fileSize(output) <= 0) {
            return "输出文件为空或不存在";
        }
        try {
            JsonObject probe = module.runtime().probeMedia(output);
            JsonArray streams = probe.has("streams") ? probe.getAsJsonArray("streams") : new JsonArray();
            return streams.isEmpty() ? "输出没有可用流" : null;
        } catch (Exception e) {
            return "ffprobe 无法读取输出：" + e.getMessage();
        }
    }

    /// Validates the produced file against what the preset promised. Audio is only
    /// required when the **input** actually has audio: a video-only source must not
    /// fail a copy-audio preset just because there was nothing to copy.
    private @Nullable String invalidOutputReason(Path output, TranscodePreset preset, Path input) {
        if (!Files.isRegularFile(output) || fileSize(output) <= 0) {
            return "输出文件为空或不存在";
        }
        try {
            JsonObject probe = module.runtime().probeMedia(output);
            JsonArray streams = probe.has("streams") ? probe.getAsJsonArray("streams") : new JsonArray();
            boolean video = false;
            boolean audio = false;
            for (JsonElement element : streams) {
                JsonObject stream = element.getAsJsonObject();
                String type = stream.has("codec_type") ? stream.get("codec_type").getAsString() : "";
                if ("video".equals(type)) video = true;
                if ("audio".equals(type)) audio = true;
            }
            if (!"none".equals(preset.videoEncoder) && !video) {
                return "输出没有视频流";
            }
            if ("none".equals(preset.videoEncoder) && !audio) {
                return "预设是纯音频，但输入/输出没有音频";
            }
            if (!"none".equals(preset.audioMode) && hasAudio(input) && !audio) {
                return "预设要求音频轨，但输出没有音频（输入有音频）";
            }
            return null;
        } catch (Exception e) {
            return "ffprobe 无法读取输出：" + e.getMessage();
        }
    }

    private boolean hasAudio(Path input) {
        try {
            JsonObject probe = module.runtime().probeMedia(input);
            JsonArray streams = probe.has("streams") ? probe.getAsJsonArray("streams") : new JsonArray();
            for (JsonElement element : streams) {
                JsonObject stream = element.getAsJsonObject();
                if ("audio".equals(stream.has("codec_type") ? stream.get("codec_type").getAsString() : "")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1;
        }
    }

    private static Path partPath(Path output, String id) {
        String name = output.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String part = dot > 0
                ? name.substring(0, dot) + ".part-" + id + name.substring(dot)
                : name + ".part-" + id;
        return output.resolveSibling(part);
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String tail(Path log) {
        try {
            List<String> lines = Files.readAllLines(log, StandardCharsets.UTF_8);
            int from = Math.max(0, lines.size() - 12);
            return "\n" + String.join("\n", lines.subList(from, lines.size()));
        } catch (IOException e) {
            return "";
        }
    }

    private static double parseSpeed(String value) {
        String normalized = value.endsWith("x") ? value.substring(0, value.length() - 1) : value;
        return parseDouble(normalized);
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private Path uniqueOutput(Path desired) {
        Path candidate = desired.toAbsolutePath().normalize();
        if (!Files.exists(candidate) && !reservedOutputs.contains(candidate)) {
            return candidate;
        }
        String name = candidate.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        String extension = dot > 0 ? name.substring(dot) : "";
        Path directory = candidate.getParent();
        int counter = 1;
        while (counter < 10_000) {
            Path next = directory.resolve(stem + "_" + counter + extension).toAbsolutePath().normalize();
            if (!Files.exists(next) && !reservedOutputs.contains(next)) {
                return next;
            }
            counter++;
        }
        throw new IllegalStateException("cannot find a free output name for " + desired);
    }

    private void releaseReservation(TranscodeJob job) {
        if (job.output == null || job.output.isBlank()) {
            return;
        }
        reservedOutputs.remove(mediaRoot.resolve(job.output).toAbsolutePath().normalize());
    }

    private String relative(Path path) {
        try {
            return mediaRoot.toRealPath().relativize(path.toRealPath()).toString().replace('\\', '/');
        } catch (IOException e) {
            return mediaRoot.relativize(path).toString().replace('\\', '/');
        }
    }

    private @Nullable TranscodeJob find(String id) {
        for (TranscodeJob job : jobs) {
            if (job.id.equals(id)) {
                return job;
            }
        }
        return null;
    }

    private void saveLocked() {
        try {
            Files.createDirectories(dataDir);
            Path temp = Files.createTempFile(dataDir, "queue", ".json");
            try {
                JsonUtils.writeToJsonFile(temp, new QueueRecord(runState, List.copyOf(jobs), pauseReason));
                move(temp, queueFile);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            Logger.LOG.warning("Could not write the transcode queue " + queueFile, e);
        }
    }

    private void loadLocked() {
        if (!Files.isRegularFile(queueFile)) {
            return;
        }
        try {
            QueueRecord record = JsonUtils.fromJsonFile(queueFile, QueueRecord.class);
            if (record == null) {
                return;
            }
            runState = record.runState() == null ? RunState.IDLE : record.runState();
            // Queue files written before the pause reason existed read as a manual pause:
            // the safe direction is "keep it paused" rather than resuming by surprise.
            pauseReason = record.pauseReason() == null ? PAUSE_OPERATOR : record.pauseReason();
            jobs.clear();
            if (record.jobs() != null) {
                jobs.addAll(record.jobs());
            }
            reservedOutputs.clear();
            for (TranscodeJob job : jobs) {
                if (!job.state.terminal() && job.output != null && !job.output.isBlank()) {
                    reservedOutputs.add(mediaRoot.resolve(job.output).toAbsolutePath().normalize());
                }
            }
            long now = System.currentTimeMillis();
            for (TranscodeJob job : jobs) {
                if (job.state == TranscodeJob.State.RUNNING
                        || job.state == TranscodeJob.State.PAUSED
                        || job.state == TranscodeJob.State.STOPPING) {
                    job.state = TranscodeJob.State.FAILED;
                    job.error = "interrupted by a panel restart";
                    job.message = "failed";
                    job.endedAt = now;
                }
            }
            // A decision-pause with no parked job left cannot be honored: nothing owes a
            // decision, so keeping it would silently strand everything queued behind it.
            if (runState == RunState.PAUSED && PAUSE_DECISION.equals(pauseReason)
                    && jobs.stream().noneMatch(job -> job.state == TranscodeJob.State.WAITING_DECISION)) {
                runState = RunState.RUNNING;
                pauseReason = PAUSE_OPERATOR;
            }
        } catch (Exception e) {
            Logger.LOG.warning("Could not read the transcode queue " + queueFile, e);
        }
    }

    private record QueueRecord(RunState runState, List<TranscodeJob> jobs, String pauseReason) {
    }
}