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
   private static final OptionSpec<String> packs = parser.accepts("packs", "A folder of datapacks to include in the world").withRequiredArg();
   private static final OptionSpec<Void> help = parser.accepts("help").forHelp();

   @SuppressForbidden(reason = "Using System.err due to no bootstrap")
   public static void runGameTestServer(String[] p_393077_, Consumer<String> p_393652_) throws Exception {
      parser.allowsUnrecognizedOptions();
      OptionSet optionset = parser.parse(p_393077_);
      if (optionset.has(help)) {
         parser.printHelpOn(System.err);
      } else {
         if ((Boolean)optionset.valueOf(verify) && !optionset.has(tests)) {
            LOGGER.error("Please specify a test selection to run the verify option. For example: --verify --tests example:test_something_*");
            System.exit(-1);
         }

         LOGGER.info("Running GameTestMain with cwd '{}', universe path '{}'", System.getProperty("user.dir"), optionset.valueOf(universe));
         if (optionset.has(report)) {
            GlobalTestReporter.replaceWith(new JUnitLikeTestReporter((File)report.value(optionset)));
         }

         Bootstrap.bootStrap();
         Util.startTimerHackThread();
         String s = (String)optionset.valueOf(universe);
         createOrResetDir(s);
         p_393652_.accept(s);
         if (optionset.has(packs)) {
            String s1 = (String)optionset.valueOf(packs);
            copyPacks(s, s1);
         }

         LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = LevelStorageSource.createDefault(Paths.get(s))
            .createAccess("gametestworld");
         PackRepository packrepository = ServerPacksSource.createPackRepository(levelstoragesource$levelstorageaccess);
         MinecraftServer.spin(
            p_421245_ -> GameTestServer.create(
               p_421245_, levelstoragesource$levelstorageaccess, packrepository, optionalFromOption(optionset, tests), optionset.has(verify)
            )
         );
      }
   }

   private static Optional<String> optionalFromOption(OptionSet p_396003_, OptionSpec<String> p_394563_) {
      return p_396003_.has(p_394563_) ? Optional.of((String)p_396003_.valueOf(p_394563_)) : Optional.empty();
   }

   private static void createOrResetDir(String p_392184_) throws IOException {
      Path path = Paths.get(p_392184_);
      if (Files.exists(path)) {
         FileUtils.deleteDirectory(path.toFile());
      }

      Files.createDirectories(path);
   }

   private static void copyPacks(String p_397956_, String p_396386_) throws IOException {
      Path path = Paths.get(p_397956_).resolve("gametestworld").resolve("datapacks");
      if (!Files.exists(path)) {
         Files.createDirectories(path);
      }

      Path path1 = Paths.get(p_396386_);
      if (Files.exists(path1)) {
         try (Stream<Path> stream = Files.list(path1)) {
            for (Path path2 : stream.toList()) {
               Path path3 = path.resolve(path2.getFileName());
               if (Files.isDirectory(path2)) {
                  if (Files.isRegularFile(path2.resolve("pack.mcmeta"))) {
                     FileUtils.copyDirectory(path2.toFile(), path3.toFile());
                     LOGGER.info("Included folder pack {}", path2.getFileName());
                  }
               } else if (path2.toString().endsWith(".zip")) {
                  Files.copy(path2, path3);
                  LOGGER.info("Included zip pack {}", path2.getFileName());
               }
            }
         }
      }
   }
}
