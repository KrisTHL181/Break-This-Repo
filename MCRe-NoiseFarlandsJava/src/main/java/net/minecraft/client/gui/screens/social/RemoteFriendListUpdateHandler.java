package net.minecraft.client.gui.screens.social;

import com.mojang.authlib.yggdrasil.FriendsService;
import com.mojang.authlib.yggdrasil.FriendsService.ResultCode;
import com.mojang.authlib.yggdrasil.response.FriendData;
import com.mojang.authlib.yggdrasil.response.FriendDto;
import com.mojang.logging.LogUtils;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.FriendToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.friends.FriendsOverlayScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public final class RemoteFriendListUpdateHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long FOREGROUND_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(1L);
    private static final long BACKGROUND_INTERVAL_MULTIPLIER = 5L;
    private static final long POLL_INTERVAL_SECONDS = 1L;
    private final FriendsService friendsService;
    private final Minecraft minecraft;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean updateInProgress = new AtomicBoolean(false);
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final Set<Runnable> updateListeners = new CopyOnWriteArraySet<>();
    private volatile long lastUpdateNanos = 0L;
    private volatile FriendData latestFriendData = FriendData.empty();
    private volatile RemoteFriendListUpdateHandler.State state = RemoteFriendListUpdateHandler.State.LOADING;
    private volatile Set<FriendDto> knownFriends = new HashSet<>();
    private volatile Set<FriendDto> knownIncoming = new HashSet<>();
    private volatile Set<FriendDto> knownOutgoing = new HashSet<>();
    private @Nullable ScheduledFuture<?> scheduledTick;

    public RemoteFriendListUpdateHandler(final FriendsService friendsService, final Minecraft minecraft) {
        this.friendsService = friendsService;
        this.minecraft = minecraft;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Friends List");
            thread.setDaemon(true);
            return thread;
        });
    }

    private void runBackgroundTick() {
        if (!this.updateInProgress.get() && this.enabled.get()) {
            long now = System.nanoTime();
            if (this.lastUpdateNanos == 0L || now - this.lastUpdateNanos >= this.getUpdateIntervalNanos()) {
                this.runUpdateFriendDataInternal();
            }
        }
    }

    public FriendData getLatestFriendData() {
        return this.latestFriendData;
    }

    public RemoteFriendListUpdateHandler.State getState() {
        return this.state;
    }

    public void addUpdateListener(final Runnable listener) {
        this.updateListeners.add(listener);
    }

    public void removeUpdateListener(final Runnable listener) {
        this.updateListeners.remove(listener);
    }

    private long getUpdateIntervalNanos() {
        long foregroundNanos = this.friendsService.getFriendsPollInterval().map(Duration::toNanos).orElse(FOREGROUND_INTERVAL_NANOS);
        Screen screen = this.minecraft.gui.screen();
        return screen instanceof FriendsOverlayScreen ? foregroundNanos : foregroundNanos * 5L;
    }

    void runUpdateFriendDataInternal() {
        if (!this.updateInProgress.compareAndSet(false, true)) {
            LOGGER.debug("Attempted to run Friends List update but update is already in progress");
        } else {
            LOGGER.debug("Performing Friends List update");
            AtomicReference<FriendData> friendData = new AtomicReference<>(FriendData.empty());
            boolean shouldNotifyListeners = false;

            try {
                ResultCode resultCode = this.friendsService.getFriendData(friendData::set);
                RemoteFriendListUpdateHandler.State newState = mapResultCodeToState(resultCode);
                RemoteFriendListUpdateHandler.State previousState = this.state;
                boolean stateTransition = previousState != newState;
                this.state = newState;
                if (resultCode == ResultCode.SUCCESS) {
                    FriendData data = friendData.get();
                    this.latestFriendData = data;
                    boolean dataChanged = this.detectChangesAndShowToast(data, previousState);
                    shouldNotifyListeners = dataChanged || stateTransition;
                    return;
                }

                LOGGER.warn("Friends List update failed with result code: {}", resultCode);
                shouldNotifyListeners = true;
            } catch (Throwable e) {
                LOGGER.warn("Failed to update friend data", e);
                return;
            } finally {
                this.updateInProgress.set(false);
                this.lastUpdateNanos = System.nanoTime();
                if (shouldNotifyListeners) {
                    this.notifyListeners();
                }
            }
        }
    }

    private static RemoteFriendListUpdateHandler.State mapResultCodeToState(final ResultCode resultCode) {
        return switch (resultCode) {
            case TEMPORARY_UNAVAILABLE, FORBIDDEN, SERVICE_NOT_AVAILABLE, TOO_MANY_REQUESTS -> RemoteFriendListUpdateHandler.State.TEMPORARY_UNAVAILABLE;
            case CONNECTION_ISSUE -> RemoteFriendListUpdateHandler.State.CONNECTION_ISSUE;
            case UPGRADE_NEEDED -> RemoteFriendListUpdateHandler.State.UPGRADE_NEEDED;
            case UNKNOWN_PROFILE -> RemoteFriendListUpdateHandler.State.USER_MAY_LACK_ACTIVE_PROFILE;
            case UNAUTHORIZED -> RemoteFriendListUpdateHandler.State.UNAUTHORIZED;
            case GENERIC_ERROR, ERROR -> RemoteFriendListUpdateHandler.State.GENERIC_ERROR;
            case SUCCESS -> RemoteFriendListUpdateHandler.State.SUCCESS;
        };
    }

    private void notifyListeners() {
        if (!this.updateListeners.isEmpty()) {
            LOGGER.debug("Notifying {} Friends List update listeners", this.updateListeners.size());
            this.minecraft.execute(() -> {
                for (Runnable listener : this.updateListeners) {
                    try {
                        listener.run();
                    } catch (Throwable e) {
                        LOGGER.warn("Friends List callback failed", e);
                    }
                }
            });
        }
    }

    private boolean detectChangesAndShowToast(final FriendData friendData, final RemoteFriendListUpdateHandler.State previousState) {
        Set<FriendDto> currentFriends = new HashSet<>(friendData.friends());
        Set<FriendDto> currentIncoming = new HashSet<>(friendData.incomingRequests());
        Set<FriendDto> currentOutgoing = new HashSet<>(friendData.outgoingRequests());
        if (previousState != RemoteFriendListUpdateHandler.State.SUCCESS) {
            this.knownFriends = currentFriends;
            this.knownIncoming = currentIncoming;
            this.knownOutgoing = currentOutgoing;
            return true;
        }

        if (!this.isInGameAndToastsDisabled()) {
            for (FriendDto friendDto : currentFriends) {
                if (!this.knownFriends.contains(friendDto)) {
                    if (!this.knownOutgoing.contains(friendDto) && !this.knownIncoming.contains(friendDto)) {
                        this.emitToastWithSkin(friendDto.profileId(), friendDto.name(), FriendToast::showFriendAdded);
                    } else {
                        this.emitToastWithSkin(friendDto.profileId(), friendDto.name(), FriendToast::showFriendRequestAccepted);
                    }
                }
            }

            for (FriendDto friendDto : currentIncoming) {
                if (!this.knownIncoming.contains(friendDto) && !currentFriends.contains(friendDto)) {
                    this.emitToastWithSkin(friendDto.profileId(), friendDto.name(), FriendToast::showFriendRequestReceived);
                }
            }

            for (FriendDto friendDto : currentOutgoing) {
                if (!this.knownOutgoing.contains(friendDto) && !currentFriends.contains(friendDto)) {
                    this.minecraft.execute(() -> FriendToast.showFriendRequestSent(this.minecraft, friendDto.name()));
                }
            }
        }

        boolean hasChanges = !this.knownFriends.equals(currentFriends)
            || !this.knownIncoming.equals(currentIncoming)
            || !this.knownOutgoing.equals(currentOutgoing);
        this.knownFriends = currentFriends;
        this.knownIncoming = currentIncoming;
        this.knownOutgoing = currentOutgoing;
        return hasChanges;
    }

    private boolean isInGameAndToastsDisabled() {
        return this.minecraft.level != null && !this.minecraft.options.inGameNotification().get();
    }

    private void emitToastWithSkin(final UUID playerId, final String playerName, final FriendToast.SkinToastEmitter emitter) {
        this.minecraft.execute(() -> emitter.emit(this.minecraft, playerName, playerId));
    }

    public CompletableFuture<Void> forceUpdate() {
        if (this.enabled.get() && !this.scheduler.isShutdown()) {
            CompletableFuture<Void> future = new CompletableFuture<>();

            try {
                this.scheduler.execute(() -> {
                    try {
                        this.runUpdateFriendDataInternal();
                    } finally {
                        future.complete(null);
                    }
                });
            } catch (Throwable e) {
                LOGGER.warn("Failed to schedule forced Friends List update", e);
                future.complete(null);
            }

            return future;
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    public synchronized void start() {
        if (this.scheduler.isShutdown()) {
            LOGGER.warn("Attempted to start Friends List updater but scheduler is already shut down");
        } else if (this.enabled.compareAndSet(false, true)) {
            if (this.scheduledTick == null || this.scheduledTick.isCancelled() || this.scheduledTick.isDone()) {
                this.scheduledTick = this.scheduler.scheduleWithFixedDelay(this::runBackgroundTick, 0L, 1L, TimeUnit.SECONDS);
            }
        }
    }

    public synchronized void stop() {
        this.enabled.set(false);
        if (this.scheduledTick != null) {
            this.scheduledTick.cancel(false);
            this.scheduledTick = null;
        }
    }

    public synchronized void close() {
        this.stop();
        this.scheduler.shutdownNow();
    }

    @OnlyIn(Dist.CLIENT)
    public enum State {
        LOADING,
        UPGRADE_NEEDED,
        CONNECTION_ISSUE,
        USER_MAY_LACK_ACTIVE_PROFILE,
        UNAUTHORIZED,
        TEMPORARY_UNAVAILABLE,
        GENERIC_ERROR,
        SUCCESS;
    }
}