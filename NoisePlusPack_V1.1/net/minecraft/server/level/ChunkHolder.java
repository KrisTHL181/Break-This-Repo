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
      ChunkPos p_142986_,
      int p_142987_,
      LevelHeightAccessor p_142988_,
      LevelLightEngine p_142989_,
      ChunkHolder.LevelChangeListener p_142990_,
      ChunkHolder.PlayerProvider p_142991_
   ) {
      super(p_142986_);
      this.levelHeightAccessor = p_142988_;
      this.lightEngine = p_142989_;
      this.onLevelChange = p_142990_;
      this.playerProvider = p_142991_;
      this.oldTicketLevel = ChunkLevel.MAX_LEVEL + 1;
      this.ticketLevel = this.oldTicketLevel;
      this.queueLevel = this.oldTicketLevel;
      this.setTicketLevel(p_142987_);
      this.changedBlocksPerSection = new ShortSet[p_142988_.getSectionsCount()];
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

   public void addSendDependency(CompletableFuture<?> p_300440_) {
      if (this.sendSync.isDone()) {
         this.sendSync = p_300440_;
      } else {
         this.sendSync = this.sendSync.thenCombine((CompletionStage<? extends Object>)p_300440_, (p_341205_, p_341206_) -> null);
      }
   }

   public CompletableFuture<?> getSaveSyncFuture() {
      return this.saveSync;
   }

   public boolean isReadyForSaving() {
      return this.saveSync.isDone();
   }

   @Override
   protected void addSaveDependency(CompletableFuture<?> p_200418_) {
      if (this.saveSync.isDone()) {
         this.saveSync = p_200418_;
      } else {
         this.saveSync = this.saveSync.thenCombine((CompletionStage<? extends Object>)p_200418_, (p_296561_, p_296562_) -> null);
      }
   }

   public boolean blockChanged(BlockPos p_140057_) {
      LevelChunk levelchunk = this.getTickingChunk();
      if (levelchunk == null) {
         return false;
      }

      boolean flag = this.hasChangedSections;
      int i = this.levelHeightAccessor.getSectionIndex(p_140057_.getY());
      ShortSet shortset = this.changedBlocksPerSection[i];
      if (shortset == null) {
         this.hasChangedSections = true;
         shortset = new ShortOpenHashSet();
         this.changedBlocksPerSection[i] = shortset;
      }

      shortset.add(SectionPos.sectionRelativePos(p_140057_));
      return !flag;
   }

   public boolean sectionLightChanged(LightLayer p_140037_, int p_140038_) {
      ChunkAccess chunkaccess = this.getChunkIfPresent(ChunkStatus.INITIALIZE_LIGHT);
      if (chunkaccess == null) {
         return false;
      }

      chunkaccess.markUnsaved();
      LevelChunk levelchunk = this.getTickingChunk();
      if (levelchunk == null) {
         return false;
      }

      int i = this.lightEngine.getMinLightSection();
      int j = this.lightEngine.getMaxLightSection();
      if (p_140038_ >= i && p_140038_ <= j) {
         BitSet bitset = p_140037_ == LightLayer.SKY ? this.skyChangedLightSectionFilter : this.blockChangedLightSectionFilter;
         int k = p_140038_ - i;
         if (!bitset.get(k)) {
            bitset.set(k);
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

   public void broadcastChanges(LevelChunk p_140055_) {
      if (this.hasChangesToBroadcast()) {
         Level level = p_140055_.getLevel();
         if (!this.skyChangedLightSectionFilter.isEmpty() || !this.blockChangedLightSectionFilter.isEmpty()) {
            List<ServerPlayer> list = this.playerProvider.getPlayers(this.pos, true);
            if (!list.isEmpty()) {
               ClientboundLightUpdatePacket clientboundlightupdatepacket = new ClientboundLightUpdatePacket(
                  p_140055_.getPos(), this.lightEngine, this.skyChangedLightSectionFilter, this.blockChangedLightSectionFilter
               );
               this.broadcast(list, clientboundlightupdatepacket);
            }

            this.skyChangedLightSectionFilter.clear();
            this.blockChangedLightSectionFilter.clear();
         }

         if (this.hasChangedSections) {
            List<ServerPlayer> list1 = this.playerProvider.getPlayers(this.pos, false);

            for (int j = 0; j < this.changedBlocksPerSection.length; j++) {
               ShortSet shortset = this.changedBlocksPerSection[j];
               if (shortset != null) {
                  this.changedBlocksPerSection[j] = null;
                  if (!list1.isEmpty()) {
                     int i = this.levelHeightAccessor.getSectionYFromSectionIndex(j);
                     SectionPos sectionpos = SectionPos.of(p_140055_.getPos(), i);
                     if (shortset.size() == 1) {
                        BlockPos blockpos = sectionpos.relativeToBlockPos(shortset.iterator().nextShort());
                        BlockState blockstate = level.getBlockState(blockpos);
                        this.broadcast(list1, new ClientboundBlockUpdatePacket(blockpos, blockstate));
                        this.broadcastBlockEntityIfNeeded(list1, level, blockpos, blockstate);
                     } else {
                        LevelChunkSection levelchunksection = p_140055_.getSection(j);
                        ClientboundSectionBlocksUpdatePacket clientboundsectionblocksupdatepacket = new ClientboundSectionBlocksUpdatePacket(
                           sectionpos, shortset, levelchunksection
                        );
                        this.broadcast(list1, clientboundsectionblocksupdatepacket);
                        clientboundsectionblocksupdatepacket.runUpdates(
                           (p_288761_, p_288762_) -> this.broadcastBlockEntityIfNeeded(list1, level, p_288761_, p_288762_)
                        );
                     }
                  }
               }
            }

            this.hasChangedSections = false;
         }
      }
   }

   private void broadcastBlockEntityIfNeeded(List<ServerPlayer> p_288982_, Level p_289011_, BlockPos p_288969_, BlockState p_288973_) {
      if (p_288973_.hasBlockEntity()) {
         this.broadcastBlockEntity(p_288982_, p_289011_, p_288969_);
      }
   }

   private void broadcastBlockEntity(List<ServerPlayer> p_288988_, Level p_289005_, BlockPos p_288981_) {
      BlockEntity blockentity = p_289005_.getBlockEntity(p_288981_);
      if (blockentity != null) {
         Packet<?> packet = blockentity.getUpdatePacket();
         if (packet != null) {
            this.broadcast(p_288988_, packet);
         }
      }
   }

   private void broadcast(List<ServerPlayer> p_288998_, Packet<?> p_289013_) {
      p_288998_.forEach(p_296560_ -> p_296560_.connection.send(p_289013_));
   }

   @Override
   public int getTicketLevel() {
      return this.ticketLevel;
   }

   @Override
   public int getQueueLevel() {
      return this.queueLevel;
   }

   private void setQueueLevel(int p_140087_) {
      this.queueLevel = p_140087_;
   }

   public void setTicketLevel(int p_140028_) {
      this.ticketLevel = p_140028_;
   }

   private void scheduleFullChunkPromotion(
      ChunkMap p_142999_, CompletableFuture<ChunkResult<LevelChunk>> p_143000_, Executor p_143001_, FullChunkStatus p_287621_
   ) {
      this.pendingFullStateConfirmation.cancel(false);
      CompletableFuture<Void> completablefuture = new CompletableFuture<>();
      completablefuture.thenRunAsync(() -> p_142999_.onFullChunkStatusChange(this.pos, p_287621_), p_143001_);
      this.pendingFullStateConfirmation = completablefuture;
      p_143000_.thenAccept(p_326372_ -> p_326372_.ifSuccess(p_200424_ -> completablefuture.complete(null)));
   }

   private void demoteFullChunk(ChunkMap p_287599_, FullChunkStatus p_287649_) {
      this.pendingFullStateConfirmation.cancel(false);
      p_287599_.onFullChunkStatusChange(this.pos, p_287649_);
   }

   protected void updateFutures(ChunkMap p_143004_, Executor p_143005_) {
      FullChunkStatus fullchunkstatus = ChunkLevel.fullStatus(this.oldTicketLevel);
      FullChunkStatus fullchunkstatus1 = ChunkLevel.fullStatus(this.ticketLevel);
      boolean flag = fullchunkstatus.isOrAfter(FullChunkStatus.FULL);
      boolean flag1 = fullchunkstatus1.isOrAfter(FullChunkStatus.FULL);
      this.wasAccessibleSinceLastSave |= flag1;
      if (!flag && flag1) {
         this.fullChunkFuture = p_143004_.prepareAccessibleChunk(this);
         this.scheduleFullChunkPromotion(p_143004_, this.fullChunkFuture, p_143005_, FullChunkStatus.FULL);
         this.addSaveDependency(this.fullChunkFuture);
      }

      if (flag && !flag1) {
         this.fullChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
         this.fullChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
      }

      boolean flag2 = fullchunkstatus.isOrAfter(FullChunkStatus.BLOCK_TICKING);
      boolean flag3 = fullchunkstatus1.isOrAfter(FullChunkStatus.BLOCK_TICKING);
      if (!flag2 && flag3) {
         this.tickingChunkFuture = p_143004_.prepareTickingChunk(this);
         this.scheduleFullChunkPromotion(p_143004_, this.tickingChunkFuture, p_143005_, FullChunkStatus.BLOCK_TICKING);
         this.addSaveDependency(this.tickingChunkFuture);
      }

      if (flag2 && !flag3) {
         this.tickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
         this.tickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
      }

      boolean flag4 = fullchunkstatus.isOrAfter(FullChunkStatus.ENTITY_TICKING);
      boolean flag5 = fullchunkstatus1.isOrAfter(FullChunkStatus.ENTITY_TICKING);
      if (!flag4 && flag5) {
         if (this.entityTickingChunkFuture != UNLOADED_LEVEL_CHUNK_FUTURE) {
            throw (IllegalStateException)Util.pauseInIde(new IllegalStateException());
         }

         this.entityTickingChunkFuture = p_143004_.prepareEntityTickingChunk(this);
         this.scheduleFullChunkPromotion(p_143004_, this.entityTickingChunkFuture, p_143005_, FullChunkStatus.ENTITY_TICKING);
         this.addSaveDependency(this.entityTickingChunkFuture);
      }

      if (flag4 && !flag5) {
         this.entityTickingChunkFuture.complete(UNLOADED_LEVEL_CHUNK);
         this.entityTickingChunkFuture = UNLOADED_LEVEL_CHUNK_FUTURE;
      }

      if (!fullchunkstatus1.isOrAfter(fullchunkstatus)) {
         this.demoteFullChunk(p_143004_, fullchunkstatus1);
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
      void onLevelChange(ChunkPos var1, IntSupplier var2, int var3, IntConsumer var4);
   }

   public interface PlayerProvider {
      List<ServerPlayer> getPlayers(ChunkPos var1, boolean var2);
   }
}
