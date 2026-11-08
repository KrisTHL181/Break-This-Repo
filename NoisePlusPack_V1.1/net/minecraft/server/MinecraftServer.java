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
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
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
import net.minecraft.obfuscate.DontObfuscate;
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
import net.minecraft.world.entity.ai.village.VillageSiege;
import net.minecraft.world.entity.npc.CatSpawner;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTraderSpawner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.alchemy.PotionBrewing;
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
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.ChunkIOErrorReporter;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.CommandStorage;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreboardSaveData;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class MinecraftServer extends ReentrantBlockableEventLoop<TickTask> implements ServerInfo, CommandSource, ChunkIOErrorReporter {
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
      "Demo World", GameType.SURVIVAL, false, Difficulty.NORMAL, false, new GameRules(FeatureFlags.DEFAULT_FLAGS), WorldDataConfiguration.DEFAULT
   );
   public static final NameAndId ANONYMOUS_PLAYER_PROFILE = new NameAndId(Util.NIL_UUID, "Anonymous Player");
   protected final LevelStorageSource.LevelStorageAccess storageSource;
   protected final PlayerDataStorage playerDataStorage;
   private final List<Runnable> tickables = Lists.newArrayList();
   private MetricsRecorder metricsRecorder = InactiveMetricsRecorder.INSTANCE;
   private Consumer<ProfileResults> onMetricsRecordingStopped = p_177903_ -> this.stopRecordingMetrics();
   private Consumer<Path> onMetricsRecordingFinished = p_177954_ -> {};
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
   private final ServerScoreboard scoreboard = new ServerScoreboard(this);
   private @Nullable Stopwatches stopwatches;
   private @Nullable CommandStorage commandStorage;
   private final CustomBossEvents customBossEvents = new CustomBossEvents();
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
   private final PotionBrewing potionBrewing;
   private FuelValues fuelValues;
   private int emptyTicks;
   private volatile boolean isSaving;
   private static final AtomicReference<@Nullable RuntimeException> fatalException = new AtomicReference<>();
   private final SuppressedExceptionCollector suppressedExceptions = new SuppressedExceptionCollector();
   private final DiscontinuousFrame tickFrame;
   private final PacketProcessor packetProcessor;

   public static <S extends MinecraftServer> S spin(Function<Thread, S> p_129873_) {
      AtomicReference<S> atomicreference = new AtomicReference<>();
      Thread thread = new Thread(() -> atomicreference.get().runServer(), "Server thread");
      thread.setUncaughtExceptionHandler((p_177909_, p_177910_) -> LOGGER.error("Uncaught exception in server thread", p_177910_));
      if (Runtime.getRuntime().availableProcessors() > 4) {
         thread.setPriority(8);
      }

      S s = (S)p_129873_.apply(thread);
      atomicreference.set(s);
      thread.start();
      return s;
   }

   public MinecraftServer(
      Thread p_236723_,
      LevelStorageSource.LevelStorageAccess p_236724_,
      PackRepository p_236725_,
      WorldStem p_236726_,
      Proxy p_236727_,
      DataFixer p_236728_,
      Services p_236729_,
      LevelLoadListener p_427762_
   ) {
      super("Server");
      this.registries = p_236726_.registries();
      this.worldData = p_236726_.worldData();
      if (!this.registries.compositeAccess().lookupOrThrow(Registries.LEVEL_STEM).containsKey(LevelStem.OVERWORLD)) {
         throw new IllegalStateException("Missing Overworld dimension data");
      }

      this.proxy = p_236727_;
      this.packRepository = p_236725_;
      this.resources = new MinecraftServer.ReloadableResources(p_236726_.resourceManager(), p_236726_.dataPackResources());
      this.services = p_236729_;
      this.connection = new ServerConnectionListener(this);
      this.tickRateManager = new ServerTickRateManager(this);
      this.levelLoadListener = p_427762_;
      this.storageSource = p_236724_;
      this.playerDataStorage = p_236724_.createPlayerStorage();
      this.fixerUpper = p_236728_;
      this.functionManager = new ServerFunctionManager(this, this.resources.managers.getFunctionLibrary());
      HolderGetter<Block> holdergetter = this.registries.compositeAccess().lookupOrThrow(Registries.BLOCK).filterFeatures(this.worldData.enabledFeatures());
      this.structureTemplateManager = new StructureTemplateManager(p_236726_.resourceManager(), p_236724_, p_236728_, holdergetter);
      this.serverThread = p_236723_;
      this.executor = Util.backgroundExecutor();
      this.potionBrewing = PotionBrewing.bootstrap(this.worldData.enabledFeatures());
      this.resources.managers.getRecipeManager().finalizeRecipeLoading(this.worldData.enabledFeatures());
      this.fuelValues = FuelValues.vanillaBurnTimes(this.registries.compositeAccess(), this.worldData.enabledFeatures());
      this.tickFrame = TracyClient.createDiscontinuousFrame("Server Tick");
      this.notificationManager = new NotificationManager();
      this.serverActivityMonitor = new ServerActivityMonitor(this.notificationManager, 30);
      this.packetProcessor = new PacketProcessor(p_236723_);
   }

   protected abstract boolean initServer() throws IOException;

   public ChunkLoadStatusView createChunkLoadStatusView(final int p_424502_) {
      return new ChunkLoadStatusView() {
         private @Nullable ChunkMap chunkMap;
         private int centerChunkX;
         private int centerChunkZ;

         @Override
         public void moveTo(ResourceKey<Level> p_422865_, ChunkPos p_424903_) {
            ServerLevel serverlevel = MinecraftServer.this.getLevel(p_422865_);
            this.chunkMap = serverlevel != null ? serverlevel.getChunkSource().chunkMap : null;
            this.centerChunkX = p_424903_.x;
            this.centerChunkZ = p_424903_.z;
         }

         @Override
         public @Nullable ChunkStatus get(int p_430685_, int p_428140_) {
            return this.chunkMap == null
               ? null
               : this.chunkMap.getLatestStatus(ChunkPos.asLong(p_430685_ + this.centerChunkX - p_424502_, p_428140_ + this.centerChunkZ - p_424502_));
         }

         @Override
         public int radius() {
            return p_424502_;
         }
      };
   }

   protected void loadLevel() {
      boolean flag = !JvmProfiler.INSTANCE.isRunning()
         && SharedConstants.DEBUG_JFR_PROFILING_ENABLE_LEVEL_LOADING
         && JvmProfiler.INSTANCE.start(Environment.from(this));
      ProfiledDuration profiledduration = JvmProfiler.INSTANCE.onWorldLoadedStarted();
      this.worldData.setModdedInfo(this.getServerModName(), this.getModdedStatus().shouldReportAsModified());
      this.createLevels();
      this.forceDifficulty();
      this.prepareLevels();
      if (profiledduration != null) {
         profiledduration.finish(true);
      }

      if (flag) {
         try {
            JvmProfiler.INSTANCE.stop();
         } catch (Throwable throwable) {
            LOGGER.warn("Failed to stop JFR profiling", throwable);
         }
      }
   }

   protected void forceDifficulty() {
   }

   protected void createLevels() {
      ServerLevelData serverleveldata = this.worldData.overworldData();
      boolean flag = this.worldData.isDebugWorld();
      Registry<LevelStem> registry = this.registries.compositeAccess().lookupOrThrow(Registries.LEVEL_STEM);
      WorldOptions worldoptions = this.worldData.worldGenOptions();
      long i = worldoptions.seed();
      long j = BiomeManager.obfuscateSeed(i);
      List<CustomSpawner> list = ImmutableList.of(
         new PhantomSpawner(), new PatrolSpawner(), new CatSpawner(), new VillageSiege(), new WanderingTraderSpawner(serverleveldata)
      );
      LevelStem levelstem = registry.getValue(LevelStem.OVERWORLD);
      ServerLevel serverlevel = new ServerLevel(this, this.executor, this.storageSource, serverleveldata, Level.OVERWORLD, levelstem, flag, j, list, true, null);
      this.levels.put(Level.OVERWORLD, serverlevel);
      DimensionDataStorage dimensiondatastorage = serverlevel.getDataStorage();
      this.scoreboard.load(dimensiondatastorage.computeIfAbsent(ScoreboardSaveData.TYPE).getData());
      this.commandStorage = new CommandStorage(dimensiondatastorage);
      this.stopwatches = dimensiondatastorage.computeIfAbsent(Stopwatches.TYPE);
      if (!serverleveldata.isInitialized()) {
         try {
            setInitialSpawn(serverlevel, serverleveldata, worldoptions.generateBonusChest(), flag, this.levelLoadListener);
            serverleveldata.setInitialized(true);
            if (flag) {
               this.setupDebugLevel(this.worldData);
            }
         } catch (Throwable throwable1) {
            CrashReport crashreport = CrashReport.forThrowable(throwable1, "Exception initializing level");

            try {
               serverlevel.fillReportDetails(crashreport);
            } catch (Throwable var27) {
            }

            throw new ReportedException(crashreport);
         }

         serverleveldata.setInitialized(true);
      }

      GlobalPos globalpos = this.selectLevelLoadFocusPos();
      this.levelLoadListener.updateFocus(globalpos.dimension(), new ChunkPos(globalpos.pos()));
      if (this.worldData.getCustomBossEvents() != null) {
         this.getCustomBossEvents().load(this.worldData.getCustomBossEvents(), this.registryAccess());
      }

      RandomSequences randomsequences = serverlevel.getRandomSequences();
      boolean flag1 = false;

      for (Entry<ResourceKey<LevelStem>, LevelStem> entry : registry.entrySet()) {
         ResourceKey<LevelStem> resourcekey = entry.getKey();
         ServerLevel serverlevel1;
         if (resourcekey != LevelStem.OVERWORLD) {
            ResourceKey<Level> resourcekey1 = ResourceKey.create(Registries.DIMENSION, resourcekey.identifier());
            DerivedLevelData derivedleveldata = new DerivedLevelData(this.worldData, serverleveldata);
            serverlevel1 = new ServerLevel(
               this, this.executor, this.storageSource, derivedleveldata, resourcekey1, entry.getValue(), flag, j, ImmutableList.of(), false, randomsequences
            );
            this.levels.put(resourcekey1, serverlevel1);
         } else {
            serverlevel1 = serverlevel;
         }

         Optional<WorldBorder.Settings> optional = serverleveldata.getLegacyWorldBorderSettings();
         if (optional.isPresent()) {
            WorldBorder.Settings worldborder$settings1 = optional.get();
            DimensionDataStorage dimensiondatastorage1 = serverlevel1.getDataStorage();
            if (dimensiondatastorage1.get(WorldBorder.TYPE) == null) {
               double d0 = serverlevel1.dimensionType().coordinateScale();
               WorldBorder.Settings worldborder$settings = new WorldBorder.Settings(
                  worldborder$settings1.centerX() / d0,
                  worldborder$settings1.centerZ() / d0,
                  worldborder$settings1.damagePerBlock(),
                  worldborder$settings1.safeZone(),
                  worldborder$settings1.warningBlocks(),
                  worldborder$settings1.warningTime(),
                  worldborder$settings1.size(),
                  worldborder$settings1.lerpTime(),
                  worldborder$settings1.lerpTarget()
               );
               WorldBorder worldborder = new WorldBorder(worldborder$settings);
               worldborder.applyInitialSettings(serverlevel1.getGameTime());
               dimensiondatastorage1.set(WorldBorder.TYPE, worldborder);
            }

            flag1 = true;
         }

         serverlevel1.getWorldBorder().setAbsoluteMaxSize(this.getAbsoluteMaxWorldSize());
         this.getPlayerList().addWorldborderListener(serverlevel1);
      }

      if (flag1) {
         serverleveldata.setLegacyWorldBorderSettings(Optional.empty());
      }
   }

   private static void setInitialSpawn(ServerLevel p_177897_, ServerLevelData p_177898_, boolean p_177899_, boolean p_177900_, LevelLoadListener p_431711_) {
      if (SharedConstants.DEBUG_ONLY_GENERATE_HALF_THE_WORLD && SharedConstants.DEBUG_WORLD_RECREATE) {
         p_177898_.setSpawn(LevelData.RespawnData.of(p_177897_.dimension(), new BlockPos(0, 64, -100), 0.0F, 0.0F));
      } else if (p_177900_) {
         p_177898_.setSpawn(LevelData.RespawnData.of(p_177897_.dimension(), BlockPos.ZERO.above(80), 0.0F, 0.0F));
      } else {
         ServerChunkCache serverchunkcache = p_177897_.getChunkSource();
         ChunkPos chunkpos = new ChunkPos(serverchunkcache.randomState().sampler().findSpawnPosition());
         p_431711_.start(LevelLoadListener.Stage.PREPARE_GLOBAL_SPAWN, 0);
         p_431711_.updateFocus(p_177897_.dimension(), chunkpos);
         int i = serverchunkcache.getGenerator().getSpawnHeight(p_177897_);
         if (i < p_177897_.getMinY()) {
            BlockPos blockpos = chunkpos.getWorldPosition();
            i = p_177897_.getHeight(Heightmap.Types.WORLD_SURFACE, blockpos.getX() + 8, blockpos.getZ() + 8);
         }

         p_177898_.setSpawn(LevelData.RespawnData.of(p_177897_.dimension(), chunkpos.getWorldPosition().offset(8, i, 8), 0.0F, 0.0F));
         int j1 = 0;
         int j = 0;
         int k = 0;
         int l = -1;

         for (int i1 = 0; i1 < Mth.square(11); i1++) {
            if (j1 >= -5 && j1 <= 5 && j >= -5 && j <= 5) {
               BlockPos blockpos1 = PlayerSpawnFinder.getSpawnPosInChunk(p_177897_, new ChunkPos(chunkpos.x + j1, chunkpos.z + j));
               if (blockpos1 != null) {
                  p_177898_.setSpawn(LevelData.RespawnData.of(p_177897_.dimension(), blockpos1, 0.0F, 0.0F));
                  break;
               }
            }

            if (j1 == j || j1 < 0 && j1 == -j || j1 > 0 && j1 == 1 - j) {
               int k1 = k;
               k = -l;
               l = k1;
            }

            j1 += k;
            j += l;
         }

         if (p_177899_) {
            p_177897_.registryAccess()
               .lookup(Registries.CONFIGURED_FEATURE)
               .flatMap(p_358516_ -> p_358516_.get(MiscOverworldFeatures.BONUS_CHEST))
               .ifPresent(p_421279_ -> p_421279_.value().place(p_177897_, serverchunkcache.getGenerator(), p_177897_.random, p_177898_.getRespawnData().pos()));
         }

         p_431711_.finish(LevelLoadListener.Stage.PREPARE_GLOBAL_SPAWN);
      }
   }

   private void setupDebugLevel(WorldData p_129848_) {
      p_129848_.setDifficulty(Difficulty.PEACEFUL);
      p_129848_.setDifficultyLocked(true);
      ServerLevelData serverleveldata = p_129848_.overworldData();
      serverleveldata.setRaining(false);
      serverleveldata.setThundering(false);
      serverleveldata.setClearWeatherTime(1000000000);
      serverleveldata.setDayTime(6000L);
      serverleveldata.setGameType(GameType.SPECTATOR);
   }

   private void prepareLevels() {
      ChunkLoadCounter chunkloadcounter = new ChunkLoadCounter();

      for (ServerLevel serverlevel : this.levels.values()) {
         chunkloadcounter.track(serverlevel, () -> {
            TicketStorage ticketstorage = serverlevel.getDataStorage().get(TicketStorage.TYPE);
            if (ticketstorage != null) {
               ticketstorage.activateAllDeactivatedTickets();
            }
         });
      }

      this.levelLoadListener.start(LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS, chunkloadcounter.totalChunks());

      do {
         this.levelLoadListener.update(LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS, chunkloadcounter.readyChunks(), chunkloadcounter.totalChunks());
         this.nextTickTimeNanos = Util.getNanos() + PREPARE_LEVELS_DEFAULT_DELAY_NANOS;
         this.waitUntilNextTick();
      } while (chunkloadcounter.pendingChunks() > 0);

      this.levelLoadListener.finish(LevelLoadListener.Stage.LOAD_INITIAL_CHUNKS);
      this.updateMobSpawningFlags();
      this.updateEffectiveRespawnData();
   }

   public GlobalPos selectLevelLoadFocusPos() {
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

   public boolean saveAllChunks(boolean p_129886_, boolean p_129887_, boolean p_129888_) {
      this.scoreboard.storeToSaveDataIfDirty(this.overworld().getDataStorage().computeIfAbsent(ScoreboardSaveData.TYPE));
      boolean flag = false;

      for (ServerLevel serverlevel : this.getAllLevels()) {
         if (!p_129886_) {
            LOGGER.info("Saving chunks for level '{}'/{}", serverlevel, serverlevel.dimension().identifier());
         }

         serverlevel.save(null, p_129887_, SharedConstants.DEBUG_DONT_SAVE_WORLD || serverlevel.noSave && !p_129888_);
         flag = true;
      }

      this.worldData.setCustomBossEvents(this.getCustomBossEvents().save(this.registryAccess()));
      this.storageSource.saveDataTag(this.registryAccess(), this.worldData, this.getPlayerList().getSingleplayerData());
      if (p_129887_) {
         for (ServerLevel serverlevel1 : this.getAllLevels()) {
            LOGGER.info("ThreadedAnvilChunkStorage ({}): All chunks are saved", serverlevel1.getChunkSource().chunkMap.getStorageName());
         }

         LOGGER.info("ThreadedAnvilChunkStorage: All dimensions are saved");
      }

      return flag;
   }

   public boolean saveEverything(boolean p_195515_, boolean p_195516_, boolean p_195517_) {
      boolean flag;
      try {
         this.isSaving = true;
         this.getPlayerList().saveAll();
         flag = this.saveAllChunks(p_195515_, p_195516_, p_195517_);
      } finally {
         this.isSaving = false;
      }

      return flag;
   }

   @Override
   public void close() {
      this.stopServer();
   }

   public void stopServer() {
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

      for (ServerLevel serverlevel : this.getAllLevels()) {
         if (serverlevel != null) {
            serverlevel.noSave = false;
         }
      }

      while (this.levels.values().stream().anyMatch(p_202480_ -> p_202480_.getChunkSource().chunkMap.hasWork())) {
         this.nextTickTimeNanos = Util.getNanos() + TimeUtil.NANOSECONDS_PER_MILLISECOND;

         for (ServerLevel serverlevel1 : this.getAllLevels()) {
            serverlevel1.getChunkSource().deactivateTicketsOnClosing();
            serverlevel1.getChunkSource().tick(() -> true, false);
         }

         this.waitUntilNextTick();
      }

      this.saveAllChunks(false, true, false);

      for (ServerLevel serverlevel2 : this.getAllLevels()) {
         if (serverlevel2 != null) {
            try {
               serverlevel2.close();
            } catch (IOException ioexception1) {
               LOGGER.error("Exception closing the level", ioexception1);
            }
         }
      }

      this.isSaving = false;
      this.resources.close();

      try {
         this.storageSource.close();
      } catch (IOException ioexception) {
         LOGGER.error("Failed to unlock level {}", this.storageSource.getLevelId(), ioexception);
      }
   }

   public String getLocalIp() {
      return this.localIp;
   }

   public void setLocalIp(String p_129914_) {
      this.localIp = p_129914_;
   }

   public boolean isRunning() {
      return this.running;
   }

   public void halt(boolean p_129884_) {
      this.running = false;
      if (p_129884_) {
         try {
            this.serverThread.join();
         } catch (InterruptedException interruptedexception) {
            LOGGER.error("Error while shutting down", interruptedexception);
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
            long i;
            if (!this.isPaused() && this.tickRateManager.isSprinting() && this.tickRateManager.checkShouldSprintThisTick()) {
               i = 0L;
               this.nextTickTimeNanos = Util.getNanos();
               this.lastOverloadWarningNanos = this.nextTickTimeNanos;
            } else {
               i = this.tickRateManager.nanosecondsPerTick();
               long k = Util.getNanos() - this.nextTickTimeNanos;
               if (k > OVERLOADED_THRESHOLD_NANOS + 20L * i
                  && this.nextTickTimeNanos - this.lastOverloadWarningNanos >= OVERLOADED_WARNING_INTERVAL_NANOS + 100L * i) {
                  long j = k / i;
                  LOGGER.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", k / TimeUtil.NANOSECONDS_PER_MILLISECOND, j);
                  this.nextTickTimeNanos += j * i;
                  this.lastOverloadWarningNanos = this.nextTickTimeNanos;
               }
            }

            boolean flag = i == 0L;
            if (this.debugCommandProfilerDelayStart) {
               this.debugCommandProfilerDelayStart = false;
               this.debugCommandProfiler = new MinecraftServer.TimeProfiler(Util.getNanos(), this.tickCount);
            }

            this.nextTickTimeNanos += i;

            try (Profiler.Scope profiler$scope = Profiler.use(this.createProfiler())) {
               this.processPacketsAndTick(flag);
               ProfilerFiller profilerfiller = Profiler.get();
               profilerfiller.push("nextTickWait");
               this.mayHaveDelayedTasks = true;
               this.delayedTasksMaxNextTickTimeNanos = Math.max(Util.getNanos() + i, this.nextTickTimeNanos);
               this.startMeasuringTaskExecutionTime();
               this.waitUntilNextTick();
               this.finishMeasuringTaskExecutionTime();
               if (flag) {
                  this.tickRateManager.endTickWork();
               }

               profilerfiller.pop();
               this.logFullTickTime();
            } finally {
               this.endMetricsRecordingTick();
            }

            this.isReady = true;
            JvmProfiler.INSTANCE.onServerTick(this.smoothedTickTimeMillis);
         }
      } catch (Throwable throwable2) {
         LOGGER.error("Encountered an unexpected exception", throwable2);
         CrashReport crashreport = constructOrExtractCrashReport(throwable2);
         this.fillSystemReport(crashreport.getSystemReport());
         Path path = this.getServerDirectory().resolve("crash-reports").resolve("crash-" + Util.getFilenameFormattedDateTime() + "-server.txt");
         if (crashreport.saveToFile(path, ReportType.CRASH)) {
            LOGGER.error("This crash report has been saved to: {}", path.toAbsolutePath());
         } else {
            LOGGER.error("We were unable to save this crash report to disk.");
         }

         this.onServerCrash(crashreport);
      } finally {
         try {
            this.stopped = true;
            this.stopServer();
         } catch (Throwable throwable) {
            LOGGER.error("Exception stopping the server", throwable);
         } finally {
            this.onServerExit();
         }
      }
   }

   private void logFullTickTime() {
      long i = Util.getNanos();
      if (this.isTickTimeLoggingEnabled()) {
         this.getTickTimeLogger().logSample(i - this.lastTickNanos);
      }

      this.lastTickNanos = i;
   }

   private void startMeasuringTaskExecutionTime() {
      if (this.isTickTimeLoggingEnabled()) {
         this.taskExecutionStartNanos = Util.getNanos();
         this.idleTimeNanos = 0L;
      }
   }

   private void finishMeasuringTaskExecutionTime() {
      if (this.isTickTimeLoggingEnabled()) {
         SampleLogger samplelogger = this.getTickTimeLogger();
         samplelogger.logPartialSample(Util.getNanos() - this.taskExecutionStartNanos - this.idleTimeNanos, TpsDebugDimensions.SCHEDULED_TASKS.ordinal());
         samplelogger.logPartialSample(this.idleTimeNanos, TpsDebugDimensions.IDLE.ordinal());
      }
   }

   private static CrashReport constructOrExtractCrashReport(Throwable p_206569_) {
      ReportedException reportedexception = null;

      for (Throwable throwable = p_206569_; throwable != null; throwable = throwable.getCause()) {
         if (throwable instanceof ReportedException reportedexception1) {
            reportedexception = reportedexception1;
         }
      }

      CrashReport crashreport;
      if (reportedexception != null) {
         crashreport = reportedexception.getReport();
         if (reportedexception != p_206569_) {
            crashreport.addCategory("Wrapped in").setDetailError("Wrapping exception", p_206569_);
         }
      } else {
         crashreport = new CrashReport("Exception in server tick loop", p_206569_);
      }

      return crashreport;
   }

   private boolean haveTime() {
      return this.runningTask() || Util.getNanos() < (this.mayHaveDelayedTasks ? this.delayedTasksMaxNextTickTimeNanos : this.nextTickTimeNanos);
   }

   public static boolean throwIfFatalException() {
      RuntimeException runtimeexception = fatalException.get();
      if (runtimeexception != null) {
         throw runtimeexception;
      } else {
         return true;
      }
   }

   public static void setFatalException(RuntimeException p_343685_) {
      fatalException.compareAndSet(null, p_343685_);
   }

   @Override
   public void managedBlock(BooleanSupplier p_343833_) {
      super.managedBlock(() -> throwIfFatalException() && p_343833_.getAsBoolean());
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
   public void waitForTasks() {
      boolean flag = this.isTickTimeLoggingEnabled();
      long i = flag ? Util.getNanos() : 0L;
      long j = this.waitingForNextTick ? this.nextTickTimeNanos - Util.getNanos() : 100000L;
      LockSupport.parkNanos("waiting for tasks", j);
      if (flag) {
         this.idleTimeNanos = this.idleTimeNanos + (Util.getNanos() - i);
      }
   }

   public TickTask wrapRunnable(Runnable p_129852_) {
      return new TickTask(this.tickCount, p_129852_);
   }

   protected boolean shouldRun(TickTask p_129883_) {
      return p_129883_.getTick() + 3 < this.tickCount || this.haveTime();
   }

   @Override
   public boolean pollTask() {
      boolean flag = this.pollTaskInternal();
      this.mayHaveDelayedTasks = flag;
      return flag;
   }

   private boolean pollTaskInternal() {
      if (super.pollTask()) {
         return true;
      }

      if (this.tickRateManager.isSprinting() || this.shouldRunAllTasks() || this.haveTime()) {
         for (ServerLevel serverlevel : this.getAllLevels()) {
            if (serverlevel.getChunkSource().pollTask()) {
               return true;
            }
         }
      }

      return false;
   }

   public void doRunTask(TickTask p_129957_) {
      Profiler.get().incrementCounter("runTask");
      super.doRunTask(p_129957_);
   }

   private Optional<ServerStatus.Favicon> loadStatusIcon() {
      Optional<Path> optional = Optional.of(this.getFile("server-icon.png"))
         .filter(p_272385_ -> Files.isRegularFile(p_272385_))
         .or(() -> this.storageSource.getIconFile().filter(p_272387_ -> Files.isRegularFile(p_272387_)));
      return optional.flatMap(p_448815_ -> {
         try {
            byte[] abyte = Files.readAllBytes(p_448815_);
            PngInfo pnginfo = PngInfo.fromBytes(abyte);
            if (pnginfo.width() == 64 && pnginfo.height() == 64) {
               return Optional.of(new ServerStatus.Favicon(abyte));
            } else {
               throw new IllegalArgumentException("Invalid world icon size [" + pnginfo.width() + ", " + pnginfo.height() + "], but expected [64, 64]");
            }
         } catch (Exception exception) {
            LOGGER.error("Couldn't load server icon", exception);
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

   public void onServerCrash(CrashReport p_129874_) {
   }

   public void onServerExit() {
   }

   public boolean isPaused() {
      return false;
   }

   public void tickServer(BooleanSupplier p_129871_) {
      long i = Util.getNanos();
      int j = this.pauseWhenEmptySeconds() * 20;
      if (j > 0) {
         if (this.playerList.getPlayerCount() == 0 && !this.tickRateManager.isSprinting()) {
            this.emptyTicks++;
         } else {
            this.emptyTicks = 0;
         }

         if (this.emptyTicks >= j) {
            if (this.emptyTicks == j) {
               LOGGER.info("Server empty for {} seconds, pausing", this.pauseWhenEmptySeconds());
               this.autoSave();
            }

            this.tickConnection();
            return;
         }
      }

      this.tickCount++;
      this.tickRateManager.tick();
      this.tickChildren(p_129871_);
      if (i - this.lastServerStatus >= STATUS_EXPIRE_TIME_NANOS) {
         this.lastServerStatus = i;
         this.status = this.buildServerStatus();
      }

      this.ticksUntilAutosave--;
      if (this.ticksUntilAutosave <= 0) {
         this.autoSave();
      }

      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("tallying");
      long k = Util.getNanos() - i;
      int l = this.tickCount % 100;
      this.aggregatedTickTimesNanos = this.aggregatedTickTimesNanos - this.tickTimesNanos[l];
      this.aggregatedTickTimesNanos += k;
      this.tickTimesNanos[l] = k;
      this.smoothedTickTimeMillis = this.smoothedTickTimeMillis * 0.8F + (float)k / (float)TimeUtil.NANOSECONDS_PER_MILLISECOND * 0.19999999F;
      this.logTickMethodTime(i);
      profilerfiller.pop();
   }

   public void processPacketsAndTick(boolean p_452829_) {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("tick");
      this.tickFrame.start();
      profilerfiller.push("scheduledPacketProcessing");
      this.packetProcessor.processQueuedPackets();
      profilerfiller.pop();
      this.tickServer(p_452829_ ? () -> false : this::haveTime);
      this.tickFrame.end();
      profilerfiller.pop();
   }

   private void autoSave() {
      this.ticksUntilAutosave = this.computeNextAutosaveInterval();
      LOGGER.debug("Autosave started");
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("save");
      this.saveEverything(true, false, false);
      profilerfiller.pop();
      LOGGER.debug("Autosave finished");
   }

   private void logTickMethodTime(long p_331549_) {
      if (this.isTickTimeLoggingEnabled()) {
         this.getTickTimeLogger().logPartialSample(Util.getNanos() - p_331549_, TpsDebugDimensions.TICK_SERVER_METHOD.ordinal());
      }
   }

   private int computeNextAutosaveInterval() {
      float f;
      if (this.tickRateManager.isSprinting()) {
         long i = this.getAverageTickTimeNanos() + 1L;
         f = (float)TimeUtil.NANOSECONDS_PER_SECOND / (float)i;
      } else {
         f = this.tickRateManager.tickrate();
      }

      int j = 300;
      return Math.max(100, (int)(f * 300.0F));
   }

   public void onTickRateChanged() {
      int i = this.computeNextAutosaveInterval();
      if (i < this.ticksUntilAutosave) {
         this.ticksUntilAutosave = i;
      }
   }

   protected abstract SampleLogger getTickTimeLogger();

   public abstract boolean isTickTimeLoggingEnabled();

   private ServerStatus buildServerStatus() {
      ServerStatus.Players serverstatus$players = this.buildPlayerStatus();
      return new ServerStatus(
         Component.nullToEmpty(this.getMotd()),
         Optional.of(serverstatus$players),
         Optional.of(ServerStatus.Version.current()),
         Optional.ofNullable(this.statusIcon),
         this.enforceSecureProfile()
      );
   }

   private ServerStatus.Players buildPlayerStatus() {
      List<ServerPlayer> list = this.playerList.getPlayers();
      int i = this.getMaxPlayers();
      if (this.hidesOnlinePlayers()) {
         return new ServerStatus.Players(i, list.size(), List.of());
      }

      int j = Math.min(list.size(), 12);
      ObjectArrayList<NameAndId> objectarraylist = new ObjectArrayList(j);
      int k = Mth.nextInt(this.random, 0, list.size() - j);

      for (int l = 0; l < j; l++) {
         ServerPlayer serverplayer = list.get(k + l);
         objectarraylist.add(serverplayer.allowsListing() ? serverplayer.nameAndId() : ANONYMOUS_PLAYER_PROFILE);
      }

      Util.shuffle(objectarraylist, this.random);
      return new ServerStatus.Players(i, list.size(), objectarraylist);
   }

   protected void tickChildren(BooleanSupplier p_129954_) {
      ProfilerFiller profilerfiller = Profiler.get();
      this.getPlayerList().getPlayers().forEach(p_326187_ -> p_326187_.connection.suspendFlushing());
      profilerfiller.push("commandFunctions");
      this.getFunctions().tick();
      profilerfiller.popPush("levels");
      this.updateEffectiveRespawnData();

      for (ServerLevel serverlevel : this.getAllLevels()) {
         profilerfiller.push(() -> serverlevel + " " + serverlevel.dimension().identifier());
         if (this.tickCount % 20 == 0) {
            profilerfiller.push("timeSync");
            this.synchronizeTime(serverlevel);
            profilerfiller.pop();
         }

         profilerfiller.push("tick");

         try {
            serverlevel.tick(p_129954_);
         } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Exception ticking world");
            serverlevel.fillReportDetails(crashreport);
            throw new ReportedException(crashreport);
         }

         profilerfiller.pop();
         profilerfiller.pop();
      }

      profilerfiller.popPush("connection");
      this.tickConnection();
      profilerfiller.popPush("players");
      this.playerList.tick();
      profilerfiller.popPush("debugSubscribers");
      this.debugSubscribers.tick();
      if (this.tickRateManager.runsNormally()) {
         profilerfiller.popPush("gameTests");
         GameTestTicker.SINGLETON.tick();
      }

      profilerfiller.popPush("server gui refresh");

      for (Runnable runnable : this.tickables) {
         runnable.run();
      }

      profilerfiller.popPush("send chunks");

      for (ServerPlayer serverplayer : this.playerList.getPlayers()) {
         serverplayer.connection.chunkSender.sendNextChunks(serverplayer);
         serverplayer.connection.resumeFlushing();
      }

      profilerfiller.pop();
      this.serverActivityMonitor.tick();
   }

   private void updateEffectiveRespawnData() {
      LevelData.RespawnData leveldata$respawndata = this.worldData.overworldData().getRespawnData();
      ServerLevel serverlevel = this.findRespawnDimension();
      this.effectiveRespawnData = serverlevel.getWorldBorderAdjustedRespawnData(leveldata$respawndata);
   }

   public void tickConnection() {
      this.getConnection().tick();
   }

   private void synchronizeTime(ServerLevel p_276371_) {
      this.playerList
         .broadcastAll(
            new ClientboundSetTimePacket(p_276371_.getGameTime(), p_276371_.getDayTime(), p_276371_.getGameRules().get(GameRules.ADVANCE_TIME)),
            p_276371_.dimension()
         );
   }

   public void forceTimeSynchronization() {
      ProfilerFiller profilerfiller = Profiler.get();
      profilerfiller.push("timeSync");

      for (ServerLevel serverlevel : this.getAllLevels()) {
         this.synchronizeTime(serverlevel);
      }

      profilerfiller.pop();
   }

   public void addTickable(Runnable p_129947_) {
      this.tickables.add(p_129947_);
   }

   protected void setId(String p_129949_) {
      this.serverId = p_129949_;
   }

   public boolean isShutdown() {
      return !this.serverThread.isAlive();
   }

   public Path getFile(String p_129972_) {
      return this.getServerDirectory().resolve(p_129972_);
   }

   public final ServerLevel overworld() {
      return this.levels.get(Level.OVERWORLD);
   }

   public @Nullable ServerLevel getLevel(ResourceKey<Level> p_129881_) {
      return this.levels.get(p_129881_);
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

   @DontObfuscate
   public String getServerModName() {
      return "vanilla";
   }

   public SystemReport fillSystemReport(SystemReport p_177936_) {
      p_177936_.setDetail("Server Running", () -> Boolean.toString(this.running));
      if (this.playerList != null) {
         p_177936_.setDetail(
            "Player Count", () -> this.playerList.getPlayerCount() + " / " + this.playerList.getMaxPlayers() + "; " + this.playerList.getPlayers()
         );
      }

      p_177936_.setDetail("Active Data Packs", () -> PackRepository.displayPackList(this.packRepository.getSelectedPacks()));
      p_177936_.setDetail("Available Data Packs", () -> PackRepository.displayPackList(this.packRepository.getAvailablePacks()));
      p_177936_.setDetail(
         "Enabled Feature Flags",
         () -> FeatureFlags.REGISTRY.toNames(this.worldData.enabledFeatures()).stream().map(Identifier::toString).collect(Collectors.joining(", "))
      );
      p_177936_.setDetail("World Generation", () -> this.worldData.worldGenSettingsLifecycle().toString());
      p_177936_.setDetail("World Seed", () -> String.valueOf(this.worldData.worldGenOptions().seed()));
      p_177936_.setDetail("Suppressed Exceptions", this.suppressedExceptions::dump);
      if (this.serverId != null) {
         p_177936_.setDetail("Server Id", () -> this.serverId);
      }

      return this.fillServerSystemReport(p_177936_);
   }

   public abstract SystemReport fillServerSystemReport(SystemReport var1);

   public ModCheck getModdedStatus() {
      return ModCheck.identify("vanilla", this::getServerModName, "Server", MinecraftServer.class);
   }

   @Override
   public void sendSystemMessage(Component p_236736_) {
      LOGGER.info(p_236736_.getString());
   }

   public KeyPair getKeyPair() {
      return Objects.requireNonNull(this.keyPair);
   }

   public int getPort() {
      return this.port;
   }

   public void setPort(int p_129802_) {
      this.port = p_129802_;
   }

   public @Nullable GameProfile getSingleplayerProfile() {
      return this.singleplayerProfile;
   }

   public void setSingleplayerProfile(@Nullable GameProfile p_236741_) {
      this.singleplayerProfile = p_236741_;
   }

   public boolean isSingleplayer() {
      return this.singleplayerProfile != null;
   }

   protected void initializeKeyPair() {
      LOGGER.info("Generating keypair");

      try {
         this.keyPair = Crypt.generateKeyPair();
      } catch (CryptException cryptexception) {
         throw new IllegalStateException("Failed to generate key pair", cryptexception);
      }
   }

   public void setDifficulty(Difficulty p_129828_, boolean p_129829_) {
      if (p_129829_ || !this.worldData.isDifficultyLocked()) {
         this.worldData.setDifficulty(this.worldData.isHardcore() ? Difficulty.HARD : p_129828_);
         this.updateMobSpawningFlags();
         this.getPlayerList().getPlayers().forEach(this::sendDifficultyUpdate);
      }
   }

   public int getScaledTrackingDistance(int p_129935_) {
      return p_129935_;
   }

   public void updateMobSpawningFlags() {
      for (ServerLevel serverlevel : this.getAllLevels()) {
         serverlevel.setSpawnSettings(serverlevel.isSpawningMonsters());
      }
   }

   public void setDifficultyLocked(boolean p_129959_) {
      this.worldData.setDifficultyLocked(p_129959_);
      this.getPlayerList().getPlayers().forEach(this::sendDifficultyUpdate);
   }

   private void sendDifficultyUpdate(ServerPlayer p_129939_) {
      LevelData leveldata = p_129939_.level().getLevelData();
      p_129939_.connection.send(new ClientboundChangeDifficultyPacket(leveldata.getDifficulty(), leveldata.isDifficultyLocked()));
   }

   public boolean isDemo() {
      return this.isDemo;
   }

   public void setDemo(boolean p_129976_) {
      this.isDemo = p_129976_;
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

   public boolean usesAuthentication() {
      return this.onlineMode;
   }

   public void setUsesAuthentication(boolean p_129986_) {
      this.onlineMode = p_129986_;
   }

   public boolean getPreventProxyConnections() {
      return this.preventProxyConnections;
   }

   public void setPreventProxyConnections(boolean p_129994_) {
      this.preventProxyConnections = p_129994_;
   }

   public abstract boolean useNativeTransport();

   public boolean allowFlight() {
      return true;
   }

   @Override
   public String getMotd() {
      return this.motd;
   }

   public void setMotd(String p_129990_) {
      this.motd = p_129990_;
   }

   public boolean isStopped() {
      return this.stopped;
   }

   public PlayerList getPlayerList() {
      return this.playerList;
   }

   public void setPlayerList(PlayerList p_129824_) {
      this.playerList = p_129824_;
   }

   public abstract boolean isPublished();

   public void setDefaultGameType(GameType p_129832_) {
      this.worldData.setGameType(p_129832_);
   }

   public int enforceGameTypeForPlayers(@Nullable GameType p_425092_) {
      if (p_425092_ == null) {
         return 0;
      }

      int i = 0;

      for (ServerPlayer serverplayer : this.getPlayerList().getPlayers()) {
         if (serverplayer.setGameMode(p_425092_)) {
            i++;
         }
      }

      return i;
   }

   public ServerConnectionListener getConnection() {
      return this.connection;
   }

   public boolean isReady() {
      return this.isReady;
   }

   public boolean publishServer(@Nullable GameType p_129833_, boolean p_129834_, int p_129835_) {
      return false;
   }

   public int getTickCount() {
      return this.tickCount;
   }

   public boolean isUnderSpawnProtection(ServerLevel p_129811_, BlockPos p_129812_, Player p_129813_) {
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

   public void setPlayerIdleTimeout(int p_129978_) {
      this.playerIdleTimeout = p_129978_;
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
      return Integer.MAX_VALUE;
   }

   @Override
   public boolean scheduleExecutables() {
      return super.scheduleExecutables() && !this.isStopped();
   }

   @Override
   public void executeIfPossible(Runnable p_202482_) {
      if (this.isStopped()) {
         throw new RejectedExecutionException("Server already shutting down");
      }

      super.executeIfPossible(p_202482_);
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

   public CompletableFuture<Void> reloadResources(Collection<String> p_129862_) {
      CompletableFuture<Void> completablefuture = CompletableFuture.<ImmutableList>supplyAsync(
            () -> p_129862_.stream().map(this.packRepository::getPack).filter(Objects::nonNull).map(Pack::open).collect(ImmutableList.toImmutableList()), this
         )
         .thenCompose(
            p_358514_ -> {
               CloseableResourceManager closeableresourcemanager = new MultiPackResourceManager(PackType.SERVER_DATA, p_358514_);
               List<Registry.PendingTags<?>> list = TagLoader.loadTagsForExistingRegistries(closeableresourcemanager, this.registries.compositeAccess());
               return ReloadableServerResources.loadResources(
                     closeableresourcemanager,
                     this.registries,
                     list,
                     this.worldData.enabledFeatures(),
                     this.isDedicatedServer() ? Commands.CommandSelection.DEDICATED : Commands.CommandSelection.INTEGRATED,
                     this.getFunctionCompilationPermissions(),
                     this.executor,
                     this
                  )
                  .whenComplete((p_212907_, p_212908_) -> {
                     if (p_212908_ != null) {
                        closeableresourcemanager.close();
                     }
                  })
                  .thenApply(p_212904_ -> new MinecraftServer.ReloadableResources(closeableresourcemanager, p_212904_));
            }
         )
         .thenAcceptAsync(
            p_358513_ -> {
               this.resources.close();
               this.resources = p_358513_;
               this.packRepository.setSelected(p_129862_);
               WorldDataConfiguration worlddataconfiguration = new WorldDataConfiguration(
                  getSelectedPacks(this.packRepository, true), this.worldData.enabledFeatures()
               );
               this.worldData.setDataConfiguration(worlddataconfiguration);
               this.resources.managers.updateStaticRegistryTags();
               this.resources.managers.getRecipeManager().finalizeRecipeLoading(this.worldData.enabledFeatures());
               this.getPlayerList().saveAll();
               this.getPlayerList().reloadResources();
               this.functionManager.replaceLibrary(this.resources.managers.getFunctionLibrary());
               this.structureTemplateManager.onResourceManagerReload(this.resources.resourceManager);
               this.fuelValues = FuelValues.vanillaBurnTimes(this.registries.compositeAccess(), this.worldData.enabledFeatures());
            },
            this
         );
      if (this.isSameThread()) {
         this.managedBlock(completablefuture::isDone);
      }

      return completablefuture;
   }

   public static WorldDataConfiguration configurePackRepository(
      PackRepository p_248681_, WorldDataConfiguration p_331931_, boolean p_249869_, boolean p_330480_
   ) {
      DataPackConfig datapackconfig = p_331931_.dataPacks();
      FeatureFlagSet featureflagset = p_249869_ ? FeatureFlagSet.of() : p_331931_.enabledFeatures();
      FeatureFlagSet featureflagset1 = p_249869_ ? FeatureFlags.REGISTRY.allFlags() : p_331931_.enabledFeatures();
      p_248681_.reload();
      if (p_330480_) {
         return configureRepositoryWithSelection(p_248681_, List.of("vanilla"), featureflagset, false);
      }

      Set<String> set = Sets.newLinkedHashSet();

      for (String s : datapackconfig.getEnabled()) {
         if (p_248681_.isAvailable(s)) {
            set.add(s);
         } else {
            LOGGER.warn("Missing data pack {}", s);
         }
      }

      for (Pack pack : p_248681_.getAvailablePacks()) {
         String s1 = pack.getId();
         if (!datapackconfig.getDisabled().contains(s1)) {
            FeatureFlagSet featureflagset2 = pack.getRequestedFeatures();
            boolean flag = set.contains(s1);
            if (!flag && pack.getPackSource().shouldAddAutomatically()) {
               if (featureflagset2.isSubsetOf(featureflagset1)) {
                  LOGGER.info("Found new data pack {}, loading it automatically", s1);
                  set.add(s1);
               } else {
                  LOGGER.info(
                     "Found new data pack {}, but can't load it due to missing features {}",
                     s1,
                     FeatureFlags.printMissingFlags(featureflagset1, featureflagset2)
                  );
               }
            }

            if (flag && !featureflagset2.isSubsetOf(featureflagset1)) {
               LOGGER.warn(
                  "Pack {} requires features {} that are not enabled for this world, disabling pack.",
                  s1,
                  FeatureFlags.printMissingFlags(featureflagset1, featureflagset2)
               );
               set.remove(s1);
            }
         }
      }

      if (set.isEmpty()) {
         LOGGER.info("No datapacks selected, forcing vanilla");
         set.add("vanilla");
      }

      return configureRepositoryWithSelection(p_248681_, set, featureflagset, true);
   }

   private static WorldDataConfiguration configureRepositoryWithSelection(
      PackRepository p_331926_, Collection<String> p_333329_, FeatureFlagSet p_331153_, boolean p_334241_
   ) {
      p_331926_.setSelected(p_333329_);
      enableForcedFeaturePacks(p_331926_, p_331153_);
      DataPackConfig datapackconfig = getSelectedPacks(p_331926_, p_334241_);
      FeatureFlagSet featureflagset = p_331926_.getRequestedFeatureFlags().join(p_331153_);
      return new WorldDataConfiguration(datapackconfig, featureflagset);
   }

   private static void enableForcedFeaturePacks(PackRepository p_335711_, FeatureFlagSet p_335242_) {
      FeatureFlagSet featureflagset = p_335711_.getRequestedFeatureFlags();
      FeatureFlagSet featureflagset1 = p_335242_.subtract(featureflagset);
      if (!featureflagset1.isEmpty()) {
         Set<String> set = new ObjectArraySet(p_335711_.getSelectedIds());

         for (Pack pack : p_335711_.getAvailablePacks()) {
            if (featureflagset1.isEmpty()) {
               break;
            }

            if (pack.getPackSource() == PackSource.FEATURE) {
               String s = pack.getId();
               FeatureFlagSet featureflagset2 = pack.getRequestedFeatures();
               if (!featureflagset2.isEmpty() && featureflagset2.intersects(featureflagset1) && featureflagset2.isSubsetOf(p_335242_)) {
                  if (!set.add(s)) {
                     throw new IllegalStateException("Tried to force '" + s + "', but it was already enabled");
                  }

                  LOGGER.info("Found feature pack ('{}') for requested feature, forcing to enabled", s);
                  featureflagset1 = featureflagset1.subtract(featureflagset2);
               }
            }
         }

         p_335711_.setSelected(set);
      }
   }

   private static DataPackConfig getSelectedPacks(PackRepository p_129818_, boolean p_334831_) {
      Collection<String> collection = p_129818_.getSelectedIds();
      List<String> list = ImmutableList.copyOf(collection);
      List<String> list1 = p_334831_ ? p_129818_.getAvailableIds().stream().filter(p_212916_ -> !collection.contains(p_212916_)).toList() : List.of();
      return new DataPackConfig(list, list1);
   }

   public void kickUnlistedPlayers() {
      if (this.isEnforceWhitelist() && this.isUsingWhitelist()) {
         PlayerList playerlist = this.getPlayerList();
         UserWhiteList userwhitelist = playerlist.getWhiteList();

         for (ServerPlayer serverplayer : Lists.newArrayList(playerlist.getPlayers())) {
            if (!userwhitelist.isWhiteListed(serverplayer.nameAndId())) {
               serverplayer.connection.disconnect(Component.translatable("multiplayer.disconnect.not_whitelisted"));
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
      ServerLevel serverlevel = this.findRespawnDimension();
      return new CommandSourceStack(
         this,
         Vec3.atLowerCornerOf(this.getRespawnData().pos()),
         Vec2.ZERO,
         serverlevel,
         LevelBasedPermissionSet.OWNER,
         "Server",
         Component.literal("Server"),
         this,
         null
      );
   }

   public ServerLevel findRespawnDimension() {
      LevelData.RespawnData leveldata$respawndata = this.getWorldData().overworldData().getRespawnData();
      ResourceKey<Level> resourcekey = leveldata$respawndata.dimension();
      ServerLevel serverlevel = this.getLevel(resourcekey);
      return serverlevel != null ? serverlevel : this.overworld();
   }

   public void setRespawnData(LevelData.RespawnData p_431063_) {
      ServerLevelData serverleveldata = this.worldData.overworldData();
      LevelData.RespawnData leveldata$respawndata = serverleveldata.getRespawnData();
      if (!leveldata$respawndata.equals(p_431063_)) {
         serverleveldata.setSpawn(p_431063_);
         this.getPlayerList().broadcastAll(new ClientboundSetDefaultSpawnPositionPacket(p_431063_));
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

   public boolean isEnforceWhitelist() {
      return this.enforceWhitelist;
   }

   public void setEnforceWhitelist(boolean p_130005_) {
      this.enforceWhitelist = p_130005_;
   }

   public boolean isUsingWhitelist() {
      return this.usingWhitelist;
   }

   public void setUsingWhitelist(boolean p_430372_) {
      this.usingWhitelist = p_430372_;
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

   public LevelBasedPermissionSet getProfilePermissions(NameAndId p_424119_) {
      if (this.getPlayerList().isOp(p_424119_)) {
         ServerOpListEntry serveroplistentry = this.getPlayerList().getOps().get(p_424119_);
         if (serveroplistentry != null) {
            return serveroplistentry.permissions();
         } else if (this.isSingleplayerOwner(p_424119_)) {
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

   public abstract boolean isSingleplayerOwner(NameAndId var1);

   public void dumpServerProperties(Path p_177911_) throws IOException {
   }

   private void saveDebugReport(Path p_129860_) {
      Path path = p_129860_.resolve("levels");

      try {
         for (Entry<ResourceKey<Level>, ServerLevel> entry : this.levels.entrySet()) {
            Identifier identifier = entry.getKey().identifier();
            Path path1 = path.resolve(identifier.getNamespace()).resolve(identifier.getPath());
            Files.createDirectories(path1);
            entry.getValue().saveDebugReport(path1);
         }

         this.dumpGameRules(p_129860_.resolve("gamerules.txt"));
         this.dumpClasspath(p_129860_.resolve("classpath.txt"));
         this.dumpMiscStats(p_129860_.resolve("stats.txt"));
         this.dumpThreads(p_129860_.resolve("threads.txt"));
         this.dumpServerProperties(p_129860_.resolve("server.properties.txt"));
         this.dumpNativeModules(p_129860_.resolve("modules.txt"));
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to save debug report", ioexception);
      }
   }

   private void dumpMiscStats(Path p_129951_) throws IOException {
      try (Writer writer = Files.newBufferedWriter(p_129951_)) {
         writer.write(String.format(Locale.ROOT, "pending_tasks: %d\n", this.getPendingTasksCount()));
         writer.write(String.format(Locale.ROOT, "average_tick_time: %f\n", this.getCurrentSmoothedTickTime()));
         writer.write(String.format(Locale.ROOT, "tick_times: %s\n", Arrays.toString(this.tickTimesNanos)));
         writer.write(String.format(Locale.ROOT, "queue: %s\n", Util.backgroundExecutor()));
      }
   }

   private void dumpGameRules(Path p_129984_) throws IOException {
      try (Writer writer = Files.newBufferedWriter(p_129984_)) {
         final List<String> list = Lists.newArrayList();
         final GameRules gamerules = this.worldData.getGameRules();
         gamerules.visitGameRuleTypes(new GameRuleTypeVisitor() {
            @Override
            public <T> void visit(GameRule<T> p_460648_) {
               list.add(String.format(Locale.ROOT, "%s=%s\n", p_460648_.getIdentifier(), gamerules.getAsString(p_460648_)));
            }
         });

         for (String s : list) {
            writer.write(s);
         }
      }
   }

   private void dumpClasspath(Path p_129992_) throws IOException {
      try (Writer writer = Files.newBufferedWriter(p_129992_)) {
         String s = System.getProperty("java.class.path");
         String s1 = File.pathSeparator;

         for (String s2 : Splitter.on(s1).split(s)) {
            writer.write(s2);
            writer.write("\n");
         }
      }
   }

   private void dumpThreads(Path p_129996_) throws IOException {
      ThreadMXBean threadmxbean = ManagementFactory.getThreadMXBean();
      ThreadInfo[] athreadinfo = threadmxbean.dumpAllThreads(true, true);
      Arrays.sort(athreadinfo, Comparator.comparing(ThreadInfo::getThreadName));

      try (Writer writer = Files.newBufferedWriter(p_129996_)) {
         for (ThreadInfo threadinfo : athreadinfo) {
            writer.write(threadinfo.toString());
            writer.write(10);
         }
      }
   }

   private void dumpNativeModules(Path p_195522_) throws IOException {
      try (Writer writer = Files.newBufferedWriter(p_195522_)) {
         List<NativeModuleLister.NativeModuleInfo> list;
         try {
            list = Lists.newArrayList(NativeModuleLister.listModules());
         } catch (Throwable throwable) {
            LOGGER.warn("Failed to list native modules", throwable);
            return;
         }

         list.sort(Comparator.comparing(p_212910_ -> p_212910_.name));

         for (NativeModuleLister.NativeModuleInfo nativemodulelister$nativemoduleinfo : list) {
            writer.write(nativemodulelister$nativemoduleinfo.toString());
            writer.write(10);
         }
      }
   }

   private ProfilerFiller createProfiler() {
      if (this.willStartRecordingMetrics) {
         this.metricsRecorder = ActiveMetricsRecorder.createStarted(
            new ServerMetricsSamplersProvider(Util.timeSource, this.isDedicatedServer()),
            Util.timeSource,
            Util.ioPool(),
            new MetricsPersister("server"),
            this.onMetricsRecordingStopped,
            p_212927_ -> {
               this.executeBlocking(() -> this.saveDebugReport(p_212927_.resolve("server")));
               this.onMetricsRecordingFinished.accept(p_212927_);
            }
         );
         this.willStartRecordingMetrics = false;
      }

      this.metricsRecorder.startTick();
      return SingleTickProfiler.decorateFiller(this.metricsRecorder.getProfiler(), SingleTickProfiler.createTickProfiler("Server"));
   }

   public void endMetricsRecordingTick() {
      this.metricsRecorder.endTick();
   }

   public boolean isRecordingMetrics() {
      return this.metricsRecorder.isRecording();
   }

   public void startRecordingMetrics(Consumer<ProfileResults> p_177924_, Consumer<Path> p_177925_) {
      this.onMetricsRecordingStopped = p_212922_ -> {
         this.stopRecordingMetrics();
         p_177924_.accept(p_212922_);
      };
      this.onMetricsRecordingFinished = p_177925_;
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

   public Path getWorldPath(LevelResource p_129844_) {
      return this.storageSource.getLevelPath(p_129844_);
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

   public TextFilter createTextFilterForPlayer(ServerPlayer p_129814_) {
      return TextFilter.DUMMY;
   }

   public ServerPlayerGameMode createGameModeForPlayer(ServerPlayer p_177934_) {
      return this.isDemo() ? new DemoMode(p_177934_) : new ServerPlayerGameMode(p_177934_);
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

      ProfileResults profileresults = this.debugCommandProfiler.stop(Util.getNanos(), this.tickCount);
      this.debugCommandProfiler = null;
      return profileresults;
   }

   public int getMaxChainedNeighborUpdates() {
      return 1000000;
   }

   public void logChatMessage(Component p_241503_, ChatType.Bound p_241402_, @Nullable String p_241481_) {
      String s = p_241402_.decorate(p_241503_).getString();
      if (p_241481_ != null) {
         LOGGER.info("[{}] {}", p_241481_, s);
      } else {
         LOGGER.info("{}", s);
      }
   }

   public ChatDecorator getChatDecorator() {
      return ChatDecorator.PLAIN;
   }

   public boolean logIPs() {
      return true;
   }

   public void handleCustomClickAction(Identifier p_459923_, Optional<Tag> p_410582_) {
      LOGGER.debug("Received custom click action {} with payload {}", p_459923_, p_410582_.orElse(null));
   }

   public LevelLoadListener getLevelLoadListener() {
      return this.levelLoadListener;
   }

   public boolean setAutoSave(boolean p_424680_) {
      boolean flag = false;

      for (ServerLevel serverlevel : this.getAllLevels()) {
         if (serverlevel != null && serverlevel.noSave == p_424680_) {
            serverlevel.noSave = !p_424680_;
            flag = true;
         }
      }

      return flag;
   }

   public boolean isAutoSave() {
      for (ServerLevel serverlevel : this.getAllLevels()) {
         if (serverlevel != null && !serverlevel.noSave) {
            return true;
         }
      }

      return false;
   }

   public <T> void onGameRuleChanged(GameRule<T> p_453346_, T p_459874_) {
      this.notificationManager().onGameRuleChanged(p_453346_, p_459874_);
      if (p_453346_ == GameRules.REDUCED_DEBUG_INFO) {
         byte b0 = (byte)((Boolean)p_459874_ ? 22 : 23);

         for (ServerPlayer serverplayer : this.getPlayerList().getPlayers()) {
            serverplayer.connection.send(new ClientboundEntityEventPacket(serverplayer, b0));
         }
      } else if (p_453346_ == GameRules.LIMITED_CRAFTING || p_453346_ == GameRules.IMMEDIATE_RESPAWN) {
         ClientboundGameEventPacket.Type clientboundgameeventpacket$type = p_453346_ == GameRules.LIMITED_CRAFTING
            ? ClientboundGameEventPacket.LIMITED_CRAFTING
            : ClientboundGameEventPacket.IMMEDIATE_RESPAWN;
         ClientboundGameEventPacket clientboundgameeventpacket = new ClientboundGameEventPacket(
            clientboundgameeventpacket$type, (Boolean)p_459874_ ? 1.0F : 0.0F
         );
         this.getPlayerList().getPlayers().forEach(p_448817_ -> p_448817_.connection.send(clientboundgameeventpacket));
      } else if (p_453346_ == GameRules.LOCATOR_BAR) {
         this.getAllLevels().forEach(p_448820_ -> {
            ServerWaypointManager serverwaypointmanager = p_448820_.getWaypointManager();
            if ((Boolean)p_459874_) {
               p_448820_.players().forEach(serverwaypointmanager::updatePlayer);
            } else {
               serverwaypointmanager.breakAllConnections();
            }
         });
      } else if (p_453346_ == GameRules.SPAWN_MONSTERS) {
         this.updateMobSpawningFlags();
      }
   }

   public boolean acceptsTransfers() {
      return false;
   }

   private void storeChunkIoError(CrashReport p_344874_, ChunkPos p_342523_, RegionStorageInfo p_343084_) {
      Util.ioPool().execute(() -> {
         try {
            Path path = this.getFile("debug");
            FileUtil.createDirectoriesSafe(path);
            String s = FileUtil.sanitizeName(p_343084_.level());
            Path path1 = path.resolve("chunk-" + s + "-" + Util.getFilenameFormattedDateTime() + "-server.txt");
            FileStore filestore = Files.getFileStore(path);
            long i = filestore.getUsableSpace();
            if (i < 8192L) {
               LOGGER.warn("Not storing chunk IO report due to low space on drive {}", filestore.name());
               return;
            }

            CrashReportCategory crashreportcategory = p_344874_.addCategory("Chunk Info");
            crashreportcategory.setDetail("Level", p_343084_::level);
            crashreportcategory.setDetail("Dimension", () -> p_343084_.dimension().identifier().toString());
            crashreportcategory.setDetail("Storage", p_343084_::type);
            crashreportcategory.setDetail("Position", p_342523_::toString);
            p_344874_.saveToFile(path1, ReportType.CHUNK_IO_ERROR);
            LOGGER.info("Saved details to {}", p_344874_.getSaveFile());
         } catch (Exception exception) {
            LOGGER.warn("Failed to store chunk IO exception", exception);
         }
      });
   }

   @Override
   public void reportChunkLoadFailure(Throwable p_345315_, RegionStorageInfo p_344502_, ChunkPos p_330022_) {
      LOGGER.error("Failed to load chunk {},{}", new Object[]{p_330022_.x, p_330022_.z, p_345315_});
      this.suppressedExceptions.addEntry("chunk/load", p_345315_);
      this.storeChunkIoError(CrashReport.forThrowable(p_345315_, "Chunk load failure"), p_330022_, p_344502_);
   }

   @Override
   public void reportChunkSaveFailure(Throwable p_343979_, RegionStorageInfo p_345051_, ChunkPos p_333073_) {
      LOGGER.error("Failed to save chunk {},{}", new Object[]{p_333073_.x, p_333073_.z, p_343979_});
      this.suppressedExceptions.addEntry("chunk/save", p_343979_);
      this.storeChunkIoError(CrashReport.forThrowable(p_343979_, "Chunk save failure"), p_333073_, p_345051_);
   }

   public void reportPacketHandlingException(Throwable p_366196_, PacketType<?> p_362971_) {
      this.suppressedExceptions.addEntry("packet/" + p_362971_, p_366196_);
   }

   public PotionBrewing potionBrewing() {
      return this.potionBrewing;
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

   record ReloadableResources(CloseableResourceManager resourceManager, ReloadableServerResources managers) implements AutoCloseable {
      @Override
      public void close() {
         this.resourceManager.close();
      }
   }

   public record ServerResourcePackInfo(UUID id, String url, String hash, boolean isRequired, @Nullable Component prompt) {
   }

   static class TimeProfiler {
      final long startNanos;
      final int startTick;

      TimeProfiler(long p_177958_, int p_177959_) {
         this.startNanos = p_177958_;
         this.startTick = p_177959_;
      }

      ProfileResults stop(final long p_177961_, final int p_177962_) {
         return new ProfileResults() {
            @Override
            public List<ResultField> getTimes(String p_177972_) {
               return Collections.emptyList();
            }

            @Override
            public boolean saveResults(Path p_177974_) {
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
               return p_177961_;
            }

            @Override
            public int getEndTimeTicks() {
               return p_177962_;
            }

            @Override
            public String getProfilerResults() {
               return "";
            }
         };
      }
   }
}
