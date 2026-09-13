package net.minecraft.server.level;

import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import java.util.BitSet;
import java.util.List;
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
   private final @Nullable ShortSet[] changedBlocksPerSection;
   private final BitSet blockChangedLightSectionFilter = new BitSet();
   private final BitSet skyChangedLightSectionFilter = new BitSet();
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
      this.changedBlocksPerSection = new ShortSet[levelHeightAccessor.getSectionsCount()];
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
      int sectionIndex = this.levelHeightAccessor.getSectionIndex(pos.getY());
      ShortSet changedBlocksInSection = this.changedBlocksPerSection[sectionIndex];
      if (changedBlocksInSection == null) {
         this.hasChangedSections = true;
         changedBlocksInSection = new ShortOpenHashSet();
         this.changedBlocksPerSection[sectionIndex] = changedBlocksInSection;
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

      int minLightSection = this.lightEngine.getMinLightSection();
      int maxLightSection = this.lightEngine.getMaxLightSection();
      if (chunkY >= minLightSection && chunkY <= maxLightSection) {
         BitSet filter = layer == LightLayer.SKY ? this.skyChangedLightSectionFilter : this.blockChangedLightSectionFilter;
         int index = chunkY - minLightSection;
         if (!filter.get(index)) {
            filter.set(index);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean hasChangesToBroadcast() {
      return this.hasChangedSections || !this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty();
   }

   public void broadcastChanges(final LevelChunk chunk) {
      if (this.hasChangesToBroadcast()) {
         Level level = chunk.getLevel();
         if (!this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty()) {
            List<ServerPlayer> borderPlayers = this.playerProvider.getPlayers(this.pos, true);
            if (!borderPlayers.isEmpty()) {
               ClientboundLightUpdatePacket lightPacket = new ClientboundLightUpdatePacket(
                  chunk.getPos(), this.lightEngine, this.skyChangedLightSectionFilter, this.blockChangedLightSectionFilter
               );
               this.broadcast(borderPlayers, lightPacket);
            }

            this.skyChangedLightSectionFilter.clear();
            this.blockChangedLightSectionFilter.clear();
         }

         if (this.hasChangedSections) {
            List<ServerPlayer> players = this.playerProvider.getPlayers(this.pos, false);

            for (int sectionIndex = 0; sectionIndex < this.changedBlocksPerSection.length; sectionIndex++) {
               ShortSet changedBlocks = this.changedBlocksPerSection[sectionIndex];
               if (changedBlocks != null) {
                  this.changedBlocksPerSection[sectionIndex] = null;
                  if (!players.isEmpty()) {
                     int sectionY = this.levelHeightAccessor.getSectionYFromSectionIndex(sectionIndex);
                     SectionPos sectionPos = SectionPos.of(chunk.getPos(), sectionY);
                     if (changedBlocks.size() == 1) {
                        BlockPos pos = sectionPos.relativeToBlockPos(changedBlocks.iterator().nextShort());
                        BlockState state = level.getBlockState(pos);
                        this.broadcast(players, new ClientboundBlockUpdatePacket(pos, state));
                        this.broadcastBlockEntityIfNeeded(players, level, pos, state);
                     } else {
                        LevelChunkSection section = chunk.getSection(sectionIndex);
                        ClientboundSectionBlocksUpdatePacket packet = new ClientboundSectionBlocksUpdatePacket(sectionPos, changedBlocks, section);
                        this.broadcast(players, packet);
                        packet.runUpdates((pos, state) -> this.broadcastBlockEntityIfNeeded(players, level, pos, state));
                     }
                  }
               }
            }

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
