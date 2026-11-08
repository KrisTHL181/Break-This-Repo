package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.players.SleepStatus;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Continuation;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.Mth;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.LevelDebugSynchronizers;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ReputationEventHandler;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathTypeCache;
import net.minecraft.world.level.portal.PortalForcer;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapIndex;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class ServerLevel extends Level implements WorldGenLevel, ServerEntityGetter {
   public static final BlockPos END_SPAWN_POINT = new BlockPos(100, 50, 0);
   public static final IntProvider RAIN_DELAY = UniformInt.of(12000, 180000);
   public static final IntProvider RAIN_DURATION = UniformInt.of(12000, 24000);
   private static final IntProvider THUNDER_DELAY = UniformInt.of(12000, 180000);
   public static final IntProvider THUNDER_DURATION = UniformInt.of(3600, 15600);
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int EMPTY_TIME_NO_TICK = 300;
   private static final int MAX_SCHEDULED_TICKS_PER_TICK = 65536;
   private static final AtomicInteger ENTITY_COUNTER = new AtomicInteger();
   private final List<ServerPlayer> players = Lists.newArrayList();
   private final ServerChunkCache chunkSource;
   private final MinecraftServer server;
   private final ServerLevelData serverLevelData;
   private final EntityTickList entityTickList = new EntityTickList();
   private final ServerWaypointManager waypointManager;
   private EnvironmentAttributeSystem environmentAttributes;
   private final PersistentEntitySectionManager<Entity> entityManager;
   private final GameEventDispatcher gameEventDispatcher;
   public boolean noSave;
   private final SleepStatus sleepStatus;
   private int emptyTime;
   private final PortalForcer portalForcer;
   private final LevelTicks<Block> blockTicks = new LevelTicks<>(this::isPositionTickingWithEntitiesLoaded);
   private final LevelTicks<Fluid> fluidTicks = new LevelTicks<>(this::isPositionTickingWithEntitiesLoaded);
   private final PathTypeCache pathTypesByPosCache = new PathTypeCache();
   private final Set<Mob> navigatingMobs = new ObjectOpenHashSet();
   private volatile boolean isUpdatingNavigations;
   protected final Raids raids;
   private final ObjectLinkedOpenHashSet<BlockEventData> blockEvents = new ObjectLinkedOpenHashSet();
   private final List<BlockEventData> blockEventsToReschedule = new ArrayList<>(64);
   private boolean handlingTick;
   private final List<CustomSpawner> customSpawners;
   private @Nullable EnderDragonFight dragonFight;
   private final Int2ObjectMap<EnderDragonPart> dragonParts = new Int2ObjectOpenHashMap();
   private final StructureManager structureManager;
   private final StructureCheck structureCheck;
   private final boolean tickTime;
   private final LevelDebugSynchronizers debugSynchronizers = new LevelDebugSynchronizers(this);

   public ServerLevel(
      final MinecraftServer server,
      final Executor executor,
      final LevelStorageSource.LevelStorageAccess levelStorage,
      final ServerLevelData levelData,
      final ResourceKey<Level> dimension,
      final LevelStem levelStem,
      final boolean isDebug,
      final long biomeZoomSeed,
      final List<CustomSpawner> customSpawners,
      final boolean tickTime
   ) {
      super(levelData, dimension, server.registryAccess(), levelStem.type(), false, isDebug, biomeZoomSeed, server.getMaxChainedNeighborUpdates());
      this.tickTime = tickTime;
      this.server = server;
      this.customSpawners = customSpawners;
      this.serverLevelData = levelData;
      ChunkGenerator generator = levelStem.generator();
      boolean syncWrites = server.forceSynchronousWrites();
      DataFixer fixerUpper = server.getFixerUpper();
      EntityPersistentStorage<Entity> entityStorage = new EntityStorage(
         new SimpleRegionStorage(
            new RegionStorageInfo(levelStorage.getLevelId(), dimension, "entities"),
            levelStorage.getDimensionPath(dimension).resolve("entities"),
            fixerUpper,
            syncWrites,
            DataFixTypes.ENTITY_CHUNK
         ),
         this,
         server
      );
      this.entityManager = new PersistentEntitySectionManager<>(Entity.class, new ServerLevel.EntityCallbacks(), entityStorage);
      this.chunkSource = new ServerChunkCache(
         this,
         levelStorage,
         fixerUpper,
         server.getStructureManager(),
         executor,
         generator,
         server.getPlayerList().getViewDistance(),
         server.getPlayerList().getSimulationDistance(),
         syncWrites,
         this.entityManager::updateChunkStatus
      );
      this.chunkSource.getGeneratorState().ensureStructuresGenerated();
      this.portalForcer = new PortalForcer(this);
      if (this.canHaveWeather()) {
         this.prepareWeather(server.getWeatherData());
      }

      this.raids = this.getDataStorage().computeIfAbsent(Raids.TYPE);
      if (!server.isSingleplayer()) {
         levelData.setGameType(server.getDefaultGameType());
      }

      WorldGenSettings worldGenSettings = server.getWorldGenSettings();
      WorldOptions options = worldGenSettings.options();
      long seed = options.seed();
      this.structureCheck = new StructureCheck(
         this.chunkSource.chunkScanner(),
         this.registryAccess(),
         server.getStructureManager(),
         dimension,
         generator,
         this.chunkSource.randomState(),
         this,
         generator.getBiomeSource(),
         seed,
         fixerUpper
      );
      this.structureManager = new StructureManager(this, options, this.structureCheck);
      if (this.dimensionType().hasEnderDragonFight()) {
         this.dragonFight = this.getDataStorage().computeIfAbsent(EnderDragonFight.TYPE);
         this.dragonFight.init(this, seed, BlockPos.ZERO);
      }

      this.sleepStatus = new SleepStatus();
      this.gameEventDispatcher = new GameEventDispatcher(this);
      this.waypointManager = new ServerWaypointManager();
      this.environmentAttributes = EnvironmentAttributeSystem.builder().addDefaultLayers(this).build();
      this.updateSkyBrightness();
   }

   @Override
   public int getNextEntityId() {
      int id = 0;

      while (id == 0 || this.chunkSource.hasEntityWithId(id)) {
         id = ENTITY_COUNTER.incrementAndGet();
      }

      return id;
   }

   @Deprecated
   @VisibleForTesting
   public void setDragonFight(final @Nullable EnderDragonFight fight) {
      this.dragonFight = fight;
   }

   @Override
   public Holder<Biome> getUncachedNoiseBiome(final int quartX, final int quartY, final int quartZ) {
      return this.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(quartX, quartY, quartZ, this.getChunkSource().randomState().sampler());
   }

   public StructureManager structureManager() {
      return this.structureManager;
   }

   public ServerClockManager clockManager() {
      return this.server.clockManager();
   }

   @Override
   public EnvironmentAttributeSystem environmentAttributes() {
      return this.environmentAttributes;
   }

   @Deprecated
   @VisibleForTesting
   public EnvironmentAttributeSystem setEnvironmentAttributes(final EnvironmentAttributeSystem environmentAttributes) {
      EnvironmentAttributeSystem previous = this.environmentAttributes;
      this.environmentAttributes = environmentAttributes;
      return previous;
   }

   public void tick(final BooleanSupplier haveTime) {
      ProfilerFiller profiler = Profiler.get();
      this.handlingTick = true;
      this.environmentAttributes().invalidateTickCache();
      TickRateManager tickRateManager = this.tickRateManager();
      boolean runs = tickRateManager.runsNormally();
      if (runs) {
         profiler.push("world border");
         this.getWorldBorder().tick();
         profiler.popPush("weather");
         this.advanceWeatherCycle();
         profiler.pop();
      }

      int percentage = this.getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
      if (this.sleepStatus.areEnoughSleeping(percentage) && this.sleepStatus.areEnoughDeepSleeping(percentage, this.players)) {
         Optional<Holder<WorldClock>> defaultClock = this.dimensionType().defaultClock();
         if (this.getGameRules().get(GameRules.ADVANCE_TIME) && defaultClock.isPresent()) {
            this.server.clockManager().moveToTimeMarker(defaultClock.get(), ClockTimeMarkers.WAKE_UP_FROM_SLEEP);
         }

         this.wakeUpAllPlayers();
         if (this.getGameRules().get(GameRules.ADVANCE_WEATHER) && this.isRaining()) {
            this.resetWeatherCycle();
         }
      }

      this.updateSkyBrightness();
      if (runs) {
         this.tickTime();
      }

      profiler.push("tickPending");
      if (!this.isDebug() && runs) {
         long tick = this.getGameTime();
         profiler.push("blockTicks");
         this.blockTicks.tick(tick, 65536, this::tickBlock);
         profiler.popPush("fluidTicks");
         this.fluidTicks.tick(tick, 65536, this::tickFluid);
         profiler.pop();
      }

      profiler.popPush("raid");
      if (runs) {
         this.raids.tick(this);
      }

      profiler.popPush("chunkSource");
      this.getChunkSource().tick(haveTime, true);
      profiler.popPush("blockEvents");
      if (runs) {
         this.runBlockEvents();
      }

      this.handlingTick = false;
      profiler.pop();
      boolean isActive = this.chunkSource.hasActiveTickets();
      if (isActive) {
         this.resetEmptyTime();
      }

      if (runs) {
         this.emptyTime++;
      }

      if (this.emptyTime < 300) {
         profiler.push("entities");
         if (this.dragonFight != null && runs) {
            profiler.push("dragonFight");
            this.dragonFight.tick();
            profiler.pop();
         }

         this.entityTickList.forEach(entity -> {
            if (!entity.isRemoved()) {
               if (!tickRateManager.isEntityFrozen(entity)) {
                  profiler.push("checkDespawn");
                  entity.checkDespawn();
                  profiler.pop();
                  if (entity instanceof ServerPlayer || this.chunkSource.chunkMap.getDistanceManager().inEntityTickingRange(entity.chunkPosition().pack())) {
                     Entity vehicle = entity.getVehicle();
                     if (vehicle != null) {
                        if (!vehicle.isRemoved() && vehicle.hasPassenger(entity)) {
                           return;
                        }

                        entity.stopRiding();
                     }

                     profiler.push("tick");
                     this.guardEntityTick(this::tickNonPassenger, entity);
                     profiler.pop();
                  }
               }
            }
         });
         profiler.popPush("blockEntities");
         this.tickBlockEntities();
         profiler.pop();
      }

      profiler.push("entityManagement");
      this.entityManager.tick();
      profiler.pop();
      profiler.push("debugSynchronizers");
      if (this.debugSynchronizers.hasAnySubscriberFor(DebugSubscriptions.NEIGHBOR_UPDATES)) {
         this.neighborUpdater
            .setDebugListener(blockPos -> this.debugSynchronizers.broadcastEventToTracking(blockPos, DebugSubscriptions.NEIGHBOR_UPDATES, blockPos));
      } else {
         this.neighborUpdater.setDebugListener(null);
      }

      this.debugSynchronizers.tick(this.server.debugSubscribers());
      profiler.pop();
   }

   @Override
   public boolean shouldTickBlocksAt(final long chunkPos) {
      return this.chunkSource.chunkMap.getDistanceManager().inBlockTickingRange(chunkPos);
   }

   protected void tickTime() {
      if (this.tickTime) {
         long time = this.levelData.getGameTime() + 1L;
         this.serverLevelData.setGameTime(time);
         Profiler.get().push("scheduledFunctions");
         this.server.getScheduledEvents().tick(this.server, time);
         Profiler.get().pop();
      }
   }

   public void tickCustomSpawners(final boolean spawnEnemies) {
      for (CustomSpawner spawner : this.customSpawners) {
         spawner.tick(this, spawnEnemies);
      }
   }

   private void wakeUpAllPlayers() {
      this.sleepStatus.removeAllSleepers();
      this.players.stream().filter(LivingEntity::isSleeping).collect(Collectors.toList()).forEach(player -> player.stopSleepInBed(false, false));
   }

   public void tickChunk(final LevelChunk chunk, final int tickSpeed) {
      ChunkPos chunkPos = chunk.getPos();
      int minX = chunkPos.getMinBlockX();
      int minZ = chunkPos.getMinBlockZ();
      ProfilerFiller profiler = Profiler.get();
      profiler.push("iceandsnow");

      for (int i = 0; i < tickSpeed; i++) {
         if (this.random.nextInt(48) == 0) {
            this.tickPrecipitation(this.getBlockRandomPos(minX, 0, minZ, 15));
         }
      }

      profiler.popPush("tickBlocks");
      if (tickSpeed > 0) {
         LevelChunkSection[] sections = chunk.getSections();

         for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section.isRandomlyTicking()) {
               int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
               int minYInSection = SectionPos.sectionToBlockCoord(sectionY);

               for (int i = 0; i < tickSpeed; i++) {
                  BlockPos pos = this.getBlockRandomPos(minX, minYInSection, minZ, 15);
                  profiler.push("randomTick");
                  BlockState blockState = section.getBlockState(pos.getX() - minX, pos.getY() - minYInSection, pos.getZ() - minZ);
                  if (blockState.isRandomlyTicking()) {
                     blockState.randomTick(this, pos, this.random);
                  }

                  FluidState fluidState = blockState.getFluidState();
                  if (fluidState.isRandomlyTicking()) {
                     fluidState.randomTick(this, pos, this.random);
                  }

                  profiler.pop();
               }
            }
         }
      }

      profiler.pop();
   }

   public void tickThunder(final LevelChunk chunk) {
      ChunkPos chunkPos = chunk.getPos();
      boolean raining = this.isRaining();
      int minX = chunkPos.getMinBlockX();
      int minZ = chunkPos.getMinBlockZ();
      ProfilerFiller profiler = Profiler.get();
      profiler.push("thunder");
      if (raining && this.isThundering() && this.random.nextInt(100000) == 0) {
         BlockPos pos = this.findLightningTargetAround(this.getBlockRandomPos(minX, 0, minZ, 15));
         if (this.isRainingAt(pos)) {
            DifficultyInstance difficulty = this.getCurrentDifficultyAt(pos);
            boolean isTrap = this.getGameRules().get(GameRules.SPAWN_MOBS)
               && this.random.nextDouble() < difficulty.getEffectiveDifficulty() * 0.01
               && !this.getBlockState(pos.below()).is(BlockTags.LIGHTNING_RODS);
            if (isTrap) {
               SkeletonHorse horse = EntityTypes.SKELETON_HORSE.create(this, EntitySpawnReason.EVENT);
               if (horse != null) {
                  horse.setTrap(true);
                  horse.setAge(0);
                  horse.setPos(pos.getX(), pos.getY(), pos.getZ());
                  this.addFreshEntity(horse);
               }
            }

            LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(this, EntitySpawnReason.EVENT);
            if (bolt != null) {
               bolt.snapTo(Vec3.atBottomCenterOf(pos));
               bolt.setVisualOnly(isTrap);
               this.addFreshEntity(bolt);
            }
         }
      }

      profiler.pop();
   }

   @VisibleForTesting
   public void tickPrecipitation(final BlockPos pos) {
      BlockPos topPos = this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos);
      BlockPos belowPos = topPos.below();
      Biome biome = this.getBiome(topPos).value();
      if (biome.shouldFreeze(this, belowPos)) {
         this.setBlockAndUpdate(belowPos, Blocks.ICE.defaultBlockState());
      }

      if (this.isRaining()) {
         int maxHeight = this.getGameRules().get(GameRules.MAX_SNOW_ACCUMULATION_HEIGHT);
         if (maxHeight > 0 && biome.shouldSnow(this, topPos)) {
            BlockState state = this.getBlockState(topPos);
            if (state.is(Blocks.SNOW)) {
               int currentLayers = state.getValue(SnowLayerBlock.LAYERS);
               if (currentLayers < Math.min(maxHeight, 8)) {
                  BlockState newState = state.setValue(SnowLayerBlock.LAYERS, currentLayers + 1);
                  Block.pushEntitiesUp(state, newState, this, topPos);
                  this.setBlockAndUpdate(topPos, newState);
               }
            } else {
               this.setBlockAndUpdate(topPos, Blocks.SNOW.defaultBlockState());
            }
         }

         Biome.Precipitation precipitation = biome.getPrecipitationAt(belowPos, this.getSeaLevel());
         if (precipitation != Biome.Precipitation.NONE) {
            BlockState belowState = this.getBlockState(belowPos);
            belowState.getBlock().handlePrecipitation(belowState, this, belowPos, precipitation);
         }
      }
   }

   private Optional<BlockPos> findLightningRod(final BlockPos center) {
      Optional<BlockPos> nearbyLightningRod = this.getPoiManager()
         .findClosest(
            p -> p.is(PoiTypes.LIGHTNING_ROD),
            lightningRodPos -> lightningRodPos.getY() == this.getHeight(Heightmap.Types.WORLD_SURFACE, lightningRodPos.getX(), lightningRodPos.getZ()) - 1,
            center,
            128,
            PoiManager.Occupancy.ANY
         );
      return nearbyLightningRod.map(blockPos -> blockPos.above(1));
   }

   protected BlockPos findLightningTargetAround(final BlockPos pos) {
      BlockPos center = this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos);
      Optional<BlockPos> lightningRodTarget = this.findLightningRod(center);
      if (lightningRodTarget.isPresent()) {
         return lightningRodTarget.get();
      }

      AABB search = AABB.encapsulatingFullBlocks(center, center.atY(this.getMaxY() + 1)).inflate(3.0);
      List<LivingEntity> entities = this.getEntitiesOfClass(LivingEntity.class, search, input -> input.isAlive() && this.canSeeSky(input.blockPosition()));
      if (!entities.isEmpty()) {
         return entities.get(this.random.nextInt(entities.size())).blockPosition();
      }

      if (center.getY() == this.getMinY() - 1) {
         center = center.above(2);
      }

      return center;
   }

   public boolean isHandlingTick() {
      return this.handlingTick;
   }

   public boolean canSleepThroughNights() {
      return this.getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE) <= 100;
   }

   private void announceSleepStatus() {
      if (this.canSleepThroughNights()) {
         if (!this.getServer().isSingleplayer() || this.getServer().isPublished()) {
            int percentage = this.getGameRules().get(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
            Component message;
            if (this.sleepStatus.areEnoughSleeping(percentage)) {
               message = Component.translatable("sleep.skipping_night");
            } else {
               message = Component.translatable("sleep.players_sleeping", this.sleepStatus.amountSleeping(), this.sleepStatus.sleepersNeeded(percentage));
            }

            for (ServerPlayer player : this.players) {
               player.sendOverlayMessage(message);
            }
         }
      }
   }

   public void updateSleepingPlayerList() {
      if (!this.players.isEmpty() && this.sleepStatus.update(this.players)) {
         this.announceSleepStatus();
      }
   }

   public ServerScoreboard getScoreboard() {
      return this.server.getScoreboard();
   }

   public ServerWaypointManager getWaypointManager() {
      return this.waypointManager;
   }

   @Override
   public DifficultyInstance getCurrentDifficultyAt(final BlockPos pos) {
      long localTime = 0L;
      float moonBrightness = 0.0F;
      ChunkAccess chunk = this.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()), ChunkStatus.FULL, false);
      if (chunk != null) {
         localTime = chunk.getInhabitedTime();
         moonBrightness = this.getMoonBrightness(pos);
      }

      return new DifficultyInstance(this.getDifficulty(), this.getOverworldClockTime(), localTime, moonBrightness);
   }

   public float getMoonBrightness(final BlockPos pos) {
      MoonPhase moonPhase = this.environmentAttributes.getValue(EnvironmentAttributes.MOON_PHASE, pos);
      return DimensionType.MOON_BRIGHTNESS_PER_PHASE[moonPhase.index()];
   }

   private void prepareWeather(final WeatherData weatherData) {
      if (weatherData.isRaining()) {
         this.rainLevel = 1.0F;
         if (weatherData.isThundering()) {
            this.thunderLevel = 1.0F;
         }
      }
   }

   private void advanceWeatherCycle() {
      boolean wasRaining = this.isRaining();
      if (this.canHaveWeather()) {
         WeatherData weatherData = this.getWeatherData();
         if (this.getGameRules().get(GameRules.ADVANCE_WEATHER)) {
            int clearWeatherTime = weatherData.getClearWeatherTime();
            int thunderTime = weatherData.getThunderTime();
            int rainTime = weatherData.getRainTime();
            boolean thundering = weatherData.isThundering();
            boolean raining = weatherData.isRaining();
            if (clearWeatherTime > 0) {
               clearWeatherTime--;
               thunderTime = thundering ? 0 : 1;
               rainTime = raining ? 0 : 1;
               thundering = false;
               raining = false;
            } else {
               if (thunderTime > 0) {
                  if (--thunderTime == 0) {
                     thundering = !thundering;
                  }
               } else if (thundering) {
                  thunderTime = THUNDER_DURATION.sample(this.random);
               } else {
                  thunderTime = THUNDER_DELAY.sample(this.random);
               }

               if (rainTime > 0) {
                  if (--rainTime == 0) {
                     raining = !raining;
                  }
               } else if (raining) {
                  rainTime = RAIN_DURATION.sample(this.random);
               } else {
                  rainTime = RAIN_DELAY.sample(this.random);
               }
            }

            weatherData.setThunderTime(thunderTime);
            weatherData.setRainTime(rainTime);
            weatherData.setClearWeatherTime(clearWeatherTime);
            weatherData.setThundering(thundering);
            weatherData.setRaining(raining);
         }

         this.oThunderLevel = this.thunderLevel;
         if (weatherData.isThundering()) {
            this.thunderLevel += 0.01F;
         } else {
            this.thunderLevel -= 0.01F;
         }

         this.thunderLevel = Mth.clamp(this.thunderLevel, 0.0F, 1.0F);
         this.oRainLevel = this.rainLevel;
         if (weatherData.isRaining()) {
            this.rainLevel += 0.01F;
         } else {
            this.rainLevel -= 0.01F;
         }

         this.rainLevel = Mth.clamp(this.rainLevel, 0.0F, 1.0F);
      }

      if (this.oRainLevel != this.rainLevel) {
         this.server
            .getPlayerList()
            .broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.rainLevel), this.dimension());
      }

      if (this.oThunderLevel != this.thunderLevel) {
         this.server
            .getPlayerList()
            .broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, this.thunderLevel), this.dimension());
      }

      if (wasRaining != this.isRaining()) {
         if (wasRaining) {
            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0.0F));
         } else {
            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F));
         }

         this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.rainLevel));
         this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, this.thunderLevel));
      }
   }

   @VisibleForTesting
   public void resetWeatherCycle() {
      WeatherData weatherData = this.getWeatherData();
      weatherData.setRainTime(0);
      weatherData.setRaining(false);
      weatherData.setThunderTime(0);
      weatherData.setThundering(false);
   }

   public void resetEmptyTime() {
      this.emptyTime = 0;
   }

   private void tickFluid(final BlockPos pos, final Fluid type) {
      BlockState blockState = this.getBlockState(pos);
      FluidState fluidState = blockState.getFluidState();
      if (fluidState.is(type)) {
         fluidState.tick(this, pos, blockState);
      }
   }

   private void tickBlock(final BlockPos pos, final Block type) {
      BlockState state = this.getBlockState(pos);
      if (state.is(type)) {
         state.tick(this, pos, this.random);
      }
   }

   public void tickNonPassenger(final Entity entity) {
      entity.setOldPosAndRot();
      ProfilerFiller profiler = Profiler.get();
      entity.tickCount++;
      profiler.push(entity.typeHolder()::getRegisteredName);
      profiler.incrementCounter("tickNonPassenger");
      entity.tick();
      profiler.pop();

      for (Entity passenger : entity.getPassengers()) {
         this.tickPassenger(entity, passenger);
      }
   }

   private void tickPassenger(final Entity vehicle, final Entity entity) {
      if (entity.isRemoved() || entity.getVehicle() != vehicle) {
         entity.stopRiding();
      } else if (entity instanceof Player || this.entityTickList.contains(entity)) {
         entity.setOldPosAndRot();
         entity.tickCount++;
         ProfilerFiller profiler = Profiler.get();
         profiler.push(entity.typeHolder()::getRegisteredName);
         profiler.incrementCounter("tickPassenger");
         entity.rideTick();
         profiler.pop();

         for (Entity passenger : entity.getPassengers()) {
            this.tickPassenger(entity, passenger);
         }
      }
   }

   public void updateNeighboursOnBlockSet(final BlockPos pos, final BlockState oldState) {
      BlockState blockState = this.getBlockState(pos);
      Block newBlock = blockState.getBlock();
      boolean blockChanged = !oldState.is(newBlock);
      if (blockChanged) {
         oldState.affectNeighborsAfterRemoval(this, pos, false);
      }

      this.updateNeighborsAt(pos, blockState.getBlock());
      if (blockState.hasAnalogOutputSignal()) {
         this.updateNeighbourForOutputSignal(pos, newBlock);
      }
   }

   @Override
   public boolean mayInteract(final Entity entity, final BlockPos pos) {
      return !(entity instanceof Player player && (this.server.isUnderSpawnProtection(this, pos, player) || !this.getWorldBorder().isWithinBounds(pos)));
   }

   public void save(final @Nullable ProgressListener progressListener, final boolean flush, final boolean noSave) {
      ServerChunkCache chunkSource = this.getChunkSource();
      if (!noSave) {
         if (progressListener != null) {
            progressListener.progressStartNoAbort(Component.translatable("menu.savingLevel"));
         }

         this.saveLevelData(flush);
         if (progressListener != null) {
            progressListener.progressStage(Component.translatable("menu.savingChunks"));
         }

         chunkSource.save(flush);
         if (flush) {
            this.entityManager.saveAll();
         } else {
            this.entityManager.autoSave();
         }
      }
   }

   private void saveLevelData(final boolean sync) {
      SavedDataStorage savedDataStorage = this.getChunkSource().getDataStorage();
      if (sync) {
         savedDataStorage.saveAndJoin();
      } else {
         savedDataStorage.scheduleSave();
      }
   }

   public <T extends Entity> List<? extends T> getEntities(final EntityTypeTest<Entity, T> type, final Predicate<? super T> selector) {
      List<T> result = Lists.newArrayList();
      this.getEntities(type, selector, result);
      return result;
   }

   public <T extends Entity> void getEntities(final EntityTypeTest<Entity, T> type, final Predicate<? super T> selector, final List<? super T> result) {
      this.getEntities(type, selector, result, Integer.MAX_VALUE);
   }

   public <T extends Entity> void getEntities(
      final EntityTypeTest<Entity, T> type, final Predicate<? super T> selector, final List<? super T> result, final int maxResults
   ) {
      this.getEntities().get(type, entity -> {
         if (selector.test(entity)) {
            result.add(entity);
            if (result.size() >= maxResults) {
               return Continuation.ABORT;
            }
         }

         return Continuation.CONTINUE;
      });
   }

   public List<? extends EnderDragon> getDragons() {
      return this.getEntities(EntityTypes.ENDER_DRAGON, LivingEntity::isAlive);
   }

   public List<ServerPlayer> getPlayers(final Predicate<? super ServerPlayer> selector) {
      return this.getPlayers(selector, Integer.MAX_VALUE);
   }

   public List<ServerPlayer> getPlayers(final Predicate<? super ServerPlayer> selector, final int maxResults) {
      List<ServerPlayer> result = Lists.newArrayList();

      for (ServerPlayer player : this.players) {
         if (selector.test(player)) {
            result.add(player);
            if (result.size() >= maxResults) {
               return result;
            }
         }
      }

      return result;
   }

   public @Nullable ServerPlayer getRandomPlayer() {
      List<ServerPlayer> players = this.getPlayers(LivingEntity::isAlive);
      return players.isEmpty() ? null : players.get(this.random.nextInt(players.size()));
   }

   @Override
   public boolean addFreshEntity(final Entity entity) {
      return this.addEntity(entity);
   }

   public boolean addWithUUID(final Entity entity) {
      return this.addEntity(entity);
   }

   public void addDuringTeleport(final Entity entity) {
      if (entity instanceof ServerPlayer player) {
         this.addPlayer(player);
      } else {
         this.addEntity(entity);
      }
   }

   public void addNewPlayer(final ServerPlayer player) {
      this.addPlayer(player);
   }

   public void addRespawnedPlayer(final ServerPlayer player) {
      this.addPlayer(player);
   }

   private void addPlayer(final ServerPlayer player) {
      Entity existing = this.getEntity(player.getUUID());
      if (existing != null) {
         LOGGER.warn("Force-added player with duplicate UUID {}", player.getUUID());
         existing.unRide();
         this.removePlayerImmediately((ServerPlayer)existing, Entity.RemovalReason.DISCARDED);
      }

      this.entityManager.addNewEntity(player);
   }

   private boolean addEntity(final Entity entity) {
      if (entity.isRemoved()) {
         LOGGER.warn("Tried to add entity {} but it was marked as removed already", entity.typeHolder().getRegisteredName());
         return false;
      } else {
         return this.entityManager.addNewEntity(entity);
      }
   }

   public boolean tryAddFreshEntityWithPassengers(final Entity entity) {
      if (entity.getSelfAndPassengers().map(Entity::getUUID).anyMatch(this.entityManager::isLoaded)) {
         return false;
      }

      this.addFreshEntityWithPassengers(entity);
      return true;
   }

   public void unload(final LevelChunk levelChunk) {
      levelChunk.clearAllBlockEntities();
      levelChunk.unregisterTickContainerFromLevel(this);
      this.debugSynchronizers.dropChunk(levelChunk.getPos());
   }

   public void removePlayerImmediately(final ServerPlayer player, final Entity.RemovalReason reason) {
      player.remove(reason);
   }

   @Override
   public void destroyBlockProgress(final int id, final BlockPos blockPos, final int progress) {
      for (ServerPlayer player : this.server.getPlayerList().getPlayers()) {
         if (player.level() == this && player.getId() != id) {
            double xd = blockPos.getX() - player.getX();
            double yd = blockPos.getY() - player.getY();
            double zd = blockPos.getZ() - player.getZ();
            if (xd * xd + yd * yd + zd * zd < 1024.0) {
               player.connection.send(new ClientboundBlockDestructionPacket(id, blockPos, progress));
            }
         }
      }
   }

   @Override
   public void playSeededSound(
      final @Nullable Entity except,
      final double x,
      final double y,
      final double z,
      final Holder<SoundEvent> sound,
      final SoundSource source,
      final float volume,
      final float pitch,
      final long seed
   ) {
      this.server
         .getPlayerList()
         .broadcast(
            except instanceof Player player ? player : null,
            x,
            y,
            z,
            sound.value().getRange(volume),
            this.dimension(),
            new ClientboundSoundPacket(sound, source, x, y, z, volume, pitch, seed)
         );
   }

   @Override
   public void playSeededSound(
      final @Nullable Entity except,
      final Entity sourceEntity,
      final Holder<SoundEvent> sound,
      final SoundSource source,
      final float volume,
      final float pitch,
      final long seed
   ) {
      this.server
         .getPlayerList()
         .broadcast(
            except instanceof Player player ? player : null,
            sourceEntity.getX(),
            sourceEntity.getY(),
            sourceEntity.getZ(),
            sound.value().getRange(volume),
            this.dimension(),
            new ClientboundSoundEntityPacket(sound, source, sourceEntity, volume, pitch, seed)
         );
   }

   @Override
   public void globalLevelEvent(final int type, final BlockPos pos, final int data) {
      if (this.getGameRules().get(GameRules.GLOBAL_SOUND_EVENTS)) {
         this.server.getPlayerList().getPlayers().forEach(player -> {
            Vec3 soundPos;
            if (player.level() == this) {
               Vec3 centerOfBlock = Vec3.atCenterOf(pos);
               if (player.distanceToSqr(centerOfBlock) < Mth.square(32)) {
                  soundPos = centerOfBlock;
               } else {
                  Vec3 directionToEvent = centerOfBlock.subtract(player.position()).normalize();
                  soundPos = player.position().add(directionToEvent.scale(32.0));
               }
            } else {
               soundPos = player.position();
            }

            player.connection.send(new ClientboundLevelEventPacket(type, BlockPos.containing(soundPos), data, true));
         });
      } else {
         this.levelEvent(null, type, pos, data);
      }
   }

   @Override
   public void levelEvent(final @Nullable Entity source, final int type, final BlockPos pos, final int data) {
      this.server
         .getPlayerList()
         .broadcast(
            source instanceof Player player ? player : null,
            pos.getX(),
            pos.getY(),
            pos.getZ(),
            64.0,
            this.dimension(),
            new ClientboundLevelEventPacket(type, pos, data, false)
         );
   }

   public int getLogicalHeight() {
      return this.dimensionType().logicalHeight();
   }

   @Override
   public void gameEvent(final Holder<GameEvent> gameEvent, final Vec3 position, final GameEvent.Context context) {
      this.gameEventDispatcher.post(gameEvent, position, context);
   }

   @Override
   public void sendBlockUpdated(final BlockPos pos, final BlockState old, final BlockState current, final @Block.UpdateFlags int updateFlags) {
      if (this.isUpdatingNavigations) {
         String message = "recursive call to sendBlockUpdated";
         Util.logAndPauseIfInIde("recursive call to sendBlockUpdated", new IllegalStateException("recursive call to sendBlockUpdated"));
      }

      this.getChunkSource().blockChanged(pos);
      this.pathTypesByPosCache.invalidate(pos);
      VoxelShape oldShape = old.getCollisionShape(this, pos);
      VoxelShape newShape = current.getCollisionShape(this, pos);
      if (Shapes.joinIsNotEmpty(oldShape, newShape, BooleanOp.NOT_SAME)) {
         List<PathNavigation> navigationsToUpdate = new ObjectArrayList();

         for (Mob navigatingMob : this.navigatingMobs) {
            PathNavigation pathNavigation = navigatingMob.getNavigation();
            if (pathNavigation.shouldRecomputePath(pos)) {
               navigationsToUpdate.add(pathNavigation);
            }
         }

         try {
            this.isUpdatingNavigations = true;

            for (PathNavigation navigation : navigationsToUpdate) {
               navigation.recomputePath();
            }
         } finally {
            this.isUpdatingNavigations = false;
         }
      }
   }

   @Override
   public void updateNeighborsAt(final BlockPos pos, final Block sourceBlock) {
      this.updateNeighborsAt(pos, sourceBlock, ExperimentalRedstoneUtils.initialOrientation(this, null, null));
   }

   @Override
   public void updateNeighborsAt(final BlockPos pos, final Block sourceBlock, final @Nullable Orientation orientation) {
      this.neighborUpdater.updateNeighborsAtExceptFromFacing(pos, sourceBlock, null, orientation);
   }

   @Override
   public void updateNeighborsAtExceptFromFacing(
      final BlockPos pos, final Block blockObject, final Direction skipDirection, final @Nullable Orientation orientation
   ) {
      this.neighborUpdater.updateNeighborsAtExceptFromFacing(pos, blockObject, skipDirection, orientation);
   }

   @Override
   public void neighborChanged(final BlockPos pos, final Block changedBlock, final @Nullable Orientation orientation) {
      this.neighborUpdater.neighborChanged(pos, changedBlock, orientation);
   }

   @Override
   public void neighborChanged(
      final BlockState state, final BlockPos pos, final Block changedBlock, final @Nullable Orientation orientation, final boolean movedByPiston
   ) {
      this.neighborUpdater.neighborChanged(state, pos, changedBlock, orientation, movedByPiston);
   }

   @Override
   public void broadcastEntityEvent(final Entity entity, final byte event) {
      this.getChunkSource().sendToTrackingPlayersAndSelf(entity, new ClientboundEntityEventPacket(entity, event));
   }

   @Override
   public void broadcastDamageEvent(final Entity entity, final DamageSource source) {
      this.getChunkSource().sendToTrackingPlayersAndSelf(entity, new ClientboundDamageEventPacket(entity, source));
   }

   public ServerChunkCache getChunkSource() {
      return this.chunkSource;
   }

   @Override
   public void explode(
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
   ) {
      Explosion.BlockInteraction blockInteraction = switch (interactionType) {
         case NONE -> Explosion.BlockInteraction.KEEP;
         case BLOCK -> this.getDestroyType(GameRules.BLOCK_EXPLOSION_DROP_DECAY);
         case MOB -> this.getGameRules().get(GameRules.MOB_GRIEFING)
            ? this.getDestroyType(GameRules.MOB_EXPLOSION_DROP_DECAY)
            : Explosion.BlockInteraction.KEEP;
         case TNT -> this.getDestroyType(GameRules.TNT_EXPLOSION_DROP_DECAY);
         case TRIGGER -> Explosion.BlockInteraction.TRIGGER_BLOCK;
      };
      Vec3 center = new Vec3(x, y, z);
      ServerExplosion explosion = new ServerExplosion(this, source, damageSource, damageCalculator, center, r, fire, blockInteraction);
      int blockCount = explosion.explode();
      ParticleOptions explosionParticle = explosion.isSmall() ? smallExplosionParticles : largeExplosionParticles;

      for (ServerPlayer player : this.players) {
         if (player.distanceToSqr(center) < 4096.0) {
            Optional<Vec3> playerKnockback = Optional.ofNullable(explosion.getHitPlayers().get(player));
            player.connection
               .send(
                  new ClientboundExplodePacket(
                     center, r, blockCount, playerKnockback, explosionParticle, explosionSound, blockParticles, source == null || !source.isSilent()
                  )
               );
         }
      }
   }

   private Explosion.BlockInteraction getDestroyType(final GameRule<Boolean> gameRule) {
      return this.getGameRules().get(gameRule) ? Explosion.BlockInteraction.DESTROY_WITH_DECAY : Explosion.BlockInteraction.DESTROY;
   }

   @Override
   public void blockEvent(final BlockPos pos, final Block block, final int b0, final int b1) {
      this.blockEvents.add(new BlockEventData(pos, block, b0, b1));
   }

   private void runBlockEvents() {
      this.blockEventsToReschedule.clear();

      while (!this.blockEvents.isEmpty()) {
         BlockEventData eventData = (BlockEventData)this.blockEvents.removeFirst();
         if (this.shouldTickBlocksAt(eventData.pos())) {
            if (this.doBlockEvent(eventData)) {
               this.server
                  .getPlayerList()
                  .broadcast(
                     null,
                     eventData.pos().getX(),
                     eventData.pos().getY(),
                     eventData.pos().getZ(),
                     64.0,
                     this.dimension(),
                     new ClientboundBlockEventPacket(eventData.pos(), eventData.block(), eventData.paramA(), eventData.paramB())
                  );
            }
         } else {
            this.blockEventsToReschedule.add(eventData);
         }
      }

      this.blockEvents.addAll(this.blockEventsToReschedule);
   }

   private boolean doBlockEvent(final BlockEventData eventData) {
      BlockState state = this.getBlockState(eventData.pos());
      return state.is(eventData.block()) ? state.triggerEvent(this, eventData.pos(), eventData.paramA(), eventData.paramB()) : false;
   }

   public LevelTicks<Block> getBlockTicks() {
      return this.blockTicks;
   }

   public LevelTicks<Fluid> getFluidTicks() {
      return this.fluidTicks;
   }

   @Override
   public MinecraftServer getServer() {
      return this.server;
   }

   public PortalForcer getPortalForcer() {
      return this.portalForcer;
   }

   public StructureTemplateManager getStructureManager() {
      return this.server.getStructureManager();
   }

   public <T extends ParticleOptions> int sendParticles(
      final T particle,
      final double x,
      final double y,
      final double z,
      final int count,
      final double xDist,
      final double yDist,
      final double zDist,
      final double speed
   ) {
      return this.sendParticles(particle, false, false, x, y, z, count, xDist, yDist, zDist, speed);
   }

   public <T extends ParticleOptions> int sendParticles(
      final T particle,
      final boolean overrideLimiter,
      final boolean alwaysShow,
      final double x,
      final double y,
      final double z,
      final int count,
      final double xDist,
      final double yDist,
      final double zDist,
      final double speed
   ) {
      ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
         particle, overrideLimiter, alwaysShow, x, y, z, (float)xDist, (float)yDist, (float)zDist, (float)speed, count
      );
      int result = 0;

      for (int i = 0; i < this.players.size(); i++) {
         ServerPlayer player = this.players.get(i);
         if (this.sendParticles(player, overrideLimiter, x, y, z, packet)) {
            result++;
         }
      }

      return result;
   }

   public <T extends ParticleOptions> boolean sendParticles(
      final ServerPlayer player,
      final T particle,
      final boolean overrideLimiter,
      final boolean alwaysShow,
      final double x,
      final double y,
      final double z,
      final int count,
      final double xDist,
      final double yDist,
      final double zDist,
      final double speed
   ) {
      Packet<?> packet = new ClientboundLevelParticlesPacket(
         particle, overrideLimiter, alwaysShow, x, y, z, (float)xDist, (float)yDist, (float)zDist, (float)speed, count
      );
      return this.sendParticles(player, overrideLimiter, x, y, z, packet);
   }

   private boolean sendParticles(
      final ServerPlayer player, final boolean overrideLimiter, final double x, final double y, final double z, final Packet<?> packet
   ) {
      if (player.level() != this) {
         return false;
      } else {
         BlockPos pos = player.blockPosition();
         if (pos.closerToCenterThan(new Vec3(x, y, z), overrideLimiter ? 512.0 : 32.0)) {
            player.connection.send(packet);
            return true;
         } else {
            return false;
         }
      }
   }

   @Override
   public @Nullable Entity getEntity(final int id) {
      return this.getEntities().get(id);
   }

   @Override
   public @Nullable Entity getEntityInAnyDimension(final UUID uuid) {
      Entity entity = this.getEntity(uuid);
      if (entity != null) {
         return entity;
      }

      for (ServerLevel otherLevel : this.getServer().getAllLevels()) {
         if (otherLevel != this) {
            Entity otherEntity = otherLevel.getEntity(uuid);
            if (otherEntity != null) {
               return otherEntity;
            }
         }
      }

      return null;
   }

   @Override
   public @Nullable Player getPlayerInAnyDimension(final UUID uuid) {
      return this.getServer().getPlayerList().getPlayer(uuid);
   }

   @Deprecated
   public @Nullable Entity getEntityOrPart(final int id) {
      Entity entity = this.getEntities().get(id);
      return entity != null ? entity : (Entity)this.dragonParts.get(id);
   }

   @Override
   public Collection<EnderDragonPart> dragonParts() {
      return this.dragonParts.values();
   }

   public @Nullable BlockPos findNearestMapStructure(
      final TagKey<Structure> structureTag, final BlockPos origin, final int maxSearchRadius, final boolean createReference
   ) {
      Optional<HolderSet.Named<Structure>> tag = this.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(structureTag);
      if (tag.isEmpty()) {
         return null;
      }

      Pair<BlockPos, Holder<Structure>> result = this.getChunkSource()
         .getGenerator()
         .findNearestMapStructure(this, tag.get(), origin, maxSearchRadius, createReference);
      return result != null ? (BlockPos)result.getFirst() : null;
   }

   public @Nullable Pair<BlockPos, Holder<Biome>> findClosestBiome3d(
      final Predicate<Holder<Biome>> biomeTest,
      final BlockPos origin,
      final int maxSearchRadius,
      final int sampleResolutionHorizontal,
      final int sampleResolutionVertical
   ) {
      return this.getChunkSource()
         .getGenerator()
         .getBiomeSource()
         .findClosestBiome3d(
            origin, maxSearchRadius, sampleResolutionHorizontal, sampleResolutionVertical, biomeTest, this.getChunkSource().randomState().sampler(), this
         );
   }

   @Override
   public WorldBorder getWorldBorder() {
      WorldBorder worldBorder = this.getDataStorage().computeIfAbsent(WorldBorder.TYPE);
      worldBorder.applyInitialSettings(this.levelData.getGameTime());
      return worldBorder;
   }

   public RecipeManager recipeAccess() {
      return this.server.getRecipeManager();
   }

   @Override
   public TickRateManager tickRateManager() {
      return this.server.tickRateManager();
   }

   @Override
   public boolean noSave() {
      return this.noSave;
   }

   public SavedDataStorage getDataStorage() {
      return this.getChunkSource().getDataStorage();
   }

   @Override
   public @Nullable MapItemSavedData getMapData(final MapId id) {
      return this.getServer().getDataStorage().get(MapItemSavedData.type(id));
   }

   public void setMapData(final MapId id, final MapItemSavedData data) {
      this.getServer().getDataStorage().set(MapItemSavedData.type(id), data);
   }

   public MapId getFreeMapId() {
      return this.getServer().getDataStorage().computeIfAbsent(MapIndex.TYPE).getNextMapId();
   }

   @Override
   public void setRespawnData(final LevelData.RespawnData respawnData) {
      this.getServer().setRespawnData(respawnData);
   }

   @Override
   public LevelData.RespawnData getRespawnData() {
      return this.getServer().getRespawnData();
   }

   public LongSet getForceLoadedChunks() {
      return this.chunkSource.getForceLoadedChunks();
   }

   public boolean setChunkForced(final int chunkX, final int chunkZ, final boolean forced) {
      boolean updated = this.chunkSource.updateChunkForced(new ChunkPos(chunkX, chunkZ), forced);
      if (forced && updated) {
         this.getChunk(chunkX, chunkZ);
      }

      return updated;
   }

   @Override
   public List<ServerPlayer> players() {
      return this.players;
   }

   @Override
   public void updatePOIOnBlockStateChange(final BlockPos pos, final BlockState oldState, final BlockState newState) {
      Optional<Holder<PoiType>> oldType = PoiTypes.forState(oldState);
      Optional<Holder<PoiType>> newType = PoiTypes.forState(newState);
      if (!Objects.equals(oldType, newType)) {
         BlockPos immutable = pos.immutable();
         oldType.ifPresent(poiType -> this.getServer().execute(() -> {
            this.getPoiManager().remove(immutable);
            this.debugSynchronizers.dropPoi(immutable);
         }));
         newType.ifPresent(poiType -> this.getServer().execute(() -> {
            PoiRecord record = this.getPoiManager().add(immutable, (Holder<PoiType>)poiType);
            if (record != null) {
               this.debugSynchronizers.registerPoi(record);
            }
         }));
      }
   }

   public PoiManager getPoiManager() {
      return this.getChunkSource().getPoiManager();
   }

   public boolean isVillage(final BlockPos pos) {
      return this.isCloseToVillage(pos, 1);
   }

   public boolean isVillage(final SectionPos sectionPos) {
      return this.isVillage(sectionPos.center());
   }

   public boolean isCloseToVillage(final BlockPos pos, final int sectionDistance) {
      return sectionDistance > 6 ? false : this.sectionsToVillage(SectionPos.of(pos)) <= sectionDistance;
   }

   public int sectionsToVillage(final SectionPos pos) {
      return this.getPoiManager().sectionsToVillage(pos);
   }

   public Raids getRaids() {
      return this.raids;
   }

   public @Nullable Raid getRaidAt(final BlockPos pos) {
      return this.raids.getNearbyRaid(pos, 9216);
   }

   public boolean isRaided(final BlockPos pos) {
      return this.getRaidAt(pos) != null;
   }

   public void onReputationEvent(final ReputationEventType type, final Entity source, final ReputationEventHandler target) {
      target.onReputationEventFrom(type, source);
   }

   public void saveDebugReport(final Path rootDir) throws IOException {
      ChunkMap chunkMap = this.getChunkSource().chunkMap;

      try (Writer output = Files.newBufferedWriter(rootDir.resolve("stats.txt"))) {
         output.write(String.format(Locale.ROOT, "spawning_chunks: %d\n", chunkMap.getDistanceManager().getNaturalSpawnChunkCount()));
         NaturalSpawner.SpawnState lastSpawnState = this.getChunkSource().getLastSpawnState();
         if (lastSpawnState != null) {
            ObjectIterator entityChunks = lastSpawnState.getMobCategoryCounts().object2IntEntrySet().iterator();

            while (entityChunks.hasNext()) {
               Entry<MobCategory> entry = (Entry<MobCategory>)entityChunks.next();
               output.write(String.format(Locale.ROOT, "spawn_count.%s: %d\n", ((MobCategory)entry.getKey()).getName(), entry.getIntValue()));
            }
         }

         output.write(String.format(Locale.ROOT, "entities: %s\n", this.entityManager.gatherStats()));
         output.write(String.format(Locale.ROOT, "block_entity_tickers: %d\n", this.blockEntityTickers.size()));
         output.write(String.format(Locale.ROOT, "block_ticks: %d\n", this.getBlockTicks().count()));
         output.write(String.format(Locale.ROOT, "fluid_ticks: %d\n", this.getFluidTicks().count()));
         output.write("distance_manager: " + chunkMap.getDistanceManager().getDebugStatus() + "\n");
         output.write(String.format(Locale.ROOT, "pending_tasks: %d\n", this.getChunkSource().getPendingTasksCount()));
      }

      CrashReport test = new CrashReport("Level dump", new Exception("dummy"));
      this.fillReportDetails(test);

      try (Writer output = Files.newBufferedWriter(rootDir.resolve("example_crash.txt"))) {
         output.write(test.getFriendlyReport(ReportType.TEST));
      }

      Path chunks = rootDir.resolve("chunks.csv");

      try (Writer output = Files.newBufferedWriter(chunks)) {
         chunkMap.dumpChunks(output);
      }

      Path entityChunks = rootDir.resolve("entity_chunks.csv");

      try (Writer output = Files.newBufferedWriter(entityChunks)) {
         this.entityManager.dumpSections(output);
      }

      Path entities = rootDir.resolve("entities.csv");

      try (Writer output = Files.newBufferedWriter(entities)) {
         dumpEntities(output, this.getEntities().getAll());
      }

      Path blockEntities = rootDir.resolve("block_entities.csv");

      try (Writer output = Files.newBufferedWriter(blockEntities)) {
         this.dumpBlockEntityTickers(output);
      }
   }

   private static void dumpEntities(final Writer output, final Iterable<Entity> entities) throws IOException {
      CsvOutput csvOutput = CsvOutput.builder()
         .addColumn("x")
         .addColumn("y")
         .addColumn("z")
         .addColumn("uuid")
         .addColumn("type")
         .addColumn("alive")
         .addColumn("display_name")
         .addColumn("custom_name")
         .build(output);

      for (Entity entity : entities) {
         Component customName = entity.getCustomName();
         Component displayName = entity.getDisplayName();
         csvOutput.writeRow(
            entity.getX(),
            entity.getY(),
            entity.getZ(),
            entity.getUUID(),
            entity.typeHolder().getRegisteredName(),
            entity.isAlive(),
            displayName.getString(),
            customName != null ? customName.getString() : null
         );
      }
   }

   private void dumpBlockEntityTickers(final Writer output) throws IOException {
      CsvOutput csvOutput = CsvOutput.builder().addColumn("x").addColumn("y").addColumn("z").addColumn("type").build(output);

      for (TickingBlockEntity ticker : this.blockEntityTickers) {
         BlockPos blockPos = ticker.getPos();
         csvOutput.writeRow(blockPos.getX(), blockPos.getY(), blockPos.getZ(), ticker.getType());
      }
   }

   @VisibleForTesting
   public void clearBlockEvents(final BoundingBox bb) {
      this.blockEvents.removeIf(e -> bb.isInside(e.pos()));
   }

   public Iterable<Entity> getAllEntities() {
      return this.getEntities().getAll();
   }

   @Override
   public String toString() {
      return "ServerLevel[" + this.serverLevelData.getLevelName() + "]";
   }

   public boolean isFlat() {
      return this.server.getWorldData().isFlatWorld();
   }

   @Override
   public long getSeed() {
      return this.server.getWorldGenSettings().options().seed();
   }

   public @Nullable EnderDragonFight getDragonFight() {
      return this.dragonFight;
   }

   public WeatherData getWeatherData() {
      return this.server.getWeatherData();
   }

   @Override
   public ServerLevel getLevel() {
      return this;
   }

   @VisibleForTesting
   public String getWatchdogStats() {
      return String.format(
         Locale.ROOT,
         "players: %s, entities: %s [%s], block_entities: %d [%s], block_ticks: %d, fluid_ticks: %d, chunk_source: %s",
         this.players.size(),
         this.entityManager.gatherStats(),
         getTypeCount(this.entityManager.getEntityGetter().getAll(), e -> e.typeHolder().getRegisteredName()),
         this.blockEntityTickers.size(),
         getTypeCount(this.blockEntityTickers, TickingBlockEntity::getType),
         this.getBlockTicks().count(),
         this.getFluidTicks().count(),
         this.gatherChunkSourceStats()
      );
   }

   private static <T> String getTypeCount(final Iterable<T> values, final Function<T, String> typeGetter) {
      try {
         Object2IntOpenHashMap<String> countByType = new Object2IntOpenHashMap();

         for (T e : values) {
            String type = typeGetter.apply(e);
            countByType.addTo(type, 1);
         }

         Comparator<Entry<String>> compareByCount = Comparator.comparingInt(Entry::getIntValue);
         return countByType.object2IntEntrySet()
            .stream()
            .sorted(compareByCount.reversed())
            .limit(5L)
            .map(ex -> (String)ex.getKey() + ":" + ex.getIntValue())
            .collect(Collectors.joining(","));
      } catch (Exception e) {
         return "";
      }
   }

   @Override
   protected LevelEntityGetter<Entity> getEntities() {
      return this.entityManager.getEntityGetter();
   }

   public void addLegacyChunkEntities(final Stream<Entity> loaded) {
      this.entityManager.addLegacyChunkEntities(loaded);
   }

   public void addWorldGenChunkEntities(final Stream<Entity> loaded) {
      this.entityManager.addWorldGenChunkEntities(loaded);
   }

   public void startTickingChunk(final LevelChunk levelChunk) {
      levelChunk.unpackTicks(this.getGameTime());
   }

   public void onStructureStartsAvailable(final ChunkAccess chunk) {
      this.server.execute(() -> this.structureCheck.onStructureLoad(chunk.getPos(), chunk.getAllStarts()));
   }

   public PathTypeCache getPathTypeCache() {
      return this.pathTypesByPosCache;
   }

   public void waitForEntities(final ChunkPos centerChunk, final int radius) {
      List<ChunkPos> chunks = ChunkPos.rangeClosed(centerChunk, radius).toList();
      this.server.managedBlock(() -> {
         this.entityManager.processPendingLoads();

         for (ChunkPos chunk : chunks) {
            if (!this.areEntitiesLoaded(chunk.pack())) {
               return false;
            }
         }

         return true;
      });
   }

   public boolean isSpawningMonsters() {
      return this.getGameRules().get(GameRules.SPAWN_MOBS) && this.getGameRules().get(GameRules.SPAWN_MONSTERS);
   }

   @Override
   public void close() throws IOException {
      super.close();
      this.entityManager.close();
   }

   @Override
   public String gatherChunkSourceStats() {
      return "Chunks[S] W: " + this.chunkSource.gatherStats() + " E: " + this.entityManager.gatherStats();
   }

   public boolean areEntitiesLoaded(final long chunkKey) {
      return this.entityManager.areEntitiesLoaded(chunkKey);
   }

   public boolean isPositionTickingWithEntitiesLoaded(final long key) {
      return this.areEntitiesLoaded(key) && this.chunkSource.isPositionTicking(key);
   }

   public boolean isPositionEntityTicking(final BlockPos pos) {
      return this.entityManager.canPositionTick(pos) && this.chunkSource.chunkMap.getDistanceManager().inEntityTickingRange(ChunkPos.pack(pos));
   }

   public boolean areEntitiesActuallyLoadedAndTicking(final ChunkPos pos) {
      return this.entityManager.isTicking(pos) && this.entityManager.areEntitiesLoaded(pos.pack());
   }

   public boolean anyPlayerCloseEnoughForSpawning(final BlockPos pos) {
      return this.anyPlayerCloseEnoughForSpawning(ChunkPos.containing(pos));
   }

   public boolean anyPlayerCloseEnoughForSpawning(final ChunkPos pos) {
      return this.chunkSource.chunkMap.anyPlayerCloseEnoughForSpawning(pos);
   }

   public boolean canSpreadFireAround(final BlockPos pos) {
      int spreadRadius = this.getGameRules().get(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER);
      return spreadRadius == -1 || this.chunkSource.chunkMap.anyPlayerCloseEnoughTo(pos, spreadRadius);
   }

   public boolean canSpawnEntitiesInChunk(final ChunkPos pos) {
      return this.entityManager.canPositionTick(pos) && this.getWorldBorder().isWithinBounds(pos);
   }

   @Override
   public FeatureFlagSet enabledFeatures() {
      return this.server.getWorldData().enabledFeatures();
   }

   @Override
   public FuelValues fuelValues() {
      return this.server.fuelValues();
   }

   public GameRules getGameRules() {
      return this.server.getGameRules();
   }

   @Override
   public CrashReportCategory fillReportDetails(final CrashReport report) {
      CrashReportCategory category = super.fillReportDetails(report);
      WeatherData weatherData = this.getWeatherData();
      category.setDetail("Loaded entity count", () -> String.valueOf(this.entityManager.count()));
      category.setDetail(
         "Server weather",
         () -> String.format(
            Locale.ROOT,
            "Rain time: %d (now: %b), thunder time: %d (now: %b)",
            weatherData.getRainTime(),
            this.isRaining(),
            weatherData.getThunderTime(),
            this.isThundering()
         )
      );
      return category;
   }

   @Override
   public int getSeaLevel() {
      return this.chunkSource.getGenerator().getSeaLevel();
   }

   @Override
   public void onBlockEntityAdded(final BlockEntity blockEntity) {
      super.onBlockEntityAdded(blockEntity);
      this.debugSynchronizers.registerBlockEntity(blockEntity);
   }

   public LevelDebugSynchronizers debugSynchronizers() {
      return this.debugSynchronizers;
   }

   public boolean isAllowedToEnterPortal(final Level toLevel) {
      return toLevel.dimension() == Level.NETHER ? this.getGameRules().get(GameRules.ALLOW_ENTERING_NETHER_USING_PORTALS) : true;
   }

   public boolean isPvpAllowed() {
      return this.getGameRules().get(GameRules.PVP);
   }

   public boolean isCommandBlockEnabled() {
      return this.getGameRules().get(GameRules.COMMAND_BLOCKS_WORK);
   }

   public boolean isSpawnerBlockEnabled() {
      return this.getGameRules().get(GameRules.SPAWNER_BLOCKS_WORK);
   }

   private final class EntityCallbacks implements LevelCallback<Entity> {
      public void onCreated(final Entity entity) {
         if (entity instanceof WaypointTransmitter waypoint && waypoint.isTransmittingWaypoint()) {
            ServerLevel.this.getWaypointManager().trackWaypoint(waypoint);
         }
      }

      public void onDestroyed(final Entity entity) {
         if (entity instanceof WaypointTransmitter waypoint) {
            ServerLevel.this.getWaypointManager().untrackWaypoint(waypoint);
         }

         ServerLevel.this.getScoreboard().entityRemoved(entity);
      }

      public void onTickingStart(final Entity entity) {
         ServerLevel.this.entityTickList.add(entity);
      }

      public void onTickingEnd(final Entity entity) {
         ServerLevel.this.entityTickList.remove(entity);
      }

      public void onTrackingStart(final Entity entity) {
         ServerLevel.this.getChunkSource().addEntity(entity);
         if (entity instanceof ServerPlayer player) {
            ServerLevel.this.players.add(player);
            if (player.isReceivingWaypoints()) {
               ServerLevel.this.getWaypointManager().addPlayer(player);
            }

            ServerLevel.this.updateSleepingPlayerList();
         }

         if (entity instanceof WaypointTransmitter waypoint && waypoint.isTransmittingWaypoint()) {
            ServerLevel.this.getWaypointManager().trackWaypoint(waypoint);
         }

         if (entity instanceof Mob mob) {
            if (ServerLevel.this.isUpdatingNavigations) {
               String message = "onTrackingStart called during navigation iteration";
               Util.logAndPauseIfInIde(
                  "onTrackingStart called during navigation iteration", new IllegalStateException("onTrackingStart called during navigation iteration")
               );
            }

            ServerLevel.this.navigatingMobs.add(mob);
         }

         if (entity instanceof EnderDragon dragon) {
            for (EnderDragonPart subEntity : dragon.getSubEntities()) {
               ServerLevel.this.dragonParts.put(subEntity.getId(), subEntity);
            }
         }

         entity.updateDynamicGameEventListener(DynamicGameEventListener::add);
      }

      public void onTrackingEnd(final Entity entity) {
         ServerLevel.this.getChunkSource().removeEntity(entity);
         if (entity instanceof ServerPlayer player) {
            ServerLevel.this.players.remove(player);
            ServerLevel.this.getWaypointManager().removePlayer(player);
            ServerLevel.this.updateSleepingPlayerList();
         }

         if (entity instanceof Mob mob) {
            if (ServerLevel.this.isUpdatingNavigations) {
               String message = "onTrackingStart called during navigation iteration";
               Util.logAndPauseIfInIde(
                  "onTrackingStart called during navigation iteration", new IllegalStateException("onTrackingStart called during navigation iteration")
               );
            }

            ServerLevel.this.navigatingMobs.remove(mob);
         }

         if (entity instanceof EnderDragon dragon) {
            for (EnderDragonPart subEntity : dragon.getSubEntities()) {
               ServerLevel.this.dragonParts.remove(subEntity.getId());
            }
         }

         entity.updateDynamicGameEventListener(DynamicGameEventListener::remove);
         ServerLevel.this.debugSynchronizers.dropEntity(entity);
      }

      public void onSectionChange(final Entity entity) {
         entity.updateDynamicGameEventListener(DynamicGameEventListener::move);
      }
   }
}
