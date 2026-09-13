package net.minecraft.world.level.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.ShortList;
import it.unimi.dsi.fastutil.shorts.ShortListIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.debug.DebugStructureInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gameevent.EuclideanGameEventListenerRegistry;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.GameEventListenerRegistry;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.TickContainerAccess;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class LevelChunk extends ChunkAccess implements DebugValueSource {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final TickingBlockEntity NULL_TICKER = new TickingBlockEntity() {
      @Override
      public void tick() {
      }

      @Override
      public boolean isRemoved() {
         return true;
      }

      @Override
      public BlockPos getPos() {
         return BlockPos.ZERO;
      }

      @Override
      public String getType() {
         return "<null>";
      }
   };
   private final Map<BlockPos, LevelChunk.RebindableTickingBlockEntityWrapper> tickersInLevel = Maps.newHashMap();
   private boolean loaded;
   final Level level;
   private @Nullable Supplier<FullChunkStatus> fullStatus;
   private LevelChunk.@Nullable PostLoadProcessor postLoad;
   private final Int2ObjectMap<GameEventListenerRegistry> gameEventListenerRegistrySections;
   private final LevelChunkTicks<Block> blockTicks;
   private final LevelChunkTicks<Fluid> fluidTicks;
   private LevelChunk.UnsavedListener unsavedListener = p_360556_ -> {};

   public LevelChunk(Level p_187945_, ChunkPos p_187946_) {
      this(p_187945_, p_187946_, UpgradeData.EMPTY, new LevelChunkTicks<>(), new LevelChunkTicks<>(), 0L, null, null, null);
   }

   public LevelChunk(
      Level p_196854_,
      ChunkPos p_196855_,
      UpgradeData p_196856_,
      LevelChunkTicks<Block> p_196857_,
      LevelChunkTicks<Fluid> p_196858_,
      long p_196859_,
      LevelChunkSection @Nullable [] p_196860_,
      LevelChunk.@Nullable PostLoadProcessor p_196861_,
      @Nullable BlendingData p_196862_
   ) {
      super(p_196855_, p_196856_, p_196854_, p_196854_.palettedContainerFactory(), p_196859_, p_196860_, p_196862_);
      this.level = p_196854_;
      this.gameEventListenerRegistrySections = new Int2ObjectOpenHashMap();

      for (Heightmap.Types heightmap$types : Heightmap.Types.values()) {
         if (ChunkStatus.FULL.heightmapsAfter().contains(heightmap$types)) {
            this.heightmaps.put(heightmap$types, new Heightmap(this, heightmap$types));
         }
      }

      this.postLoad = p_196861_;
      this.blockTicks = p_196857_;
      this.fluidTicks = p_196858_;
   }

   public LevelChunk(ServerLevel p_196850_, ProtoChunk p_196851_, LevelChunk.@Nullable PostLoadProcessor p_196852_) {
      this(
         p_196850_,
         p_196851_.getPos(),
         p_196851_.getUpgradeData(),
         p_196851_.unpackBlockTicks(),
         p_196851_.unpackFluidTicks(),
         p_196851_.getInhabitedTime(),
         p_196851_.getSections(),
         p_196852_,
         p_196851_.getBlendingData()
      );
      if (!Collections.disjoint(p_196851_.pendingBlockEntities.keySet(), p_196851_.blockEntities.keySet())) {
         LOGGER.error("Chunk at {} contains duplicated block entities", p_196851_.getPos());
      }

      for (BlockEntity blockentity : p_196851_.getBlockEntities().values()) {
         this.setBlockEntity(blockentity);
      }

      this.pendingBlockEntities.putAll(p_196851_.getBlockEntityNbts());

      for (int i = 0; i < p_196851_.getPostProcessing().length; i++) {
         this.postProcessing[i] = p_196851_.getPostProcessing()[i];
      }

      this.setAllStarts(p_196851_.getAllStarts());
      this.setAllReferences(p_196851_.getAllReferences());

      for (Entry<Heightmap.Types, Heightmap> entry : p_196851_.getHeightmaps()) {
         if (ChunkStatus.FULL.heightmapsAfter().contains(entry.getKey())) {
            this.setHeightmap(entry.getKey(), entry.getValue().getRawData());
         }
      }

      this.skyLightSources = p_196851_.skyLightSources;
      this.setLightCorrect(p_196851_.isLightCorrect());
      this.markUnsaved();
   }

   public void setUnsavedListener(LevelChunk.UnsavedListener p_364949_) {
      this.unsavedListener = p_364949_;
      if (this.isUnsaved()) {
         p_364949_.setUnsaved(this.chunkPos);
      }
   }

   @Override
   public void markUnsaved() {
      boolean flag = this.isUnsaved();
      super.markUnsaved();
      if (!flag) {
         this.unsavedListener.setUnsaved(this.chunkPos);
      }
   }

   @Override
   public TickContainerAccess<Block> getBlockTicks() {
      return this.blockTicks;
   }

   @Override
   public TickContainerAccess<Fluid> getFluidTicks() {
      return this.fluidTicks;
   }

   @Override
   public ChunkAccess.PackedTicks getTicksForSerialization(long p_361110_) {
      return new ChunkAccess.PackedTicks(this.blockTicks.pack(p_361110_), this.fluidTicks.pack(p_361110_));
   }

   @Override
   public GameEventListenerRegistry getListenerRegistry(int p_251193_) {
      return this.level instanceof ServerLevel serverlevel
         ? (GameEventListenerRegistry)this.gameEventListenerRegistrySections
            .computeIfAbsent(p_251193_, p_281221_ -> new EuclideanGameEventListenerRegistry(serverlevel, p_251193_, this::removeGameEventListenerRegistry))
         : super.getListenerRegistry(p_251193_);
   }

   @Override
   public BlockState getBlockState(BlockPos p_62923_) {
      int i = p_62923_.getX();
      int j = p_62923_.getY();
      int k = p_62923_.getZ();
      if (this.level.isDebug()) {
         BlockState blockstate = null;
         if (j == 60) {
            blockstate = Blocks.BARRIER.defaultBlockState();
         }

         if (j == 70) {
            blockstate = DebugLevelSource.getBlockStateFor(i, k);
         }

         return blockstate == null ? Blocks.AIR.defaultBlockState() : blockstate;
      } else {
         try {
            int l = this.getSectionIndex(j);
            if (l >= 0 && l < this.sections.length) {
               LevelChunkSection levelchunksection = this.sections[l];
               if (!levelchunksection.hasOnlyAir()) {
                  return levelchunksection.getBlockState(i & 15, j & 15, k & 15);
               }
            }

            return Blocks.AIR.defaultBlockState();
         } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting block state");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being got");
            crashreportcategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this, i, j, k));
            throw new ReportedException(crashreport);
         }
      }
   }

   @Override
   public FluidState getFluidState(BlockPos p_62895_) {
      return this.getFluidState(p_62895_.getX(), p_62895_.getY(), p_62895_.getZ());
   }

   public FluidState getFluidState(int p_62815_, int p_62816_, int p_62817_) {
      try {
         int i = this.getSectionIndex(p_62816_);
         if (i >= 0 && i < this.sections.length) {
            LevelChunkSection levelchunksection = this.sections[i];
            if (!levelchunksection.hasOnlyAir()) {
               return levelchunksection.getFluidState(p_62815_ & 15, p_62816_ & 15, p_62817_ & 15);
            }
         }

         return Fluids.EMPTY.defaultFluidState();
      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting fluid state");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Block being got");
         crashreportcategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this, p_62815_, p_62816_, p_62817_));
         throw new ReportedException(crashreport);
      }
   }

   @Override
   public @Nullable BlockState setBlockState(BlockPos p_62865_, BlockState p_62866_, @Block.UpdateFlags int p_393998_) {
      int i = p_62865_.getY();
      LevelChunkSection levelchunksection = this.getSection(this.getSectionIndex(i));
      boolean flag = levelchunksection.hasOnlyAir();
      if (flag && p_62866_.isAir()) {
         return null;
      }

      int j = p_62865_.getX() & 15;
      int k = i & 15;
      int l = p_62865_.getZ() & 15;
      BlockState blockstate = levelchunksection.setBlockState(j, k, l, p_62866_);
      if (blockstate == p_62866_) {
         return null;
      }

      Block block = p_62866_.getBlock();
      this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING).update(j, i, l, p_62866_);
      this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES).update(j, i, l, p_62866_);
      this.heightmaps.get(Heightmap.Types.OCEAN_FLOOR).update(j, i, l, p_62866_);
      this.heightmaps.get(Heightmap.Types.WORLD_SURFACE).update(j, i, l, p_62866_);
      boolean flag1 = levelchunksection.hasOnlyAir();
      if (flag != flag1) {
         this.level.getChunkSource().getLightEngine().updateSectionStatus(p_62865_, flag1);
         this.level.getChunkSource().onSectionEmptinessChanged(this.chunkPos.x, SectionPos.blockToSectionCoord(i), this.chunkPos.z, flag1);
      }

      if (LightEngine.hasDifferentLightProperties(blockstate, p_62866_)) {
         ProfilerFiller profilerfiller = Profiler.get();
         profilerfiller.push("updateSkyLightSources");
         this.skyLightSources.update(this, j, i, l);
         profilerfiller.popPush("queueCheckLight");
         this.level.getChunkSource().getLightEngine().checkBlock(p_62865_);
         profilerfiller.pop();
      }

      boolean flag4 = !blockstate.is(block);
      boolean flag2 = (p_393998_ & 64) != 0;
      boolean flag3 = (p_393998_ & 256) == 0;
      if (flag4 && blockstate.hasBlockEntity() && !p_62866_.shouldChangedStateKeepBlockEntity(blockstate)) {
         if (!this.level.isClientSide() && flag3) {
            BlockEntity blockentity = this.level.getBlockEntity(p_62865_);
            if (blockentity != null) {
               blockentity.preRemoveSideEffects(p_62865_, blockstate);
            }
         }

         this.removeBlockEntity(p_62865_);
      }

      if ((flag4 || block instanceof BaseRailBlock) && this.level instanceof ServerLevel serverlevel && ((p_393998_ & 1) != 0 || flag2)) {
         blockstate.affectNeighborsAfterRemoval(serverlevel, p_62865_, flag2);
      }

      if (!levelchunksection.getBlockState(j, k, l).is(block)) {
         return null;
      }

      if (!this.level.isClientSide() && (p_393998_ & 512) == 0) {
         p_62866_.onPlace(this.level, p_62865_, blockstate, flag2);
      }

      if (p_62866_.hasBlockEntity()) {
         BlockEntity blockentity1 = this.getBlockEntity(p_62865_, LevelChunk.EntityCreationType.CHECK);
         if (blockentity1 != null && !blockentity1.isValidBlockState(p_62866_)) {
            LOGGER.warn(
               "Found mismatched block entity @ {}: type = {}, state = {}",
               new Object[]{p_62865_, blockentity1.getType().builtInRegistryHolder().key().identifier(), p_62866_}
            );
            this.removeBlockEntity(p_62865_);
            blockentity1 = null;
         }

         if (blockentity1 == null) {
            blockentity1 = ((EntityBlock)block).newBlockEntity(p_62865_, p_62866_);
            if (blockentity1 != null) {
               this.addAndRegisterBlockEntity(blockentity1);
            }
         } else {
            blockentity1.setBlockState(p_62866_);
            this.updateBlockEntityTicker(blockentity1);
         }
      }

      this.markUnsaved();
      return blockstate;
   }

   @Deprecated
   @Override
   public void addEntity(Entity p_62826_) {
   }

   private @Nullable BlockEntity createBlockEntity(BlockPos p_62935_) {
      BlockState blockstate = this.getBlockState(p_62935_);
      return !blockstate.hasBlockEntity() ? null : ((EntityBlock)blockstate.getBlock()).newBlockEntity(p_62935_, blockstate);
   }

   @Override
   public @Nullable BlockEntity getBlockEntity(BlockPos p_62912_) {
      return this.getBlockEntity(p_62912_, LevelChunk.EntityCreationType.CHECK);
   }

   public @Nullable BlockEntity getBlockEntity(BlockPos p_62868_, LevelChunk.EntityCreationType p_62869_) {
      BlockEntity blockentity = this.blockEntities.get(p_62868_);
      if (blockentity == null) {
         CompoundTag compoundtag = this.pendingBlockEntities.remove(p_62868_);
         if (compoundtag != null) {
            BlockEntity blockentity1 = this.promotePendingBlockEntity(p_62868_, compoundtag);
            if (blockentity1 != null) {
               return blockentity1;
            }
         }
      }

      if (blockentity == null) {
         if (p_62869_ == LevelChunk.EntityCreationType.IMMEDIATE) {
            blockentity = this.createBlockEntity(p_62868_);
            if (blockentity != null) {
               this.addAndRegisterBlockEntity(blockentity);
            }
         }
      } else if (blockentity.isRemoved()) {
         this.blockEntities.remove(p_62868_);
         return null;
      }

      return blockentity;
   }

   public void addAndRegisterBlockEntity(BlockEntity p_156391_) {
      this.setBlockEntity(p_156391_);
      if (this.isInLevel()) {
         if (this.level instanceof ServerLevel serverlevel) {
            this.addGameEventListener(p_156391_, serverlevel);
         }

         this.level.onBlockEntityAdded(p_156391_);
         this.updateBlockEntityTicker(p_156391_);
      }
   }

   private boolean isInLevel() {
      return this.loaded || this.level.isClientSide();
   }

   boolean isTicking(BlockPos p_156411_) {
      if (!this.level.getWorldBorder().isWithinBounds(p_156411_)) {
         return false;
      } else {
         return this.level instanceof ServerLevel serverlevel
            ? this.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING) && serverlevel.areEntitiesLoaded(ChunkPos.asLong(p_156411_))
            : true;
      }
   }

   @Override
   public void setBlockEntity(BlockEntity p_156374_) {
      BlockPos blockpos = p_156374_.getBlockPos();
      BlockState blockstate = this.getBlockState(blockpos);
      if (!blockstate.hasBlockEntity()) {
         LOGGER.warn("Trying to set block entity {} at position {}, but state {} does not allow it", new Object[]{p_156374_, blockpos, blockstate});
      } else {
         BlockState blockstate1 = p_156374_.getBlockState();
         if (blockstate != blockstate1) {
            if (!p_156374_.getType().isValid(blockstate)) {
               LOGGER.warn("Trying to set block entity {} at position {}, but state {} does not allow it", new Object[]{p_156374_, blockpos, blockstate});
               return;
            }

            if (blockstate.getBlock() != blockstate1.getBlock()) {
               LOGGER.warn(
                  "Block state mismatch on block entity {} in position {}, {} != {}, updating", new Object[]{p_156374_, blockpos, blockstate, blockstate1}
               );
            }

            p_156374_.setBlockState(blockstate);
         }

         p_156374_.setLevel(this.level);
         p_156374_.clearRemoved();
         BlockEntity blockentity = this.blockEntities.put(blockpos.immutable(), p_156374_);
         if (blockentity != null && blockentity != p_156374_) {
            blockentity.setRemoved();
         }
      }
   }

   @Override
   public @Nullable CompoundTag getBlockEntityNbtForSaving(BlockPos p_62932_, HolderLookup.Provider p_329605_) {
      BlockEntity blockentity = this.getBlockEntity(p_62932_);
      if (blockentity != null && !blockentity.isRemoved()) {
         CompoundTag compoundtag1 = blockentity.saveWithFullMetadata(this.level.registryAccess());
         compoundtag1.putBoolean("keepPacked", false);
         return compoundtag1;
      }

      CompoundTag compoundtag = this.pendingBlockEntities.get(p_62932_);
      if (compoundtag != null) {
         compoundtag = compoundtag.copy();
         compoundtag.putBoolean("keepPacked", true);
      }

      return compoundtag;
   }

   @Override
   public void removeBlockEntity(BlockPos p_62919_) {
      if (this.isInLevel()) {
         BlockEntity blockentity = this.blockEntities.remove(p_62919_);
         if (blockentity != null) {
            if (this.level instanceof ServerLevel serverlevel) {
               this.removeGameEventListener(blockentity, serverlevel);
               serverlevel.debugSynchronizers().dropBlockEntity(p_62919_);
            }

            blockentity.setRemoved();
         }
      }

      this.removeBlockEntityTicker(p_62919_);
   }

   private <T extends BlockEntity> void removeGameEventListener(T p_223413_, ServerLevel p_223414_) {
      Block block = p_223413_.getBlockState().getBlock();
      if (block instanceof EntityBlock) {
         GameEventListener gameeventlistener = ((EntityBlock)block).getListener(p_223414_, p_223413_);
         if (gameeventlistener != null) {
            int i = SectionPos.blockToSectionCoord(p_223413_.getBlockPos().getY());
            GameEventListenerRegistry gameeventlistenerregistry = this.getListenerRegistry(i);
            gameeventlistenerregistry.unregister(gameeventlistener);
         }
      }
   }

   private void removeGameEventListenerRegistry(int p_283355_) {
      this.gameEventListenerRegistrySections.remove(p_283355_);
   }

   private void removeBlockEntityTicker(BlockPos p_156413_) {
      LevelChunk.RebindableTickingBlockEntityWrapper levelchunk$rebindabletickingblockentitywrapper = this.tickersInLevel.remove(p_156413_);
      if (levelchunk$rebindabletickingblockentitywrapper != null) {
         levelchunk$rebindabletickingblockentitywrapper.rebind(NULL_TICKER);
      }
   }

   public void runPostLoad() {
      if (this.postLoad != null) {
         this.postLoad.run(this);
         this.postLoad = null;
      }
   }

   public boolean isEmpty() {
      return false;
   }

   public void replaceWithPacketData(
      FriendlyByteBuf p_187972_, Map<Heightmap.Types, long[]> p_394716_, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> p_187974_
   ) {
      this.clearAllBlockEntities();

      for (LevelChunkSection levelchunksection : this.sections) {
         levelchunksection.read(p_187972_);
      }

      p_394716_.forEach(this::setHeightmap);
      this.initializeLightSources();

      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
         p_187974_.accept(
            (p_405747_, p_405748_, p_405749_) -> {
               BlockEntity blockentity = this.getBlockEntity(p_405747_, LevelChunk.EntityCreationType.IMMEDIATE);
               if (blockentity != null && p_405749_ != null && blockentity.getType() == p_405748_) {
                  blockentity.loadWithComponents(
                     TagValueInput.create(problemreporter$scopedcollector.forChild(blockentity.problemPath()), this.level.registryAccess(), p_405749_)
                  );
               }
            }
         );
      }
   }

   public void replaceBiomes(FriendlyByteBuf p_275574_) {
      for (LevelChunkSection levelchunksection : this.sections) {
         levelchunksection.readBiomes(p_275574_);
      }
   }

   public void setLoaded(boolean p_62914_) {
      this.loaded = p_62914_;
   }

   public Level getLevel() {
      return this.level;
   }

   public Map<BlockPos, BlockEntity> getBlockEntities() {
      return this.blockEntities;
   }

   public void postProcessGeneration(ServerLevel p_364672_) {
      ChunkPos chunkpos = this.getPos();

      for (int i = 0; i < this.postProcessing.length; i++) {
         ShortList shortlist = this.postProcessing[i];
         if (shortlist != null) {
            ShortListIterator var5 = shortlist.iterator();

            while (var5.hasNext()) {
               Short oshort = (Short)var5.next();
               BlockPos blockpos = ProtoChunk.unpackOffsetCoordinates(oshort, this.getSectionYFromSectionIndex(i), chunkpos);
               BlockState blockstate = this.getBlockState(blockpos);
               FluidState fluidstate = blockstate.getFluidState();
               if (!fluidstate.isEmpty()) {
                  fluidstate.tick(p_364672_, blockpos, blockstate);
               }

               if (!(blockstate.getBlock() instanceof LiquidBlock)) {
                  BlockState blockstate1 = Block.updateFromNeighbourShapes(blockstate, p_364672_, blockpos);
                  if (blockstate1 != blockstate) {
                     p_364672_.setBlock(blockpos, blockstate1, 276);
                  }
               }
            }

            shortlist.clear();
         }
      }

      UnmodifiableIterator var11 = ImmutableList.copyOf(this.pendingBlockEntities.keySet()).iterator();

      while (var11.hasNext()) {
         BlockPos blockpos1 = (BlockPos)var11.next();
         this.getBlockEntity(blockpos1);
      }

      this.pendingBlockEntities.clear();
      this.upgradeData.upgrade(this);
   }

   private @Nullable BlockEntity promotePendingBlockEntity(BlockPos p_62871_, CompoundTag p_62872_) {
      BlockState blockstate = this.getBlockState(p_62871_);
      BlockEntity blockentity;
      if ("DUMMY".equals(p_62872_.getStringOr("id", ""))) {
         if (blockstate.hasBlockEntity()) {
            blockentity = ((EntityBlock)blockstate.getBlock()).newBlockEntity(p_62871_, blockstate);
         } else {
            blockentity = null;
            LOGGER.warn("Tried to load a DUMMY block entity @ {} but found not block entity block {} at location", p_62871_, blockstate);
         }
      } else {
         blockentity = BlockEntity.loadStatic(p_62871_, blockstate, p_62872_, this.level.registryAccess());
      }

      if (blockentity != null) {
         blockentity.setLevel(this.level);
         this.addAndRegisterBlockEntity(blockentity);
      } else {
         LOGGER.warn("Tried to load a block entity for block {} but failed at location {}", blockstate, p_62871_);
      }

      return blockentity;
   }

   public void unpackTicks(long p_187986_) {
      this.blockTicks.unpack(p_187986_);
      this.fluidTicks.unpack(p_187986_);
   }

   public void registerTickContainerInLevel(ServerLevel p_187959_) {
      p_187959_.getBlockTicks().addContainer(this.chunkPos, this.blockTicks);
      p_187959_.getFluidTicks().addContainer(this.chunkPos, this.fluidTicks);
   }

   public void unregisterTickContainerFromLevel(ServerLevel p_187980_) {
      p_187980_.getBlockTicks().removeContainer(this.chunkPos);
      p_187980_.getFluidTicks().removeContainer(this.chunkPos);
   }

   @Override
   public void registerDebugValues(ServerLevel p_426361_, DebugValueSource.Registration p_423737_) {
      if (!this.getAllStarts().isEmpty()) {
         p_423737_.register(DebugSubscriptions.STRUCTURES, () -> {
            List<DebugStructureInfo> list = new ArrayList<>();

            for (StructureStart structurestart : this.getAllStarts().values()) {
               BoundingBox boundingbox = structurestart.getBoundingBox();
               List<StructurePiece> list1 = structurestart.getPieces();
               List<DebugStructureInfo.Piece> list2 = new ArrayList<>(list1.size());

               for (int i = 0; i < list1.size(); i++) {
                  boolean flag = i == 0;
                  list2.add(new DebugStructureInfo.Piece(list1.get(i).getBoundingBox(), flag));
               }

               list.add(new DebugStructureInfo(boundingbox, list2));
            }

            return list;
         });
      }

      p_423737_.register(DebugSubscriptions.RAIDS, () -> p_426361_.getRaids().getRaidCentersInChunk(this.chunkPos));
   }

   @Override
   public ChunkStatus getPersistedStatus() {
      return ChunkStatus.FULL;
   }

   public FullChunkStatus getFullStatus() {
      return this.fullStatus == null ? FullChunkStatus.FULL : this.fullStatus.get();
   }

   public void setFullStatus(Supplier<FullChunkStatus> p_62880_) {
      this.fullStatus = p_62880_;
   }

   public void clearAllBlockEntities() {
      this.blockEntities.values().forEach(BlockEntity::setRemoved);
      this.blockEntities.clear();
      this.tickersInLevel.values().forEach(p_187966_ -> p_187966_.rebind(NULL_TICKER));
      this.tickersInLevel.clear();
   }

   public void registerAllBlockEntitiesAfterLevelLoad() {
      this.blockEntities.values().forEach(p_405750_ -> {
         if (this.level instanceof ServerLevel serverlevel) {
            this.addGameEventListener(p_405750_, serverlevel);
         }

         this.level.onBlockEntityAdded(p_405750_);
         this.updateBlockEntityTicker(p_405750_);
      });
   }

   private <T extends BlockEntity> void addGameEventListener(T p_223416_, ServerLevel p_223417_) {
      Block block = p_223416_.getBlockState().getBlock();
      if (block instanceof EntityBlock) {
         GameEventListener gameeventlistener = ((EntityBlock)block).getListener(p_223417_, p_223416_);
         if (gameeventlistener != null) {
            this.getListenerRegistry(SectionPos.blockToSectionCoord(p_223416_.getBlockPos().getY())).register(gameeventlistener);
         }
      }
   }

   private <T extends BlockEntity> void updateBlockEntityTicker(T p_156407_) {
      BlockState blockstate = p_156407_.getBlockState();
      BlockEntityTicker<T> blockentityticker = blockstate.getTicker(this.level, (BlockEntityType<T>)p_156407_.getType());
      if (blockentityticker == null) {
         this.removeBlockEntityTicker(p_156407_.getBlockPos());
      } else {
         this.tickersInLevel
            .compute(
               p_156407_.getBlockPos(),
               (p_375350_, p_375351_) -> {
                  TickingBlockEntity tickingblockentity = this.createTicker(p_156407_, blockentityticker);
                  if (p_375351_ != null) {
                     p_375351_.rebind(tickingblockentity);
                     return (LevelChunk.RebindableTickingBlockEntityWrapper)p_375351_;
                  } else if (this.isInLevel()) {
                     LevelChunk.RebindableTickingBlockEntityWrapper levelchunk$rebindabletickingblockentitywrapper = new LevelChunk.RebindableTickingBlockEntityWrapper(
                        tickingblockentity
                     );
                     this.level.addBlockEntityTicker(levelchunk$rebindabletickingblockentitywrapper);
                     return levelchunk$rebindabletickingblockentitywrapper;
                  } else {
                     return null;
                  }
               }
            );
      }
   }

   private <T extends BlockEntity> TickingBlockEntity createTicker(T p_156376_, BlockEntityTicker<T> p_156377_) {
      return new LevelChunk.BoundTickingBlockEntity<>(p_156376_, p_156377_);
   }

   class BoundTickingBlockEntity<T extends BlockEntity> implements TickingBlockEntity {
      private final T blockEntity;
      private final BlockEntityTicker<T> ticker;
      private boolean loggedInvalidBlockState;

      BoundTickingBlockEntity(final T p_156433_, final BlockEntityTicker<T> p_156434_) {
         this.blockEntity = p_156433_;
         this.ticker = p_156434_;
      }

      @Override
      public void tick() {
         if (!this.blockEntity.isRemoved() && this.blockEntity.hasLevel()) {
            BlockPos blockpos = this.blockEntity.getBlockPos();
            if (LevelChunk.this.isTicking(blockpos)) {
               try {
                  ProfilerFiller profilerfiller = Profiler.get();
                  profilerfiller.push(this::getType);
                  BlockState blockstate = LevelChunk.this.getBlockState(blockpos);
                  if (this.blockEntity.getType().isValid(blockstate)) {
                     this.ticker.tick(LevelChunk.this.level, this.blockEntity.getBlockPos(), blockstate, this.blockEntity);
                     this.loggedInvalidBlockState = false;
                  } else if (!this.loggedInvalidBlockState) {
                     this.loggedInvalidBlockState = true;
                     LevelChunk.LOGGER
                        .warn(
                           "Block entity {} @ {} state {} invalid for ticking:",
                           new Object[]{LogUtils.defer(this::getType), LogUtils.defer(this::getPos), blockstate}
                        );
                  }

                  profilerfiller.pop();
               } catch (Throwable throwable) {
                  CrashReport crashreport = CrashReport.forThrowable(throwable, "Ticking block entity");
                  CrashReportCategory crashreportcategory = crashreport.addCategory("Block entity being ticked");
                  this.blockEntity.fillCrashReportCategory(crashreportcategory);
                  throw new ReportedException(crashreport);
               }
            }
         }
      }

      @Override
      public boolean isRemoved() {
         return this.blockEntity.isRemoved();
      }

      @Override
      public BlockPos getPos() {
         return this.blockEntity.getBlockPos();
      }

      @Override
      public String getType() {
         return BlockEntityType.getKey(this.blockEntity.getType()).toString();
      }

      @Override
      public String toString() {
         return "Level ticker for " + this.getType() + "@" + this.getPos();
      }
   }

   public enum EntityCreationType {
      IMMEDIATE,
      QUEUED,
      CHECK;
   }

   @FunctionalInterface
   public interface PostLoadProcessor {
      void run(LevelChunk var1);
   }

   static class RebindableTickingBlockEntityWrapper implements TickingBlockEntity {
      private TickingBlockEntity ticker;

      RebindableTickingBlockEntityWrapper(TickingBlockEntity p_156447_) {
         this.ticker = p_156447_;
      }

      void rebind(TickingBlockEntity p_156450_) {
         this.ticker = p_156450_;
      }

      @Override
      public void tick() {
         this.ticker.tick();
      }

      @Override
      public boolean isRemoved() {
         return this.ticker.isRemoved();
      }

      @Override
      public BlockPos getPos() {
         return this.ticker.getPos();
      }

      @Override
      public String getType() {
         return this.ticker.getType();
      }

      @Override
      public String toString() {
         return this.ticker + " <wrapped>";
      }
   }

   @FunctionalInterface
   public interface UnsavedListener {
      void setUnsaved(ChunkPos var1);
   }
}
