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
   private static final Logger LOGGER = LogUtils.getLogger();
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
   private final Level level;
   private @Nullable Supplier<FullChunkStatus> fullStatus;
   private LevelChunk.@Nullable PostLoadProcessor postLoad;
   private final Int2ObjectMap<GameEventListenerRegistry> gameEventListenerRegistrySections;
   private final LevelChunkTicks<Block> blockTicks;
   private final LevelChunkTicks<Fluid> fluidTicks;
   private LevelChunk.UnsavedListener unsavedListener = chunkPos -> {};

   public LevelChunk(final Level level, final ChunkPos pos) {
      this(level, pos, UpgradeData.EMPTY, new LevelChunkTicks<>(), new LevelChunkTicks<>(), 0L, null, null, null);
   }

   public LevelChunk(
      final Level level,
      final ChunkPos pos,
      final UpgradeData upgradeData,
      final LevelChunkTicks<Block> blockTicks,
      final LevelChunkTicks<Fluid> fluidTicks,
      final long inhabitedTime,
      final LevelChunkSection @Nullable [] sections,
      final LevelChunk.@Nullable PostLoadProcessor postLoad,
      final @Nullable BlendingData blendingData
   ) {
      super(pos, upgradeData, level, level.palettedContainerFactory(), inhabitedTime, sections, blendingData);
      this.level = level;
      this.gameEventListenerRegistrySections = new Int2ObjectOpenHashMap();

      for (Heightmap.Types type : Heightmap.Types.values()) {
         if (ChunkStatus.FULL.heightmapsAfter().contains(type)) {
            this.heightmaps.put(type, new Heightmap(this, type));
         }
      }

      this.postLoad = postLoad;
      this.blockTicks = blockTicks;
      this.fluidTicks = fluidTicks;
   }

   public LevelChunk(final ServerLevel level, final ProtoChunk protoChunk, final LevelChunk.@Nullable PostLoadProcessor postLoad) {
      this(
         level,
         protoChunk.getPos(),
         protoChunk.getUpgradeData(),
         protoChunk.unpackBlockTicks(),
         protoChunk.unpackFluidTicks(),
         protoChunk.getInhabitedTime(),
         protoChunk.getSections(),
         postLoad,
         protoChunk.getBlendingData()
      );
      if (!Collections.disjoint(protoChunk.pendingBlockEntities.keySet(), protoChunk.blockEntities.keySet())) {
         LOGGER.error("Chunk at {} contains duplicated block entities", protoChunk.getPos());
      }

      for (BlockEntity blockEntity : protoChunk.getBlockEntities().values()) {
         this.setBlockEntity(blockEntity);
      }

      this.pendingBlockEntities.putAll(protoChunk.getBlockEntityNbts());

      for (int i = 0; i < protoChunk.getPostProcessing().length; i++) {
         this.postProcessing[i] = protoChunk.getPostProcessing()[i];
      }

      this.setAllStarts(protoChunk.getAllStarts());
      this.setAllReferences(protoChunk.getAllReferences());

      for (Entry<Heightmap.Types, Heightmap> entry : protoChunk.getHeightmaps()) {
         if (ChunkStatus.FULL.heightmapsAfter().contains(entry.getKey())) {
            this.setHeightmap(entry.getKey(), entry.getValue().getRawData());
         }
      }

      this.skyLightSources = protoChunk.skyLightSources;
      this.setLightCorrect(protoChunk.isLightCorrect());
      this.markUnsaved();
   }

   public void setUnsavedListener(final LevelChunk.UnsavedListener unsavedListener) {
      this.unsavedListener = unsavedListener;
      if (this.isUnsaved()) {
         unsavedListener.setUnsaved(this.chunkPos);
      }
   }

   @Override
   public void markUnsaved() {
      boolean wasUnsaved = this.isUnsaved();
      super.markUnsaved();
      if (!wasUnsaved) {
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
   public ChunkAccess.PackedTicks getTicksForSerialization(final long currentTick) {
      return new ChunkAccess.PackedTicks(this.blockTicks.pack(currentTick), this.fluidTicks.pack(currentTick));
   }

   @Override
   public GameEventListenerRegistry getListenerRegistry(final int section) {
      return this.level instanceof ServerLevel serverLevel
         ? (GameEventListenerRegistry)this.gameEventListenerRegistrySections
            .computeIfAbsent(section, key -> new EuclideanGameEventListenerRegistry(serverLevel, section, this::removeGameEventListenerRegistry))
         : super.getListenerRegistry(section);
   }

   @Override
   public BlockState getBlockState(final BlockPos pos) {
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();
      if (this.level.isDebug()) {
         BlockState blockState = null;
         if (y == 60) {
            blockState = Blocks.BARRIER.defaultBlockState();
         }

         if (y == 70) {
            blockState = DebugLevelSource.getBlockStateFor(x, z);
         }

         return blockState == null ? Blocks.AIR.defaultBlockState() : blockState;
      } else {
         try {
            int sectionIndex = this.getSectionIndex(y);
            if (sectionIndex >= 0 && sectionIndex < this.sections.length) {
               LevelChunkSection currentSection = this.sections[sectionIndex];
               if (!currentSection.hasOnlyAir()) {
                  return currentSection.getBlockState(x & 15, y & 15, z & 15);
               }
            }

            return Blocks.AIR.defaultBlockState();
         } catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "Getting block state");
            CrashReportCategory category = report.addCategory("Block being got");
            category.setDetail("Location", () -> CrashReportCategory.formatLocation(this, x, y, z));
            throw new ReportedException(report);
         }
      }
   }

   @Override
   public FluidState getFluidState(final BlockPos pos) {
      return this.getFluidState(pos.getX(), pos.getY(), pos.getZ());
   }

   public FluidState getFluidState(final int x, final int y, final int z) {
      try {
         int sectionIndex = this.getSectionIndex(y);
         if (sectionIndex >= 0 && sectionIndex < this.sections.length) {
            LevelChunkSection currentSection = this.sections[sectionIndex];
            if (!currentSection.hasOnlyAir()) {
               return currentSection.getFluidState(x & 15, y & 15, z & 15);
            }
         }

         return Fluids.EMPTY.defaultFluidState();
      } catch (Throwable t) {
         CrashReport report = CrashReport.forThrowable(t, "Getting fluid state");
         CrashReportCategory category = report.addCategory("Block being got");
         category.setDetail("Location", () -> CrashReportCategory.formatLocation(this, x, y, z));
         throw new ReportedException(report);
      }
   }

   @Override
   public @Nullable BlockState setBlockState(final BlockPos pos, final BlockState state, final @Block.UpdateFlags int flags) {
      int y = pos.getY();
      LevelChunkSection section = this.getSection(this.getSectionIndex(y));
      boolean wasEmpty = section.hasOnlyAir();
      if (wasEmpty && state.isAir()) {
         return null;
      }

      int localX = pos.getX() & 15;
      int localY = y & 15;
      int localZ = pos.getZ() & 15;
      BlockState oldState = section.setBlockState(localX, localY, localZ, state);
      if (oldState == state) {
         return null;
      }

      Block newBlock = state.getBlock();
      this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING).update(localX, y, localZ, state);
      this.heightmaps.get(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES).update(localX, y, localZ, state);
      this.heightmaps.get(Heightmap.Types.OCEAN_FLOOR).update(localX, y, localZ, state);
      this.heightmaps.get(Heightmap.Types.WORLD_SURFACE).update(localX, y, localZ, state);
      boolean isEmpty = section.hasOnlyAir();
      if (wasEmpty != isEmpty) {
         this.level.getChunkSource().getLightEngine().updateSectionStatus(pos, isEmpty);
         this.level.getChunkSource().onSectionEmptinessChanged(this.chunkPos.x(), SectionPos.blockToSectionCoord(y), this.chunkPos.z(), isEmpty);
      }

      if (LightEngine.hasDifferentLightProperties(oldState, state)) {
         ProfilerFiller profiler = Profiler.get();
         profiler.push("updateSkyLightSources");
         this.skyLightSources.update(this, localX, y, localZ);
         profiler.popPush("queueCheckLight");
         this.level.getChunkSource().getLightEngine().checkBlock(pos);
         profiler.pop();
      }

      boolean blockChanged = !oldState.is(newBlock);
      boolean movedByPiston = (flags & 64) != 0;
      boolean sideEffects = (flags & 256) == 0;
      if (blockChanged && oldState.hasBlockEntity() && !state.shouldChangedStateKeepBlockEntity(oldState)) {
         if (!this.level.isClientSide() && sideEffects) {
            BlockEntity blockEntity = this.level.getBlockEntity(pos);
            if (blockEntity != null) {
               blockEntity.preRemoveSideEffects(pos, oldState);
            }
         }

         this.removeBlockEntity(pos);
      }

      if ((blockChanged || newBlock instanceof BaseRailBlock) && this.level instanceof ServerLevel serverLevel && ((flags & 1) != 0 || movedByPiston)) {
         oldState.affectNeighborsAfterRemoval(serverLevel, pos, movedByPiston);
      }

      if (!section.getBlockState(localX, localY, localZ).is(newBlock)) {
         return null;
      }

      if (!this.level.isClientSide() && (flags & 512) == 0) {
         state.onPlace(this.level, pos, oldState, movedByPiston);
      }

      if (state.hasBlockEntity() && section.getBlockState(localX, localY, localZ).is(newBlock)) {
         BlockEntity blockEntity = this.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
         if (blockEntity != null && !blockEntity.isValidBlockState(state)) {
            LOGGER.warn("Found mismatched block entity @ {}: type = {}, state = {}", new Object[]{pos, blockEntity.typeHolder().getRegisteredName(), state});
            this.removeBlockEntity(pos);
            blockEntity = null;
         }

         if (blockEntity == null) {
            blockEntity = ((EntityBlock)newBlock).newBlockEntity(pos, state);
            if (blockEntity != null) {
               this.addAndRegisterBlockEntity(blockEntity);
            }
         } else {
            blockEntity.setBlockState(state);
            this.updateBlockEntityTicker(blockEntity);
         }
      }

      this.markUnsaved();
      return oldState;
   }

   @Deprecated
   @Override
   public void addEntity(final Entity entity) {
   }

   private @Nullable BlockEntity createBlockEntity(final BlockPos pos) {
      BlockState state = this.getBlockState(pos);
      return !state.hasBlockEntity() ? null : ((EntityBlock)state.getBlock()).newBlockEntity(pos, state);
   }

   @Override
   public @Nullable BlockEntity getBlockEntity(final BlockPos pos) {
      return this.getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
   }

   public @Nullable BlockEntity getBlockEntity(final BlockPos pos, final LevelChunk.EntityCreationType creationType) {
      BlockEntity blockEntity = this.blockEntities.get(pos);
      if (blockEntity == null) {
         CompoundTag tag = this.pendingBlockEntities.remove(pos);
         if (tag != null) {
            BlockEntity promoted = this.promotePendingBlockEntity(pos, tag);
            if (promoted != null) {
               return promoted;
            }
         }
      }

      if (blockEntity == null) {
         if (creationType == LevelChunk.EntityCreationType.IMMEDIATE) {
            blockEntity = this.createBlockEntity(pos);
            if (blockEntity != null) {
               this.addAndRegisterBlockEntity(blockEntity);
            }
         }
      } else if (blockEntity.isRemoved()) {
         this.blockEntities.remove(pos);
         return null;
      }

      return blockEntity;
   }

   public void addAndRegisterBlockEntity(final BlockEntity blockEntity) {
      this.setBlockEntity(blockEntity);
      if (this.isInLevel()) {
         if (this.level instanceof ServerLevel serverLevel) {
            this.addGameEventListener(blockEntity, serverLevel);
         }

         this.level.onBlockEntityAdded(blockEntity);
         this.updateBlockEntityTicker(blockEntity);
      }
   }

   private boolean isInLevel() {
      return this.loaded || this.level.isClientSide();
   }

   private boolean isTicking(final BlockPos pos) {
      if (!this.level.getWorldBorder().isWithinBounds(pos)) {
         return false;
      } else {
         return !(this.level instanceof ServerLevel serverLevel)
            ? true
            : this.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING) && serverLevel.areEntitiesLoaded(ChunkPos.pack(pos));
      }
   }

   @Override
   public void setBlockEntity(final BlockEntity blockEntity) {
      BlockPos pos = blockEntity.getBlockPos();
      BlockState blockState = this.getBlockState(pos);
      if (!blockState.hasBlockEntity()) {
         LOGGER.warn("Trying to set block entity {} at position {}, but state {} does not allow it", new Object[]{blockEntity, pos, blockState});
      } else {
         BlockState cachedBlockState = blockEntity.getBlockState();
         if (blockState != cachedBlockState) {
            if (!blockEntity.getType().isValid(blockState)) {
               LOGGER.warn("Trying to set block entity {} at position {}, but state {} does not allow it", new Object[]{blockEntity, pos, blockState});
               return;
            }

            if (blockState.getBlock() != cachedBlockState.getBlock()) {
               LOGGER.warn(
                  "Block state mismatch on block entity {} in position {}, {} != {}, updating", new Object[]{blockEntity, pos, blockState, cachedBlockState}
               );
            }

            blockEntity.setBlockState(blockState);
         }

         blockEntity.setLevel(this.level);
         blockEntity.clearRemoved();
         BlockEntity previousEntry = this.blockEntities.put(pos.immutable(), blockEntity);
         if (previousEntry != null && previousEntry != blockEntity) {
            previousEntry.setRemoved();
         }
      }
   }

   @Override
   public @Nullable CompoundTag getBlockEntityNbtForSaving(final BlockPos blockPos, final HolderLookup.Provider registryAccess) {
      BlockEntity blockEntity = this.getBlockEntity(blockPos);
      if (blockEntity != null && !blockEntity.isRemoved()) {
         CompoundTag result = blockEntity.saveWithFullMetadata(this.level.registryAccess());
         result.putBoolean("keepPacked", false);
         return result;
      }

      CompoundTag result = this.pendingBlockEntities.get(blockPos);
      if (result != null) {
         result = result.copy();
         result.putBoolean("keepPacked", true);
      }

      return result;
   }

   @Override
   public void removeBlockEntity(final BlockPos pos) {
      if (this.isInLevel()) {
         BlockEntity removeThis = this.blockEntities.remove(pos);
         if (removeThis != null) {
            if (this.level instanceof ServerLevel serverLevel) {
               this.removeGameEventListener(removeThis, serverLevel);
               serverLevel.debugSynchronizers().dropBlockEntity(pos);
            }

            removeThis.setRemoved();
         }
      }

      this.removeBlockEntityTicker(pos);
   }

   private <T extends BlockEntity> void removeGameEventListener(final T blockEntity, final ServerLevel level) {
      if (blockEntity.getBlockState().getBlock() instanceof EntityBlock entityBlock) {
         GameEventListener listener = entityBlock.getListener(level, blockEntity);
         if (listener != null) {
            int section = SectionPos.blockToSectionCoord(blockEntity.getBlockPos().getY());
            GameEventListenerRegistry listenerRegistry = this.getListenerRegistry(section);
            listenerRegistry.unregister(listener);
         }
      }
   }

   private void removeGameEventListenerRegistry(final int sectionY) {
      this.gameEventListenerRegistrySections.remove(sectionY);
   }

   private void removeBlockEntityTicker(final BlockPos pos) {
      LevelChunk.RebindableTickingBlockEntityWrapper ticker = this.tickersInLevel.remove(pos);
      if (ticker != null) {
         ticker.rebind(NULL_TICKER);
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
      final FriendlyByteBuf buffer,
      final Map<Heightmap.Types, long[]> heightmaps,
      final Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> blockEntities
   ) {
      this.clearAllBlockEntities();

      for (LevelChunkSection section : this.sections) {
         section.read(buffer);
      }

      heightmaps.forEach(this::setHeightmap);
      this.initializeLightSources();

      try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
         blockEntities.accept((pos, type, tag) -> {
            BlockEntity blockEntity = this.getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
            if (blockEntity != null && tag != null && blockEntity.getType() == type) {
               blockEntity.loadWithComponents(TagValueInput.create(reporter.forChild(blockEntity.problemPath()), this.level.registryAccess(), tag));
            }
         });
      }
   }

   public void replaceBiomes(final FriendlyByteBuf buffer) {
      for (LevelChunkSection section : this.sections) {
         section.readBiomes(buffer);
      }
   }

   public void setLoaded(final boolean loaded) {
      this.loaded = loaded;
   }

   public Level getLevel() {
      return this.level;
   }

   public Map<BlockPos, BlockEntity> getBlockEntities() {
      return this.blockEntities;
   }

   public void postProcessGeneration(final ServerLevel level) {
      ChunkPos chunkPos = this.getPos();

      for (int sectionIndex = 0; sectionIndex < this.postProcessing.length; sectionIndex++) {
         ShortList postProcessingSection = this.postProcessing[sectionIndex];
         if (postProcessingSection != null) {
            ShortListIterator var5 = postProcessingSection.iterator();

            while (var5.hasNext()) {
               Short packedOffset = (Short)var5.next();
               BlockPos blockPos = ProtoChunk.unpackOffsetCoordinates(packedOffset, this.getSectionYFromSectionIndex(sectionIndex), chunkPos);
               BlockState blockState = this.getBlockState(blockPos);
               FluidState fluidState = blockState.getFluidState();
               if (!fluidState.isEmpty()) {
                  fluidState.tick(level, blockPos, blockState);
               }

               if (blockState.getBlock() instanceof LiquidBlock) {
                  blockState.tick(level, blockPos, level.getRandom());
               } else {
                  BlockState blockStateNew = Block.updateFromNeighbourShapes(blockState, level, blockPos);
                  if (blockStateNew != blockState) {
                     level.setBlock(blockPos, blockStateNew, 276);
                  }
               }
            }

            postProcessingSection.clear();
         }
      }

      UnmodifiableIterator var11 = ImmutableList.copyOf(this.pendingBlockEntities.keySet()).iterator();

      while (var11.hasNext()) {
         BlockPos pos = (BlockPos)var11.next();
         this.getBlockEntity(pos);
      }

      this.pendingBlockEntities.clear();
      this.upgradeData.upgrade(this);
   }

   private @Nullable BlockEntity promotePendingBlockEntity(final BlockPos pos, final CompoundTag tag) {
      BlockState state = this.getBlockState(pos);
      BlockEntity blockEntity;
      if ("DUMMY".equals(tag.getStringOr("id", ""))) {
         if (state.hasBlockEntity()) {
            blockEntity = ((EntityBlock)state.getBlock()).newBlockEntity(pos, state);
         } else {
            blockEntity = null;
            LOGGER.warn("Tried to load a DUMMY block entity @ {} but found not block entity block {} at location", pos, state);
         }
      } else {
         blockEntity = BlockEntity.loadStatic(pos, state, tag, this.level.registryAccess());
      }

      if (blockEntity != null) {
         blockEntity.setLevel(this.level);
         this.addAndRegisterBlockEntity(blockEntity);
      } else {
         LOGGER.warn("Tried to load a block entity for block {} but failed at location {}", state, pos);
      }

      return blockEntity;
   }

   public void unpackTicks(final long currentTick) {
      this.blockTicks.unpack(currentTick);
      this.fluidTicks.unpack(currentTick);
   }

   public void registerTickContainerInLevel(final ServerLevel level) {
      level.getBlockTicks().addContainer(this.chunkPos, this.blockTicks);
      level.getFluidTicks().addContainer(this.chunkPos, this.fluidTicks);
   }

   public void unregisterTickContainerFromLevel(final ServerLevel level) {
      level.getBlockTicks().removeContainer(this.chunkPos);
      level.getFluidTicks().removeContainer(this.chunkPos);
   }

   @Override
   public void registerDebugValues(final ServerLevel level, final DebugValueSource.Registration registration) {
      if (!this.getAllStarts().isEmpty()) {
         registration.register(DebugSubscriptions.STRUCTURES, () -> {
            List<DebugStructureInfo> structures = new ArrayList<>();

            for (StructureStart start : this.getAllStarts().values()) {
               BoundingBox boundingBox = start.getBoundingBox();
               List<StructurePiece> pieces = start.getPieces();
               List<DebugStructureInfo.Piece> pieceInfos = new ArrayList<>(pieces.size());

               for (int i = 0; i < pieces.size(); i++) {
                  boolean isStart = i == 0;
                  pieceInfos.add(new DebugStructureInfo.Piece(pieces.get(i).getBoundingBox(), isStart));
               }

               structures.add(new DebugStructureInfo(boundingBox, pieceInfos));
            }

            return structures;
         });
      }

      registration.register(DebugSubscriptions.RAIDS, () -> level.getRaids().getRaidCentersInChunk(this.chunkPos));
   }

   @Override
   public ChunkStatus getPersistedStatus() {
      return ChunkStatus.FULL;
   }

   public FullChunkStatus getFullStatus() {
      return this.fullStatus == null ? FullChunkStatus.FULL : this.fullStatus.get();
   }

   public void setFullStatus(final Supplier<FullChunkStatus> fullStatus) {
      this.fullStatus = fullStatus;
   }

   public void clearAllBlockEntities() {
      this.blockEntities.values().forEach(BlockEntity::setRemoved);
      this.blockEntities.clear();
      this.tickersInLevel.values().forEach(ticker -> ticker.rebind(NULL_TICKER));
      this.tickersInLevel.clear();
   }

   public void registerAllBlockEntitiesAfterLevelLoad() {
      this.blockEntities.values().forEach(blockEntity -> {
         if (this.level instanceof ServerLevel serverLevel) {
            this.addGameEventListener(blockEntity, serverLevel);
         }

         this.level.onBlockEntityAdded(blockEntity);
         this.updateBlockEntityTicker(blockEntity);
      });
   }

   private <T extends BlockEntity> void addGameEventListener(final T blockEntity, final ServerLevel level) {
      if (blockEntity.getBlockState().getBlock() instanceof EntityBlock entityBlock) {
         GameEventListener listener = entityBlock.getListener(level, blockEntity);
         if (listener != null) {
            this.getListenerRegistry(SectionPos.blockToSectionCoord(blockEntity.getBlockPos().getY())).register(listener);
         }
      }
   }

   private <T extends BlockEntity> void updateBlockEntityTicker(final T blockEntity) {
      BlockState state = blockEntity.getBlockState();
      BlockEntityTicker<T> ticker = state.getTicker(this.level, (BlockEntityType<T>)blockEntity.getType());
      if (ticker == null) {
         this.removeBlockEntityTicker(blockEntity.getBlockPos());
      } else {
         this.tickersInLevel.compute(blockEntity.getBlockPos(), (blockPos, existingTicker) -> {
            TickingBlockEntity actualTicker = this.createTicker(blockEntity, ticker);
            if (existingTicker != null) {
               existingTicker.rebind(actualTicker);
               return (LevelChunk.RebindableTickingBlockEntityWrapper)existingTicker;
            } else if (this.isInLevel()) {
               LevelChunk.RebindableTickingBlockEntityWrapper result = new LevelChunk.RebindableTickingBlockEntityWrapper(actualTicker);
               this.level.addBlockEntityTicker(result);
               return result;
            } else {
               return null;
            }
         });
      }
   }

   private <T extends BlockEntity> TickingBlockEntity createTicker(final T blockEntity, final BlockEntityTicker<T> ticker) {
      return new LevelChunk.BoundTickingBlockEntity<>(blockEntity, ticker);
   }

   private class BoundTickingBlockEntity<T extends BlockEntity> implements TickingBlockEntity {
      private final T blockEntity;
      private final BlockEntityTicker<T> ticker;
      private boolean loggedInvalidBlockState;

      private BoundTickingBlockEntity(final T blockEntity, final BlockEntityTicker<T> ticker) {
         this.blockEntity = blockEntity;
         this.ticker = ticker;
      }

      @Override
      public void tick() {
         if (!this.blockEntity.isRemoved() && this.blockEntity.hasLevel()) {
            BlockPos pos = this.blockEntity.getBlockPos();
            if (LevelChunk.this.isTicking(pos)) {
               try {
                  ProfilerFiller profiler = Profiler.get();
                  profiler.push(this::getType);
                  BlockState blockState = LevelChunk.this.getBlockState(pos);
                  if (this.blockEntity.getType().isValid(blockState)) {
                     this.ticker.tick(LevelChunk.this.level, this.blockEntity.getBlockPos(), blockState, this.blockEntity);
                     this.loggedInvalidBlockState = false;
                  } else if (!this.loggedInvalidBlockState) {
                     this.loggedInvalidBlockState = true;
                     LevelChunk.LOGGER
                        .warn(
                           "Block entity {} @ {} state {} invalid for ticking:",
                           new Object[]{LogUtils.defer(this::getType), LogUtils.defer(this::getPos), blockState}
                        );
                  }

                  profiler.pop();
               } catch (Throwable t) {
                  CrashReport report = CrashReport.forThrowable(t, "Ticking block entity");
                  CrashReportCategory category = report.addCategory("Block entity being ticked");
                  this.blockEntity.fillCrashReportCategory(category);
                  throw new ReportedException(report);
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
         return this.blockEntity.typeHolder().getRegisteredName();
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
      void run(LevelChunk levelChunk);
   }

   private static class RebindableTickingBlockEntityWrapper implements TickingBlockEntity {
      private TickingBlockEntity ticker;

      private RebindableTickingBlockEntityWrapper(final TickingBlockEntity ticker) {
         this.ticker = ticker;
      }

      private void rebind(final TickingBlockEntity ticker) {
         this.ticker = ticker;
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
      void setUnsaved(ChunkPos chunkPos);
   }
}
