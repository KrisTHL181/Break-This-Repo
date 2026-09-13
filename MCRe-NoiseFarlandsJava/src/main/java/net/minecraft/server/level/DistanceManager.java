package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.core.SectionPos;
import net.minecraft.util.TriState;
import net.minecraft.util.thread.TaskScheduler;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * DistanceManager — 区块距离管理器（MCRe NoiseFarlands 对象化版）
 * 原版以 long 打包键（ChunkPos.pack），本版直接用 ChunkPos 对象为键。
 */
public abstract class DistanceManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PLAYER_TICKET_LEVEL = ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING);
    private final Map<ChunkPos, Set<ServerPlayer>> playersPerChunk = new HashMap<>();
    private final LoadingChunkTracker loadingChunkTracker;
    private final SimulationChunkTracker simulationChunkTracker;
    private final TicketStorage ticketStorage;
    private final DistanceManager.FixedPlayerDistanceChunkTracker naturalSpawnChunkCounter = new DistanceManager.FixedPlayerDistanceChunkTracker(8);
    private final DistanceManager.PlayerTicketTracker playerTicketManager = new DistanceManager.PlayerTicketTracker(32);
    protected final Set<ChunkHolder> chunksToUpdateFutures = new HashSet<>();
    private final ThrottlingChunkTaskDispatcher ticketDispatcher;
    private final Set<ChunkPos> ticketsToRelease = new HashSet<>();
    private final Executor mainThreadExecutor;
    private int simulationDistance = 10;

    protected DistanceManager(final TicketStorage ticketStorage, final Executor executor, final Executor mainThreadExecutor) {
        this.ticketStorage = ticketStorage;
        this.loadingChunkTracker = new LoadingChunkTracker(this, ticketStorage);
        this.simulationChunkTracker = new SimulationChunkTracker(ticketStorage);
        TaskScheduler<Runnable> mainThreadTaskScheduler = TaskScheduler.wrapExecutor("player ticket throttler", mainThreadExecutor);
        this.ticketDispatcher = new ThrottlingChunkTaskDispatcher(mainThreadTaskScheduler, executor, 4);
        this.mainThreadExecutor = mainThreadExecutor;
    }

    protected abstract boolean isChunkToRemove(final ChunkPos node);

    protected abstract @Nullable ChunkHolder getChunk(final ChunkPos node);

    protected abstract @Nullable ChunkHolder updateChunkScheduling(final ChunkPos node, final int level, final @Nullable ChunkHolder chunk, final int oldLevel);

    public boolean runAllUpdates(final ChunkMap scheduler) {
        this.naturalSpawnChunkCounter.runAllUpdates();
        this.simulationChunkTracker.runAllUpdates();
        this.playerTicketManager.runAllUpdates();
        int updates = Integer.MAX_VALUE - this.loadingChunkTracker.runDistanceUpdates(Integer.MAX_VALUE);
        boolean updated = updates != 0;
        if (updated && SharedConstants.DEBUG_VERBOSE_SERVER_EVENTS) {
            LOGGER.debug("DMU {}", updates);
        }

        if (!this.chunksToUpdateFutures.isEmpty()) {
            for (ChunkHolder chunksToUpdateFuture : this.chunksToUpdateFutures) {
                chunksToUpdateFuture.updateHighestAllowedStatus(scheduler);
            }

            for (ChunkHolder chunkHolder : this.chunksToUpdateFutures) {
                chunkHolder.updateFutures(scheduler, this.mainThreadExecutor);
            }

            this.chunksToUpdateFutures.clear();
            return true;
        } else {
            if (!this.ticketsToRelease.isEmpty()) {
                Iterator<ChunkPos> iterator = this.ticketsToRelease.iterator();

                while (iterator.hasNext()) {
                    ChunkPos pos = iterator.next();
                    if (this.ticketStorage.getTickets(pos).stream().anyMatch(t -> t.getType() == TicketType.PLAYER_LOADING)) {
                        ChunkHolder chunk = scheduler.getUpdatingChunkIfPresent(pos);
                        if (chunk == null) {
                            throw new IllegalStateException();
                        }

                        CompletableFuture<ChunkResult<LevelChunk>> future = chunk.getEntityTickingChunkFuture();
                        future.thenAccept(c -> this.mainThreadExecutor.execute(() -> this.ticketDispatcher.release(pos, () -> {}, false)));
                    }
                }

                this.ticketsToRelease.clear();
            }

            return updated;
        }
    }

    public void addPlayer(final SectionPos pos, final ServerPlayer player) {
        ChunkPos chunk = pos.chunk();
        this.playersPerChunk.computeIfAbsent(chunk, k -> new HashSet<>()).add(player);
        this.naturalSpawnChunkCounter.update(chunk, 0, true);
        this.playerTicketManager.update(chunk, 0, true);
        this.ticketStorage.addTicket(new Ticket(TicketType.PLAYER_SIMULATION, this.getPlayerTicketLevel()), chunk);
    }

    public void removePlayer(final SectionPos pos, final ServerPlayer player) {
        ChunkPos chunk = pos.chunk();
        Set<ServerPlayer> chunkPlayers = this.playersPerChunk.get(chunk);
        chunkPlayers.remove(player);
        if (chunkPlayers.isEmpty()) {
            this.playersPerChunk.remove(chunk);
            this.naturalSpawnChunkCounter.update(chunk, Integer.MAX_VALUE, false);
            this.playerTicketManager.update(chunk, Integer.MAX_VALUE, false);
            this.ticketStorage.removeTicket(new Ticket(TicketType.PLAYER_SIMULATION, this.getPlayerTicketLevel()), chunk);
        }
    }

    private int getPlayerTicketLevel() {
        return Math.max(0, ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING) - this.simulationDistance);
    }

    public boolean inEntityTickingRange(final ChunkPos key) {
        return ChunkLevel.isEntityTicking(this.simulationChunkTracker.getLevel(key));
    }

    public boolean inBlockTickingRange(final ChunkPos key) {
        return ChunkLevel.isBlockTicking(this.simulationChunkTracker.getLevel(key));
    }

    public int getChunkLevel(final ChunkPos key, final boolean simulation) {
        return simulation ? this.simulationChunkTracker.getLevel(key) : this.loadingChunkTracker.getLevel(key);
    }

    protected void updatePlayerTickets(final int viewDistance) {
        this.playerTicketManager.updateViewDistance(viewDistance);
    }

    public void updateSimulationDistance(final int newDistance) {
        if (newDistance != this.simulationDistance) {
            this.simulationDistance = newDistance;
            this.ticketStorage.replaceTicketLevelOfType(this.getPlayerTicketLevel(), TicketType.PLAYER_SIMULATION);
        }
    }

    public int getNaturalSpawnChunkCount() {
        this.naturalSpawnChunkCounter.runAllUpdates();
        return this.naturalSpawnChunkCounter.chunks.size();
    }

    public TriState hasPlayersNearby(final ChunkPos pos) {
        this.naturalSpawnChunkCounter.runAllUpdates();
        int distance = this.naturalSpawnChunkCounter.getLevel(pos);
        if (distance <= NaturalSpawner.INSCRIBED_SQUARE_SPAWN_DISTANCE_CHUNK) {
            return TriState.TRUE;
        } else {
            return distance > 8 ? TriState.FALSE : TriState.DEFAULT;
        }
    }

    public void forEachEntityTickingChunk(final Consumer<ChunkPos> consumer) {
        for (Map.Entry<ChunkPos, Byte> entry : this.simulationChunkTracker.chunks.entrySet()) {
            byte level = entry.getValue();
            ChunkPos key = entry.getKey();
            if (ChunkLevel.isEntityTicking(level)) {
                consumer.accept(key);
            }
        }
    }

    public Iterator<ChunkPos> getSpawnCandidateChunks() {
        this.naturalSpawnChunkCounter.runAllUpdates();
        return this.naturalSpawnChunkCounter.chunks.keySet().iterator();
    }

    public String getDebugStatus() {
        return this.ticketDispatcher.getDebugStatus();
    }

    public boolean hasTickets() {
        return this.ticketStorage.hasTickets();
    }

    private class FixedPlayerDistanceChunkTracker extends ChunkTracker {
        protected final Map<ChunkPos, Byte> chunks = new HashMap<>();
        protected final int maxDistance;

        protected FixedPlayerDistanceChunkTracker(final int maxDistance) {
            super(maxDistance + 2, 16, 256);
            this.maxDistance = maxDistance;
        }

        @Override
        protected int getLevel(final ChunkPos node) {
            return this.chunks.getOrDefault(node, (byte)(this.maxDistance + 2));
        }

        @Override
        protected void setLevel(final ChunkPos node, final int level) {
            byte oldLevel;
            if (level > this.maxDistance) {
                Byte removed = this.chunks.remove(node);
                oldLevel = removed == null ? (byte)(this.maxDistance + 2) : removed;
            } else {
                Byte prev = this.chunks.put(node, (byte)level);
                oldLevel = prev == null ? (byte)(this.maxDistance + 2) : prev;
            }

            this.onLevelChange(node, oldLevel, level);
        }

        protected void onLevelChange(final ChunkPos node, final int oldLevel, final int level) {
        }

        @Override
        protected int getLevelFromSource(final ChunkPos to) {
            return this.havePlayer(to) ? 0 : Integer.MAX_VALUE;
        }

        private boolean havePlayer(final ChunkPos chunkPos) {
            Set<ServerPlayer> players = DistanceManager.this.playersPerChunk.get(chunkPos);
            return players != null && !players.isEmpty();
        }

        public void runAllUpdates() {
            this.runUpdates(Integer.MAX_VALUE);
        }
    }

    private class PlayerTicketTracker extends DistanceManager.FixedPlayerDistanceChunkTracker {
        private int viewDistance;
        private final Map<ChunkPos, Integer> queueLevels = java.util.Collections.synchronizedMap(new HashMap<>());
        private final Set<ChunkPos> toUpdate = new HashSet<>();

        protected PlayerTicketTracker(final int maxDistance) {
            super(maxDistance);
            this.viewDistance = 0;
        }

        @Override
        protected void onLevelChange(final ChunkPos node, final int oldLevel, final int level) {
            this.toUpdate.add(node);
        }

        public void updateViewDistance(final int viewDistance) {
            for (Map.Entry<ChunkPos, Byte> entry : this.chunks.entrySet()) {
                byte level = entry.getValue();
                ChunkPos key = entry.getKey();
                this.onLevelChange(key, level, this.haveTicketFor(level), level <= viewDistance);
            }

            this.viewDistance = viewDistance;
        }

        private void onLevelChange(final ChunkPos key, final int level, final boolean saw, final boolean sees) {
            if (saw != sees) {
                Ticket ticket = new Ticket(TicketType.PLAYER_LOADING, DistanceManager.PLAYER_TICKET_LEVEL);
                if (sees) {
                    DistanceManager.this.ticketDispatcher.submit(() -> DistanceManager.this.mainThreadExecutor.execute(() -> {
                        if (this.haveTicketFor(this.getLevel(key))) {
                            DistanceManager.this.ticketStorage.addTicket(key, ticket);
                            DistanceManager.this.ticketsToRelease.add(key);
                        } else {
                            DistanceManager.this.ticketDispatcher.release(key, () -> {}, false);
                        }
                    }), key, () -> level);
                } else {
                    DistanceManager.this.ticketDispatcher
                        .release(
                            key,
                            () -> DistanceManager.this.mainThreadExecutor.execute(() -> DistanceManager.this.ticketStorage.removeTicket(key, ticket)),
                            true
                        );
                }
            }
        }

        @Override
        public void runAllUpdates() {
            super.runAllUpdates();
            if (!this.toUpdate.isEmpty()) {
                Iterator<ChunkPos> iterator = this.toUpdate.iterator();

                while (iterator.hasNext()) {
                    ChunkPos node = iterator.next();
                    int oldLevel = this.queueLevels.getOrDefault(node, this.maxDistance + 2);
                    int level = this.getLevel(node);
                    if (oldLevel != level) {
                        DistanceManager.this.ticketDispatcher.onLevelChange(node, () -> this.queueLevels.getOrDefault(node, this.maxDistance + 2), level, l -> {
                            if (l >= this.maxDistance + 2) {
                                this.queueLevels.remove(node);
                            } else {
                                this.queueLevels.put(node, l);
                            }
                        });
                        this.onLevelChange(node, level, this.haveTicketFor(oldLevel), this.haveTicketFor(level));
                    }
                }

                this.toUpdate.clear();
            }
        }

        private boolean haveTicketFor(final int level) {
            return level <= this.viewDistance;
        }
    }
}
