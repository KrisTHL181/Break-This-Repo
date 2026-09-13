package net.minecraft.gametest.framework;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.SuppressForbidden;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

public class GameTestMainUtil {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String DEFAULT_UNIVERSE_DIR = "gametestserver";
   private static final String LEVEL_NAME = "gametestworld";
   private static final OptionParser parser = new OptionParser();
   private static final OptionSpec<String> universe = parser.accepts(
         "universe", "The path to where the test server world will be created. Any existing folder will be replaced."
      )
      .withRequiredArg()
      .defaultsTo("gametestserver", new String[0]);
   private static final OptionSpec<File> report = parser.accepts("report", "Exports results in a junit-like XML report at the given path.")
      .withRequiredArg()
      .ofType(File.class);
   private static final OptionSpec<String> tests = parser.accepts(
         "tests", "Which test(s) to run (namespaced ID selector using wildcards). Empty means run all."
      )
      .withRequiredArg();
   private static final OptionSpec<Boolean> verify = parser.accepts(
         "verify", "Runs the tests specified with `test` or `testNamespace` 100 times for each 90 degree rotation step"
      )
      .withRequiredArg()
      .ofType(Boolean.class)
      .defaultsTo(false, new Boolean[0]);
   private static final OptionSpec<Integer> repeatCount = parser.accepts("repeatCount", "Runs each of the specified tests this many times")
      .withRequiredArg()
      .ofType(Integer.class)
      .defaultsTo(1, new Integer[0]);
   private static final OptionSpec<String> packs = parser.accepts("packs", "A folder of datapacks to include in the world").withRequiredArg();
   private static final OptionSpec<Void> help = parser.accepts("help").forHelp();

   @SuppressForbidden(reason = "Using System.err due to no bootstrap")
   public static void runGameTestServer(final String[] args, final Consumer<String> onUniverseCreated) throws Exception {
      parser.allowsUnrecognizedOptions();
      OptionSet options = parser.parse(args);
      if (options.has(help)) {
         parser.printHelpOn(System.err);
      } else {
         if ((Boolean)options.valueOf(verify) && !options.has(tests)) {
            LOGGER.error("Please specify a test selection to run the verify option. For example: --verify --tests example:test_something_*");
            System.exit(-1);
         }

         if ((Boolean)options.valueOf(verify) && options.has(repeatCount)) {
            LOGGER.info("Flag --verify is true, the --repeatCount value will be ignored");
         }

         LOGGER.info("Running GameTestMain with cwd '{}', universe path '{}'", System.getProperty("user.dir"), options.valueOf(universe));
         if (options.has(report)) {
            GlobalTestReporter.replaceWith(new JUnitLikeTestReporter((File)report.value(options)));
         }

         Bootstrap.bootStrap();
         Util.startTimerHackThread();
         String universePath = (String)options.valueOf(universe);
         createOrResetDir(universePath);
         onUniverseCreated.accept(universePath);
         if (options.has(packs)) {
            String packFolder = (String)options.valueOf(packs);
            copyPacks(universePath, packFolder);
         }

         LevelStorageSource.LevelStorageAccess levelStorageSource = LevelStorageSource.createDefault(Paths.get(universePath)).createAccess("gametestworld");
         PackRepository packRepository = ServerPacksSource.createPackRepository(levelStorageSource);
         MinecraftServer.spin(
            thread -> GameTestServer.create(
               thread,
               levelStorageSource,
               packRepository,
               optionalFromOption(options, tests),
               (Boolean)options.valueOf(verify),
               (Integer)options.valueOf(repeatCount)
            )
         );
      }
   }

   private static Optional<String> optionalFromOption(final OptionSet options, final OptionSpec<String> option) {
      return options.has(option) ? Optional.of((String)options.valueOf(option)) : Optional.empty();
   }

   private static void createOrResetDir(final String universePath) throws IOException {
      Path universeDir = Paths.get(universePath);
      if (Files.exists(universeDir)) {
         FileUtils.deleteDirectory(universeDir.toFile());
      }

      Files.createDirectories(universeDir);
   }

   private static void copyPacks(final String serverPath, final String packSourcePath) throws IOException {
      Path worldPackFolder = Paths.get(serverPath).resolve("gametestworld").resolve("datapacks");
      if (!Files.exists(worldPackFolder)) {
         Files.createDirectories(worldPackFolder);
      }

      Path sourceFolder = Paths.get(packSourcePath);
      if (Files.exists(sourceFolder)) {
         try (Stream<Path> list = Files.list(sourceFolder)) {
            for (Path path : list.toList()) {
               Path destination = worldPackFolder.resolve(path.getFileName());
               if (Files.isDirectory(path)) {
                  if (Files.isRegularFile(path.resolve("pack.mcmeta"))) {
                     FileUtils.copyDirectory(path.toFile(), destination.toFile());
                     LOGGER.info("Included folder pack {}", path.getFileName());
                  }
               } else if (path.toString().endsWith(".zip")) {
                  Files.copy(path, destination);
                  LOGGER.info("Included zip pack {}", path.getFileName());
               }
            }
         }
      }
   }
}
