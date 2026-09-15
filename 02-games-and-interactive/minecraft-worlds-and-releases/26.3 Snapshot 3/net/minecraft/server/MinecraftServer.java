package net.minecraft.server;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.DataFixer;
import com.mojang.jtracy.DiscontinuousFrame;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.management.ThreadInfo;
import java.net.Proxy;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportType;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.gametest.framework.GameTestTicker;
import net.minecraft.nbt.Tag;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.level.ChunkLoadCounter;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DemoMode;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.server.network.ServerConnectionListener;
import net.minecraft.server.network.TextFilter;
import net.minecraft.server.notifications.NotificationManager;
import net.minecraft.server.notifications.ServerActivityMonitor;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.waypoints.ServerWaypointManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.util.FileUtil;
import net.minecraft.util.ModCheck;
import net.minecraft.util.Mth;
import net.minecraft.util.NativeModuleLister;
import net.minecraft.util.PngInfo;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.Util;
import net.minecraft.util.debug.ServerDebugSubscribers;
import net.minecraft.util.debugchart.SampleLogger;
import net.minecraft.util.debugchart.TpsDebugDimensions;
import net.minecraft.util.profiling.EmptyProfileResults;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.ResultField;
import net.minecraft.util.profiling.SingleTickProfiler;
import net.minecraft.util.profiling.jfr.Environment;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.util.profiling.metrics.profiling.ActiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.InactiveMetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.MetricsRecorder;
import net.minecraft.util.profiling.metrics.profiling.ServerMetricsSamplersProvider;
import net.minecraft.util.profiling.metrics.storage.MetricsPersister;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.Difficulty;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.Stopwatches;
import net.minecraft.world.clock.ClockTimeMarkers;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.entity.ai.village.VillageSiege;
import net.minecraft.world.entity.npc.CatSpawner;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTraderSpawner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.ChunkIOErrorReporter;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.storage.CommandStorage;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.timers.TimerQueue;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreboardSaveData;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class MinecraftServer extends ReentrantBlockableEventLoop<TickTask> implements CommandSource, ServerInfo, ChunkIOErrorReporter {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String VANILLA_BRAND = "vanilla";
   private static final float AVERAGE_TICK_TIME_SMOOTHING = 0.8F;
   private static final int TICK_STATS_SPAN = 100;
   private static final long OVERLOADED_THRESHOLD_NANOS = 20L * TimeUtil.NANOSECONDS_PER_SECOND / 20L;
   private static final int OVERLOADED_TICKS_THRESHOLD = 20;
   private static final long OVERLOADED_WARNING_INTERVAL_NANOS = 10L * TimeUtil.NANOSECONDS_PER_SECOND;
   private static final int OVERLOADED_TICKS_WARNING_INTERVAL = 100;
   private static final long STATUS_EXPIRE_TIME_NANOS = 5L * TimeUtil.NANOSECONDS_PER_SECOND;
   private static final long PREPARE_LEVELS_DEFAULT_DELAY_NANOS = 10L * TimeUtil.NANOSECONDS_PER_MILLISECOND;
   private static final int MAX_STATUS_PLAYER_SAMPLE = 12;
   public static final int SPAWN_POSITION_SEARCH_RADIUS = 5;
   private static final int SERVER_ACTIVITY_MONITOR_SECONDS_BETWEEN_NOTIFICATIONS = 30;
   private static final int AUTOSAVE_INTERVAL = 6000;
   private static final int MIMINUM_AUTOSAVE_TICKS = 100;
   private static final int MAX_TICK_LATENCY = 3;
   public static final int ABSOLUTE_MAX_WORLD_SIZE = 29999984;
   public static final LevelSettings DEMO_SETTINGS = new LevelSettings(
      "Demo World", GameType.SURVIVAL, LevelSettings.DifficultySettings.DEFAULT, false, WorldDataConfiguration.DEFAULT
   );
   public static final Supplier<GameRules> DEFAULT_GAME_RULES = () -> new GameRules(WorldDataConfiguration.DEFAULT.enabledFeatures());
   public static final NameAndId ANONYMOUS_PLAYER_PROFILE = new NameAndId(Util.NIL_UUID, "Anonymous Player");
   public static final String SERVER_THREAD_NAME = "Server thread";
   protected final LevelStorageSource.LevelStorageAccess storageSource;
   protected final PlayerDataStorage playerDataStorage;
   private final SavedDataStorage savedDataStorage;
   private final List<Runnable> tickables = Lists.newArrayList();
   private final GameRules gameRules;
   private MetricsRecorder metricsRecorder = InactiveMetricsRecorder.INSTANCE;
   private Consumer<ProfileResults> onMetricsRecordingStopped = results -> this.stopRecordingMetrics();
   private Consumer<Path> onMetricsRecordingFinished = ignored -> {};
   private boolean willStartRecordingMetrics;
   private MinecraftServer.@Nullable TimeProfiler debugCommandProfiler;
   private boolean debugCommandProfilerDelayStart;
   private final ServerConnectionListener connection;
   private final LevelLoadListener levelLoadListener;
   private @Nullable ServerStatus status;
   private ServerStatus.@Nullable Favicon statusIcon;
   private final RandomSource random = RandomSource.create();
   private final DataFixer fixerUpper;
   private String localIp;
   private int port = -1;
   private final LayeredRegistryAccess<RegistryLayer> registries;
   private final Map<ResourceKey<Level>, ServerLevel> levels = Maps.newLinkedHashMap();
   private PlayerList playerList;
   private volatile boolean running = true;
   private boolean stopped;
   private int tickCount;
   private int ticksUntilAutosave = 6000;
   protected final Proxy proxy;
   private boolean onlineMode;
   private boolean preventProxyConnections;
   private @Nullable String motd;
   private int playerIdleTimeout;
   private final long[] tickTimesNanos = new long[100];
   private long aggregatedTickTimesNanos = 0L;
   private @Nullable KeyPair keyPair;
   private @Nullable GameProfile singleplayerProfile;
   private boolean isDemo;
   private volatile boolean isReady;
   private long lastOverloadWarningNanos;
   protected final Services services;
   private final NotificationManager notificationManager;
   private final ServerActivityMonitor serverActivityMonitor;
   private long lastServerStatus;
   private final Thread serverThread;
   private long lastTickNanos = Util.getNanos();
   private long taskExecutionStartNanos = Util.getNanos();
   private long idleTimeNanos;
   private long nextTickTimeNanos = Util.getNanos();
   private boolean waitingForNextTick = false;
   private long delayedTasksMaxNextTickTimeNanos;
   private boolean mayHaveDelayedTasks;
   private final PackRepository packRepository;
   private final WorldGenSettings worldGenSettings;
   private final ServerScoreboard scoreboard = new ServerScoreboard(this);
   private @Nullable Stopwatches stopwatches;
   private @Nullable CommandStorage commandStorage;
   private final CustomBossEvents customBossEvents;
   private final RandomSequences randomSequences;
   private final WeatherData weatherData;
   private final ServerFunctionManager functionManager;
   private boolean enforceWhitelist;
   private boolean usingWhitelist;
   private float smoothedTickTimeMillis;
   private final Executor executor;
   private @Nullable String serverId;
   private MinecraftServer.ReloadableResources resources;
   private final StructureTemplateManager structureTemplateManager;
   private final ServerTickRateManager tickRateManager;
   private final ServerDebugSubscribers debugSubscribers = new ServerDebugSubscribers(this);
   protected final WorldData worldData;
   private LevelData.RespawnData effectiveRespawnData = LevelData.RespawnData.DEFAULT;
   private FuelValues fuelValues;
   private int emptyTicks;
   private volatile boolean isSaving;
   private final SuppressedExceptionCollector suppressedExceptions = new SuppressedExceptionCollector();
   private final DiscontinuousFrame tickFrame;
   private final PacketProcessor packetProcessor;
   private final TimerQueue<MinecraftServer> scheduledEvents;
   private final ServerClockManager clockManager;

   public static <S extends MinecraftServer> S spin(final Function<Thread, S> factory) {
      AtomicReference<S> serverReference = new AtomicReference<>();
      Thread thread = new Thread(() -> serverReference.get().runServer(), "Server thread");
      thread.setUncaughtExceptionHandler((t, e) -> LOGGER.error("Uncaught exception in server thread", e));
      if (Runtime.getRuntime().availableProcessors() > 4) {
         thread.setPriority(8);
      }

      S server = (S)factory.apply(thread);
      serverReference.set(server);
      thread.start();
      return server;
   }

   public MinecraftServer(
      final Thread serverThread,
      final LevelStorageSource.LevelStorageAccess storageSource,
      final PackRepository packRepository,
      final WorldStem worldStem,
      final Optional<GameRules> gameRules,
      final Proxy proxy,
      final DataFixer fixerUpper,
      final Services services,
      final LevelLoadListener levelLoadListener,
      final boolean propagatesCrashes,
      final NotificationManager notificationManager
   ) {
      super("Server", propagatesCrashes);
      this.registries = worldStem.registries();
      if (!this.registries.compositeAccess().lookupOrThrow(Registries.LEVEL_STEM).containsKey(LevelStem.OVERWORLD)) {
         throw new IllegalStateException("Missing Overworld dimension data");
      }

      this.savedDataStorage = new SavedDataStorage(storageSource.getLevelPath(LevelResource.DATA), fixerUpper, this.registries.compositeAccess());
      this.worldData = worldStem.worldDataAndGenSettings().data();
      this.worldGenSettings = worldStem.worldDataAndGenSettings().genSettings();
      this.savedDataStorage.set(WorldGenSettings.TYPE, this.worldGenSettings);
      this.proxy = proxy;
      this.packRepository = packRepository;
      this.resources = new MinecraftServer.ReloadableResources(worldStem.resourceManager(), worldStem.dataPackResources());
      this.services = services;
      this.connection = new ServerConnectionListener(this);
      this.tickRateManager = new ServerTickRateManager(this);
      this.levelLoadListener = levelLoadListener;
      this.storageSource = storageSource;
      this.playerDataStorage = storageSource.createPlayerStorage();
      this.randomSequences = this.savedDataStorage.computeIfAbsent(RandomSequences.TYPE);
      this.weatherData = this.getDataStorage().computeIfAbsent(WeatherData.TYPE);
      this.gameRules = new GameRules(this.worldData.enabledFeatures(), this.savedDataStorage.computeIfAbsent(GameRuleMap.TYPE));
      gameRules.ifPresent(g -> this.gameRules.setAll(g, null));
      this.fixerUpper = fixerUpper;
      this.functionManager = new ServerFunctionManager(this, this.resources.managers.getFunctionLibrary());
      HolderGetter<Block> blockLookup = this.registries.compositeAccess().lookupOrThrow(Registries.BLOCK).filterFeatures(this.worldData.enabledFeatures());
      this.structureTemplateManager = new StructureTemplateManager(worldStem.resourceManager(), storageSource, fixerUpper, blockLookup);
      this.serverThread = serverThread;
      this.executor = Util.backgroundExecutor();
      this.resources.managers.getRecipeManager().finalizeRecipeLoading(this.worldData.enabledFeatures());
      this.fuelValues = FuelValues.vanillaBurnTimes(this.registries.compositeAccess(), this.worldData.enabledFeatures());
      this.tickFrame = TracyClient.createDiscontinuousFrame("Server Tick");
      this.notificationManager = notificationManager;
      this.serverActivityMonitor = new ServerActivityMonitor(notificationManager, 30);
      this.packetProcessor = new PacketProcessor(serverThread);
      this.clockManager = this.getDataStorage().computeIfAbsent(ServerClockManager.TYPE);
      this.clockManager.init(this);
      this.customBossEvents = this.savedDataStorage.computeIfAbsent(CustomBossEvents.TYPE);
      this.scheduledEvents = this.savedDataStorage.computeIfAbsent(TimerQueue.TYPE);
   }

   protected abstract boolean initServer() throws IOException;

   public ChunkLoadStatusView createChunkLoadStatusView(final int radius) {
      return new ChunkLoadStatusView() {
         private @Nullable ChunkMap chunkMap;
         private int centerChunkX;
         private int centerChunkZ;

         @Override
         public void moveTo(final ResourceKey<Level> dimension, final ChunkPos centerChunk) {
            ServerLevel level = MinecraftServer.this.getLevel(dimension);
            this.chunkMap = level != null ? level.getChunkSource().chunkMap : null;
            this.centerChunkX = centerChunk.x();
            this.centerChunkZ = centerChunk.z();
         }

         @Override
         public @Nullable ChunkStatus get(final int x, final int z) {
            return this.chunkMap == null ? null : this.chunkMap.getLatestStatus(ChunkPos.pack(x + this.centerChunkX - radius, z + this.centerChunkZ - radius));
         }

         @Override
         public int radius() {
            return radius;
         }
      };
   }

   protected void loadLevel() {
      boolean startedWorldLoadProfiling = !JvmProfiler.INSTANCE.isRunning()
         && SharedConstants.DEBUG_JFR_PROFILING_ENABLE_LEVEL_LOADING
         && JvmProfiler.INSTANCE.start(Environment.from(this));
      ProfiledDuration profiledDuration = JvmProfiler.INSTANCE.onWorldLoadedStarted();
      this.worldData.setModdedInfo(this.getServerModName(), this.getModdedStatus().shouldReportAsModified());
      this.createLevels();
      this.forceDifficulty();
      this.prepareLevels();
      if (profiledDuration != null) {
         profiledDuration.finish(true);
      }

      if (startedWorldLoadProfiling) {
         try {
            JvmProfiler.INSTANCE.stop();
         } catch (Throwable t) {
            LOGGER.warn("Failed to stop JFR profiling", t);
         }
      }
   }

   protected void forceDifficulty() {
   }

   protected void createLevels() {
      ServerLevelData levelData = this.worldData.overworldData();
      boolean isDebug = this.worldData.isDebugWorld();
      Registry<LevelStem> dimensions = this.registries.compositeAccess().lookupOrThrow(Registries.LEVEL_STEM);
      WorldOptions worldOptions = this.worldGenSettings.options();
      long seed = worldOptions.seed();
      long biomeZoomSeed = BiomeManager.obfuscateSeed(seed);
      List<CustomSpawner> overworldCustomSpawners = ImmutableList.of(
         new PhantomSpawner(), new PatrolSpawner(), new CatSpawner(), new VillageSiege(), new WanderingTraderSpawner(this.savedDataStorage)
      );
      LevelStem overworldData = dimensions.getValue(LevelStem.OVERWORLD);
      ServerLevel overworld = new ServerLevel(
         this, this.executor, this.storageSource, levelData, Level.OVERWORLD, overworldData, isDebug, biomeZoomSeed, overworldCustomSpawners, true
      );
      this.levels.put(Level.OVERWORLD, overworld);
      this.scoreboard.load(this.savedDataStorage.computeIfAbsent(ScoreboardSaveData.TYPE).getData());
      this.commandStorage = new CommandStorage(this.savedDataStorage);
      this.stopwatches = this.savedDataStorage.computeIfAbsent(Stopwatches.TYPE);
      if (!levelData.isInitialized()) {
         try {
            setInitialSpawn(overworld, levelData, worldOptions.generateBonusChest(), isDebug, this.levelLoadListener);
            levelData.setInitialized(true);
            if (isDebug) {
               this.setupDebugLevel(this.worldData);
            }
         } catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "Exception initializing level");

            try {
               overworld.fillReportDetails(report);
            } catch (Throwable var19) {
            }

            throw new ReportedException(report);
         }

         levelData.setInitialized(true);
      }

      GlobalPos focusPos = this.selectLevelLoadFocusPos();
      this.levelLoadListener.updateFocus(focusPos.dimension(), ChunkPos.containing(focusPos.pos()));

      for (Entry<ResourceKey<LevelStem>, LevelStem> entry : dimensions.entrySet()) {
         ResourceKey<LevelStem> name = entry.getKey();
         ServerLevel level;
         if (name != LevelStem.OVERWORLD) {
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, name.identifier());
            DerivedLevelData derivedLevelData = new DerivedLevelData(this.worldData, levelData);
            level = new ServerLevel(
               this, this.executor, this.storageSource, derivedLevelData, dimension, entry.getValue(), isDebug, biomeZoomSeed, ImmutableList.of(), false
            );
            this.levels.put(dimension, level);
         } else {
            level = overworld;
         }

         level.getWorldBorder().setAbsoluteMaxSize(this.getAbsoluteMaxWorldSize());
         this.getPlayerList().addWorldborderListener(level);
      }
   }

   private static void setInitialSpawn(
      final ServerLevel level, final ServerLevelData levelData, final boolean spawnBonusChest, final boolean isDebug, final LevelLoadListener levelLoadListener
   ) {
      if (SharedConstants.DEBUG_ONLY_GENERATE_HALF_THE_WORLD && SharedConstants.DEBUG_WORLD_RECREATE) {
         levelData.setSpawn(LevelData.RespawnData.of(level.dimension(), new BlockPos(0, 64, -100), 0.0F, 0.0F));
      } else if (isDebug) {
         levelData.setSpawn(LevelData.RespawnData.of(level.dimension(), BlockPos.ZERO.above(80), 0.0F, 0.0F));
      } else {
         ServerChunkCache chunkSource = level.getChunkSource();
         ChunkPos spawnChunk = chunkSource.getGenerator().getOrigin(chunkSource.randomState());
         levelLoadListener.start(LevelLoadListener.Stage.PREPARE_GLOBAL_SPAWN, 0);
         levelLoadListener.updateFocus(level.dimension(), spawnChunk);
         int height = chunkSource.getGenerator().getSpawnHeight(level);
         if (height < level.getMinY()) {
            BlockPos worldPosition = spawnChunk.getWorldPosition();
            height = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldPosition.getX() + 8, worldPosition.getZ() + 8);
         }

         levelData.setSpawn(LevelData.RespawnData.of(level.dimension(), spawnChunk.getWorldPosition().offset(8, height, 8), 0.0F, 0.0F));
         int xChunkOffset = 0;
         int zChunkOffset = 0;
         int dXChunk = 0;
         int dZChunk = -1;

         for (int i = 0; i < Mth.square(11); i++) {
            if (xChunkOffset >= -5 && xChunkOffset <= 5 && zChunkOffset >= -5 && zChunkOffset <= 5) {
               BlockPos testedPos = PlayerSpawnFinder.getSpawnPosInChunk(level, new ChunkPos(spawnChunk.x() + xChunkOffset, spawnChunk.z() + zChunkOffset));
               if (testedPos != null) {
                  levelData.setSpawn(LevelData.RespawnData.of(level.dimension(), testedPos, 0.0F, 0.0F));
                  break;
               }
            }

            if (xChunkOffset == zChunkOffset || xChunkOffset < 0 && xChunkOffset == -zChunkOffset || xChunkOffset > 0 && xChunkOffset == 1 - zChunkOffset) {
               int olddx = dXChunk;
               dXChunk = -dZChunk;
               dZChunk = olddx;
            }

            xChunkOffset += dXChunk;
            zChunkOffset += dZChunk;
         }

         if (spawnBonusChest) {
            level.registryAccess()
               .lookup(Registries.FEATURE)
               .flatMap(registry -> registry.get(MiscOverworldFeatures.BONUS_CHEST))
               .ifPresent(feature -> feature.value().place(level, chunkSource.getGenerator(), level.getRandom(), levelData.getRespawnData().pos()));
         }

         levelLoadListener.finish(LevelLoadListener.Stage.PREPARE_GLOBAL_SPAWN);
      }
   }

   private void setupDebugLevel(final WorldData worldData) {
      worldData.setDifficulty(Difficulty.PEACEFUL);
      worldData.setDifficultyLocked(true);
      ServerLevelData levelData = worldData.overworldData();
      this.getGameRules().set(GameRules.ADVANCE_WEATHER, false, this);
      this.clockManager.moveToTimeMarker(this.registryAccess().getOrThrow(WorldClocks.OVERWORLD), ClockTimeMarkers.NOON);
      levelData.setGameType(GameType.SPECTATOR);
   }

   private void prepareLevels() {
      ChunkLoadCounter chunkLoadCounter = new ChunkLoadCounter();

      for (ServerLevel level : this.levels.values()) {
         chunkLoadCounter.track(level, () -> {
            TicketStorage savedTickets = level.getDataStorage().get(TicketStorage.TYPE);
            if (savedTickets != null) {
               savedTickets.activateAllDeactivatedTickets();
            }
         });
      }

      this.levelLoadListener.start(LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS, chunkLoadCounter.totalChunks());

      do {
         this.levelLoadListener.update(LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS, chunkLoadCounter.readyChunks(), chunkLoadCounter.totalChunks());
         this.nextTickTimeNanos = Util.getNanos() + PREPARE_LEVELS_DEFAULT_DELAY_NANOS;
         this.waitUntilNextTick();
      } while (chunkLoadCounter.pendingChunks() > 0);

      this.levelLoadListener.finish(LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS);
      this.updateMobSpawningFlags();
      this.updateEffectiveRespawnData();
   }

   protected GlobalPos selectLevelLoadFocusPos() {
      return this.worldData.overworldData().getRespawnData().globalPos();
   }

   public GameType getDefaultGameType() {
      return this.worldData.getGameType();
   }

   public boolean isHardcore() {
      return this.worldData.isHardcore();
   }

   public abstract LevelBasedPermissionSet operatorUserPermissions();

   public abstract PermissionSet getFunctionCompilationPermissions();

   public abstract boolean shouldRconBroadcast();

   public boolean saveAllChunks(final boolean silent, final boolean flush, final boolean force) {
      this.scoreboard.storeToSaveDataIfDirty(this.getDataStorage().computeIfAbsent(ScoreboardSaveData.TYPE));
      boolean result = false;

      for (ServerLevel level : this.getAllLevels()) {
         if (!silent) {
            LOGGER.info("Saving chunks for level '{}'/{}", level, level.dimension().identifier());
         }

         level.save(null, flush, SharedConstants.DEBUG_DONT_SAVE_WORLD || level.noSave && !force);
         result = true;
      }

      GameProfile singleplayerProfile = this.getSingleplayerProfile();
      this.storageSource.saveDataTag(this.worldData, singleplayerProfile == null ? null : singleplayerProfile.id());
      if (flush) {
         this.savedDataStorage.saveAndJoin();
      } else {
         this.savedDataStorage.scheduleSave();
      }

      return result;
   }

   public boolean saveEverything(final boolean silent, final boolean flush, final boolean force) {
      try {
         this.isSaving = true;
         this.getPlayerList().saveAll();
         boolean result = this.saveAllChunks(silent, flush, force);
         this.warnOnLowDiskSpace();
         return result;
      } finally {
         this.isSaving = false;
      }
   }

   @Override
   public void close() {
      this.stopServer();
   }

   protected void stopServer() {
      this.packetProcessor.close();
      if (this.metricsRecorder.isRecording()) {
         this.cancelRecordingMetrics();
      }

      LOGGER.info("Stopping server");
      this.getConnection().stop();
      this.isSaving = true;
      if (this.playerList != null) {
         LOGGER.info("Saving players");
         this.playerList.saveAll();
         this.playerList.removeAll();
      }

      LOGGER.info("Saving worlds");

      for (ServerLevel level : this.getAllLevels()) {
         if (level != null) {
            level.noSave = false;
         }
      }

      while (this.levels.values().stream().anyMatch(l -> l.getChunkSource().chunkMap.hasWork())) {
         this.nextTickTimeNanos = Util.getNanos() + TimeUtil.NANOSECONDS_PER_MILLISECOND;

         for (ServerLevel level : this.getAllLevels()) {
            level.getChunkSource().deactivateTicketsOnClosing();
            level.getChunkSource().tick(() -> true, false);
         }

         this.waitUntilNextTick();
      }

      this.saveAllChunks(false, true, false);

      for (ServerLevel level : this.getAllLevels()) {
         if (level != null) {
            try {
               level.close();
            } catch (IOException e) {
               LOGGER.error("Exception closing the level", e);
            }
         }
      }

      this.isSaving = false;
      this.savedDataStorage.close();
      this.resources.close();

      try {
         this.storageSource.close();
      } catch (IOException e) {
         LOGGER.error("Failed to unlock level {}", this.storageSource.getLevelId(), e);
      }
   }

   public String getLocalIp() {
      return this.localIp;
   }

   public void setLocalIp(final String ip) {
      this.localIp = ip;
   }

   public boolean isRunning() {
      return this.running;
   }

   public void halt(final boolean wait) {
      this.running = false;
      if (wait) {
         try {
            this.serverThread.join();
         } catch (InterruptedException e) {
            LOGGER.error("Error while shutting down", e);
         }
      }
   }

   protected void runServer() {
      try {
         if (!this.initServer()) {
            throw new IllegalStateException("Failed to initialize server");
         }

         this.nextTickTimeNanos = Util.getNanos();
         this.statusIcon = this.loadStatusIcon().orElse(null);
         this.status = this.buildServerStatus();

         while (this.running) {
            long thisTickNanos;
            if (!this.isPaused() && this.tickRateManager.isSprinting() && this.tickRateManager.checkShouldSprintThisTick()) {
               thisTickNanos = 0L;
               this.nextTickTimeNanos = Util.getNanos();
               this.lastOverloadWarningNanos = this.nextTickTimeNanos;
            } else {
               thisTickNanos = this.tickRateManager.nanosecondsPerTick();
               long behindTimeNanos = Util.getNanos() - this.nextTickTimeNanos;
               if (behindTimeNanos > OVERLOADED_THRESHOLD_NANOS + 20L * thisTickNanos
                  && this.nextTickTimeNanos - this.lastOverloadWarningNanos >= OVERLOADED_WARNING_INTERVAL_NANOS + 100L * thisTickNanos) {
                  long ticks = behindTimeNanos / thisTickNanos;
                  LOGGER.warn(
                     "Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", behindTimeNanos / TimeUtil.NANOSECONDS_PER_MILLISECOND, ticks
                  );
                  this.nextTickTimeNanos += ticks * thisTickNanos;
                  this.lastOverloadWarningNanos = this.nextTickTimeNanos;
               }
            }

            boolean sprinting = thisTickNanos == 0L;
            if (this.debugCommandProfilerDelayStart) {
               this.debugCommandProfilerDelayStart = false;
               this.debugCommandProfiler = new MinecraftServer.TimeProfiler(Util.getNanos(), this.tickCount);
            }

            this.nextTickTimeNanos += thisTickNanos;

            try (Profiler.Scope ignored = Profiler.use(this.createProfiler())) {
               this.processPacketsAndTick(sprinting);
               ProfilerFiller profiler = Profiler.get();
               profiler.push("nextTickWait");
               this.mayHaveDelayedTasks = true;
               this.delayedTasksMaxNextTickTimeNanos = Math.max(Util.getNanos() + thisTickNanos, this.nextTickTimeNanos);
               this.startMeasuringTaskExecutionTime();
               this.waitUntilNextTick();
               this.finishMeasuringTaskExecutionTime();
               if (sprinting) {
                  this.tickRateManager.endTickWork();
               }

               profiler.pop();
               this.logFullTickTime();
            } finally {
               this.endMetricsRecordingTick();
            }

            this.isReady = true;
            JvmProfiler.INSTANCE.onServerTick(this.smoothedTickTimeMillis);
         }
      } catch (Throwable t) {
         LOGGER.error("Encountered an unexpected exception", t);
         CrashReport report = constructOrExtractCrashReport(t);
         this.fillSystemReport(report.getSystemReport());
         Path file = this.getServerDirectory().resolve("crash-reports").resolve("crash-" + Util.getFilenameFormattedDateTime() + "-server.txt");
         if (report.saveToFile(file, ReportType.CRASH)) {
            LOGGER.error("This crash report has been saved to: {}", file.toAbsolutePath());
         } else {
            LOGGER.error("We were unable to save this crash report to disk.");
         }

         this.onServerCrash(report);
      } finally {
         try {
            this.stopped = true;
            this.stopServer();
         } catch (Throwable t) {
            LOGGER.error("Exception stopping the server", t);
         } finally {
            this.onServerExit();
         }
      }
   }

   private void logFullTickTime() {
      long currentTime = Util.getNanos();
      if (this.isTickTimeLoggingEnabled()) {
         this.getTickTimeLogger().logSample(currentTime - this.lastTickNanos);
      }

      this.lastTickNanos = currentTime;
   }

   private void startMeasuringTaskExecutionTime() {
      if (this.isTickTimeLoggingEnabled()) {
         this.taskExecutionStartNanos = Util.getNanos();
         this.idleTimeNanos = 0L;
      }
   }

   private void finishMeasuringTaskExecutionTime() {
      if (this.isTickTimeLoggingEnabled()) {
         SampleLogger tickTimelogger = this.getTickTimeLogger();
         tickTimelogger.logPartialSample(Util.getNanos() - this.taskExecutionStartNanos - this.idleTimeNanos, TpsDebugDimensions.SCHEDULED_TASKS.ordinal());
         tickTimelogger.logPartialSample(this.idleTimeNanos, TpsDebugDimensions.IDLE.ordinal());
      }
   }

   private static CrashReport constructOrExtractCrashReport(final Throwable t) {
      ReportedException firstReported = null;

      for (Throwable cause = t; cause != null; cause = cause.getCause()) {
         if (cause instanceof ReportedException reportedException) {
            firstReported = reportedException;
         }
      }

      CrashReport report;
      if (firstReported != null) {
         report = firstReported.getReport();
         if (firstReported != t) {
            report.addCategory("Wrapped in").setDetailError("Wrapping exception", t);
         }
      } else {
         report = new CrashReport("Exception in server tick loop", t);
      }

      return report;
   }

   private boolean haveTime() {
      return this.runningTask() || Util.getNanos() < (this.mayHaveDelayedTasks ? this.delayedTasksMaxNextTickTimeNanos : this.nextTickTimeNanos);
   }

   public NotificationManager notificationManager() {
      return this.notificationManager;
   }

   protected void waitUntilNextTick() {
      this.runAllTasks();
      this.waitingForNextTick = true;

      try {
         this.managedBlock(() -> !this.haveTime());
      } finally {
         this.waitingForNextTick = false;
      }
   }

   @Override
   protected void waitForTasks() {
      boolean shouldLogTime = this.isTickTimeLoggingEnabled();
      long waitStart = shouldLogTime ? Util.getNanos() : 0L;
      long waitNanos = this.waitingForNextTick ? this.nextTickTimeNanos - Util.getNanos() : 100000L;
      LockSupport.parkNanos("waiting for tasks", waitNanos);
      if (shouldLogTime) {
         this.idleTimeNanos = this.idleTimeNanos + (Util.getNanos() - waitStart);
      }
   }

   public TickTask wrapRunnable(final Runnable runnable) {
      return new TickTask(this.tickCount, runnable);
   }

   protected boolean shouldRun(final TickTask task) {
      return task.getTick() + 3 < this.tickCount || this.haveTime();
   }

   @Override
   protected boolean pollTask() {
      boolean mayHaveMoreTasks = this.pollTaskInternal();
      this.mayHaveDelayedTasks = mayHaveMoreTasks;
      return mayHaveMoreTasks;
   }

   private boolean pollTaskInternal() {
      if (super.pollTask()) {
         return true;
      }

      if (this.tickRateManager.isSprinting() || this.shouldRunAllTasks() || this.haveTime()) {
         for (ServerLevel level : this.getAllLevels()) {
            if (level.getChunkSource().pollTask()) {
               return true;
            }
         }
      }

      return false;
   }

   protected void doRunTask(final TickTask task) {
      Profiler.get().incrementCounter("runTask");
      super.doRunTask(task);
   }

   private Optional<ServerStatus.Favicon> loadStatusIcon() {
      Optional<Path> iconPath = Optional.of(this.getFile("server-icon.png"))
         .filter(x$0 -> Files.isRegularFile(x$0))
         .or(() -> this.storageSource.getIconFile().filter(x$0 -> Files.isRegularFile(x$0)));
      return iconPath.flatMap(path -> {
         try {
            byte[] contents = Files.readAllBytes(path);
            PngInfo pngInfo = PngInfo.fromBytes(contents);
            if (pngInfo.width() == 64 && pngInfo.height() == 64) {
               return Optional.of(new ServerStatus.Favicon(contents));
            } else {
               throw new IllegalArgumentException("Invalid world icon size [" + pngInfo.width() + ", " + pngInfo.height() + "], but expected [64, 64]");
            }
         } catch (Exception e) {
            LOGGER.error("Couldn't load server icon", e);
            return Optional.empty();
         }
      });
   }

   public Optional<Path> getWorldScreenshotFile() {
      return this.storageSource.getIconFile();
   }

   public Path getServerDirectory() {
      return Path.of("");
   }

   public ServerActivityMonitor getServerActivityMonitor() {
      return this.serverActivityMonitor;
   }

   protected void onServerCrash(final CrashReport report) {
   }

   protected void onServerExit() {
   }

   public boolean isPaused() {
      return false;
   }

   protected void tickServer(final BooleanSupplier haveTime) {
      long nano = Util.getNanos();
      int emptyTickThreshold = this.pauseWhenEmptySeconds() * 20;
      if (emptyTickThreshold > 0) {
         if (this.playerList.getPlayerCount() == 0 && !this.tickRateManager.isSprinting()) {
            this.emptyTicks++;
         } else {
            this.emptyTicks = 0;
         }

         if (this.emptyTicks >= emptyTickThreshold) {
            if (this.emptyTicks == emptyTickThreshold) {
               LOGGER.info("Server empty for {} seconds, pausing", this.pauseWhenEmptySeconds());
               this.autoSave();
            }

            this.tickConnection();
            return;
         }
      }

      this.tickCount++;
      this.tickRateManager.tick();
      this.tickChildren(haveTime);
      if (nano - this.lastServerStatus >= STATUS_EXPIRE_TIME_NANOS) {
         this.lastServerStatus = nano;
         this.status = this.buildServerStatus();
      }

      this.ticksUntilAutosave--;
      if (this.ticksUntilAutosave <= 0) {
         this.autoSave();
      }

      ProfilerFiller profiler = Profiler.get();
      profiler.push("tallying");
      long tickTime = Util.getNanos() - nano;
      int tickIndex = this.tickCount % 100;
      this.aggregatedTickTimesNanos = this.aggregatedTickTimesNanos - this.tickTimesNanos[tickIndex];
      this.aggregatedTickTimesNanos += tickTime;
      this.tickTimesNanos[tickIndex] = tickTime;
      this.smoothedTickTimeMillis = this.smoothedTickTimeMillis * 0.8F + (float)tickTime / (float)TimeUtil.NANOSECONDS_PER_MILLISECOND * 0.19999999F;
      this.logTickMethodTime(nano);
      profiler.pop();
   }

   protected void processPacketsAndTick(final boolean sprinting) {
      ProfilerFiller profiler = Profiler.get();
      profiler.push("tick");
      this.tickFrame.start();
      profiler.push("scheduledPacketProcessing");
      this.packetProcessor.processQueuedPackets();
      profiler.pop();
      this.tickServer(sprinting ? () -> false : this::haveTime);
      this.tickFrame.end();
      profiler.pop();
   }

   private void autoSave() {
      this.ticksUntilAutosave = this.computeNextAutosaveInterval();
      LOGGER.debug("Autosave started");
      ProfilerFiller profiler = Profiler.get();
      profiler.push("save");
      this.saveEverything(true, false, false);
      profiler.pop();
      LOGGER.debug("Autosave finished");
   }

   private void logTickMethodTime(final long startTime) {
      if (this.isTickTimeLoggingEnabled()) {
         this.getTickTimeLogger().logPartialSample(Util.getNanos() - startTime, TpsDebugDimensions.TICK_SERVER_METHOD.ordinal());
      }
   }

   private int computeNextAutosaveInterval() {
      float ticksPerSecond;
      if (this.tickRateManager.isSprinting()) {
         long estimatedTickTimeNanos = this.getAverageTickTimeNanos() + 1L;
         ticksPerSecond = (float)TimeUtil.NANOSECONDS_PER_SECOND / (float)estimatedTickTimeNanos;
      } else {
         ticksPerSecond = this.tickRateManager.tickrate();
      }

      int intendedIntervalInSeconds = 300;
      return Math.max(100, (int)(ticksPerSecond * 300.0F));
   }

   public void onTickRateChanged() {
      int newAutosaveInterval = this.computeNextAutosaveInterval();
      if (newAutosaveInterval < this.ticksUntilAutosave) {
         this.ticksUntilAutosave = newAutosaveInterval;
      }
   }

   protected abstract SampleLogger getTickTimeLogger();

   public abstract boolean isTickTimeLoggingEnabled();

   private ServerStatus buildServerStatus() {
      ServerStatus.Players players = this.buildPlayerStatus();
      return new ServerStatus(
         Component.nullToEmpty(this.getMotd()),
         Optional.of(players),
         Optional.of(ServerStatus.Version.current()),
         Optional.ofNullable(this.statusIcon),
         this.enforceSecureProfile()
      );
   }

   private ServerStatus.Players buildPlayerStatus() {
      List<ServerPlayer> players = this.playerList.getPlayers();
      int maxPlayers = this.getMaxPlayers();
      if (this.hidesOnlinePlayers()) {
         return new ServerStatus.Players(maxPlayers, players.size(), List.of());
      }

      int sampleSize = Math.min(players.size(), 12);
      ObjectArrayList<NameAndId> sample = new ObjectArrayList(sampleSize);
      int offset = Mth.nextInt(this.random, 0, players.size() - sampleSize);

      for (int i = 0; i < sampleSize; i++) {
         ServerPlayer player = players.get(offset + i);
         sample.add(player.allowsListing() ? player.nameAndId() : ANONYMOUS_PLAYER_PROFILE);
      }

      Util.shuffle(sample, this.random);
      return new ServerStatus.Players(maxPlayers, players.size(), sample);
   }

   protected void tickChildren(final BooleanSupplier haveTime) {
      ProfilerFiller profiler = Profiler.get();
      this.getPlayerList().getPlayers().forEach(playerx -> playerx.connection.suspendFlushing());
      profiler.push("commandFunctions");
      this.getFunctions().tick();
      profiler.pop();
      if (this.tickRateManager.runsNormally()) {
         profiler.push("clocks");
         this.clockManager.tick();
         profiler.pop();
      }

      if (this.tickCount % 20 == 0) {
         profiler.push("timeSync");
         this.forceGameTimeSynchronization();
         profiler.pop();
      }

      profiler.push("levels");
      this.updateEffectiveRespawnData();

      for (ServerLevel level : this.getAllLevels()) {
         profiler.push(() -> level + " " + level.dimension().identifier());
         profiler.push("tick");

         try {
            level.tick(haveTime);
         } catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "Exception ticking world");
            level.fillReportDetails(report);
            throw new ReportedException(report);
         }

         profiler.pop();
         profiler.pop();
      }

      profiler.popPush("connection");
      this.tickConnection();
      profiler.popPush("players");
      this.playerList.tick();
      profiler.popPush("debugSubscribers");
      this.debugSubscribers.tick();
      if (this.tickRateManager.runsNormally()) {
         profiler.popPush("gameTests");
         GameTestTicker.SINGLETON.tick();
      }

      profiler.popPush("server gui refresh");

      for (Runnable tickable : this.tickables) {
         tickable.run();
      }

      profiler.popPush("send chunks");

      for (ServerPlayer player : this.playerList.getPlayers()) {
         player.connection.chunkSender.sendNextChunks(player);
         player.connection.resumeFlushing();
      }

      profiler.pop();
      this.serverActivityMonitor.tick();
   }

   private void updateEffectiveRespawnData() {
      LevelData.RespawnData respawnData = this.worldData.overworldData().getRespawnData();
      ServerLevel respawnLevel = this.findRespawnDimension();
      this.effectiveRespawnData = respawnLevel.getWorldBorderAdjustedRespawnData(respawnData);
   }

   protected void tickConnection() {
      this.getConnection().tick();
   }

   public void forceGameTimeSynchronization() {
      ProfilerFiller profiler = Profiler.get();
      profiler.push("timeSync");
      this.playerList.broadcastAll(new ClientboundSetTimePacket(this.overworld().getGameTime(), Map.of()));
      profiler.pop();
   }

   public void addTickable(final Runnable tickable) {
      this.tickables.add(tickable);
   }

   protected void setId(final String serverId) {
      this.serverId = serverId;
   }

   public boolean isShutdown() {
      return !this.serverThread.isAlive();
   }

   public Path getFile(final String name) {
      return this.getServerDirectory().resolve(name);
   }

   public final ServerLevel overworld() {
      return this.levels.get(Level.OVERWORLD);
   }

   public @Nullable ServerLevel getLevel(final ResourceKey<Level> dimension) {
      return this.levels.get(dimension);
   }

   public Set<ResourceKey<Level>> levelKeys() {
      return this.levels.keySet();
   }

   public Iterable<ServerLevel> getAllLevels() {
      return this.levels.values();
   }

   @Override
   public String getServerVersion() {
      return SharedConstants.getCurrentVersion().name();
   }

   @Override
   public int getPlayerCount() {
      return this.playerList.getPlayerCount();
   }

   public String[] getPlayerNames() {
      return this.playerList.getPlayerNamesArray();
   }

   public String getServerModName() {
      return "vanilla";
   }

   public ServerClockManager clockManager() {
      return this.clockManager;
   }

   public SystemReport fillSystemReport(final SystemReport systemReport) {
      systemReport.setDetail("Server Running", () -> Boolean.toString(this.running));
      if (this.playerList != null) {
         systemReport.setDetail(
            "Player Count", () -> this.playerList.getPlayerCount() + " / " + this.playerList.getMaxPlayers() + "; " + this.playerList.getPlayers()
         );
      }

      systemReport.setDetail("Active Data Packs", () -> PackRepository.displayPackList(this.packRepository.getSelectedPacks()));
      systemReport.setDetail("Available Data Packs", () -> PackRepository.displayPackList(this.packRepository.getAvailablePacks()));
      systemReport.setDetail(
         "Enabled Feature Flags",
         () -> FeatureFlags.REGISTRY.toNames(this.worldData.enabledFeatures()).stream().map(Identifier::toString).collect(Collectors.joining(", "))
      );
      systemReport.setDetail("World Generation", () -> this.worldData.worldGenSettingsLifecycle().toString());
      systemReport.setDetail("World Seed", () -> String.valueOf(this.worldGenSettings.options().seed()));
      systemReport.setDetail("Suppressed Exceptions", this.suppressedExceptions::dump);
      if (this.serverId != null) {
         systemReport.setDetail("Server Id", () -> this.serverId);
      }

      return this.fillServerSystemReport(systemReport);
   }

   public abstract SystemReport fillServerSystemReport(final SystemReport systemReport);

   public ModCheck getModdedStatus() {
      return ModCheck.identify("vanilla", this::getServerModName, "Server", MinecraftServer.class);
   }

   @Override
   public void sendSystemMessage(final Component message) {
      LOGGER.info("System chat: {}", message.getString());
   }

   public KeyPair getKeyPair() {
      return Objects.requireNonNull(this.keyPair);
   }

   public int getPort() {
      return this.port;
   }

   public void setPort(final int port) {
      this.port = port;
   }

   public @Nullable GameProfile getSingleplayerProfile() {
      return this.singleplayerProfile;
   }

   public void setSingleplayerProfile(final @Nullable GameProfile singleplayerProfile) {
      this.singleplayerProfile = singleplayerProfile;
   }

   public boolean isSingleplayer() {
      return this.singleplayerProfile != null;
   }

   protected void initializeKeyPair() {
      LOGGER.info("Generating keypair");

      try {
         this.keyPair = Crypt.generateKeyPair();
      } catch (CryptException e) {
         throw new IllegalStateException("Failed to generate key pair", e);
      }
   }

   public void setDifficulty(final Difficulty difficulty, final boolean ignoreLock) {
      if (ignoreLock || !this.worldData.isDifficultyLocked()) {
         this.worldData.setDifficulty(this.worldData.isHardcore() ? Difficulty.HARD : difficulty);
         this.updateMobSpawningFlags();
         this.getPlayerList().getPlayers().forEach(this::sendDifficultyUpdate);
      }
   }

   public int getScaledTrackingDistance(final int baseRange) {
      return baseRange;
   }

   public void updateMobSpawningFlags() {
      for (ServerLevel level : this.getAllLevels()) {
         level.setSpawnSettings(level.isSpawningMonsters());
      }
   }

   public void setDifficultyLocked(final boolean locked) {
      this.worldData.setDifficultyLocked(locked);
      this.getPlayerList().getPlayers().forEach(this::sendDifficultyUpdate);
   }

   private void sendDifficultyUpdate(final ServerPlayer player) {
      LevelData levelData = player.level().getLevelData();
      player.connection.send(new ClientboundChangeDifficultyPacket(levelData.getDifficulty(), levelData.isDifficultyLocked()));
   }

   public boolean isDemo() {
      return this.isDemo;
   }

   public void setDemo(final boolean demo) {
      this.isDemo = demo;
   }

   public Map<String, String> getCodeOfConducts() {
      return Map.of();
   }

   public Optional<MinecraftServer.ServerResourcePackInfo> getServerResourcePack() {
      return Optional.empty();
   }

   public boolean isResourcePackRequired() {
      return this.getServerResourcePack().filter(MinecraftServer.ServerResourcePackInfo::isRequired).isPresent();
   }

   public abstract boolean isDedicatedServer();

   public abstract int getRateLimitPacketsPerSecond();

   public abstract int getCommandSpamThresholdSeconds();

   public abstract int getChatSpamThresholdSeconds();

   public boolean usesAuthentication() {
      return this.onlineMode;
   }

   public void setUsesAuthentication(final boolean onlineMode) {
      this.onlineMode = onlineMode;
   }

   public boolean getPreventProxyConnections() {
      return this.preventProxyConnections;
   }

   public void setPreventProxyConnections(final boolean preventProxyConnections) {
      this.preventProxyConnections = preventProxyConnections;
   }

   public abstract boolean useNativeTransport();

   public boolean allowFlight() {
      return true;
   }

   @Override
   public String getMotd() {
      return this.motd;
   }

   public void setMotd(final String motd) {
      this.motd = motd;
   }

   public boolean isStopped() {
      return this.stopped;
   }

   public PlayerList getPlayerList() {
      return this.playerList;
   }

   public void setPlayerList(final PlayerList players) {
      this.playerList = players;
   }

   public abstract boolean isPublished();

   public void setDefaultGameType(final GameType gameType) {
      this.worldData.setGameType(gameType);
   }

   public int enforceGameTypeForPlayers(final @Nullable GameType gameType) {
      if (gameType == null) {
         return 0;
      }

      int count = 0;

      for (ServerPlayer player : this.getPlayerList().getPlayers()) {
         if (player.setGameMode(gameType)) {
            count++;
         }
      }

      return count;
   }

   public ServerConnectionListener getConnection() {
      return this.connection;
   }

   public boolean isReady() {
      return this.isReady;
   }

   public boolean publishServer(final MinecraftServer.MultiplayerScope scope, final @Nullable GameType gameMode, final boolean allowCommands, final int port) {
      return false;
   }

   public boolean unpublishServer() {
      return false;
   }

   public int getTickCount() {
      return this.tickCount;
   }

   public boolean isUnderSpawnProtection(final ServerLevel level, final BlockPos pos, final Player player) {
      return false;
   }

   public boolean repliesToStatus() {
      return true;
   }

   public boolean hidesOnlinePlayers() {
      return false;
   }

   public Proxy getProxy() {
      return this.proxy;
   }

   public int playerIdleTimeout() {
      return this.playerIdleTimeout;
   }

   public void setPlayerIdleTimeout(final int playerIdleTimeout) {
      this.playerIdleTimeout = playerIdleTimeout;
   }

   public Services services() {
      return this.services;
   }

   public @Nullable ServerStatus getStatus() {
      return this.status;
   }

   public void invalidateStatus() {
      this.lastServerStatus = 0L;
   }

   public int getAbsoluteMaxWorldSize() {
      return 29999984;
   }

   @Override
   public boolean scheduleExecutables() {
      return super.scheduleExecutables() && !this.isStopped();
   }

   @Override
   public void executeIfPossible(final Runnable command) {
      if (this.isStopped()) {
         throw new RejectedExecutionException("Server already shutting down");
      }

      super.executeIfPossible(command);
   }

   @Override
   public Thread getRunningThread() {
      return this.serverThread;
   }

   public int getCompressionThreshold() {
      return 256;
   }

   public boolean enforceSecureProfile() {
      return false;
   }

   public long getNextTickTime() {
      return this.nextTickTimeNanos;
   }

   public DataFixer getFixerUpper() {
      return this.fixerUpper;
   }

   public ServerAdvancementManager getAdvancements() {
      return this.resources.managers.getAdvancements();
   }

   public ServerFunctionManager getFunctions() {
      return this.functionManager;
   }

   public CompletableFuture<Void> reloadResources(final Collection<String> packsToEnable) {
      CompletableFuture<Void> result = CompletableFuture.<ImmutableList>supplyAsync(
            () -> packsToEnable.stream()
               .map(this.packRepository::getPack)
               .filter(Objects::nonNull)
               .flatMap(Pack::open)
               .collect(ImmutableList.toImmutableList()),
            this
         )
         .thenCompose(
            packsToLoad -> {
               CloseableResourceManager resources = new MultiPackResourceManager(PackType.SERVER_DATA, packsToLoad);
               List<Registry.PendingTags<?>> postponedTags = TagLoader.loadTagsForExistingRegistries(resources, this.registries.compositeAccess());
               return ReloadableServerResources.loadResources(
                     resources,
                     this.registries,
                     postponedTags,
                     this.worldData.enabledFeatures(),
                     this.isDedicatedServer() ? Commands.CommandSelection.DEDICATED : Commands.CommandSelection.INTEGRATED,
                     this.getFunctionCompilationPermissions(),
                     this.executor,
                     this
                  )
                  .whenComplete((unit, throwable) -> {
                     if (throwable != null) {
                        resources.close();
                     }
                  })
                  .thenApply(managers -> new MinecraftServer.ReloadableResources(resources, managers));
            }
         )
         .thenAcceptAsync(newResources -> {
            this.resources.close();
            this.resources = newResources;
            this.packRepository.setSelected(packsToEnable);
            WorldDataConfiguration newConfig = new WorldDataConfiguration(getSelectedPacks(this.packRepository, true), this.worldData.enabledFeatures());
            this.worldData.setDataConfiguration(newConfig);
            this.resources.managers.updateComponentsAndStaticRegistryTags();
            this.resources.managers.getRecipeManager().finalizeRecipeLoading(this.worldData.enabledFeatures());
            this.getPlayerList().saveAll();
            this.getPlayerList().reloadResources();
            this.functionManager.replaceLibrary(this.resources.managers.getFunctionLibrary());
            this.structureTemplateManager.onResourceManagerReload(this.resources.resourceManager);
            this.fuelValues = FuelValues.vanillaBurnTimes(this.registries.compositeAccess(), this.worldData.enabledFeatures());
         }, this);
      if (this.isSameThread()) {
         this.managedBlock(result::isDone);
      }

      return result;
   }

   public static WorldDataConfiguration configurePackRepository(
      final PackRepository packRepository, final WorldDataConfiguration initialDataConfig, final boolean initMode, final boolean safeMode
   ) {
      DataPackConfig dataPackConfig = initialDataConfig.dataPacks();
      FeatureFlagSet forcedFeatures = initMode ? FeatureFlagSet.of() : initialDataConfig.enabledFeatures();
      FeatureFlagSet allowedFeatures = initMode ? FeatureFlags.REGISTRY.allFlags() : initialDataConfig.enabledFeatures();
      packRepository.reload();
      if (safeMode) {
         return configureRepositoryWithSelection(packRepository, List.of("vanilla"), forcedFeatures, false);
      }

      Set<String> selected = Sets.newLinkedHashSet();

      for (String id : dataPackConfig.getEnabled()) {
         if (packRepository.isAvailable(id)) {
            selected.add(id);
         } else {
            LOGGER.warn("Missing data pack {}", id);
         }
      }

      for (Pack pack : packRepository.getAvailablePacks()) {
         String packId = pack.getId();
         if (!dataPackConfig.getDisabled().contains(packId)) {
            FeatureFlagSet packFeatures = pack.getRequestedFeatures();
            boolean isSelected = selected.contains(packId);
            if (!isSelected && pack.getPackSource().shouldAddAutomatically()) {
               if (packFeatures.isSubsetOf(allowedFeatures)) {
                  LOGGER.info("Found new data pack {}, loading it automatically", packId);
                  selected.add(packId);
               } else {
                  LOGGER.info(
                     "Found new data pack {}, but can't load it due to missing features {}",
                     packId,
                     FeatureFlags.printMissingFlags(allowedFeatures, packFeatures)
                  );
               }
            }

            if (isSelected && !packFeatures.isSubsetOf(allowedFeatures)) {
               LOGGER.warn(
                  "Pack {} requires features {} that are not enabled for this world, disabling pack.",
                  packId,
                  FeatureFlags.printMissingFlags(allowedFeatures, packFeatures)
               );
               selected.remove(packId);
            }
         }
      }

      if (selected.isEmpty()) {
         LOGGER.info("No datapacks selected, forcing vanilla");
         selected.add("vanilla");
      }

      return configureRepositoryWithSelection(packRepository, selected, forcedFeatures, true);
   }

   private static WorldDataConfiguration configureRepositoryWithSelection(
      final PackRepository packRepository, final Collection<String> selected, final FeatureFlagSet forcedFeatures, final boolean disableInactive
   ) {
      packRepository.setSelected(selected);
      enableForcedFeaturePacks(packRepository, forcedFeatures);
      DataPackConfig packConfig = getSelectedPacks(packRepository, disableInactive);
      FeatureFlagSet packRequestedFeatures = packRepository.getRequestedFeatureFlags().join(forcedFeatures);
      return new WorldDataConfiguration(packConfig, packRequestedFeatures);
   }

   private static void enableForcedFeaturePacks(final PackRepository packRepository, final FeatureFlagSet forcedFeatures) {
      FeatureFlagSet providedFeatures = packRepository.getRequestedFeatureFlags();
      FeatureFlagSet missingFeatures = forcedFeatures.subtract(providedFeatures);
      if (!missingFeatures.isEmpty()) {
         Set<String> selected = new ObjectArraySet(packRepository.getSelectedIds());

         for (Pack pack : packRepository.getAvailablePacks()) {
            if (missingFeatures.isEmpty()) {
               break;
            }

            if (pack.getPackSource() == PackSource.FEATURE) {
               String packId = pack.getId();
               FeatureFlagSet packFeatures = pack.getRequestedFeatures();
               if (!packFeatures.isEmpty() && packFeatures.intersects(missingFeatures) && packFeatures.isSubsetOf(forcedFeatures)) {
                  if (!selected.add(packId)) {
                     throw new IllegalStateException("Tried to force '" + packId + "', but it was already enabled");
                  }

                  LOGGER.info("Found feature pack ('{}') for requested feature, forcing to enabled", packId);
                  missingFeatures = missingFeatures.subtract(packFeatures);
               }
            }
         }

         packRepository.setSelected(selected);
      }
   }

   private static DataPackConfig getSelectedPacks(final PackRepository packRepository, final boolean disableInactive) {
      Collection<String> selected = packRepository.getSelectedIds();
      List<String> enabled = ImmutableList.copyOf(selected);
      List<String> disabled = disableInactive ? packRepository.getAvailableIds().stream().filter(id -> !selected.contains(id)).toList() : List.of();
      return new DataPackConfig(enabled, disabled);
   }

   public void kickUnlistedPlayers() {
      if (this.isEnforceWhitelist() && this.isUsingWhitelist()) {
         PlayerList playerList = this.getPlayerList();
         UserWhiteList whiteList = playerList.getWhiteList();

         for (ServerPlayer player : Lists.newArrayList(playerList.getPlayers())) {
            if (!whiteList.isWhiteListed(player.nameAndId())) {
               player.connection.disconnect(Component.translatable("multiplayer.disconnect.not_whitelisted"));
            }
         }
      }
   }

   public PackRepository getPackRepository() {
      return this.packRepository;
   }

   public Commands getCommands() {
      return this.resources.managers.getCommands();
   }

   public CommandSourceStack createCommandSourceStack() {
      ServerLevel level = this.findRespawnDimension();
      return new CommandSourceStack(
         this,
         Vec3.atLowerCornerOf(this.getRespawnData().pos()),
         Vec2.ZERO,
         level,
         LevelBasedPermissionSet.OWNER,
         "Server",
         Component.literal("Server"),
         this,
         null
      );
   }

   public ServerLevel findRespawnDimension() {
      LevelData.RespawnData respawnData = this.getWorldData().overworldData().getRespawnData();
      ResourceKey<Level> respawnDimension = respawnData.dimension();
      ServerLevel respawnLevel = this.getLevel(respawnDimension);
      return respawnLevel != null ? respawnLevel : this.overworld();
   }

   public void setRespawnData(final LevelData.RespawnData respawnData) {
      ServerLevelData levelData = this.worldData.overworldData();
      LevelData.RespawnData oldRespawnData = levelData.getRespawnData();
      if (!oldRespawnData.equals(respawnData)) {
         levelData.setSpawn(respawnData);
         this.getPlayerList().broadcastAll(new ClientboundSetDefaultSpawnPositionPacket(respawnData));
         this.updateEffectiveRespawnData();
      }
   }

   public LevelData.RespawnData getRespawnData() {
      return this.effectiveRespawnData;
   }

   @Override
   public boolean acceptsSuccess() {
      return true;
   }

   @Override
   public boolean acceptsFailure() {
      return true;
   }

   @Override
   public abstract boolean shouldInformAdmins();

   public WorldGenSettings getWorldGenSettings() {
      return this.worldGenSettings;
   }

   public RecipeManager getRecipeManager() {
      return this.resources.managers.getRecipeManager();
   }

   public ServerScoreboard getScoreboard() {
      return this.scoreboard;
   }

   public CommandStorage getCommandStorage() {
      if (this.commandStorage == null) {
         throw new NullPointerException("Called before server init");
      } else {
         return this.commandStorage;
      }
   }

   public Stopwatches getStopwatches() {
      if (this.stopwatches == null) {
         throw new NullPointerException("Called before server init");
      } else {
         return this.stopwatches;
      }
   }

   public CustomBossEvents getCustomBossEvents() {
      return this.customBossEvents;
   }

   public RandomSource getRandomSequence(final Identifier key) {
      return this.randomSequences.get(key, this.worldGenSettings.options().seed());
   }

   public RandomSequences getRandomSequences() {
      return this.randomSequences;
   }

   public void setWeatherParameters(final int clearTime, final int rainTime, final boolean raining, final boolean thundering) {
      WeatherData weatherData = this.getWeatherData();
      weatherData.setClearWeatherTime(clearTime);
      weatherData.setRainTime(rainTime);
      weatherData.setThunderTime(rainTime);
      weatherData.setRaining(raining);
      weatherData.setThundering(thundering);
   }

   public WeatherData getWeatherData() {
      return this.weatherData;
   }

   public boolean isEnforceWhitelist() {
      return this.enforceWhitelist;
   }

   public void setEnforceWhitelist(final boolean enforceWhitelist) {
      this.enforceWhitelist = enforceWhitelist;
   }

   public boolean isUsingWhitelist() {
      return this.usingWhitelist;
   }

   public void setUsingWhitelist(final boolean usingWhitelist) {
      this.usingWhitelist = usingWhitelist;
   }

   public float getCurrentSmoothedTickTime() {
      return this.smoothedTickTimeMillis;
   }

   public ServerTickRateManager tickRateManager() {
      return this.tickRateManager;
   }

   public long getAverageTickTimeNanos() {
      return this.aggregatedTickTimesNanos / Math.min(100, Math.max(this.tickCount, 1));
   }

   public long[] getTickTimesNanos() {
      return this.tickTimesNanos;
   }

   public LevelBasedPermissionSet getProfilePermissions(final NameAndId nameAndId) {
      if (this.getPlayerList().isOp(nameAndId)) {
         ServerOpListEntry opListEntry = this.getPlayerList().getOps().get(nameAndId);
         if (opListEntry != null) {
            return opListEntry.permissions();
         } else if (this.isSingleplayerOwner(nameAndId)) {
            return LevelBasedPermissionSet.OWNER;
         } else if (this.isSingleplayer()) {
            return this.getPlayerList().isAllowCommandsForAllPlayers() ? LevelBasedPermissionSet.OWNER : LevelBasedPermissionSet.ALL;
         } else {
            return this.operatorUserPermissions();
         }
      } else {
         return LevelBasedPermissionSet.ALL;
      }
   }

   public abstract boolean isSingleplayerOwner(NameAndId nameAndId);

   public void dumpServerProperties(final Path path) throws IOException {
   }

   private void saveDebugReport(final Path output) {
      Path levelsDir = output.resolve("levels");

      try {
         for (Entry<ResourceKey<Level>, ServerLevel> level : this.levels.entrySet()) {
            Identifier levelId = level.getKey().identifier();
            Path levelPath = levelId.resolveAgainst(levelsDir);
            Files.createDirectories(levelPath);
            level.getValue().saveDebugReport(levelPath);
         }

         this.dumpGameRules(output.resolve("gamerules.txt"));
         this.dumpClasspath(output.resolve("classpath.txt"));
         this.dumpMiscStats(output.resolve("stats.txt"));
         this.dumpThreads(output.resolve("threads.txt"));
         this.dumpServerProperties(output.resolve("server.properties.txt"));
         this.dumpNativeModules(output.resolve("modules.txt"));
      } catch (IOException e) {
         LOGGER.warn("Failed to save debug report", e);
      }
   }

   private void dumpMiscStats(final Path path) throws IOException {
      try (Writer output = Files.newBufferedWriter(path)) {
         output.write(String.format(Locale.ROOT, "pending_tasks: %d\n", this.getPendingTasksCount()));
         output.write(String.format(Locale.ROOT, "average_tick_time: %f\n", this.getCurrentSmoothedTickTime()));
         output.write(String.format(Locale.ROOT, "tick_times: %s\n", Arrays.toString(this.tickTimesNanos)));
         output.write(String.format(Locale.ROOT, "queue: %s\n", Util.backgroundExecutor()));
      }
   }

   private void dumpGameRules(final Path path) throws IOException {
      try (Writer output = Files.newBufferedWriter(path)) {
         final List<String> entries = Lists.newArrayList();
         final GameRules gameRules = this.getGameRules();
         gameRules.visitGameRuleTypes(new GameRuleTypeVisitor() {
            @Override
            public <T> void visit(final GameRule<T> gameRule) {
               entries.add(String.format(Locale.ROOT, "%s=%s\n", gameRule.getIdentifier(), gameRules.getAsString(gameRule)));
            }
         });

         for (String entry : entries) {
            output.write(entry);
         }
      }
   }

   private void dumpClasspath(final Path path) throws IOException {
      try (Writer output = Files.newBufferedWriter(path)) {
         String classpath = System.getProperty("java.class.path");
         String separator = File.pathSeparator;

         for (String s : Splitter.on(separator).split(classpath)) {
            output.write(s);
            output.write("\n");
         }
      }
   }

   private void dumpThreads(final Path path) throws IOException {
      ThreadInfo[] threadInfos = Util.dumpThreadInfo();
      Arrays.sort(threadInfos, Comparator.comparing(ThreadInfo::getThreadName));

      try (Writer output = Files.newBufferedWriter(path)) {
         for (ThreadInfo threadInfo : threadInfos) {
            output.write(threadInfo.toString());
            output.write(10);
         }
      }
   }

   private void dumpNativeModules(final Path path) throws IOException {
      try (Writer output = Files.newBufferedWriter(path)) {
         List<NativeModuleLister.NativeModuleInfo> modules;
         try {
            modules = Lists.newArrayList(NativeModuleLister.listModules());
         } catch (Throwable t) {
            LOGGER.warn("Failed to list native modules", t);
            return;
         }

         modules.sort(Comparator.comparing(NativeModuleLister.NativeModuleInfo::name, String.CASE_INSENSITIVE_ORDER));

         for (NativeModuleLister.NativeModuleInfo module : modules) {
            output.write(module.toString());
            output.write(10);
         }
      }
   }

   private ProfilerFiller createProfiler() {
      if (this.willStartRecordingMetrics) {
         this.metricsRecorder = ActiveMetricsRecorder.createStarted(
            new ServerMetricsSamplersProvider(Util.timeSource(), this.isDedicatedServer()),
            Util.timeSource(),
            Util.ioPool(),
            new MetricsPersister("server"),
            this.onMetricsRecordingStopped,
            reportPath -> {
               this.executeBlocking(() -> this.saveDebugReport(reportPath.resolve("server")));
               this.onMetricsRecordingFinished.accept(reportPath);
            }
         );
         this.willStartRecordingMetrics = false;
      }

      this.metricsRecorder.startTick();
      return SingleTickProfiler.decorateFiller(this.metricsRecorder.getProfiler(), SingleTickProfiler.createTickProfiler("Server"));
   }

   protected void endMetricsRecordingTick() {
      this.metricsRecorder.endTick();
   }

   public boolean isRecordingMetrics() {
      return this.metricsRecorder.isRecording();
   }

   public void startRecordingMetrics(final Consumer<ProfileResults> onStopped, final Consumer<Path> onFinished) {
      this.onMetricsRecordingStopped = report -> {
         this.stopRecordingMetrics();
         onStopped.accept(report);
      };
      this.onMetricsRecordingFinished = onFinished;
      this.willStartRecordingMetrics = true;
   }

   public void stopRecordingMetrics() {
      this.metricsRecorder = InactiveMetricsRecorder.INSTANCE;
   }

   public void finishRecordingMetrics() {
      this.metricsRecorder.end();
   }

   public void cancelRecordingMetrics() {
      this.metricsRecorder.cancel();
   }

   public Path getWorldPath(final LevelResource resource) {
      return this.storageSource.getLevelPath(resource);
   }

   public boolean forceSynchronousWrites() {
      return true;
   }

   public StructureTemplateManager getStructureManager() {
      return this.structureTemplateManager;
   }

   public WorldData getWorldData() {
      return this.worldData;
   }

   public RegistryAccess.Frozen registryAccess() {
      return this.registries.compositeAccess();
   }

   public LayeredRegistryAccess<RegistryLayer> registries() {
      return this.registries;
   }

   public ReloadableServerRegistries.Holder reloadableRegistries() {
      return this.resources.managers.fullRegistries();
   }

   public TextFilter createTextFilterForPlayer(final ServerPlayer player) {
      return TextFilter.DUMMY;
   }

   public ServerPlayerGameMode createGameModeForPlayer(final ServerPlayer player) {
      return this.isDemo() ? new DemoMode(player) : new ServerPlayerGameMode(player);
   }

   public @Nullable GameType getForcedGameType() {
      return null;
   }

   public ResourceManager getResourceManager() {
      return this.resources.resourceManager;
   }

   public boolean isCurrentlySaving() {
      return this.isSaving;
   }

   public boolean isTimeProfilerRunning() {
      return this.debugCommandProfilerDelayStart || this.debugCommandProfiler != null;
   }

   public void startTimeProfiler() {
      this.debugCommandProfilerDelayStart = true;
   }

   public ProfileResults stopTimeProfiler() {
      if (this.debugCommandProfiler == null) {
         return EmptyProfileResults.EMPTY;
      }

      ProfileResults results = this.debugCommandProfiler.stop(Util.getNanos(), this.tickCount);
      this.debugCommandProfiler = null;
      return results;
   }

   public int getMaxChainedNeighborUpdates() {
      return 1000000;
   }

   public void logChatMessage(final Component message, final ChatType.Bound chatType, final @Nullable String tag) {
      String decoratedMessage = chatType.decorate(message).getString();
      if (tag != null) {
         LOGGER.info("[{}] {}", tag, decoratedMessage);
      } else {
         LOGGER.info("{}", decoratedMessage);
      }
   }

   public ChatDecorator getChatDecorator() {
      return ChatDecorator.PLAIN;
   }

   public boolean logIPs() {
      return true;
   }

   public void handleCustomClickAction(final Identifier id, final Optional<Tag> payload) {
      LOGGER.debug("Received custom click action {} with payload {}", id, payload.orElse(null));
   }

   public LevelLoadListener getLevelLoadListener() {
      return this.levelLoadListener;
   }

   public boolean setAutoSave(final boolean enable) {
      boolean success = false;

      for (ServerLevel level : this.getAllLevels()) {
         if (level != null && level.noSave == enable) {
            level.noSave = !enable;
            success = true;
         }
      }

      return success;
   }

   public boolean isAutoSave() {
      for (ServerLevel level : this.getAllLevels()) {
         if (level != null && !level.noSave) {
            return true;
         }
      }

      return false;
   }

   public <T> void onGameRuleChanged(final GameRule<T> rule, final T value) {
      this.notificationManager().onGameRuleChanged(rule, value);
      if (rule == GameRules.REDUCED_DEBUG_INFO) {
         byte event = (byte)((Boolean)value ? 22 : 23);

         for (ServerPlayer player : this.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundEntityEventPacket(player, event));
         }
      } else if (rule == GameRules.LIMITED_CRAFTING || rule == GameRules.IMMEDIATE_RESPAWN) {
         ClientboundGameEventPacket.Type eventType = rule == GameRules.LIMITED_CRAFTING
            ? ClientboundGameEventPacket.LIMITED_CRAFTING
            : ClientboundGameEventPacket.IMMEDIATE_RESPAWN;
         ClientboundGameEventPacket packet = new ClientboundGameEventPacket(eventType, (Boolean)value ? 1.0F : 0.0F);
         this.getPlayerList().getPlayers().forEach(playerx -> playerx.connection.send(packet));
      } else if (rule == GameRules.LOCATOR_BAR) {
         this.getAllLevels().forEach(level -> {
            ServerWaypointManager waypointManager = level.getWaypointManager();
            if ((Boolean)value) {
               level.players().forEach(waypointManager::updatePlayer);
            } else {
               waypointManager.breakAllConnections();
            }
         });
      } else if (rule == GameRules.SPAWN_MONSTERS) {
         this.updateMobSpawningFlags();
      } else if (rule == GameRules.ADVANCE_TIME) {
         this.getPlayerList().broadcastAll(this.clockManager().createFullSyncPacket());
      }
   }

   @Deprecated
   public GameRules getGlobalGameRules() {
      return this.overworld().getGameRules();
   }

   public SavedDataStorage getDataStorage() {
      return this.savedDataStorage;
   }

   public TimerQueue<MinecraftServer> getScheduledEvents() {
      return this.scheduledEvents;
   }

   public GameRules getGameRules() {
      return this.gameRules;
   }

   public boolean acceptsTransfers() {
      return false;
   }

   private void storeChunkIoError(final CrashReport report, final ChunkPos pos, final RegionStorageInfo storageInfo) {
      Util.ioPool().execute(() -> {
         try {
            Path debugDir = this.getFile("debug");
            FileUtil.createDirectoriesSafe(debugDir);
            String sanitizedLevelName = FileUtil.sanitizeName(storageInfo.level());
            Path reportFile = debugDir.resolve("chunk-" + sanitizedLevelName + "-" + Util.getFilenameFormattedDateTime() + "-server.txt");
            FileStore fileStore = Files.getFileStore(debugDir);
            long remainingSpace = fileStore.getUsableSpace();
            if (remainingSpace < 8192L) {
               LOGGER.warn("Not storing chunk IO report due to low space on drive {}", fileStore.name());
               return;
            }

            CrashReportCategory category = report.addCategory("Chunk Info");
            category.setDetail("Level", storageInfo::level);
            category.setDetail("Dimension", () -> storageInfo.dimension().identifier().toString());
            category.setDetail("Storage", storageInfo::type);
            category.setDetail("Position", pos::toString);
            report.saveToFile(reportFile, ReportType.CHUNK_IO_ERROR);
            LOGGER.info("Saved details to {}", report.getSaveFile());
         } catch (Exception e) {
            LOGGER.warn("Failed to store chunk IO exception", e);
         }
      });
   }

   @Override
   public void reportChunkLoadFailure(final Throwable throwable, final RegionStorageInfo storageInfo, final ChunkPos pos) {
      LOGGER.error("Failed to load chunk {},{}", new Object[]{pos.x(), pos.z(), throwable});
      this.suppressedExceptions.addEntry("chunk/load", throwable);
      this.storeChunkIoError(CrashReport.forThrowable(throwable, "Chunk load failure"), pos, storageInfo);
      this.warnOnLowDiskSpace();
   }

   @Override
   public void reportChunkSaveFailure(final Throwable throwable, final RegionStorageInfo storageInfo, final ChunkPos pos) {
      LOGGER.error("Failed to save chunk {},{}", new Object[]{pos.x(), pos.z(), throwable});
      this.suppressedExceptions.addEntry("chunk/save", throwable);
      this.storeChunkIoError(CrashReport.forThrowable(throwable, "Chunk save failure"), pos, storageInfo);
      this.warnOnLowDiskSpace();
   }

   protected void warnOnLowDiskSpace() {
      if (this.storageSource.checkForLowDiskSpace()) {
         this.sendLowDiskSpaceWarning();
      }
   }

   public void sendLowDiskSpaceWarning() {
      LOGGER.warn("Low disk space! Might not be able to save the world.");
   }

   public void reportPacketHandlingException(final Throwable throwable, final PacketType<?> packetType) {
      this.suppressedExceptions.addEntry("packet/" + packetType, throwable);
   }

   public FuelValues fuelValues() {
      return this.fuelValues;
   }

   public ServerLinks serverLinks() {
      return ServerLinks.EMPTY;
   }

   protected int pauseWhenEmptySeconds() {
      return 0;
   }

   public PacketProcessor packetProcessor() {
      return this.packetProcessor;
   }

   public ServerDebugSubscribers debugSubscribers() {
      return this.debugSubscribers;
   }

   public enum MultiplayerScope {
      OFF("off"),
      LAN("lan");

      private final Component translatable;
      private final Component tooltip;

      MultiplayerScope(final String key) {
         this.translatable = Component.translatable("menu.multiplayerOptions.network." + key);
         this.tooltip = Component.translatable("menu.multiplayerOptions.network." + key + ".tooltip");
      }

      public Component getDisplayName() {
         return this.translatable;
      }

      public Component getTooltip() {
         return this.tooltip;
      }
   }

   private record ReloadableResources(CloseableResourceManager resourceManager, ReloadableServerResources managers) implements AutoCloseable {
      @Override
      public void close() {
         this.resourceManager.close();
      }
   }

   public record ServerResourcePackInfo(UUID id, String url, String hash, boolean isRequired, @Nullable Component prompt) {
   }

   private static class TimeProfiler {
      private final long startNanos;
      private final int startTick;

      private TimeProfiler(final long startNanos, final int startTick) {
         this.startNanos = startNanos;
         this.startTick = startTick;
      }

      private ProfileResults stop(final long stopNanos, final int stopTick) {
         return new ProfileResults() {
            @Override
            public List<ResultField> getTimes(final String path) {
               return Collections.emptyList();
            }

            @Override
            public boolean saveResults(final Path file) {
               return false;
            }

            @Override
            public long getStartTimeNano() {
               return TimeProfiler.this.startNanos;
            }

            @Override
            public int getStartTimeTicks() {
               return TimeProfiler.this.startTick;
            }

            @Override
            public long getEndTimeNano() {
               return stopNanos;
            }

            @Override
            public int getEndTimeTicks() {
               return stopTick;
            }

            @Override
            public String getProfilerResults() {
               return "";
            }
         };
      }
   }
}
