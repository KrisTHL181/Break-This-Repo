package net.minecraft.data;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.minecraft.WorldVersion;
import net.minecraft.server.Bootstrap;
import org.slf4j.Logger;

public abstract class DataGenerator {
   private static final Logger LOGGER = LogUtils.getLogger();
   protected final PackOutput vanillaPackOutput;
   protected final Set<String> allProviderIds = new HashSet<>();
   protected final Map<String, DataProvider> providersToRun = new LinkedHashMap<>();

   public DataGenerator(final Path output) {
      this.vanillaPackOutput = new PackOutput(output);
   }

   public abstract void run() throws IOException;

   public DataGenerator.PackGenerator getVanillaPack(final boolean toRun) {
      return new DataGenerator.PackGenerator(toRun, "vanilla", this.vanillaPackOutput);
   }

   public DataGenerator.PackGenerator getBuiltinDatapack(final boolean toRun, final String packId) {
      Path packOutputDir = this.vanillaPackOutput.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("minecraft").resolve("datapacks").resolve(packId);
      return new DataGenerator.PackGenerator(toRun, packId, new PackOutput(packOutputDir));
   }

   static {
      Bootstrap.bootStrap();
   }

   public static class Cached extends DataGenerator {
      private final Path rootOutputFolder;
      private final WorldVersion version;
      private final boolean alwaysGenerate;

      public Cached(final Path output, final WorldVersion version, final boolean alwaysGenerate) {
         super(output);
         this.rootOutputFolder = output;
         this.alwaysGenerate = alwaysGenerate;
         this.version = version;
      }

      @Override
      public void run() throws IOException {
         HashCache cache = new HashCache(this.rootOutputFolder, this.allProviderIds, this.version);
         Stopwatch totalTime = Stopwatch.createStarted();
         Stopwatch stopwatch = Stopwatch.createUnstarted();
         this.providersToRun.forEach((providerId, provider) -> {
            if (!this.alwaysGenerate && !cache.shouldRunInThisVersion(providerId)) {
               DataGenerator.LOGGER.debug("Generator {} already run for version {}", providerId, this.version.name());
            } else {
               DataGenerator.LOGGER.info("Starting provider: {}", providerId);
               stopwatch.start();
               cache.applyUpdate(cache.generateUpdate(providerId, provider::run).join());
               stopwatch.stop();
               DataGenerator.LOGGER.info("{} finished after {} ms", providerId, stopwatch.elapsed(TimeUnit.MILLISECONDS));
               stopwatch.reset();
            }
         });
         DataGenerator.LOGGER.info("All providers took: {} ms", totalTime.elapsed(TimeUnit.MILLISECONDS));
         cache.purgeStaleAndWrite();
      }
   }

   public class PackGenerator {
      private final boolean toRun;
      private final String providerPrefix;
      private final PackOutput output;

      private PackGenerator(final boolean toRun, final String providerPrefix, final PackOutput output) {
         this.toRun = toRun;
         this.providerPrefix = providerPrefix;
         this.output = output;
      }

      public <T extends DataProvider> T addProvider(final DataProvider.Factory<T> factory) {
         T provider = factory.create(this.output);
         String providerId = this.providerPrefix + "/" + provider.getName();
         if (!DataGenerator.this.allProviderIds.add(providerId)) {
            throw new IllegalStateException("Duplicate provider: " + providerId);
         }

         if (this.toRun) {
            DataGenerator.this.providersToRun.put(providerId, provider);
         }

         return provider;
      }
   }

   public static class Uncached extends DataGenerator {
      public Uncached(final Path output) {
         super(output);
      }

      @Override
      public void run() throws IOException {
         Stopwatch totalTime = Stopwatch.createStarted();
         Stopwatch stopwatch = Stopwatch.createUnstarted();
         this.providersToRun.forEach((providerId, provider) -> {
            DataGenerator.LOGGER.info("Starting uncached provider: {}", providerId);
            stopwatch.start();
            provider.run(CachedOutput.NO_CACHE).join();
            stopwatch.stop();
            DataGenerator.LOGGER.info("{} finished after {} ms", providerId, stopwatch.elapsed(TimeUnit.MILLISECONDS));
            stopwatch.reset();
         });
         DataGenerator.LOGGER.info("All providers took: {} ms", totalTime.elapsed(TimeUnit.MILLISECONDS));
      }
   }
}
