package net.minecraft.server;

import com.mojang.authlib.services.MinecraftServicesDiscoveryService;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import joptsimple.util.PathProperties;
import net.minecraft.CrashReport;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.SharedConstants;
import net.minecraft.SuppressForbidden;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.dedicated.DedicatedServerSettings;
import net.minecraft.server.jsonrpc.JsonRpc;
import net.minecraft.server.jsonrpc.ManagementServer;
import net.minecraft.server.notifications.NotificationManager;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.profiling.jfr.Environment;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.worldupdate.UpgradeProgress;
import net.minecraft.util.worldupdate.WorldUpgrader;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Main {
   private static final Logger LOGGER = LogUtils.getLogger();

   @SuppressForbidden(reason = "System.out needed before bootstrap")
   public static void main(final String[] args) {
      SharedConstants.tryDetectVersion();
      OptionParser parser = new OptionParser();
      OptionSpec<Void> nogui = parser.accepts("nogui");
      OptionSpec<Void> initSettings = parser.accepts("initSettings", "Initializes 'server.properties' and 'eula.txt', then quits");
      OptionSpec<Void> demo = parser.accepts("demo");
      OptionSpec<Void> bonusChest = parser.accepts("bonusChest");
      OptionSpec<Void> forceUpgrade = parser.accepts("forceUpgrade");
      OptionSpec<Void> eraseCache = parser.accepts("eraseCache");
      OptionSpec<Void> recreateRegionFiles = parser.accepts("recreateRegionFiles");
      OptionSpec<Void> safeMode = parser.accepts("safeMode", "Loads level with vanilla datapack only");
      OptionSpec<Void> help = parser.accepts("help").forHelp();
      OptionSpec<String> universe = parser.accepts("universe").withRequiredArg().defaultsTo(".", new String[0]);
      OptionSpec<String> worldName = parser.accepts("world").withRequiredArg();
      OptionSpec<Integer> port = parser.accepts("port").withRequiredArg().ofType(Integer.class).defaultsTo(-1, new Integer[0]);
      OptionSpec<String> serverId = parser.accepts("serverId").withRequiredArg();
      OptionSpec<Void> jfrProfilingOption = parser.accepts("jfrProfile");
      OptionSpec<Path> pidFile = parser.accepts("pidFile").withRequiredArg().withValuesConvertedBy(new PathConverter(new PathProperties[0]));
      OptionSpec<String> nonOptions = parser.nonOptions();

      try {
         OptionSet options = parser.parse(args);
         if (options.has(help)) {
            parser.printHelpOn(System.err);
            return;
         }

         Path pidFilePath = (Path)options.valueOf(pidFile);
         if (pidFilePath != null) {
            writePidFile(pidFilePath);
         }

         CrashReport.preload();
         if (options.has(jfrProfilingOption)) {
            JvmProfiler.INSTANCE.start(Environment.SERVER);
         }

         Bootstrap.bootStrap();
         Bootstrap.validate();
         Util.startTimerHackThread();
         Path settingsFile = Paths.get("server.properties");
         DedicatedServerSettings settings = new DedicatedServerSettings(settingsFile);
         settings.forceSave();
         RegionFileVersion.configure(settings.getProperties().regionFileComression);
         Path eulaFile = Paths.get("eula.txt");
         Eula eula = new Eula(eulaFile);
         if (options.has(initSettings)) {
            LOGGER.info("Initialized '{}' and '{}'", settingsFile.toAbsolutePath(), eulaFile.toAbsolutePath());
            return;
         }

         if (!eula.hasAgreedToEULA()) {
            LOGGER.info("You need to agree to the EULA in order to run the server. Go to eula.txt for more info.");
            return;
         }

         File universePath = new File((String)options.valueOf(universe));
         Services services = Services.create(MinecraftServicesDiscoveryService.create(Proxy.NO_PROXY), universePath);
         NotificationManager notificationManager = new NotificationManager();
         ManagementServer jsonRpcServer = JsonRpc.create(settings, notificationManager);
         String levelName = (String)Optional.ofNullable((String)options.valueOf(worldName)).orElse(settings.getProperties().levelName);
         LevelStorageSource levelStorageSource = LevelStorageSource.createDefault(universePath.toPath());
         LevelStorageSource.LevelStorageAccess access = levelStorageSource.validateAndCreateAccess(levelName);
         Dynamic<?> levelDataTag;
         if (access.hasWorldData()) {
            Dynamic<?> levelDataUnfixed;
            try {
               levelDataUnfixed = access.getUnfixedDataTagWithFallback();
            } catch (IOException | NbtException | ReportedNbtException ex) {
               LOGGER.error("Failed to load world data. World files may be corrupted. Shutting down.", ex);
               return;
            }

            LevelSummary summary = access.fixAndGetSummaryFromTag(levelDataUnfixed);
            if (summary.requiresManualConversion()) {
               LOGGER.info("This world must be opened in an older version (like 1.6.4) to be safely converted");
               return;
            }

            if (!summary.isCompatible()) {
               LOGGER.info("This world was created by an incompatible version.");
               return;
            }

            levelDataTag = DataFixers.getFileFixer().fix(access, levelDataUnfixed, new UpgradeProgress(notificationManager));
         } else {
            levelDataTag = null;
         }

         boolean safeModeEnabled = options.has(safeMode);
         if (safeModeEnabled) {
            LOGGER.warn("Safe mode active, only vanilla datapack will be loaded");
         }

         PackRepository packRepository = ServerPacksSource.createPackRepository(access);

         WorldStem worldStem;
         try {
            WorldLoader.InitConfig worldLoadConfig = loadOrCreateConfig(settings.getProperties(), levelDataTag, safeModeEnabled, packRepository);
            worldStem = Util.<WorldStem>blockUntilDone(
                  executor -> WorldLoader.load(
                     worldLoadConfig,
                     context -> {
                        Registry<LevelStem> datapackDimensions = context.datapackDimensions().lookupOrThrow(Registries.LEVEL_STEM);
                        if (levelDataTag != null) {
                           LevelDataAndDimensions worldData = LevelStorageSource.getLevelDataAndDimensions(
                              access, levelDataTag, context.dataConfiguration(), datapackDimensions, context.datapackWorldRegistries()
                           );
                           return new WorldLoader.DataLoadOutput<>(worldData.worldDataAndGenSettings(), worldData.dimensions().dimensionsRegistryAccess());
                        } else {
                           LOGGER.info("No existing world data, creating new world");
                           return createNewWorldData(settings, context, datapackDimensions, options.has(demo), options.has(bonusChest));
                        }
                     },
                     WorldStem::new,
                     Util.backgroundExecutor(),
                     executor
                  )
               )
               .get();
         } catch (Exception e) {
            LOGGER.warn("Failed to load datapacks, can't proceed with server load. You can either fix your datapacks or reset to vanilla with --safeMode", e);
            return;
         }

         RegistryAccess.Frozen registryHolder = worldStem.registries().compositeAccess();
         WorldData data = worldStem.worldDataAndGenSettings().data();
         boolean recreateRegionFilesValue = options.has(recreateRegionFiles);
         if (options.has(forceUpgrade) || recreateRegionFilesValue) {
            forceUpgrade(access, DataFixers.getDataFixer(), options.has(eraseCache), () -> true, registryHolder, recreateRegionFilesValue);
         }

         access.saveDataTag(data);
         final DedicatedServer dedicatedServer = MinecraftServer.spin(
            thread -> {
               DedicatedServer server = new DedicatedServer(
                  thread,
                  access,
                  packRepository,
                  worldStem,
                  Optional.empty(),
                  settings,
                  DataFixers.getDataFixer(),
                  services,
                  jsonRpcServer,
                  notificationManager
               );
               notificationManager.setServer(server);
               server.setPort((Integer)options.valueOf(port));
               server.setDemo(options.has(demo));
               server.setId((String)options.valueOf(serverId));
               boolean gui = !options.has(nogui) && !options.valuesOf(nonOptions).contains("nogui");
               if (gui && !GraphicsEnvironment.isHeadless()) {
                  server.showGui();
               }

               return server;
            }
         );
         Thread shutdownThread = new Thread("Server Shutdown Thread") {
            @Override
            public void run() {
               dedicatedServer.halt(true);
            }
         };
         shutdownThread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
         Runtime.getRuntime().addShutdownHook(shutdownThread);
      } catch (Throwable t) {
         LOGGER.error(LogUtils.FATAL_MARKER, "Failed to start the minecraft server", t);
      }
   }

   private static WorldLoader.DataLoadOutput<LevelDataAndDimensions.WorldDataAndGenSettings> createNewWorldData(
      final DedicatedServerSettings settings,
      final WorldLoader.DataLoadContext context,
      final Registry<LevelStem> datapackDimensions,
      final boolean demoMode,
      final boolean bonusChest
   ) {
      LevelSettings createLevelSettings;
      WorldOptions worldOptions;
      WorldDimensions dimensions;
      if (demoMode) {
         createLevelSettings = MinecraftServer.DEMO_SETTINGS;
         worldOptions = WorldOptions.DEMO_OPTIONS;
         dimensions = WorldPresets.createNormalWorldDimensions(context.datapackWorldRegistries());
      } else {
         DedicatedServerProperties properties = settings.getProperties();
         createLevelSettings = new LevelSettings(
            properties.levelName,
            properties.gameMode.get(),
            new LevelSettings.DifficultySettings(properties.difficulty.get(), properties.hardcore, false),
            false,
            context.dataConfiguration()
         );
         worldOptions = bonusChest ? properties.worldOptions.withBonusChest(true) : properties.worldOptions;
         dimensions = properties.createDimensions(context.datapackWorldRegistries());
      }

      WorldDimensions.Complete finalDimensions = dimensions.bake(datapackDimensions);
      Lifecycle lifecycle = finalDimensions.lifecycle().add(context.datapackWorldRegistries().allRegistriesLifecycle());
      PrimaryLevelData primaryLevelData = new PrimaryLevelData(createLevelSettings, finalDimensions.specialWorldProperty(), lifecycle);
      return new WorldLoader.DataLoadOutput<>(
         new LevelDataAndDimensions.WorldDataAndGenSettings(primaryLevelData, new WorldGenSettings(worldOptions, dimensions)),
         finalDimensions.dimensionsRegistryAccess()
      );
   }

   private static void writePidFile(final Path path) {
      try {
         long pid = ProcessHandle.current().pid();
         Files.writeString(path, Long.toString(pid));
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }

   private static WorldLoader.InitConfig loadOrCreateConfig(
      final DedicatedServerProperties properties, final @Nullable Dynamic<?> levelDataTag, final boolean safeModeEnabled, final PackRepository packRepository
   ) {
      boolean initMode;
      WorldDataConfiguration dataConfigToUse;
      if (levelDataTag != null) {
         WorldDataConfiguration storedConfiguration = LevelStorageSource.readDataConfig(levelDataTag);
         initMode = false;
         dataConfigToUse = storedConfiguration;
      } else {
         initMode = true;
         dataConfigToUse = new WorldDataConfiguration(properties.initialDataPackConfiguration, FeatureFlags.DEFAULT_FLAGS);
      }

      WorldLoader.PackConfig packConfig = new WorldLoader.PackConfig(packRepository, dataConfigToUse, safeModeEnabled, initMode);
      return new WorldLoader.InitConfig(packConfig, Commands.CommandSelection.DEDICATED, properties.functionPermissions);
   }

   private static void forceUpgrade(
      final LevelStorageSource.LevelStorageAccess storageSource,
      final DataFixer fixerUpper,
      final boolean eraseCache,
      final BooleanSupplier isRunning,
      final RegistryAccess registryAccess,
      final boolean recreateRegionFiles
   ) {
      LOGGER.info("Forcing world upgrade!");

      try (WorldUpgrader upgrader = new WorldUpgrader(storageSource, fixerUpper, registryAccess, eraseCache, recreateRegionFiles)) {
         Component lastStatus = null;

         while (!upgrader.isFinished()) {
            Component status = upgrader.getStatus();
            if (lastStatus != status) {
               lastStatus = status;
               LOGGER.info("{}", upgrader.getStatus().getString());
            }

            int totalChunks = upgrader.getTotalChunks();
            if (totalChunks > 0) {
               int done = upgrader.getConverted() + upgrader.getSkipped();
               LOGGER.info("{}% completed ({} / {} chunks)...", new Object[]{Mth.floor((float)done / totalChunks * 100.0F), done, totalChunks});
            }

            if (!isRunning.getAsBoolean()) {
               upgrader.cancel();
            } else {
               try {
                  Thread.sleep(1000L);
               } catch (InterruptedException var12) {
               }
            }
         }
      }
   }
}
