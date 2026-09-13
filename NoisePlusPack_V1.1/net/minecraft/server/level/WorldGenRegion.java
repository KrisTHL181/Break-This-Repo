package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StaticCache2D;
import net.minecraft.util.Util;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.attribute.EnvironmentAttributeReader;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.WorldGenTickAccess;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class WorldGenRegion implements WorldGenLevel {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final StaticCache2D<GenerationChunkHolder> cache;
   private final ChunkAccess center;
   private final ServerLevel level;
   private final long seed;
   private final LevelData levelData;
   private final RandomSource random;
   private final DimensionType dimensionType;
   private final WorldGenTickAccess<Block> blockTicks = new WorldGenTickAccess<>(p_449156_ -> this.getChunk(p_449156_).getBlockTicks());
   private final WorldGenTickAccess<Fluid> fluidTicks = new WorldGenTickAccess<>(p_449157_ -> this.getChunk(p_449157_).getFluidTicks());
   private final BiomeManager biomeManager;
   private final ChunkStep generatingStep;
   private @Nullable Supplier<String> currentlyGenerating;
   private final AtomicLong subTickCount = new AtomicLong();
   private static final Identifier WORLDGEN_REGION_RANDOM = Identifier.withDefaultNamespace("worldgen_region_random");

   public WorldGenRegion(ServerLevel p_143484_, StaticCache2D<GenerationChunkHolder> p_345015_, ChunkStep p_344631_, ChunkAccess p_342729_) {
      this.generatingStep = p_344631_;
      this.cache = p_345015_;
      this.center = p_342729_;
      this.level = p_143484_;
      this.seed = p_143484_.getSeed();
      this.levelData = p_143484_.getLevelData();
      this.random = p_143484_.getChunkSource().randomState().getOrCreateRandomFactory(WORLDGEN_REGION_RANDOM).at(this.center.getPos().getWorldPosition());
      this.dimensionType = p_143484_.dimensionType();
      this.biomeManager = new BiomeManager(this, BiomeManager.obfuscateSeed(this.seed));
   }

   public boolean isOldChunkAround(ChunkPos p_215160_, int p_215161_) {
      return this.level.getChunkSource().chunkMap.isOldChunkAround(p_215160_, p_215161_);
   }

   public ChunkPos getCenter() {
      return this.center.getPos();
   }

   @Override
   public void setCurrentlyGenerating(@Nullable Supplier<String> p_143498_) {
      this.currentlyGenerating = p_143498_;
   }

   @Override
   public ChunkAccess getChunk(int p_9507_, int p_9508_) {
      return this.getChunk(p_9507_, p_9508_, ChunkStatus.EMPTY);
   }

   @Override
   public @Nullable ChunkAccess getChunk(int p_9514_, int p_9515_, ChunkStatus p_332757_, boolean p_9517_) {
      int i = this.center.getPos().getChessboardDistance(p_9514_, p_9515_);
      ChunkStatus chunkstatus = i >= this.generatingStep.directDependencies().size() ? null : this.generatingStep.directDependencies().get(i);
      GenerationChunkHolder generationchunkholder;
      if (chunkstatus != null) {
         generationchunkholder = this.cache.get(p_9514_, p_9515_);
         if (p_332757_.isOrBefore(chunkstatus)) {
            ChunkAccess chunkaccess = generationchunkholder.getChunkIfPresentUnchecked(chunkstatus);
            if (chunkaccess != null) {
               return chunkaccess;
            }
         }
      } else {
         generationchunkholder = null;
      }

      CrashReport crashreport = CrashReport.forThrowable(
         new IllegalStateException("Requested chunk unavailable during world generation"), "Exception generating new chunk"
      );
      CrashReportCategory crashreportcategory = crashreport.addCategory("Chunk request details");
      crashreportcategory.setDetail("Requested chunk", String.format(Locale.ROOT, "%d, %d", p_9514_, p_9515_));
      crashreportcategory.setDetail("Generating status", () -> this.generatingStep.targetStatus().getName());
      crashreportcategory.setDetail("Requested status", p_332757_::getName);
      crashreportcategory.setDetail(
         "Actual status", () -> generationchunkholder == null ? "[out of cache bounds]" : generationchunkholder.getPersistedStatus().getName()
      );
      crashreportcategory.setDetail("Maximum allowed status", () -> chunkstatus == null ? "null" : chunkstatus.getName());
      crashreportcategory.setDetail("Dependencies", this.generatingStep.directDependencies()::toString);
      crashreportcategory.setDetail("Requested distance", i);
      crashreportcategory.setDetail("Generating chunk", this.center.getPos()::toString);
      throw new ReportedException(crashreport);
   }

   @Override
   public boolean hasChunk(int p_9574_, int p_9575_) {
      int i = this.center.getPos().getChessboardDistance(p_9574_, p_9575_);
      return i < this.generatingStep.directDependencies().size();
   }

   @Override
   public BlockState getBlockState(BlockPos p_9587_) {
      return this.getChunk(SectionPos.blockToSectionCoord(p_9587_.getX()), SectionPos.blockToSectionCoord(p_9587_.getZ())).getBlockState(p_9587_);
   }

   @Override
   public FluidState getFluidState(BlockPos p_9577_) {
      return this.getChunk(p_9577_).getFluidState(p_9577_);
   }

   @Override
   public @Nullable Player getNearestPlayer(double p_9501_, double p_9502_, double p_9503_, double p_9504_, @Nullable Predicate<Entity> p_9505_) {
      return null;
   }

   @Override
   public int getSkyDarken() {
      return 0;
   }

   @Override
   public BiomeManager getBiomeManager() {
      return this.biomeManager;
   }

   @Override
   public Holder<Biome> getUncachedNoiseBiome(int p_203787_, int p_203788_, int p_203789_) {
      return this.level.getUncachedNoiseBiome(p_203787_, p_203788_, p_203789_);
   }

   @Override
   public float getShade(Direction p_9555_, boolean p_9556_) {
      return 1.0F;
   }

   @Override
   public LevelLightEngine getLightEngine() {
      return this.level.getLightEngine();
   }

   @Override
   public boolean destroyBlock(BlockPos p_9550_, boolean p_9551_, @Nullable Entity p_9552_, int p_9553_) {
      BlockState blockstate = this.getBlockState(p_9550_);
      if (blockstate.isAir()) {
         return false;
      }

      if (p_9551_) {
         BlockEntity blockentity = blockstate.hasBlockEntity() ? this.getBlockEntity(p_9550_) : null;
         Block.dropResources(blockstate, this.level, p_9550_, blockentity, p_9552_, ItemStack.EMPTY);
      }

      return this.setBlock(p_9550_, Blocks.AIR.defaultBlockState(), 3, p_9553_);
   }

   @Override
   public @Nullable BlockEntity getBlockEntity(BlockPos p_9582_) {
      ChunkAccess chunkaccess = this.getChunk(p_9582_);
      BlockEntity blockentity = chunkaccess.getBlockEntity(p_9582_);
      if (blockentity != null) {
         return blockentity;
      }

      CompoundTag compoundtag = chunkaccess.getBlockEntityNbt(p_9582_);
      BlockState blockstate = chunkaccess.getBlockState(p_9582_);
      if (compoundtag != null) {
         if ("DUMMY".equals(compoundtag.getStringOr("id", ""))) {
            if (!blockstate.hasBlockEntity()) {
               return null;
            }

            blockentity = ((EntityBlock)blockstate.getBlock()).newBlockEntity(p_9582_, blockstate);
         } else {
            blockentity = BlockEntity.loadStatic(p_9582_, blockstate, compoundtag, this.level.registryAccess());
         }

         if (blockentity != null) {
            chunkaccess.setBlockEntity(blockentity);
            return blockentity;
         }
      }

      if (blockstate.hasBlockEntity()) {
         LOGGER.warn("Tried to access a block entity before it was created. {}", p_9582_);
      }

      return null;
   }

   @Override
   public boolean ensureCanWrite(BlockPos p_181031_) {
      int i = SectionPos.blockToSectionCoord(p_181031_.getX());
      int j = SectionPos.blockToSectionCoord(p_181031_.getZ());
      ChunkPos chunkpos = this.getCenter();
      int k = Math.abs(chunkpos.x - i);
      int l = Math.abs(chunkpos.z - j);
      if (k <= this.generatingStep.blockStateWriteRadius() && l <= this.generatingStep.blockStateWriteRadius()) {
         if (this.center.isUpgrading()) {
            LevelHeightAccessor levelheightaccessor = this.center.getHeightAccessorForGeneration();
            if (levelheightaccessor.isOutsideBuildHeight(p_181031_.getY())) {
               return false;
            }
         }

         return true;
      } else {
         Util.logAndPauseIfInIde(
            "Detected setBlock in a far chunk ["
               + i
               + ", "
               + j
               + "], pos: "
               + p_181031_
               + ", status: "
               + this.generatingStep.targetStatus()
               + (this.currentlyGenerating == null ? "" : ", currently generating: " + this.currentlyGenerating.get())
         );
         return false;
      }
   }

   @Override
   public boolean setBlock(BlockPos p_9539_, BlockState p_9540_, @Block.UpdateFlags int p_9541_, int p_9542_) {
      if (!this.ensureCanWrite(p_9539_)) {
         return false;
      }

      ChunkAccess chunkaccess = this.getChunk(p_9539_);
      BlockState blockstate = chunkaccess.setBlockState(p_9539_, p_9540_, p_9541_);
      if (blockstate != null) {
         this.level.updatePOIOnBlockStateChange(p_9539_, blockstate, p_9540_);
      }

      if (p_9540_.hasBlockEntity()) {
         if (chunkaccess.getPersistedStatus().getChunkType() == ChunkType.LEVELCHUNK) {
            BlockEntity blockentity = ((EntityBlock)p_9540_.getBlock()).newBlockEntity(p_9539_, p_9540_);
            if (blockentity != null) {
               chunkaccess.setBlockEntity(blockentity);
            } else {
               chunkaccess.removeBlockEntity(p_9539_);
            }
         } else {
            CompoundTag compoundtag = new CompoundTag();
            compoundtag.putInt("x", p_9539_.getX());
            compoundtag.putInt("y", p_9539_.getY());
            compoundtag.putInt("z", p_9539_.getZ());
            compoundtag.putString("id", "DUMMY");
            chunkaccess.setBlockEntityNbt(compoundtag);
         }
      } else if (blockstate != null && blockstate.hasBlockEntity()) {
         chunkaccess.removeBlockEntity(p_9539_);
      }

      if (p_9540_.hasPostProcess(this, p_9539_) && (p_9541_ & 16) == 0) {
         this.markPosForPostprocessing(p_9539_);
      }

      return true;
   }

   private void markPosForPostprocessing(BlockPos p_9592_) {
      this.getChunk(p_9592_).markPosForPostprocessing(p_9592_);
   }

   @Override
   public boolean addFreshEntity(Entity p_9580_) {
      int i = SectionPos.blockToSectionCoord(p_9580_.getBlockX());
      int j = SectionPos.blockToSectionCoord(p_9580_.getBlockZ());
      this.getChunk(i, j).addEntity(p_9580_);
      return true;
   }

   @Override
   public boolean removeBlock(BlockPos p_9547_, boolean p_9548_) {
      return this.setBlock(p_9547_, Blocks.AIR.defaultBlockState(), 3);
   }

   @Override
   public WorldBorder getWorldBorder() {
      return this.level.getWorldBorder();
   }

   @Override
   public boolean isClientSide() {
      return false;
   }

   @Deprecated
   @Override
   public ServerLevel getLevel() {
      return this.level;
   }

   @Override
   public RegistryAccess registryAccess() {
      return this.level.registryAccess();
   }

   @Override
   public FeatureFlagSet enabledFeatures() {
      return this.level.enabledFeatures();
   }

   @Override
   public LevelData getLevelData() {
      return this.levelData;
   }

   @Override
   public DifficultyInstance getCurrentDifficultyAt(BlockPos p_9585_) {
      if (!this.hasChunk(SectionPos.blockToSectionCoord(p_9585_.getX()), SectionPos.blockToSectionCoord(p_9585_.getZ()))) {
         throw new RuntimeException("We are asking a region for a chunk out of bound");
      } else {
         return new DifficultyInstance(this.level.getDifficulty(), this.level.getDayTime(), 0L, this.level.getMoonBrightness(p_9585_));
      }
   }

   @Override
   public @Nullable MinecraftServer getServer() {
      return this.level.getServer();
   }

   @Override
   public ChunkSource getChunkSource() {
      return this.level.getChunkSource();
   }

   @Override
   public long getSeed() {
      return this.seed;
   }

   @Override
   public LevelTickAccess<Block> getBlockTicks() {
      return this.blockTicks;
   }

   @Override
   public LevelTickAccess<Fluid> getFluidTicks() {
      return this.fluidTicks;
   }

   @Override
   public int getSeaLevel() {
      return this.level.getSeaLevel();
   }

   @Override
   public RandomSource getRandom() {
      return this.random;
   }

   @Override
   public int getHeight(Heightmap.Types p_9535_, int p_9536_, int p_9537_) {
      return this.getChunk(SectionPos.blockToSectionCoord(p_9536_), SectionPos.blockToSectionCoord(p_9537_)).getHeight(p_9535_, p_9536_ & 15, p_9537_ & 15) + 1;
   }

   @Override
   public void playSound(@Nullable Entity p_397488_, BlockPos p_9529_, SoundEvent p_9530_, SoundSource p_9531_, float p_9532_, float p_9533_) {
   }

   @Override
   public void addParticle(ParticleOptions p_9561_, double p_9562_, double p_9563_, double p_9564_, double p_9565_, double p_9566_, double p_9567_) {
   }

   @Override
   public void levelEvent(@Nullable Entity p_396946_, int p_9524_, BlockPos p_9525_, int p_9526_) {
   }

   @Override
   public void gameEvent(Holder<GameEvent> p_332620_, Vec3 p_215164_, GameEvent.Context p_215165_) {
   }

   @Override
   public DimensionType dimensionType() {
      return this.dimensionType;
   }

   @Override
   public boolean isStateAtPosition(BlockPos p_9544_, Predicate<BlockState> p_9545_) {
      return p_9545_.test(this.getBlockState(p_9544_));
   }

   @Override
   public boolean isFluidAtPosition(BlockPos p_143500_, Predicate<FluidState> p_143501_) {
      return p_143501_.test(this.getFluidState(p_143500_));
   }

   @Override
   public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> p_143494_, AABB p_143495_, Predicate<? super T> p_143496_) {
      return Collections.emptyList();
   }

   @Override
   public List<Entity> getEntities(@Nullable Entity p_9519_, AABB p_9520_, @Nullable Predicate<? super Entity> p_9521_) {
      return Collections.emptyList();
   }

   @Override
   public List<Player> players() {
      return Collections.emptyList();
   }

   @Override
   public int getMinY() {
      return this.level.getMinY();
   }

   @Override
   public int getHeight() {
      return this.level.getHeight();
   }

   @Override
   public long nextSubTickCount() {
      return this.subTickCount.getAndIncrement();
   }

   @Override
   public EnvironmentAttributeReader environmentAttributes() {
      return EnvironmentAttributeReader.EMPTY;
   }
}
