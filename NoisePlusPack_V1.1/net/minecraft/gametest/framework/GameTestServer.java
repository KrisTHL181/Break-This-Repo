package net.minecraft.gametest.framework;

import com.google.common.base.Stopwatch;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.brigadier.StringReader;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;
import net.minecraft.CrashReport;
import net.minecraft.ReportType;
import net.minecraft.SystemReport;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceSelectorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gizmos.GizmoCollector;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LoggingLevelLoadListener;
import net.minecraft.server.notifications.EmptyNotificationService;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.server.players.UserNameToIdResolver;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.debugchart.LocalSampleLogger;
import net.minecraft.util.debugchart.SampleLogger;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class GameTestServer extends MinecraftServer {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int PROGRESS_REPORT_INTERVAL = 20;
   private static final int TEST_POSITION_RANGE = 14999992;
   private static final Services NO_SERVICES = new Services(
      null, ServicesKeySet.EMPTY, null, new GameTestServer.MockUserNameToIdResolver(), new GameTestServer.MockProfileResolver()
   );
   private static final FeatureFlagSet ENABLED_FEATURES = FeatureFlags.REGISTRY
      .allFlags()
      .subtract(FeatureFlagSet.of(FeatureFlags.REDSTONE_EXPERIMENTS, FeatureFlags.MINECART_IMPROVEMENTS));
   private final LocalSampleLogger sampleLogger = new LocalSampleLogger(4);
   private final Optional<String> testSelection;
   private final boolean verify;
   private List<GameTestBatch> testBatches = new ArrayList<>();
   private final Stopwatch stopwatch = Stopwatch.createUnstarted();
   private static final WorldOptions WORLD_OPTIONS = new WorldOptions(0L, false, false);
   private @Nullable MultipleTestTracker testTracker;

   public static GameTestServer create(
      Thread p_206607_, LevelStorageSource.LevelStorageAccess p_206608_, PackRepository p_206609_, Optional<String> p_392741_, boolean p_392375_
   ) {
      p_206609_.reload();
      ArrayList<String> arraylist = new ArrayList<>(p_206609_.getAvailableIds());
      arraylist.remove("vanilla");
      arraylist.addFirst("vanilla");
      WorldDataConfiguration worlddataconfiguration = new WorldDataConfiguration(new DataPackConfig(arraylist, List.of()), ENABLED_FEATURES);
      LevelSettings levelsettings = new LevelSettings(
         "Test Level", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(ENABLED_FEATURES), worlddataconfiguration
      );
      WorldLoader.PackConfig worldloader$packconfig = new WorldLoader.PackConfig(p_206609_, worlddataconfiguration, false, true);
      WorldLoader.InitConfig worldloader$initconfig = new WorldLoader.InitConfig(
         worldloader$packconfig, Commands.CommandSelection.DEDICATED, LevelBasedPermissionSet.OWNER
      );

      try {
         LOGGER.debug("Starting resource loading");
         Stopwatch stopwatch = Stopwatch.createStarted();
         WorldStem worldstem = Util.<WorldStem>blockUntilDone(
               p_448756_ -> WorldLoader.load(
                  worldloader$initconfig,
                  p_358476_ -> {
                     Registry<LevelStem> registry = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable()).freeze();
                     WorldDimensions.Complete worlddimensions$complete = p_358476_.datapackWorldgen()
                        .lookupOrThrow(Registries.WORLD_PRESET)
                        .getOrThrow(WorldPresets.FLAT)
                        .value()
                        .createWorldDimensions()
                        .bake(registry);
                     return new WorldLoader.DataLoadOutput<>(
                        new PrimaryLevelData(
                           levelsettings, WORLD_OPTIONS, worlddimensions$complete.specialWorldProperty(), worlddimensions$complete.lifecycle()
                        ),
                        worlddimensions$complete.dimensionsRegistryAccess()
                     );
                  },
                  WorldStem::new,
                  Util.backgroundExecutor(),
                  p_448756_
               )
            )
            .get();
         stopwatch.stop();
         LOGGER.debug("Finished resource loading after {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
         return new GameTestServer(p_206607_, p_206608_, p_206609_, worldstem, p_392741_, p_392375_);
      } catch (Exception exception) {
         LOGGER.warn("Failed to load vanilla datapack, bit oops", exception);
         System.exit(-1);
         throw new IllegalStateException();
      }
   }

   private GameTestServer(
      Thread p_206597_,
      LevelStorageSource.LevelStorageAccess p_206598_,
      PackRepository p_206599_,
      WorldStem p_206600_,
      Optional<String> p_391920_,
      boolean p_392965_
   ) {
      super(p_206597_, p_206598_, p_206599_, p_206600_, Proxy.NO_PROXY, DataFixers.getDataFixer(), NO_SERVICES, LoggingLevelLoadListener.forDedicatedServer());
      this.testSelection = p_391920_;
      this.verify = p_392965_;
   }

   @Override
   public boolean initServer() {
      this.setPlayerList(new PlayerList(this, this.registries(), this.playerDataStorage, new EmptyNotificationService()) {});
      Gizmos.withCollector(GizmoCollector.NOOP);
      this.loadLevel();
      ServerLevel serverlevel = this.overworld();
      this.testBatches = this.evaluateTestsToRun(serverlevel);
      LOGGER.info("Started game test server");
      return true;
   }

   private List<GameTestBatch> evaluateTestsToRun(ServerLevel p_392729_) {
      Registry<GameTestInstance> registry = p_392729_.registryAccess().lookupOrThrow(Registries.TEST_INSTANCE);
      Collection<Holder.Reference<GameTestInstance>> collection;
      GameTestBatchFactory.TestDecorator gametestbatchfactory$testdecorator;
      if (this.testSelection.isPresent()) {
         collection = getTestsForSelection(p_392729_.registryAccess(), this.testSelection.get()).filter(p_389777_ -> !p_389777_.value().manualOnly()).toList();
         if (this.verify) {
            gametestbatchfactory$testdecorator = GameTestServer::rotateAndMultiply;
            LOGGER.info("Verify requested. Will run each test that matches {} {} times", this.testSelection.get(), 100 * Rotation.values().length);
         } else {
            gametestbatchfactory$testdecorator = GameTestBatchFactory.DIRECT;
            LOGGER.info("Will run tests matching {} ({} tests)", this.testSelection.get(), collection.size());
         }
      } else {
         collection = registry.listElements().filter(p_389778_ -> !p_389778_.value().manualOnly()).toList();
         gametestbatchfactory$testdecorator = GameTestBatchFactory.DIRECT;
      }

      return GameTestBatchFactory.divideIntoBatches(collection, gametestbatchfactory$testdecorator, p_392729_);
   }

   private static Stream<GameTestInfo> rotateAndMultiply(Holder.Reference<GameTestInstance> p_391602_, ServerLevel p_395202_) {
      Builder<GameTestInfo> builder = Stream.builder();

      for (Rotation rotation : Rotation.values()) {
         for (int i = 0; i < 100; i++) {
            builder.add(new GameTestInfo(p_391602_, rotation, p_395202_, RetryOptions.noRetries()));
         }
      }

      return builder.build();
   }

   public static Stream<Holder.Reference<GameTestInstance>> getTestsForSelection(RegistryAccess p_393329_, String p_393521_) {
      return ResourceSelectorArgument.parse(new StringReader(p_393521_), p_393329_.lookupOrThrow(Registries.TEST_INSTANCE)).stream();
   }

   @Override
   public void tickServer(BooleanSupplier p_177619_) {
      super.tickServer(p_177619_);
      ServerLevel serverlevel = this.overworld();
      if (!this.haveTestsStarted()) {
         this.startTests(serverlevel);
      }

      if (serverlevel.getGameTime() % 20L == 0L) {
         LOGGER.info(this.testTracker.getProgressBar());
      }

      if (this.testTracker.isDone()) {
         this.halt(false);
         LOGGER.info(this.testTracker.getProgressBar());
         GlobalTestReporter.finish();
         LOGGER.info("========= {} GAME TESTS COMPLETE IN {} ======================", this.testTracker.getTotalCount(), this.stopwatch.stop());
         if (this.testTracker.hasFailedRequired()) {
            LOGGER.info("{} required tests failed :(", this.testTracker.getFailedRequiredCount());
            this.testTracker.getFailedRequired().forEach(GameTestServer::logFailedTest);
         } else {
            LOGGER.info("All {} required tests passed :)", this.testTracker.getTotalCount());
         }

         if (this.testTracker.hasFailedOptional()) {
            LOGGER.info("{} optional tests failed", this.testTracker.getFailedOptionalCount());
            this.testTracker.getFailedOptional().forEach(GameTestServer::logFailedTest);
         }

         LOGGER.info("====================================================");
      }
   }

   private static void logFailedTest(GameTestInfo p_397827_) {
      if (p_397827_.getRotation() != Rotation.NONE) {
         LOGGER.info(
            "   - {} with rotation {}: {}",
            new Object[]{p_397827_.id(), p_397827_.getRotation().getSerializedName(), p_397827_.getError().getDescription().getString()}
         );
      } else {
         LOGGER.info("   - {}: {}", p_397827_.id(), p_397827_.getError().getDescription().getString());
      }
   }

   @Override
   public SampleLogger getTickTimeLogger() {
      return this.sampleLogger;
   }

   @Override
   public boolean isTickTimeLoggingEnabled() {
      return false;
   }

   @Override
   public void waitUntilNextTick() {
      this.runAllTasks();
   }

   @Override
   public SystemReport fillServerSystemReport(SystemReport p_177613_) {
      p_177613_.setDetail("Type", "Game test server");
      return p_177613_;
   }

   @Override
   public void onServerExit() {
      super.onServerExit();
      LOGGER.info("Game test server shutting down");
      System.exit(this.testTracker != null ? this.testTracker.getFailedRequiredCount() : -1);
   }

   @Override
   public void onServerCrash(CrashReport p_177623_) {
      super.onServerCrash(p_177623_);
      LOGGER.error("Game test server crashed\n{}", p_177623_.getFriendlyReport(ReportType.CRASH));
      System.exit(1);
   }

   private void startTests(ServerLevel p_177625_) {
      BlockPos blockpos = new BlockPos(
         p_177625_.random.nextIntBetweenInclusive(-14999992, 14999992), -59, p_177625_.random.nextIntBetweenInclusive(-14999992, 14999992)
      );
      p_177625_.setRespawnData(LevelData.RespawnData.of(p_177625_.dimension(), blockpos, 0.0F, 0.0F));
      GameTestRunner gametestrunner = GameTestRunner.Builder.fromBatches(this.testBatches, p_177625_)
         .newStructureSpawner(new StructureGridSpawner(blockpos, 8, false))
         .build();
      Collection<GameTestInfo> collection = gametestrunner.getTestInfos();
      this.testTracker = new MultipleTestTracker(collection);
      LOGGER.info("{} tests are now running at position {}!", this.testTracker.getTotalCount(), blockpos.toShortString());
      this.stopwatch.reset();
      this.stopwatch.start();
      gametestrunner.start();
   }

   private boolean haveTestsStarted() {
      return this.testTracker != null;
   }

   @Override
   public boolean isHardcore() {
      return false;
   }

   @Override
   public LevelBasedPermissionSet operatorUserPermissions() {
      return LevelBasedPermissionSet.ALL;
   }

   @Override
   public PermissionSet getFunctionCompilationPermissions() {
      return LevelBasedPermissionSet.OWNER;
   }

   @Override
   public boolean shouldRconBroadcast() {
      return false;
   }

   @Override
   public boolean isDedicatedServer() {
      return false;
   }

   @Override
   public int getRateLimitPacketsPerSecond() {
      return 0;
   }

   @Override
   public boolean useNativeTransport() {
      return false;
   }

   @Override
   public boolean isPublished() {
      return false;
   }

   @Override
   public boolean shouldInformAdmins() {
      return false;
   }

   @Override
   public boolean isSingleplayerOwner(NameAndId p_423304_) {
      return false;
   }

   @Override
   public int getMaxPlayers() {
      return 1;
   }

   static class MockProfileResolver implements ProfileResolver {
      @Override
      public Optional<GameProfile> fetchByName(String p_425451_) {
         return Optional.empty();
      }

      @Override
      public Optional<GameProfile> fetchById(UUID p_423703_) {
         return Optional.empty();
      }
   }

   static class MockUserNameToIdResolver implements UserNameToIdResolver {
      private final Set<NameAndId> savedIds = new HashSet<>();

      @Override
      public void add(NameAndId p_428195_) {
         this.savedIds.add(p_428195_);
      }

      @Override
      public Optional<NameAndId> get(String p_431713_) {
         return this.savedIds
            .stream()
            .filter(p_422533_ -> p_422533_.name().equals(p_431713_))
            .findFirst()
            .or(() -> Optional.of(NameAndId.createOffline(p_431713_)));
      }

      @Override
      public Optional<NameAndId> get(UUID p_425069_) {
         return this.savedIds.stream().filter(p_427482_ -> p_427482_.id().equals(p_425069_)).findFirst();
      }

      @Override
      public void resolveOfflineUsers(boolean p_426279_) {
      }

      @Override
      public void save() {
      }
   }
}
