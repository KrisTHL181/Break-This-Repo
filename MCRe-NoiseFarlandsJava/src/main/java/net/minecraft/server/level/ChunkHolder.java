package net.minecraft.server.level;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jspecify.annotations.Nullable;

public class ChunkHolder extends GenerationChunkHolder {
    public static final ChunkResult<LevelChunk> UNLOADED_LEVEL_CHUNK = ChunkResult.error("Unloaded level chunk");
    private static final CompletableFuture<ChunkResult<LevelChunk>> UNLOADED_LEVEL_CHUNK_FUTURE = CompletableFuture.completedFuture(UNLOADED_LEVEL_CHUNK);
    private final LevelHeightAccessor levelHeightAccessor;
    private volatile CompletableFuture<ChunkResult<LevelChunk>> fullChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
    private volatile CompletableFuture<ChunkResult<LevelChunk>> tickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
    private volatile CompletableFuture<ChunkResult<LevelChunk>> entityTickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
    private int oldTicketLevel;
    private int ticketLevel;
    private int queueLevel;
    private boolean hasChangedSections;
    // 🔧 MCRe：方块变化按「绝对 sectionY」记录（Map），替代 vanilla 的
    // changedBlocksPerSection 数组——超高世界 getSectionsCount 可达 1.34亿，
    // 数组分配直接 OOM。绝对 sectionY 与广播侧 SectionPos 编码解耦，极端 Y 不越界。
    private final Map<Integer, ShortSet> changedBlocksPerSection = new HashMap<>();
    // 🔧 MCRe P4b：光照增量改为「绝对 sectionY 集合」——替代 vanilla 的 BitSet filter
    // （索引 = chunkY - getMinLightSection()，超高世界窗口锚定 -1.34亿：玩家区域变化被
    // 范围判断挡死，且索引差值 1.34 亿级撑爆 BitSet）。集合无窗口依赖，极端 Y 天然安全。
    private final LongOpenHashSet blockChangedLightSections = new LongOpenHashSet();
    private final LongOpenHashSet skyChangedLightSections = new LongOpenHashSet();
    private final LevelLightEngine lightEngine;
    private final ChunkHolder.LevelChangeListener onLevelChange;
    private final ChunkHolder.PlayerProvider playerProvider;
    private boolean wasAccessibleSinceLastSave;
    private CompletableFuture<?> pendingFullStateConfirmation = CompletableFuture.completedFuture(null);
    private CompletableFuture<?> sendSync = CompletableFuture.completedFuture(null);
    private CompletableFuture<?> saveSync = CompletableFuture.completedFuture(null);

    public ChunkHolder(
        final ChunkPos pos,
        final int ticketLevel,
        final LevelHeightAccessor levelHeightAccessor,
        final LevelLightEngine lightEngine,
        final ChunkHolder.LevelChangeListener onLevelChange,
        final ChunkHolder.PlayerProvider playerProvider
    ) {
        super(pos);
        this.levelHeightAccessor = levelHeightAccessor;
        this.lightEngine = lightEngine;
        this.onLevelChange = onLevelChange;
        this.playerProvider = playerProvider;
        this.oldTicketLevel = ChunkLevel.MAX_LEVEL + 1;
        this.ticketLevel = this.oldTicketLevel;
        this.queueLevel = this.oldTicketLevel;
        this.setTicketLevel(ticketLevel);
    }

    public CompletableFuture<ChunkResult<LevelChunk>> getTickingChunkFuture() {
        return this.tickingChunkFuture;
    }

    public CompletableFuture<ChunkResult<LevelChunk>> getEntityTickingChunkFuture() {
        return this.entityTickingChunkFuture;
    }

    public CompletableFuture<ChunkResult<LevelChunk>> getFullChunkFuture() {
        return this.fullChunkFuture;
    }

    public @Nullable LevelChunk getTickingChunk() {
        return this.getTickingChunkFuture().getNow(UNLOADED_LEVEL_CHUNK).orElse(null);
    }

    public @Nullable LevelChunk getChunkToSend() {
        return !this.sendSync.isDone() ? null : this.getTickingChunk();
    }

    public CompletableFuture<?> getSendSyncFuture() {
        return this.sendSync;
    }

    public void addSendDependency(final CompletableFuture<?> sync) {
        if (this.sendSync.isDone()) {
            this.sendSync = sync;
        } else {
            this.sendSync = this.sendSync.thenCombine((CompletionStage<? extends Object>)sync, (a, b) -> null);
        }
    }

    public CompletableFuture<?> getSaveSyncFuture() {
        return this.saveSync;
    }

    public boolean isReadyForSaving() {
        return this.saveSync.isDone();
    }

    @Override
    protected void addSaveDependency(final CompletableFuture<?> sync) {
        if (this.saveSync.isDone()) {
            this.saveSync = sync;
        } else {
            this.saveSync = this.saveSync.thenCombine((CompletionStage<? extends Object>)sync, (a, b) -> null);
        }
    }

    public boolean blockChanged(final BlockPos pos) {
        LevelChunk chunk = this.getTickingChunk();
        if (chunk == null) {
            return false;
        }

        boolean hadChangedSections = this.hasChangedSections;
        // 🔧 MCRe：绝对 sectionY（无范围限制），替代原数组索引（超高世界越界/OOM）
        int sectionY = SectionPos.blockToSectionCoord(pos.getY());
        ShortSet changedBlocksInSection = this.changedBlocksPerSection.get(sectionY);
        if (changedBlocksInSection == null) {
            this.hasChangedSections = true;
            changedBlocksInSection = new ShortOpenHashSet();
            this.changedBlocksPerSection.put(sectionY, changedBlocksInSection);
        }

        changedBlocksInSection.add(SectionPos.sectionRelativePos(pos));
        return !hadChangedSections;
    }

    public boolean sectionLightChanged(final LightLayer layer, final int chunkY) {
        ChunkAccess chunk = this.getChunkIfPresent(ChunkStatus.INITIALIZE_LIGHT);
        if (chunk == null) {
            return false;
        }

        chunk.markUnsaved();
        LevelChunk tickingChunk = this.getTickingChunk();
        if (tickingChunk == null) {
            return false;
        }

        // 🔧 MCRe P4b：无窗口范围判断——超高世界光照窗口锚定 -1.34亿，
        // 玩家区域真实 sectionY 不在 [minLightSection, maxLightSection] 内会被挡死。
        // 改为无条件记录绝对 sectionY（集合），广播侧直接用绝对 sectionY 取数据。
        LongOpenHashSet filter = layer == LightLayer.SKY ? this.skyChangedLightSections : this.blockChangedLightSections;
        if (!filter.add(chunkY)) {
            return false;
        } else {
            return true;
        }
    }

    public boolean hasChangesToBroadcast() {
        return this.hasChangedSections || !this.skyChangedLightSections.isEmpty() || !this.blockChangedLightSections.isEmpty();
    }

    public void broadcastChanges(final LevelChunk chunk) {
        if (this.hasChangesToBroadcast()) {
            Level level = chunk.getLevel();
            if (!this.skyChangedLightSections.isEmpty() || !this.blockChangedLightSections.isEmpty()) {
                List<ServerPlayer> borderPlayers = this.playerProvider.getPlayers(this.pos, true);
                if (!borderPlayers.isEmpty()) {
                    ClientboundLightUpdatePacket lightPacket = new ClientboundLightUpdatePacket(
                        chunk.getPos(), this.lightEngine, this.skyChangedLightSections, this.blockChangedLightSections,
                        chunk.getMinSectionY(), chunk.getMaxSectionY()
                    );
                    this.broadcast(borderPlayers, lightPacket);
                }

                this.skyChangedLightSections.clear();
                this.blockChangedLightSections.clear();
            }

            if (this.hasChangedSections) {
                List<ServerPlayer> players = this.playerProvider.getPlayers(this.pos, false);

                // 🔧 MCRe：遍历 Map（键 = 绝对 sectionY），替代原数组索引遍历
                for (Map.Entry<Integer, ShortSet> entry : this.changedBlocksPerSection.entrySet()) {
                    ShortSet changedBlocks = entry.getValue();
                    if (changedBlocks != null && !changedBlocks.isEmpty()) {
                        int sectionY = entry.getKey();
                        SectionPos sectionPos = SectionPos.of(chunk.getPos(), sectionY);
                        if (changedBlocks.size() == 1) {
                            BlockPos pos = sectionPos.relativeToBlockPos(changedBlocks.iterator().nextShort());
                            BlockState state = level.getBlockState(pos);
                            this.broadcast(players, new ClientboundBlockUpdatePacket(pos, state));
                            this.broadcastBlockEntityIfNeeded(players, level, pos, state);
                        } else {
                            LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
                            ClientboundSectionBlocksUpdatePacket packet = new ClientboundSectionBlocksUpdatePacket(sectionPos, changedBlocks, section);
                            this.broadcast(players, packet);
                            packet.runUpdates((pos, state) -> this.broadcastBlockEntityIfNeeded(players, level, pos, state));
                        }
                    }
                }

                this.changedBlocksPerSection.clear();
                this.hasChangedSections = false;
            }
        }
    }

    private void broadcastBlockEntityIfNeeded(final List<ServerPlayer> players, final Level level, final BlockPos pos, final BlockState state) {
        if (state.hasBlockEntity()) {
            this.broadcastBlockEntity(players, level, pos);
        }
    }

    private void broadcastBlockEntity(final List<ServerPlayer> players, final Level level, final BlockPos blockPos) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity != null) {
            Packet<?> packet = blockEntity.getUpdatePacket();
            if (packet != null) {
                this.broadcast(players, packet);
            }
        }
    }

    private void broadcast(final List<ServerPlayer> players, final Packet<?> packet) {
        players.forEach(player -> player.connection.send(packet));
    }

    @Override
    public int getTicketLevel() {
        return this.ticketLevel;
    }

    @Override
    public int getQueueLevel() {
        return this.queueLevel;
    }

    private void setQueueLevel(final int queueLevel) {
        this.queueLevel = queueLevel;
    }

    public void setTicketLevel(final int ticketLevel) {
        this.ticketLevel = ticketLevel;
    }

    private void scheduleFullChunkPromotion(
        final ChunkMap scheduler, final CompletableFuture<ChunkResult<LevelChunk>> task, final Executor mainThreadExecutor, final FullChunkStatus status
    ) {
        this.pendingFullStateConfirmation.cancel(false);
        CompletableFuture<Void> confirmation = new CompletableFuture<>();
        confirmation.thenRunAsync(() -> scheduler.onFullChunkStatusChange(this.pos, status), mainThreadExecutor);
        this.pendingFullStateConfirmation = confirmation;
        task.thenAccept(r -> r.ifSuccess(l -> confirmation.complete(null)));
    }

    private void demoteFullChunk(final ChunkMap scheduler, final FullChunkStatus status) {
        this.pendingFullStateConfirmation.cancel(false);
        scheduler.onFullChunkStatusChange(this.pos, status);
    }

    protected void updateFutures(final ChunkMap scheduler, final Executor mainThreadExecutor) {
        FullChunkStatus oldFullStatus = ChunkLevel.fullStatus(this.oldTicketLevel);
        FullChunkStatus newFullStatus = ChunkLevel.fullStatus(this.ticketLevel);
        boolean wasAccessible = oldFullStatus.isOrAfter(FullChunkStatus.FULL);
        boolean isAccessible = newFullStatus.isOrAfter(FullChunkStatus.FULL);
        this.wasAccessibleSinceLastSave |= isAccessible;
        if (!wasAccessible && isAccessible) {
            this.fullChunkFuture = scheduler.prepareAccessibleChunk(this);
            this.scheduleFullChunkPromotion(scheduler, this.fullChunkFuture, mainThreadExecutor, FullChunkStatus.FULL);
            this.addSaveDependency(this.fullChunkFuture);
        }

        if (wasAccessible && !isAccessible) {
            this.fullChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
            this.fullChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
        }

        boolean wasTicking = oldFullStatus.isOrAfter(FullChunkStatus.BLOCK_TICKING);
        boolean isTicking = newFullStatus.isOrAfter(FullChunkStatus.BLOCK_TICKING);
        if (!wasTicking && isTicking) {
            this.tickingChunkFuture = scheduler.prepareTickingChunk(this);
            this.scheduleFullChunkPromotion(scheduler, this.tickingChunkFuture, mainThreadExecutor, FullChunkStatus.BLOCK_TICKING);
            this.addSaveDependency(this.tickingChunkFuture);
        }

        if (wasTicking && !isTicking) {
            this.tickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
            this.tickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
        }

        boolean wasEntityTicking = oldFullStatus.isOrAfter(FullChunkStatus.ENTITY_TICKING);
        boolean isEntityTicking = newFullStatus.isOrAfter(FullChunkStatus.ENTITY_TICKING);
        if (!wasEntityTicking && isEntityTicking) {
            if (this.entityTickingChunkFuture != UNLOADED_LEVEL_CHUNK_FUTURE) {
                throw (IllegalStateException)Util.pauseInIde(new IllegalStateException());
            }

            this.entityTickingChunkFuture = scheduler.prepareEntityTickingChunk(this);
            this.scheduleFullChunkPromotion(scheduler, this.entityTickingChunkFuture, mainThreadExecutor, FullChunkStatus.ENTITY_TICKING);
            this.addSaveDependency(this.entityTickingChunkFuture);
        }

        if (wasEntityTicking && !isEntityTicking) {
            this.entityTickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
            this.entityTickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
        }

        if (!newFullStatus.isOrAfter(oldFullStatus)) {
            this.demoteFullChunk(scheduler, newFullStatus);
        }

        this.onLevelChange.onLevelChange(this.pos, this::getQueueLevel, this.ticketLevel, this::setQueueLevel);
        this.oldTicketLevel = this.ticketLevel;
    }

    public boolean wasAccessibleSinceLastSave() {
        return this.wasAccessibleSinceLastSave;
    }

    public void refreshAccessibility() {
        this.wasAccessibleSinceLastSave = ChunkLevel.fullStatus(this.ticketLevel).isOrAfter(FullChunkStatus.FULL);
    }

    @FunctionalInterface
    public interface LevelChangeListener {
        void onLevelChange(ChunkPos pos, IntSupplier oldLevel, int newLevel, IntConsumer setQueueLevel);
    }

    public interface PlayerProvider {
        List<ServerPlayer> getPlayers(ChunkPos pos, boolean borderOnly);
    }
}