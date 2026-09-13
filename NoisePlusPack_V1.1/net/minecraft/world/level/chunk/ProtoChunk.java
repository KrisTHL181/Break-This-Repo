package net.minecraft.world.level.chunk;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import net.minecraft.world.ticks.TickContainerAccess;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ProtoChunk extends ChunkAccess {
   private static final Logger LOGGER = LogUtils.getLogger();
   private volatile @Nullable LevelLightEngine lightEngine;
   private volatile ChunkStatus status = ChunkStatus.EMPTY;
   private final List<CompoundTag> entities = Lists.newArrayList();
   private @Nullable CarvingMask carvingMask;
   private @Nullable BelowZeroRetrogen belowZeroRetrogen;
   private final ProtoChunkTicks<Block> blockTicks;
   private final ProtoChunkTicks<Fluid> fluidTicks;

   public ProtoChunk(
      ChunkPos p_188167_, UpgradeData p_188168_, LevelHeightAccessor p_188169_, PalettedContainerFactory p_424311_, @Nullable BlendingData p_188171_
   ) {
      this(p_188167_, p_188168_, null, new ProtoChunkTicks<>(), new ProtoChunkTicks<>(), p_188169_, p_424311_, p_188171_);
   }

   public ProtoChunk(
      ChunkPos p_188173_,
      UpgradeData p_188174_,
      LevelChunkSection @Nullable [] p_188175_,
      ProtoChunkTicks<Block> p_188176_,
      ProtoChunkTicks<Fluid> p_188177_,
      LevelHeightAccessor p_188178_,
      PalettedContainerFactory p_426825_,
      @Nullable BlendingData p_188180_
   ) {
      super(p_188173_, p_188174_, p_188178_, p_426825_, 0L, p_188175_, p_188180_);
      this.blockTicks = p_188176_;
      this.fluidTicks = p_188177_;
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
   public ChunkAccess.PackedTicks getTicksForSerialization(long p_361508_) {
      return new ChunkAccess.PackedTicks(this.blockTicks.pack(p_361508_), this.fluidTicks.pack(p_361508_));
   }

   @Override
   public BlockState getBlockState(BlockPos p_63264_) {
      int i = p_63264_.getY();
      if (this.isOutsideBuildHeight(i)) {
         return Blocks.VOID_AIR.defaultBlockState();
      }

      LevelChunkSection levelchunksection = this.getSection(this.getSectionIndex(i));
      return levelchunksection.hasOnlyAir()
         ? Blocks.AIR.defaultBlockState()
         : levelchunksection.getBlockState(p_63264_.getX() & 15, i & 15, p_63264_.getZ() & 15);
   }

   @Override
   public FluidState getFluidState(BlockPos p_63239_) {
      int i = p_63239_.getY();
      if (this.isOutsideBuildHeight(i)) {
         return Fluids.EMPTY.defaultFluidState();
      }

      LevelChunkSection levelchunksection = this.getSection(this.getSectionIndex(i));
      return levelchunksection.hasOnlyAir()
         ? Fluids.EMPTY.defaultFluidState()
         : levelchunksection.getFluidState(p_63239_.getX() & 15, i & 15, p_63239_.getZ() & 15);
   }

   @Override
   public @Nullable BlockState setBlockState(BlockPos p_63217_, BlockState p_63218_, @Block.UpdateFlags int p_394843_) {
      int i = p_63217_.getX();
      int j = p_63217_.getY();
      int k = p_63217_.getZ();
      if (this.isOutsideBuildHeight(j)) {
         return Blocks.VOID_AIR.defaultBlockState();
      }

      int l = this.getSectionIndex(j);
      LevelChunkSection levelchunksection = this.getSection(l);
      boolean flag = levelchunksection.hasOnlyAir();
      if (flag && p_63218_.is(Blocks.AIR)) {
         return p_63218_;
      }

      int i1 = SectionPos.sectionRelative(i);
      int j1 = SectionPos.sectionRelative(j);
      int k1 = SectionPos.sectionRelative(k);
      BlockState blockstate = levelchunksection.setBlockState(i1, j1, k1, p_63218_);
      if (this.status.isOrAfter(ChunkStatus.INITIALIZE_LIGHT)) {
         boolean flag1 = levelchunksection.hasOnlyAir();
         if (flag1 != flag) {
            this.lightEngine.updateSectionStatus(p_63217_, flag1);
         }

         if (LightEngine.hasDifferentLightProperties(blockstate, p_63218_)) {
            this.skyLightSources.update(this, i1, j, k1);
            this.lightEngine.checkBlock(p_63217_);
         }
      }

      EnumSet<Heightmap.Types> enumset1 = this.getPersistedStatus().heightmapsAfter();
      EnumSet<Heightmap.Types> enumset = null;

      for (Heightmap.Types heightmap$types : enumset1) {
         Heightmap heightmap = this.heightmaps.get(heightmap$types);
         if (heightmap == null) {
            if (enumset == null) {
               enumset = EnumSet.noneOf(Heightmap.Types.class);
            }

            enumset.add(heightmap$types);
         }
      }

      if (enumset != null) {
         Heightmap.primeHeightmaps(this, enumset);
      }

      for (Heightmap.Types heightmap$types1 : enumset1) {
         this.heightmaps.get(heightmap$types1).update(i1, j, k1, p_63218_);
      }

      return blockstate;
   }

   @Override
   public void setBlockEntity(BlockEntity p_156488_) {
      this.pendingBlockEntities.remove(p_156488_.getBlockPos());
      this.blockEntities.put(p_156488_.getBlockPos(), p_156488_);
   }

   @Override
   public @Nullable BlockEntity getBlockEntity(BlockPos p_63257_) {
      return this.blockEntities.get(p_63257_);
   }

   public Map<BlockPos, BlockEntity> getBlockEntities() {
      return this.blockEntities;
   }

   public void addEntity(CompoundTag p_63243_) {
      this.entities.add(p_63243_);
   }

   @Override
   public void addEntity(Entity p_63183_) {
      if (!p_63183_.isPassenger()) {
         try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(p_63183_.problemPath(), LOGGER)) {
            TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_63183_.registryAccess());
            p_63183_.save(tagvalueoutput);
            this.addEntity(tagvalueoutput.buildResult());
         }
      }
   }

   @Override
   public void setStartForStructure(Structure p_223432_, StructureStart p_223433_) {
      BelowZeroRetrogen belowzeroretrogen = this.getBelowZeroRetrogen();
      if (belowzeroretrogen != null && p_223433_.isValid()) {
         BoundingBox boundingbox = p_223433_.getBoundingBox();
         LevelHeightAccessor levelheightaccessor = this.getHeightAccessorForGeneration();
         if (boundingbox.minY() < levelheightaccessor.getMinY() || boundingbox.maxY() > levelheightaccessor.getMaxY()) {
            return;
         }
      }

      super.setStartForStructure(p_223432_, p_223433_);
   }

   public List<CompoundTag> getEntities() {
      return this.entities;
   }

   @Override
   public ChunkStatus getPersistedStatus() {
      return this.status;
   }

   public void setPersistedStatus(ChunkStatus p_334912_) {
      this.status = p_334912_;
      if (this.belowZeroRetrogen != null && p_334912_.isOrAfter(this.belowZeroRetrogen.targetStatus())) {
         this.setBelowZeroRetrogen(null);
      }

      this.markUnsaved();
   }

   @Override
   public Holder<Biome> getNoiseBiome(int p_204450_, int p_204451_, int p_204452_) {
      if (this.getHighestGeneratedStatus().isOrAfter(ChunkStatus.BIOMES)) {
         return super.getNoiseBiome(p_204450_, p_204451_, p_204452_);
      } else {
         throw new IllegalStateException("Asking for biomes before we have biomes");
      }
   }

   public static short packOffsetCoordinates(BlockPos p_63281_) {
      int i = p_63281_.getX();
      int j = p_63281_.getY();
      int k = p_63281_.getZ();
      int l = i & 15;
      int i1 = j & 15;
      int j1 = k & 15;
      return (short)(l | i1 << 4 | j1 << 8);
   }

   public static BlockPos unpackOffsetCoordinates(short p_63228_, int p_63229_, ChunkPos p_63230_) {
      int i = SectionPos.sectionToBlockCoord(p_63230_.x, p_63228_ & 15);
      int j = SectionPos.sectionToBlockCoord(p_63229_, p_63228_ >>> 4 & 15);
      int k = SectionPos.sectionToBlockCoord(p_63230_.z, p_63228_ >>> 8 & 15);
      return new BlockPos(i, j, k);
   }

   @Override
   public void markPosForPostprocessing(BlockPos p_63266_) {
      if (!this.isOutsideBuildHeight(p_63266_)) {
         ChunkAccess.getOrCreateOffsetList(this.postProcessing, this.getSectionIndex(p_63266_.getY())).add(packOffsetCoordinates(p_63266_));
      }
   }

   @Override
   public void addPackedPostProcess(ShortList p_362697_, int p_63226_) {
      ChunkAccess.getOrCreateOffsetList(this.postProcessing, p_63226_).addAll(p_362697_);
   }

   public Map<BlockPos, CompoundTag> getBlockEntityNbts() {
      return Collections.unmodifiableMap(this.pendingBlockEntities);
   }

   @Override
   public @Nullable CompoundTag getBlockEntityNbtForSaving(BlockPos p_63275_, HolderLookup.Provider p_335105_) {
      BlockEntity blockentity = this.getBlockEntity(p_63275_);
      return blockentity != null ? blockentity.saveWithFullMetadata(p_335105_) : this.pendingBlockEntities.get(p_63275_);
   }

   @Override
   public void removeBlockEntity(BlockPos p_63262_) {
      this.blockEntities.remove(p_63262_);
      this.pendingBlockEntities.remove(p_63262_);
   }

   public @Nullable CarvingMask getCarvingMask() {
      return this.carvingMask;
   }

   public CarvingMask getOrCreateCarvingMask() {
      if (this.carvingMask == null) {
         this.carvingMask = new CarvingMask(this.getHeight(), this.getMinY());
      }

      return this.carvingMask;
   }

   public void setCarvingMask(CarvingMask p_188188_) {
      this.carvingMask = p_188188_;
   }

   public void setLightEngine(LevelLightEngine p_63210_) {
      this.lightEngine = p_63210_;
   }

   public void setBelowZeroRetrogen(@Nullable BelowZeroRetrogen p_188184_) {
      this.belowZeroRetrogen = p_188184_;
   }

   @Override
   public @Nullable BelowZeroRetrogen getBelowZeroRetrogen() {
      return this.belowZeroRetrogen;
   }

   private static <T> LevelChunkTicks<T> unpackTicks(ProtoChunkTicks<T> p_188190_) {
      return new LevelChunkTicks<>(p_188190_.scheduledTicks());
   }

   public LevelChunkTicks<Block> unpackBlockTicks() {
      return unpackTicks(this.blockTicks);
   }

   public LevelChunkTicks<Fluid> unpackFluidTicks() {
      return unpackTicks(this.fluidTicks);
   }

   @Override
   public LevelHeightAccessor getHeightAccessorForGeneration() {
      return this.isUpgrading() ? BelowZeroRetrogen.UPGRADE_HEIGHT_ACCESSOR : this;
   }
}
