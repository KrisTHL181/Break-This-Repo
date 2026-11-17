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
package org.jackhuang.hmcl.web.task;

import com.google.gson.JsonObject;
import org.jackhuang.hmcl.util.platform.VirtualThreads;
import org.jackhuang.hmcl.web.event.EventBus;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/// Runs the blocking work behind the asynchronous API endpoints and tracks it.
///
/// Everything that would hold a request thread — an npm install, a launch, a
/// stop — is submitted here and reported through [EventBus] on the `tasks`
/// topic. `/api/tasks` answers for finished work too, but the finished tail
/// is bounded (see [#pruneTerminal]): a long-running server does not grow
/// without limit.
///
/// Concurrency is bounded where it matters: at most two installs run at once
/// (the two pnpm runs would otherwise fight over the shared store), and one
/// instance can only ever have one install in flight. Every other kind of task
/// runs on an unbounded virtual-thread pool, because a launch or a stop is
/// mostly waiting on a child process.
@NotNullByDefault
public final class TaskService {

    /// How a task is doing.
    public enum TaskState {
        /// An install that is waiting for one of the [MAX_INSTALLS] slots.
        ///
        /// Its own state because `running` used to mean both "started" and "waiting for
        /// two long installs to finish": a third install reported `running` with an empty
        /// message for as long as the other two took, which reads as "started and hung" —
        /// it is how three brand installs submitted together looked like a failure on an
        /// ARM smoke run.
        QUEUED("queued"),
        RUNNING("running"),
        /// Parked on a human decision: the work reported that it needs an
        /// approval (a plugin's install scripts) and waits for
        /// `POST /api/tasks/{id}/approve`.
        WAITING_APPROVAL("waiting_approval"),
        DONE("done"),
        FAILED("failed");

        private final String wireName;

        TaskState(String wireName) {
            this.wireName = wireName;
        }

        /// The JSON spelling of the state.
        public String wireName() {
            return wireName;
        }

        /// Whether the task can still change: anything but the two terminal states.
        boolean live() {
            return this != DONE && this != FAILED;
        }
    }

    /// What a parked task is waiting for. `kind` names the sort of decision
    /// (only `build-scripts` exists), `keys` what it is about.
    public record ApprovalInfo(String kind, List<String> keys) {
    }

    /// The immutable view of a task, as `/api/tasks` reports it. `instanceId`
    /// is the instance the task belongs to, or null for global work — the
    /// panel attributes install progress by it, so it travels with every
    /// snapshot and not only with the `task` event. `approval` is
    /// present only while the state is `waiting_approval`; `result` only when
    /// the work attached one (a pack install's `instanceId`, an export's
    /// `filename`) — it is the machine-readable half of the done message.
    public record TaskInfo(String id, String kind, @Nullable String instanceId, String state, String message,
                           double fraction,
                           @Nullable String stage, @Nullable String stageKey, double stageFraction,
                           @Nullable String error, @Nullable ApprovalInfo approval,
                           @Nullable JsonObject result) {
    }

    /// Work that is handed the task it belongs to.
    ///
    /// [#submit(String, String, TaskWork)] starts the work before it returns,
    /// so a work body can report progress or attach its result the moment it
    /// begins — it does not have to wait for the submitter to publish the
    /// handle, which is a race it would otherwise sometimes lose.
    @FunctionalInterface
    public interface TaskWork {
        /// Runs the work.
        ///
        /// @param task the task this work belongs to; its return value becomes
        ///             the done message, a thrown exception becomes the
        ///             failure error
        /// @return the done message
        /// @throws Exception when the work fails
        String run(Task task) throws Exception;
    }

    /// What the submitter does with an approval decision, between the answer
    /// arriving and the work being retried: a plugin install writes the answer
    /// into the profile's `pnpm-workspace.yaml` here.
    @FunctionalInterface
    public interface ApprovalDecider {
        /// Applies the decision.
        ///
        /// @param allow whether the person allowed it
        /// @param keys  what the decision is about
        /// @throws Exception when the decision cannot be applied; the task fails
        void decide(boolean allow, List<String> keys) throws Exception;
    }

    /// The outcome of an approve call.
    public enum ApproveResult {
        /// The decision was delivered and the task runs again.
        OK,
        /// No task with that id.
        UNKNOWN_TASK,
        /// The task is not parked on an approval.
        NOT_WAITING,
    }

    /// A live task handle: the submitter updates the message and fraction as
    /// progress arrives, and the caller of [TaskService#cancel] marks it dead.
    public static final class Task {
        private static final java.util.concurrent.atomic.AtomicLong SEQUENCE =
                new java.util.concurrent.atomic.AtomicLong();

        private final String id;
        private final String kind;
        private final String instanceId;
        private final TaskService owner;
        private final long sequence = SEQUENCE.getAndIncrement();
        private volatile TaskState state = TaskState.RUNNING;
        private volatile String message = "";
        private volatile double fraction = -1;
        /// The name of the sub-step in flight, and how far it is (0–1, `-1` when unknown). A long
        /// install reports both: the bar for the whole job and the bar for the step inside it.
        /// `stageKey` is the machine-readable phase (`download` / `write` / `build` / `install` /
        /// `done`), so a page can say "downloads are over" without matching the text.
        private volatile @Nullable String stage;
        private volatile @Nullable String stageKey;
        private volatile double stageFraction = -1;
        private volatile @Nullable String error;
        private final AtomicBoolean cancelled = new AtomicBoolean();
        /// The worker thread executing this task; set when the run begins.
        /// Cancellation stops the process trees this thread started — no more.
        private volatile @Nullable Thread worker;
        /// The structured outcome the work attached, if any; published with
        /// every update once present, and part of the REST snapshot.
        private volatile @Nullable JsonObject result;
        /// What the task is parked on, and the future its decision arrives
        /// through. Both are null unless the state is WAITING_APPROVAL.
        private volatile @Nullable ApprovalInfo approval;
        private volatile @Nullable CompletableFuture<Boolean> approvalFuture;

        private Task(TaskService owner, String id, String kind, @Nullable String instanceId) {
            this.owner = owner;
            this.id = id;
            this.kind = kind;
            this.instanceId = instanceId;
        }

        /// Returns the task id.
        public String id() {
            return id;
        }

        /// Returns the current state.
        public TaskState state() {
            return state;
        }

        /// Returns the kind of work this task does.
        public String kind() {
            return kind;
        }

        /// Returns the instance the task belongs to, or `null` for global work.
        public @Nullable String instanceId() {
            return instanceId;
        }

        /// Reports whether a cancel was asked for.
        public boolean isCancelled() {
            return cancelled.get();
        }

        /// Updates the progress line and fraction, and publishes the change
        /// to the `tasks` topic — that is how install progress reaches the
        /// WebSocket clients.
        public void update(String newMessage, double newFraction) {
            this.message = newMessage;
            this.fraction = newFraction;
            owner.publish(this);
        }

        /// Updates the bar of the sub-step in flight, leaving the whole-job message and fraction
        /// alone, and publishes the change.
        ///
        /// @param newStage         what the step is called, or `null` when it has no bar
        /// @param newStageFraction how far it is (0–1), `-1` for unknown
        public void updateStage(@Nullable String newStage, double newStageFraction) {
            updateStage(newStage, null, newStageFraction);
        }

        /// The same, with the machine-readable phase the page keys its wording off.
        ///
        /// @param newStage         what the step is called, or `null` when it has no bar
        /// @param newStageKey      `download` / `write` / `build` / `install` / `done`, or `null`
        /// @param newStageFraction how far it is (0–1), `-1` for unknown
        public void updateStage(@Nullable String newStage, @Nullable String newStageKey, double newStageFraction) {
            this.stage = newStage;
            this.stageKey = newStageKey;
            this.stageFraction = newStageFraction;
            owner.publish(this);
        }

        /// Attaches the structured outcome of the work — `{"instanceId": …}`
        /// of a pack install, `{"filename": …}` of an export — and publishes
        /// the change. The REST snapshot and every later task event carry it,
        /// so a client that only sees the done event still learns what the
        /// work produced.
        public void attachResult(JsonObject attached) {
            this.result = attached;
            owner.publish(this);
        }

        private void finish(TaskState newState, String finalMessage, @Nullable String finalError) {
            this.state = newState;
            this.message = finalMessage;
            this.error = finalError;
            if (newState == TaskState.DONE) {
                this.fraction = 1;
            }
        }

        /// Parks the task on an approval about `keys`.
        private void parkForApproval(List<String> keys) {
            this.approval = new ApprovalInfo("build-scripts", List.copyOf(keys));
            this.approvalFuture = new CompletableFuture<>();
            this.state = TaskState.WAITING_APPROVAL;
        }

        /// Blocks until a decision arrives or the wait is cancelled.
        private boolean awaitApprovalDecision() throws InterruptedException {
            CompletableFuture<Boolean> future = approvalFuture;
            if (future == null) {
                throw new InterruptedException("no approval pending");
            }
            try {
                return future.get();
            } catch (CancellationException | java.util.concurrent.ExecutionException e) {
                throw new InterruptedException("cancelled");
            }
        }

        /// Back to work after the decision.
        private void resumeFromApproval() {
            this.approval = null;
            this.state = TaskState.RUNNING;
        }

        /// Marks the task as waiting for an install slot, with the reason on screen.
        ///
        /// @param waitingMessage what the row says while it waits
        private void queue(String waitingMessage) {
            this.state = TaskState.QUEUED;
            this.message = waitingMessage;
        }

        /// Leaves [TaskState#QUEUED] once a slot is in hand.
        private void leaveQueue() {
            this.state = TaskState.RUNNING;
            this.message = "";
        }

        private TaskInfo snapshot() {
            return new TaskInfo(id, kind, instanceId, state.wireName(), message, fraction,
                    stage, stageKey, stageFraction, error, approval, result);
        }
    }

    /// Raised when the instance already has an install in flight.
    public static final class InstanceBusyException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public InstanceBusyException(String message) {
            super(message);
        }
    }

    /// How many installs may run at once.
    private static final int MAX_INSTALLS = 2;

    /// How many finished tasks (`done`/`failed`) are kept at most, so the
    /// history `/api/tasks` serves stays bounded on a long-running server.
    /// Running tasks and ones parked on an approval are never pruned.
    private static final int DEFAULT_MAX_RETAINED_TERMINAL = 400;

    /// The kinds that drive a package manager against one instance's profile:
    /// they are serialised per instance (two pnpm runs over one profile would
    /// race its manifest) and bounded globally by [MAX_INSTALLS] (two pnpm
    /// runs would otherwise fight over the shared store). `pack-install`
    /// belongs here although its instance does not exist yet at submit time:
    /// it runs the same pnpm installs, and the id it will create is already
    /// known, so the mutex keeps a second pack install (or a delete) from
    /// racing the first for the same id.
    private static final java.util.Set<String> INSTALL_LIKE =
            java.util.Set.of("install", "plugin-install", "plugin-remove", "plugin-local",
                    // Disk-bound rather than install, but it owns the instance the same way: a
                    // second one must not start, and the server must not be launched mid-swap.
                    "pack-install", "mc-world-import");

    private static boolean installLike(@Nullable String kind) {
        return kind != null && INSTALL_LIKE.contains(kind);
    }

    private final EventBus bus;
    /// One thread per task, and deliberately a **platform** thread.
    ///
    /// Task work here is a child process end to end: `pnpm install`, `npm ci`, an MC server
    /// boot. It waits in `Process.waitFor()` and reads pipes — native calls, which a virtual
    /// thread cannot unmount from, so every running task pins its carrier for as long as the
    /// install takes (minutes). On a 1-core machine that is the whole carrier pool.
    ///
    /// This is also what the JDK 17 fallback of [VirtualThreads#newPerTaskExecutor] already
    /// did; on 21 it silently became virtual threads. Virtual threads get their turn in the
    /// I/O-bound places (HTTP, sockets), not here.
    private final ExecutorService executor = java.util.concurrent.Executors.newCachedThreadPool(
            runnable -> {
                Thread thread = new Thread(runnable, "hdsl-task");
                thread.setDaemon(true);
                return thread;
            });
    private final Semaphore installSlots = new Semaphore(MAX_INSTALLS);
    private final ConcurrentHashMap<String, Task> tasks = new ConcurrentHashMap<>();
    /// Instance id → the install task currently working on it.
    private final ConcurrentHashMap<String, String> activeInstalls = new ConcurrentHashMap<>();
    private final int maxRetainedTerminal;

    public TaskService(EventBus bus) {
        this(bus, DEFAULT_MAX_RETAINED_TERMINAL);
    }

    /// Test-friendly bound: small retention numbers do not need 400 submits.
    TaskService(EventBus bus, int maxRetainedTerminal) {
        this.bus = bus;
        this.maxRetainedTerminal = maxRetainedTerminal;
    }

    /// Submits work, returning its handle immediately.
    ///
    /// @param kind       the kind of work, reported in the task JSON
    /// @param instanceId the instance the task belongs to, or `null` for
    ///                   global work; install tasks are serialised per instance
    /// @param work       the work; its return value becomes the done message,
    ///                   a thrown exception becomes the failure error
    /// @return the live handle
    /// @throws InstanceBusyException when an install for the same instance is
    ///                               already running
    public Task submit(String kind, @Nullable String instanceId, Callable<String> work) {
        return submit(kind, instanceId, work, (ApprovalDecider) null);
    }

    /// Submits work that is handed its own task, returning the handle
    /// immediately.
    ///
    /// Prefer this to [#submit(String, String, Callable)] whenever the work
    /// needs the handle — to report progress or to attach a result. The work
    /// starts on another thread before `submit` returns, so a submitter that
    /// filled in a holder after the call would be racing it.
    ///
    /// @param kind       the kind of work, reported in the task JSON
    /// @param instanceId the instance the task belongs to, or `null` for
    ///                   global work
    /// @param work       the work
    /// @return the live handle
    /// @throws InstanceBusyException when an install for the same instance is
    ///                               already running
    public Task submit(String kind, @Nullable String instanceId, TaskWork work) {
        return submit(kind, instanceId, work, null);
    }

    /// Submits work that is handed its own task and may park on a human
    /// decision, returning the handle immediately.
    ///
    /// When the work throws
    /// [org.jackhuang.hmcl.dsh.DshPluginInstaller.DshBuildScriptApprovalRequired],
    /// a task with a decider enters `waiting_approval`: it publishes the
    /// pending keys and parks until [#approve] delivers a decision, applies it
    /// through `approvalDecider`, and retries the work — `true` answers the
    /// pending entries with yes (the AUTO policy's semantics), `false` with no
    /// (NEVER's). A task without a decider fails on the exception instead.
    ///
    /// @param kind            the kind of work
    /// @param instanceId      the instance the task belongs to, or `null`
    /// @param work            the work, retried after each approval
    /// @param approvalDecider applies the decision, or `null` to fail instead
    /// @return the live handle
    /// @throws InstanceBusyException when an install for the same instance is
    ///                               already running
    public Task submit(String kind, @Nullable String instanceId, TaskWork work,
                       @Nullable ApprovalDecider approvalDecider) {
        if (installLike(kind) && instanceId != null) {
            if (activeInstalls.putIfAbsent(instanceId, "pending") != null) {
                throw new InstanceBusyException("An install is already running for instance " + instanceId);
            }
        }
        Task task = new Task(this, UUID.randomUUID().toString(), kind, instanceId);
        tasks.put(task.id(), task);
        publish(task);
        executor.submit(() -> run(task, work, approvalDecider));
        return task;
    }

    /// Submits work that may park on a human decision, returning its handle
    /// immediately. The work is not handed its own task — see
    /// [#submit(String, String, TaskWork, ApprovalDecider)] when it needs it.
    ///
    /// @param kind            the kind of work
    /// @param instanceId      the instance the task belongs to, or `null`
    /// @param work            the work, retried after each approval
    /// @param approvalDecider applies the decision, or `null` to fail instead
    /// @return the live handle
    /// @throws InstanceBusyException when an install for the same instance is
    ///                               already running
    public Task submit(String kind, @Nullable String instanceId, Callable<String> work,
                       @Nullable ApprovalDecider approvalDecider) {
        return submit(kind, instanceId, task -> work.call(), approvalDecider);
    }

    /// Runs a task: acquires the install slot when needed, calls the work, and
    /// records the outcome.
    private void run(Task task, TaskWork work, @Nullable ApprovalDecider approvalDecider) {
        task.worker = Thread.currentThread();
        boolean installBound = installLike(task.kind()) && task.instanceId() != null;
        try {
            if (installBound) {
                activeInstalls.put(task.instanceId(), task.id());
                if (task.isCancelled()) {
                    throw new InterruptedException("cancelled");
                }
                awaitInstallSlot(task);
            }
            try {
                if (task.isCancelled()) {
                    throw new InterruptedException("cancelled");
                }
                String message = callWithApprovals(task, work, approvalDecider);
                // A cancel may land while the work is between its own cancellation
                // checks, in which case the work returns a normal message. The
                // outcome is still "cancelled": reporting `done` would tell the
                // page an install the operator stopped had succeeded.
                if (task.isCancelled()) {
                    task.finish(TaskState.FAILED, "cancelled", "cancelled");
                } else {
                    task.finish(TaskState.DONE, message == null ? "" : message, null);
                }
            } finally {
                if (installBound) {
                    installSlots.release();
                }
            }
        } catch (InterruptedException e) {
            task.finish(TaskState.FAILED, "cancelled", "cancelled");
        } catch (Exception e) {
            // A cancelled task usually dies because its process was destroyed —
            // report the cancellation, not a scary generic failure.
            if (task.isCancelled()) {
                task.finish(TaskState.FAILED, "cancelled", "cancelled");
            } else {
                task.finish(TaskState.FAILED, "failed", e.getMessage() == null ? e.toString() : e.getMessage());
            }
        } finally {
            if (installBound) {
                activeInstalls.remove(task.instanceId(), "pending");
                activeInstalls.remove(task.instanceId(), task.id());
            }
            publish(task);
            pruneTerminal();
        }
    }

    /// How often a queued install looks over at the slots.
    private static final long SLOT_POLL_MILLIS = 250;

    /// Waits for one of the [MAX_INSTALLS] slots, saying so while it waits.
    ///
    /// Three things differ from the bare `acquire()` this replaces:
    ///
    /// - the wait is **visible**: the task reports [TaskState#QUEUED] and a message, instead
    ///   of calling itself `running` with nothing to show — three installs submitted together
    ///   is exactly how a smoke run mistook a queue for a hang;
    /// - the wait is **cancellable**: `cancel` used to be a no-op for a parked task, because
    ///   the worker sat inside `acquire()` with no reason to look at the flag;
    /// - a slot is only ever released by the task that took it (see the `finally` in [run]).
    ///
    /// @param task the install task
    /// @throws InterruptedException when the task is cancelled while queued
    private void awaitInstallSlot(Task task) throws InterruptedException {
        if (installSlots.tryAcquire()) {
            return;
        }
        task.queue("等待安装槽位（同时进行 " + MAX_INSTALLS + " 个安装，轮到就自动开始）");
        publish(task);
        while (!installSlots.tryAcquire(SLOT_POLL_MILLIS, TimeUnit.MILLISECONDS)) {
            if (task.isCancelled()) {
                throw new InterruptedException("cancelled");
            }
        }
        task.leaveQueue();
        publish(task);
    }

    /// Drops the oldest finished tasks beyond [maxRetainedTerminal]. Called
    /// from the completion path of every task, so the bound holds without a
    /// sweeper thread — the occasional few-hundred-entry scan is cheap next
    /// to the work that just finished. Running, queued and approval-parked
    /// tasks are never candidates.
    private void pruneTerminal() {
        List<Task> finished = new ArrayList<>();
        for (Task task : tasks.values()) {
            if (!task.state().live()) {
                finished.add(task);
            }
        }
        if (finished.size() <= maxRetainedTerminal) {
            return;
        }
        finished.sort(Comparator.comparingLong(task -> task.sequence));
        int toRemove = finished.size() - maxRetainedTerminal;
        for (int i = 0; i < toRemove; i++) {
            Task oldest = finished.get(i);
            tasks.remove(oldest.id(), oldest);
        }
    }

    /// Calls the work, honouring the approval dance: a thrown
    /// `DshBuildScriptApprovalRequired` parks the task, the decision is
    /// applied, and the work runs again.
    private String callWithApprovals(Task task, TaskWork work,
                                     @Nullable ApprovalDecider approvalDecider) throws Exception {
        while (true) {
            try {
                return work.run(task);
            } catch (org.jackhuang.hmcl.dsh.DshPluginInstaller.DshBuildScriptApprovalRequired required) {
                if (approvalDecider == null || task.isCancelled()) {
                    throw required;
                }
                task.parkForApproval(required.packages());
                publish(task);
                boolean allow = task.awaitApprovalDecision();
                task.resumeFromApproval();
                publish(task);
                approvalDecider.decide(allow, required.packages());
            }
        }
    }

    /// Cancels a running task, if it is one.
    ///
    /// Cancelling stops every child command the launcher is running
    /// ([org.jackhuang.hmcl.dsh.DshCommand#stopRunning]), which is the only way
    /// to reach the pnpm process behind an install. A task that has not started
    /// yet is marked and skips its work when it comes up; one parked on an
    /// approval is released from the wait.
    ///
    /// @param taskId the task id
    /// @return whether a task with that id exists
    public boolean cancel(String taskId) {
        Task task = tasks.get(taskId);
        if (task == null || !task.state().live()) {
            return false;
        }
        task.cancelled.set(true);
        CompletableFuture<Boolean> future = task.approvalFuture;
        if (future != null) {
            future.completeExceptionally(new InterruptedException("cancelled"));
        }
        Thread worker = task.worker;
        if (worker != null) {
            org.jackhuang.hmcl.dsh.DshCommand.stopRunning(worker);
            // Killing the children is not enough for work that has no child — a
            // 200 MB download is one socket read at a time. The worker checks for
            // interruption between chunks; without this, "cancel" left the task
            // running and the `.part` growing.
            worker.interrupt();
        }
        return true;
    }

    /// Delivers an approval decision to a parked task.
    ///
    /// The task returns to `running` and retries its work, with the decision
    /// applied by the submitter's decider first.
    ///
    /// @param taskId the task id
    /// @param allow  whether the pending scripts may run
    /// @return what happened
    public ApproveResult approve(String taskId, boolean allow) {
        Task task = tasks.get(taskId);
        if (task == null) {
            return ApproveResult.UNKNOWN_TASK;
        }
        CompletableFuture<Boolean> future = task.approvalFuture;
        if (task.state() != TaskState.WAITING_APPROVAL || future == null) {
            return ApproveResult.NOT_WAITING;
        }
        // Flip the visible state before releasing the parked thread, so a
        // client polling between the two never sees a decisionless wait.
        task.resumeFromApproval();
        publish(task);
        future.complete(allow);
        return ApproveResult.OK;
    }

    /// Returns a task snapshot.
    ///
    /// @param taskId the task id
    /// @return the snapshot, or empty when unknown
    public Optional<TaskInfo> get(String taskId) {
        Task task = tasks.get(taskId);
        return task == null ? Optional.empty() : Optional.of(task.snapshot());
    }

    /// Returns every known task, newest first.
    ///
    /// @return the snapshots
    public List<TaskInfo> list() {
        List<Task> all = new ArrayList<>(tasks.values());
        all.sort(Comparator.comparingLong((Task task) -> task.sequence).reversed());
        List<TaskInfo> result = new ArrayList<>(all.size());
        for (Task task : all) {
            result.add(task.snapshot());
        }
        return result;
    }

    /// Returns the install task currently working on an instance, if any.
    ///
    /// Only a version install answers here: the other install-like kinds
    /// (plugin work) share the per-instance mutex but are not what the
    /// instance page's install progress reports.
    ///
    /// @param instanceId the instance id
    /// @return the running install task
    public Optional<TaskInfo> activeInstall(String instanceId) {
        return activeInstallLike(instanceId).filter(info -> "install".equals(info.kind()));
    }

    /// Returns the install-like task currently working on an instance, of any
    /// install-like kind. The instance page asks this for its install progress.
    ///
    /// A queued install has no task yet — [TaskService] parks the `pending`
    /// sentinel in the map — so this answers empty for it. Guards that must see
    /// a queued install ask [#hasActiveInstallLike] instead.
    ///
    /// @param instanceId the instance id
    /// @return the running install-like task
    public Optional<TaskInfo> activeInstallLike(String instanceId) {
        String taskId = activeInstalls.get(instanceId);
        if (taskId == null) {
            return Optional.empty();
        }
        return get(taskId);
    }

    /// Whether an install-like task is running **or queued** for an instance.
    ///
    /// Not expressible through [#activeInstallLike]: while a task is queued the
    /// map holds the `pending` sentinel, which names no task, so the lookup for
    /// it finds nothing. A delete or a start guard asking that way waved the
    /// request through under an install that was about to run.
    ///
    /// @param instanceId the instance id
    /// @return whether an install-like task holds the instance
    public boolean hasActiveInstallLike(String instanceId) {
        return activeInstalls.containsKey(instanceId);
    }

    /// Returns the most recent finished install for an instance, if any.
    ///
    /// @param instanceId the instance id
    /// @return the last install task that is no longer running
    public Optional<TaskInfo> lastInstall(String instanceId) {
        Task newest = null;
        for (Task task : tasks.values()) {
            if (!"install".equals(task.kind()) || !instanceId.equals(task.instanceId())
                    || task.state().live()) {
                continue;
            }
            if (newest == null || task.sequence > newest.sequence) {
                newest = task;
            }
        }
        return Optional.ofNullable(newest).map(Task::snapshot);
    }

    /// Runs work on the task pool without tracking it as a task.
    ///
    /// @param work the work
    public void execute(Runnable work) {
        executor.submit(work);
    }

    /// The executor behind this service, for callers that need to wait on it.
    public Executor executor() {
        return executor;
    }

    /// Publishes a task's current state to the `tasks` topic. The `instance`
    /// member is always present — the panel attributes progress by it — and is
    /// null only for global work. A parked task carries its `approval`, so a
    /// client that missed the REST poll still sees what is being asked.
    private void publish(Task task) {
        TaskInfo info = task.snapshot();
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "task");
        payload.addProperty("taskId", info.id());
        if (task.instanceId() != null) {
            payload.addProperty("instance", task.instanceId());
        } else {
            payload.add("instance", com.google.gson.JsonNull.INSTANCE);
        }
        payload.addProperty("message", info.message());
        payload.addProperty("fraction", info.fraction());
        if (info.stage() != null) {
            payload.addProperty("stage", info.stage());
        }
        if (info.stageKey() != null) {
            payload.addProperty("stageKey", info.stageKey());
        }
        payload.addProperty("stageFraction", info.stageFraction());
        payload.addProperty("state", info.state());
        if (info.error() != null) {
            payload.addProperty("error", info.error());
        }
        if (info.approval() != null) {
            JsonObject approval = new JsonObject();
            approval.addProperty("kind", info.approval().kind());
            com.google.gson.JsonArray keys = new com.google.gson.JsonArray();
            for (String key : info.approval().keys()) {
                keys.add(key);
            }
            approval.add("keys", keys);
            payload.add("approval", approval);
        }
        if (info.result() != null) {
            payload.add("result", info.result());
        }
        bus.publish("tasks", payload);
    }
}
