package net.minecraft.world.level;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Continuation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.clock.ClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.CollectingNeighborUpdater;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;

public abstract class Level implements LevelAccessor, AutoCloseable {
   public static final Codec<ResourceKey<Level>> RESOURCE_KEY_CODEC = ResourceKey.codec(Registries.DIMENSION);
   public static final ResourceKey<Level> OVERWORLD = ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("overworld"));
   public static final ResourceKey<Level> NETHER = ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("the_nether"));
   public static final ResourceKey<Level> END = ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("the_end"));
   public static final int MAX_LEVEL_SIZE = 30000000;
   public static final int ACROSS_THE_WHOLE_WORLD = 60000000;
   public static final int LONG_PARTICLE_CLIP_RANGE = 512;
   public static final int SHORT_PARTICLE_CLIP_RANGE = 32;
   public static final int MAX_BRIGHTNESS = 15;
   public static final int MAX_ENTITY_SPAWN_Y = 20000000;
   public static final int MIN_ENTITY_SPAWN_Y = -20000000;
   private static final WeightedList<ExplosionParticleInfo> DEFAULT_EXPLOSION_BLOCK_PARTICLES = WeightedList.<ExplosionParticleInfo>builder()
      .add(new ExplosionParticleInfo(ParticleTypes.POOF, 0.5F, 1.0F))
      .add(new ExplosionParticleInfo(ParticleTypes.SMOKE, 1.0F, 1.0F))
      .build();
   protected final List<TickingBlockEntity> blockEntityTickers = Lists.newArrayList();
   protected final CollectingNeighborUpdater neighborUpdater;
   private final List<TickingBlockEntity> pendingBlockEntityTickers = Lists.newArrayList();
   private boolean tickingBlockEntities;
   private final Thread thread;
   private final boolean isDebug;
   private int skyDarken;
   protected int randValue = RandomSource.createThreadLocalInstance().nextInt();
   protected final int addend = 1013904223;
   protected float oRainLevel;
   protected float rainLevel;
   protected float oThunderLevel;
   protected float thunderLevel;
   protected final RandomSource random = RandomSource.create();
   @Deprecated
   private final RandomSource soundSeedGenerator = RandomSource.createThreadSafe();
   private final Holder<DimensionType> dimensionTypeRegistration;
   protected final WritableLevelData levelData;
   private final boolean isClientSide;
   private final BiomeManager biomeManager;
   private final ResourceKey<Level> dimension;
   private final RegistryAccess registryAccess;
   private final DamageSources damageSources;
   private final PalettedContainerFactory palettedContainerFactory;
   private long subTickCount;

   protected Level(
      final WritableLevelData levelData,
      final ResourceKey<Level> dimension,
      final RegistryAccess registryAccess,
      final Holder<DimensionType> dimensionTypeRegistration,
      final boolean isClientSide,
      final boolean isDebug,
      final long biomeZoomSeed,
      final int maxChainedNeighborUpdates
   ) {
      this.levelData = levelData;
      this.dimensionTypeRegistration = dimensionTypeRegistration;
      this.dimension = dimension;
      this.isClientSide = isClientSide;
      this.thread = Thread.currentThread();
      this.biomeManager = new BiomeManager(this, biomeZoomSeed);
      this.isDebug = isDebug;
      this.neighborUpdater = new CollectingNeighborUpdater(this, maxChainedNeighborUpdates);
      this.registryAccess = registryAccess;
      this.palettedContainerFactory = PalettedContainerFactory.create(registryAccess);
      this.damageSources = new DamageSources(registryAccess);
   }

   public int getNextEntityId() {
      return 0;
   }

   @Override
   public boolean isClientSide() {
      return this.isClientSide;
   }

   @Override
   public @Nullable MinecraftServer getServer() {
      return null;
   }

   public boolean isInWorldBounds(final BlockPos pos) {
      return this.isInsideBuildHeight(pos) && isInWorldBoundsHorizontal(pos);
   }

   public boolean isInValidBounds(final BlockPos pos) {
      return this.isInsideBuildHeight(pos) && isInValidBoundsHorizontal(pos);
   }

   public static boolean isInSpawnableBounds(final BlockPos pos) {
      return !isOutsideSpawnableHeight(pos.getY()) && isInWorldBoundsHorizontal(pos);
   }

   private static boolean isInWorldBoundsHorizontal(final BlockPos pos) {
      return pos.getX() >= -30000000 && pos.getZ() >= -30000000 && pos.getX() < 30000000 && pos.getZ() < 30000000;
   }

   private static boolean isInValidBoundsHorizontal(final BlockPos pos) {
      int chunkX = SectionPos.blockToSectionCoord(pos.getX());
      int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
      return ChunkPos.isValid(chunkX, chunkZ);
   }

   private static boolean isOutsideSpawnableHeight(final int y) {
      return y < -20000000 || y >= 20000000;
   }

   public LevelChunk getChunkAt(final BlockPos pos) {
      return this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
   }

   public LevelChunk getChunk(final int chunkX, final int chunkZ) {
      return (LevelChunk)this.getChunk(chunkX, chunkZ, ChunkStatus.FULL);
   }

   @Override
   public @Nullable ChunkAccess getChunk(final int chunkX, final int chunkZ, final ChunkStatus status, final boolean loadOrGenerate) {
      ChunkAccess chunk = this.getChunkSource().getChunk(chunkX, chunkZ, status, loadOrGenerate);
      if (chunk == null && loadOrGenerate) {
         throw new IllegalStateException("Should always be able to create a chunk!");
      } else {
         return chunk;
      }
   }

   @Override
   public boolean setBlock(final BlockPos pos, final BlockState blockState, final @Block.UpdateFlags int updateFlags, final int updateLimit) {
      if (!this.isInValidBounds(pos)) {
         return false;
      }

      if (!this.isClientSide() && this.isDebug()) {
         return false;
      }

      LevelChunk chunk = this.getChunkAt(pos);
      Block block = blockState.getBlock();
      BlockState oldState = chunk.setBlockState(pos, blockState, updateFlags);
      if (oldState == null) {
         return false;
      }

      BlockState newState = this.getBlockState(pos);
      if (newState == blockState) {
         if (oldState != newState) {
            this.setBlocksDirty(pos, oldState, newState);
         }

         if ((updateFlags & 2) != 0
            && (!this.isClientSide() || (updateFlags & 4) == 0)
            && (this.isClientSide() || chunk.getFullStatus() != null && chunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING))) {
            this.sendBlockUpdated(pos, oldState, blockState, updateFlags);
         }

         if ((updateFlags & 1) != 0) {
            this.updateNeighborsAt(pos, oldState.getBlock());
            if (!this.isClientSide() && blockState.hasAnalogOutputSignal()) {
               this.updateNeighbourForOutputSignal(pos, block);
            }
         }

         if ((updateFlags & 16) == 0 && updateLimit > 0) {
            int neighbourUpdateFlags = updateFlags & -34;
            oldState.updateIndirectNeighbourShapes(this, pos, neighbourUpdateFlags, updateLimit - 1);
            blockState.updateNeighbourShapes(this, pos, neighbourUpdateFlags, updateLimit - 1);
            blockState.updateIndirectNeighbourShapes(this, pos, neighbourUpdateFlags, updateLimit - 1);
         }

         this.updatePOIOnBlockStateChange(pos, oldState, newState);
      }

      return true;
   }

   public void updatePOIOnBlockStateChange(final BlockPos pos, final BlockState oldState, final BlockState newState) {
   }

   @Override
   public boolean removeBlock(final BlockPos pos, final boolean movedByPiston) {
      FluidState fluidState = this.getFluidState(pos);
      return this.setBlock(pos, fluidState.createLegacyBlock(), 3 | (movedByPiston ? 64 : 0));
   }

   @Override
   public boolean destroyBlock(final BlockPos pos, final boolean dropResources, final @Nullable Entity breaker, final int updateLimit) {
      BlockState blockState = this.getBlockState(pos);
      if (blockState.isAir()) {
         return false;
      }

      blockState.getBlock().spawnDestroyParticles(this, pos, blockState);
      if (dropResources) {
         BlockEntity blockEntity = blockState.hasBlockEntity() ? this.getBlockEntity(pos) : null;
         Block.dropResources(blockState, this, pos, blockEntity, breaker, ItemStack.EMPTY);
      }

      FluidState fluidState = this.getFluidState(pos);
      boolean destroyed = this.setBlock(pos, fluidState.createLegacyBlock(), 3, updateLimit);
      if (destroyed) {
         this.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(breaker, blockState));
      }

      return destroyed;
   }

   public void addDestroyBlockEffect(final BlockPos pos, final BlockState blockState) {
   }

   public abstract void sendBlockUpdated(BlockPos pos, BlockState old, BlockState current, @Block.UpdateFlags int updateFlags);

   public void setBlocksDirty(final BlockPos pos, final BlockState oldState, final BlockState newState) {
   }

   public void updateNeighborsAt(final BlockPos pos, final Block sourceBlock, final @Nullable Orientation orientation) {
   }

   public void updateNeighborsAtExceptFromFacing(
      final BlockPos pos, final Block blockObject, final Direction skipDirection, final @Nullable Orientation orientation
   ) {
   }

   public void neighborChanged(final BlockPos pos, final Block changedBlock, final @Nullable Orientation orientation) {
   }

   public void neighborChanged(
      final BlockState state, final BlockPos pos, final Block changedBlock, final @Nullable Orientation orientation, final boolean movedByPiston
   ) {
   }

   @Override
   public void neighborShapeChanged(
      final Direction direction,
      final BlockPos pos,
      final BlockPos neighborPos,
      final BlockState neighborState,
      final @Block.UpdateFlags int updateFlags,
      final int updateLimit
   ) {
      this.neighborUpdater.shapeUpdate(direction, neighborState, pos, neighborPos, updateFlags, updateLimit);
   }

   @Override
   public int getHeight(final Heightmap.Types type, final int x, final int z) {
      int y;
      if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000) {
         if (this.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
            y = this.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z)).getHeight(type, x & 15, z & 15) + 1;
         } else {
            y = this.getMinY();
         }
      } else {
         y = this.getSeaLevel() + 1;
      }

      return y;
   }

   @Override
   public LevelLightEngine getLightEngine() {
      return this.getChunkSource().getLightEngine();
   }

   @Override
   public BlockState getBlockState(final BlockPos pos) {
      if (!this.isInValidBounds(pos)) {
         return Blocks.VOID_AIR.defaultBlockState();
      }

      LevelChunk chunk = this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
      return chunk.getBlockState(pos);
   }

   @Override
   public FluidState getFluidState(final BlockPos pos) {
      if (!this.isInValidBounds(pos)) {
         return Fluids.EMPTY.defaultFluidState();
      }

      LevelChunk chunk = this.getChunkAt(pos);
      return chunk.getFluidState(pos);
   }

   public boolean isBrightOutside() {
      return !this.dimensionType().hasFixedTime() && this.skyDarken < 4;
   }

   public boolean isDarkOutside() {
      return !this.dimensionType().hasFixedTime() && !this.isBrightOutside();
   }

   @Override
   public void playSound(
      final @Nullable Entity except, final BlockPos pos, final SoundEvent sound, final SoundSource source, final float volume, final float pitch
   ) {
      this.playSound(except, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound, source, volume, pitch);
   }

   public abstract void playSeededSound(
      final @Nullable Entity except,
      final double x,
      final double y,
      final double z,
      final Holder<SoundEvent> sound,
      final SoundSource source,
      final float volume,
      final float pitch,
      final long seed
   );

   public void playSeededSound(
      final @Nullable Entity except,
      final double x,
      final double y,
      final double z,
      final SoundEvent sound,
      final SoundSource source,
      final float volume,
      final float pitch,
      final long seed
   ) {
      this.playSeededSound(except, x, y, z, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source, volume, pitch, seed);
   }

   public abstract void playSeededSound(
      final @Nullable Entity except,
      final Entity sourceEntity,
      final Holder<SoundEvent> sound,
      final SoundSource source,
      final float volume,
      final float pitch,
      final long seed
   );

   public void playSound(final @Nullable Entity except, final double x, final double y, final double z, final SoundEvent sound, final SoundSource source) {
      this.playSound(except, x, y, z, sound, source, 1.0F, 1.0F);
   }

   public void playSound(
      final @Nullable Entity except,
      final double x,
      final double y,
      final double z,
      final SoundEvent sound,
      final SoundSource source,
      final float volume,
      final float pitch
   ) {
      this.playSeededSound(except, x, y, z, sound, source, volume, pitch, this.soundSeedGenerator.nextLong());
   }

   public void playSound(
      final @Nullable Entity except,
      final double x,
      final double y,
      final double z,
      final Holder<SoundEvent> sound,
      final SoundSource source,
      final float volume,
      final float pitch
   ) {
      this.playSeededSound(except, x, y, z, sound, source, volume, pitch, this.soundSeedGenerator.nextLong());
   }

   public void playSound(
      final @Nullable Entity except, final Entity sourceEntity, final SoundEvent sound, final SoundSource source, final float volume, final float pitch
   ) {
      this.playSeededSound(except, sourceEntity, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), source, volume, pitch, this.soundSeedGenerator.nextLong());
   }

   public void playSound(
      final @Nullable Entity except, final Entity sourceEntity, final Holder<SoundEvent> sound, final SoundSource source, final float volume, final float pitch
   ) {
      this.playSeededSound(except, sourceEntity, sound, source, volume, pitch, this.soundSeedGenerator.nextLong());
   }

   public void playLocalSound(
      final BlockPos pos, final SoundEvent sound, final SoundSource source, final float volume, final float pitch, final boolean distanceDelay
   ) {
      this.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound, source, volume, pitch, distanceDelay);
   }

   public void playLocalSound(final Entity sourceEntity, final SoundEvent sound, final SoundSource source, final float volume, final float pitch) {
   }

   public void playLocalSound(
      final double x,
      final double y,
      final double z,
      final SoundEvent sound,
      final SoundSource source,
      final float volume,
      final float pitch,
      final boolean distanceDelay
   ) {
   }

   public void playPlayerSound(final SoundEvent sound, final SoundSource source, final float volume, final float pitch) {
   }

   @Override
   public void addParticle(final ParticleOptions particle, final double x, final double y, final double z, final double xd, final double yd, final double zd) {
   }

   public void addParticle(
      final ParticleOptions particle,
      final boolean overrideLimiter,
      final boolean alwaysShow,
      final double x,
      final double y,
      final double z,
      final double xd,
      final double yd,
      final double zd
   ) {
   }

   public void addAlwaysVisibleParticle(
      final ParticleOptions particle, final double x, final double y, final double z, final double xd, final double yd, final double zd
   ) {
   }

   public void addAlwaysVisibleParticle(
      final ParticleOptions particle,
      final boolean overrideLimiter,
      final double x,
      final double y,
      final double z,
      final double xd,
      final double yd,
      final double zd
   ) {
   }

   public void addBlockEntityTicker(final TickingBlockEntity ticker) {
      (this.tickingBlockEntities ? this.pendingBlockEntityTickers : this.blockEntityTickers).add(ticker);
   }

   public void tickBlockEntities() {
      this.tickingBlockEntities = true;
      if (!this.pendingBlockEntityTickers.isEmpty()) {
         this.blockEntityTickers.addAll(this.pendingBlockEntityTickers);
         this.pendingBlockEntityTickers.clear();
      }

      Iterator<TickingBlockEntity> iterator = this.blockEntityTickers.iterator();
      boolean tickBlockEntities = this.tickRateManager().runsNormally();

      while (iterator.hasNext()) {
         TickingBlockEntity ticker = iterator.next();
         if (ticker.isRemoved()) {
            iterator.remove();
         } else if (tickBlockEntities && this.shouldTickBlocksAt(ticker.getPos())) {
            ticker.tick();
         }
      }

      this.tickingBlockEntities = false;
   }

   public <T extends Entity> void guardEntityTick(final Consumer<T> tick, final T entity) {
      try {
         tick.accept(entity);
      } catch (Throwable t) {
         CrashReport report = CrashReport.forThrowable(t, "Ticking entity");
         CrashReportCategory category = report.addCategory("Entity being ticked");
         entity.fillCrashReportCategory(category);
         throw new ReportedException(report);
      }
   }

   public boolean shouldTickDeath(final Entity entity) {
      return true;
   }

   public boolean shouldTickBlocksAt(final long chunkPos) {
      return true;
   }

   public boolean shouldTickBlocksAt(final BlockPos pos) {
      return this.shouldTickBlocksAt(ChunkPos.pack(pos));
   }

   public void explode(
      final @Nullable Entity source, final double x, final double y, final double z, final float r, final Level.ExplosionInteraction blockInteraction
   ) {
      this.explode(
         source,
         Explosion.getDefaultDamageSource(this, source),
         null,
         x,
         y,
         z,
         r,
         false,
         blockInteraction,
         ParticleTypes.EXPLOSION,
         ParticleTypes.EXPLOSION_EMITTER,
         DEFAULT_EXPLOSION_BLOCK_PARTICLES,
         SoundEvents.GENERIC_EXPLODE
      );
   }

   public void explode(
      final @Nullable Entity source,
      final double x,
      final double y,
      final double z,
      final float r,
      final boolean fire,
      final Level.ExplosionInteraction blockInteraction
   ) {
      this.explode(
         source,
         Explosion.getDefaultDamageSource(this, source),
         null,
         x,
         y,
         z,
         r,
         fire,
         blockInteraction,
         ParticleTypes.EXPLOSION,
         ParticleTypes.EXPLOSION_EMITTER,
         DEFAULT_EXPLOSION_BLOCK_PARTICLES,
         SoundEvents.GENERIC_EXPLODE
      );
   }

   public void explode(
      final @Nullable Entity source,
      final @Nullable DamageSource damageSource,
      final @Nullable ExplosionDamageCalculator damageCalculator,
      final Vec3 boomPos,
      final float r,
      final boolean fire,
      final Level.ExplosionInteraction blockInteraction
   ) {
      this.explode(
         source,
         damageSource,
         damageCalculator,
         boomPos.x(),
         boomPos.y(),
         boomPos.z(),
         r,
         fire,
         blockInteraction,
         ParticleTypes.EXPLOSION,
         ParticleTypes.EXPLOSION_EMITTER,
         DEFAULT_EXPLOSION_BLOCK_PARTICLES,
         SoundEvents.GENERIC_EXPLODE
      );
   }

   public void explode(
      final @Nullable Entity source,
      final @Nullable DamageSource damageSource,
      final @Nullable ExplosionDamageCalculator damageCalculator,
      final double x,
      final double y,
      final double z,
      final float r,
      final boolean fire,
      final Level.ExplosionInteraction interactionType
   ) {
      this.explode(
         source,
         damageSource,
         damageCalculator,
         x,
         y,
         z,
         r,
         fire,
         interactionType,
         ParticleTypes.EXPLOSION,
         ParticleTypes.EXPLOSION_EMITTER,
         DEFAULT_EXPLOSION_BLOCK_PARTICLES,
         SoundEvents.GENERIC_EXPLODE
      );
   }

   public abstract void explode(
      final @Nullable Entity source,
      final @Nullable DamageSource damageSource,
      final @Nullable ExplosionDamageCalculator damageCalculator,
      final double x,
      final double y,
      final double z,
      final float r,
      final boolean fire,
      final Level.ExplosionInteraction interactionType,
      final ParticleOptions smallExplosionParticles,
      final ParticleOptions largeExplosionParticles,
      final WeightedList<ExplosionParticleInfo> blockParticles,
      final Holder<SoundEvent> explosionSound
   );

   public abstract String gatherChunkSourceStats();

   @Override
   public @Nullable BlockEntity getBlockEntity(final BlockPos pos) {
      if (!this.isInValidBounds(pos)) {
         return null;
      } else {
         return !this.isClientSide() && Thread.currentThread() != this.thread
            ? null
            : this.getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
      }
   }

   public void setBlockEntity(final BlockEntity blockEntity) {
      BlockPos pos = blockEntity.getBlockPos();
      if (this.isInValidBounds(pos)) {
         this.getChunkAt(pos).addAndRegisterBlockEntity(blockEntity);
      }
   }

   public void removeBlockEntity(final BlockPos pos) {
      if (this.isInValidBounds(pos)) {
         this.getChunkAt(pos).removeBlockEntity(pos);
      }
   }

   public boolean isLoaded(final BlockPos pos) {
      return !this.isInValidBounds(pos)
         ? false
         : this.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
   }

   public boolean loadedAndEntityCanStandOnFace(final BlockPos pos, final Entity entity, final Direction faceDirection) {
      if (!this.isInValidBounds(pos)) {
         return false;
      }

      ChunkAccess chunk = this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), ChunkStatus.FULL, false);
      return chunk == null ? false : chunk.getBlockState(pos).entityCanStandOnFace(this, pos, entity, faceDirection);
   }

   public boolean loadedAndEntityCanStandOn(final BlockPos pos, final Entity entity) {
      return this.loadedAndEntityCanStandOnFace(pos, entity, Direction.UP);
   }

   public void updateSkyBrightness() {
      this.skyDarken = (int)(15.0F - this.environmentAttributes().getDimensionValue(EnvironmentAttributes.SKY_LIGHT_LEVEL));
   }

   public void setSpawnSettings(final boolean spawnEnemies) {
      this.getChunkSource().setSpawnSettings(spawnEnemies);
   }

   public abstract void setRespawnData(final LevelData.RespawnData respawnData);

   public abstract LevelData.RespawnData getRespawnData();

   public LevelData.RespawnData getWorldBorderAdjustedRespawnData(final LevelData.RespawnData respawnData) {
      WorldBorder worldBorder = this.getWorldBorder();
      if (!worldBorder.isWithinBounds(respawnData.pos())) {
         BlockPos newPos = this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(worldBorder.getCenterX(), 0.0, worldBorder.getCenterZ()));
         return LevelData.RespawnData.of(respawnData.dimension(), newPos, respawnData.yaw(), respawnData.pitch());
      } else {
         return respawnData;
      }
   }

   @Override
   public void close() throws IOException {
      this.getChunkSource().close();
   }

   @Override
   public @Nullable BlockGetter getChunkForCollisions(final int chunkX, final int chunkZ) {
      return this.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
   }

   @Override
   public List<Entity> getEntities(final @Nullable Entity except, final AABB bb, final Predicate<? super Entity> selector) {
      Profiler.get().incrementCounter("getEntities");
      List<Entity> output = Lists.newArrayList();
      this.getEntities().get(bb, entity -> {
         if (entity != except && selector.test(entity)) {
            output.add(entity);
         }
      });

      for (EnderDragonPart dragonPart : this.dragonParts()) {
         if (dragonPart != except && dragonPart.parentMob != except && selector.test(dragonPart) && bb.intersects(dragonPart.getBoundingBox())) {
            output.add(dragonPart);
         }
      }

      return output;
   }

   @Override
   public <T extends Entity> List<T> getEntities(final EntityTypeTest<Entity, T> type, final AABB bb, final Predicate<? super T> selector) {
      List<T> output = Lists.newArrayList();
      this.getEntities(type, bb, selector, output);
      return output;
   }

   public <T extends Entity> void getEntities(
      final EntityTypeTest<Entity, T> type, final AABB bb, final Predicate<? super T> selector, final List<? super T> output
   ) {
      this.getEntities(type, bb, selector, output, Integer.MAX_VALUE);
   }

   public <T extends Entity> void getEntities(
      final EntityTypeTest<Entity, T> type, final AABB bb, final Predicate<? super T> selector, final List<? super T> output, final int maxResults
   ) {
      Profiler.get().incrementCounter("getEntities");
      this.getEntities().get(type, bb, e -> {
         if (selector.test(e)) {
            output.add(e);
            if (output.size() >= maxResults) {
               return Continuation.ABORT;
            }
         }

         if (e instanceof EnderDragon enderDragon) {
            for (EnderDragonPart subEntity : enderDragon.getSubEntities()) {
               T castSubPart = type.tryCast(subEntity);
               if (castSubPart != null && selector.test(castSubPart)) {
                  output.add(castSubPart);
                  if (output.size() >= maxResults) {
                     return Continuation.ABORT;
                  }
               }
            }
         }

         return Continuation.CONTINUE;
      });
   }

   public <T extends Entity> boolean hasEntities(final EntityTypeTest<Entity, T> type, final AABB bb, final Predicate<? super T> selector) {
      Profiler.get().incrementCounter("hasEntities");
      MutableBoolean hasEntities = new MutableBoolean();
      this.getEntities().get(type, bb, e -> {
         if (selector.test(e)) {
            hasEntities.setTrue();
            return Continuation.ABORT;
         }

         if (e instanceof EnderDragon enderDragon) {
            for (EnderDragonPart subEntity : enderDragon.getSubEntities()) {
               T castSubPart = type.tryCast(subEntity);
               if (castSubPart != null && selector.test(castSubPart)) {
                  hasEntities.setTrue();
                  return Continuation.ABORT;
               }
            }
         }

         return Continuation.CONTINUE;
      });
      return hasEntities.isTrue();
   }

   public List<Entity> getPushableEntities(final Entity pusher, final AABB boundingBox) {
      return this.getEntities(pusher, boundingBox, EntitySelector.pushableBy(pusher));
   }

   public abstract @Nullable Entity getEntity(int id);

   public @Nullable Entity getEntity(final UUID uuid) {
      return this.getEntities().get(uuid);
   }

   public @Nullable Entity getEntityInAnyDimension(final UUID uuid) {
      return this.getEntity(uuid);
   }

   public @Nullable Player getPlayerInAnyDimension(final UUID uuid) {
      return this.getPlayerByUUID(uuid);
   }

   public abstract Collection<EnderDragonPart> dragonParts();

   public void blockEntityChanged(final BlockPos pos) {
      if (this.hasChunkAt(pos)) {
         this.getChunkAt(pos).markUnsaved();
      }
   }

   public void onBlockEntityAdded(final BlockEntity blockEntity) {
   }

   public long getOverworldClockTime() {
      return this.getClockTimeTicks(this.registryAccess().get(WorldClocks.OVERWORLD));
   }

   public long getDefaultClockTime() {
      return this.getClockTimeTicks(this.dimensionType().defaultClock());
   }

   private long getClockTimeTicks(final Optional<? extends Holder<WorldClock>> clock) {
      return clock.<Long>map(holder -> this.clockManager().getInstance((Holder<WorldClock>)holder).totalTicks()).orElse(0L);
   }

   public boolean mayInteract(final Entity entity, final BlockPos pos) {
      return true;
   }

   public void broadcastEntityEvent(final Entity entity, final byte event) {
   }

   public void broadcastDamageEvent(final Entity entity, final DamageSource source) {
   }

   public void blockEvent(final BlockPos pos, final Block block, final int b0, final int b1) {
      this.getBlockState(pos).triggerEvent(this, pos, b0, b1);
   }

   @Override
   public LevelData getLevelData() {
      return this.levelData;
   }

   public abstract TickRateManager tickRateManager();

   public float getRelativeTickSpeed() {
      return 1.0F;
   }

   public float getThunderLevel(final float a) {
      return Mth.lerp(a, this.oThunderLevel, this.thunderLevel) * this.getRainLevel(a);
   }

   public void setThunderLevel(final float thunderLevel) {
      float clampedThunderLevel = Mth.clamp(thunderLevel, 0.0F, 1.0F);
      this.oThunderLevel = clampedThunderLevel;
      this.thunderLevel = clampedThunderLevel;
   }

   public float getRainLevel(final float a) {
      return Mth.lerp(a, this.oRainLevel, this.rainLevel);
   }

   public void setRainLevel(final float rainLevel) {
      float clampedRainLevel = Mth.clamp(rainLevel, 0.0F, 1.0F);
      this.oRainLevel = clampedRainLevel;
      this.rainLevel = clampedRainLevel;
   }

   public boolean canHaveWeather() {
      return this.dimensionType().hasSkyLight() && !this.dimensionType().hasCeiling() && this.dimension() != END;
   }

   public boolean isThundering() {
      return this.canHaveWeather() && this.getThunderLevel(1.0F) > 0.9;
   }

   public boolean isRaining() {
      return this.canHaveWeather() && this.getRainLevel(1.0F) > 0.2;
   }

   public boolean isRainingAt(final BlockPos pos) {
      return this.precipitationAt(pos) == Biome.Precipitation.RAIN;
   }

   public Biome.Precipitation precipitationAt(final BlockPos pos) {
      if (!this.isRaining()) {
         return Biome.Precipitation.NONE;
      }

      if (!this.canSeeSky(pos)) {
         return Biome.Precipitation.NONE;
      }

      if (this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() > pos.getY()) {
         return Biome.Precipitation.NONE;
      }

      Biome biome = this.getBiome(pos).value();
      return biome.getPrecipitationAt(pos, this.getSeaLevel());
   }

   public abstract @Nullable MapItemSavedData getMapData(MapId id);

   public void globalLevelEvent(final int type, final BlockPos pos, final int data) {
   }

   public CrashReportCategory fillReportDetails(final CrashReport report) {
      CrashReportCategory category = report.addCategory("Affected level", 1);
      category.setDetail("All players", () -> {
         List<? extends Player> players = this.players();
         return players.size() + " total; " + players.stream().map(Player::debugInfo).collect(Collectors.joining(", "));
      });
      category.setDetail("Chunk stats", this.getChunkSource()::gatherStats);
      category.setDetail("Level dimension", () -> this.dimension().identifier().toString());
      category.setDetail("Level time", () -> String.format(Locale.ROOT, "%d game time, %d day time", this.getGameTime(), this.getOverworldClockTime()));

      try {
         this.levelData.fillCrashReportCategory(category, this);
      } catch (Throwable t) {
         category.setDetailError("Level Data Unobtainable", t);
      }

      return category;
   }

   public abstract void destroyBlockProgress(final int id, final BlockPos blockPos, final int progress);

   public void createFireworks(
      final double x,
      final double y,
      final double z,
      final double xd,
      final double yd,
      final double zd,
      final List<FireworkExplosion> explosions,
      final boolean playSound
   ) {
   }

   public abstract Scoreboard getScoreboard();

   public void updateNeighbourForOutputSignal(final BlockPos pos, final Block changedBlock) {
      for (Direction direction : Direction.Plane.HORIZONTAL) {
         BlockPos relativePos = pos.relative(direction);
         if (this.hasChunkAt(relativePos)) {
            BlockState state = this.getBlockState(relativePos);
            if (state.is(Blocks.COMPARATOR)) {
               this.neighborChanged(state, relativePos, changedBlock, null, false);
            } else if (state.isRedstoneConductor(this, relativePos)) {
               relativePos = relativePos.relative(direction);
               state = this.getBlockState(relativePos);
               if (state.is(Blocks.COMPARATOR)) {
                  this.neighborChanged(state, relativePos, changedBlock, null, false);
               }
            }
         }
      }
   }

   @Override
   public int getSkyDarken() {
      return this.skyDarken;
   }

   public void setSkyFlashTime(final int skyFlashTime) {
   }

   public void sendPacketToServer(final Packet<?> packet) {
      throw new UnsupportedOperationException("Can't send packets to server unless you're on the client.");
   }

   @Override
   public DimensionType dimensionType() {
      return this.dimensionTypeRegistration.value();
   }

   public Holder<DimensionType> dimensionTypeRegistration() {
      return this.dimensionTypeRegistration;
   }

   public ResourceKey<Level> dimension() {
      return this.dimension;
   }

   @Override
   public RandomSource getRandom() {
      return this.random;
   }

   @Override
   public boolean isStateAtPosition(final BlockPos pos, final Predicate<BlockState> predicate) {
      return predicate.test(this.getBlockState(pos));
   }

   @Override
   public boolean isFluidAtPosition(final BlockPos pos, final Predicate<FluidState> predicate) {
      return predicate.test(this.getFluidState(pos));
   }

   public abstract RecipeAccess recipeAccess();

   public BlockPos getBlockRandomPos(final int xo, final int yo, final int zo, final int yMask) {
      this.randValue = this.randValue * 3 + 1013904223;
      int val = this.randValue >> 2;
      return new BlockPos(xo + (val & 15), yo + (val >> 16 & yMask), zo + (val >> 8 & 15));
   }

   public boolean noSave() {
      return false;
   }

   @Override
   public BiomeManager getBiomeManager() {
      return this.biomeManager;
   }

   public final boolean isDebug() {
      return this.isDebug;
   }

   protected abstract LevelEntityGetter<Entity> getEntities();

   @Override
   public long nextSubTickCount() {
      return this.subTickCount++;
   }

   @Override
   public RegistryAccess registryAccess() {
      return this.registryAccess;
   }

   public DamageSources damageSources() {
      return this.damageSources;
   }

   public abstract ClockManager clockManager();

   public abstract EnvironmentAttributeSystem environmentAttributes();

   public abstract FuelValues fuelValues();

   public int getClientLeafTintColor(final BlockPos pos) {
      return 0;
   }

   public PalettedContainerFactory palettedContainerFactory() {
      return this.palettedContainerFactory;
   }

   public enum ExplosionInteraction implements StringRepresentable {
      NONE("none"),
      BLOCK("block"),
      MOB("mob"),
      TNT("tnt"),
      TRIGGER("trigger");

      public static final Codec<Level.ExplosionInteraction> CODEC = StringRepresentable.fromEnum(Level.ExplosionInteraction::values);
      private final String id;

      ExplosionInteraction(final String id) {
         this.id = id;
      }

      @Override
      public String getSerializedName() {
         return this.id;
      }
   }
}
