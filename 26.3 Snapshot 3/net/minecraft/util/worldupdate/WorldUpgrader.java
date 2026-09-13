package net.minecraft.util.worldupdate;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.slf4j.Logger;

public class WorldUpgrader implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder().setNameFormat("World Upgrader #%d").setDaemon(true).build();
   private final UpgradeStatusTranslator statusTranslator = new UpgradeStatusTranslator();
   private final Registry<LevelStem> dimensions;
   private final Set<ResourceKey<Level>> levels;
   private final boolean eraseCache;
   private final boolean recreateRegionFiles;
   private final LevelStorageSource.LevelStorageAccess levelStorage;
   private final Thread thread;
   private final DataFixer dataFixer;
   private final UpgradeProgress upgradeProgress = new UpgradeProgress();
   private final SavedDataStorage overworldSavedDataStorage;

   public WorldUpgrader(
      final LevelStorageSource.LevelStorageAccess levelSource,
      final DataFixer dataFixer,
      final RegistryAccess registryAccess,
      final boolean eraseCache,
      final boolean recreateRegionFiles
   ) {
      this.dimensions = registryAccess.lookupOrThrow(Registries.LEVEL_STEM);
      this.levels = this.dimensions.registryKeySet().stream().map(Registries::levelStemToLevel).collect(Collectors.toUnmodifiableSet());
      this.eraseCache = eraseCache;
      this.dataFixer = dataFixer;
      this.levelStorage = levelSource;
      this.overworldSavedDataStorage = new SavedDataStorage(this.levelStorage.getDimensionPath(Level.OVERWORLD).resolve("data"), dataFixer, registryAccess);
      this.recreateRegionFiles = recreateRegionFiles;
      this.thread = THREAD_FACTORY.newThread(this::work);
      this.thread.setUncaughtExceptionHandler((t, e) -> {
         LOGGER.error("Error upgrading world", e);
         this.upgradeProgress.setStatus(UpgradeProgress.Status.FAILED);
         this.upgradeProgress.setFinished(true);
      });
      this.thread.start();
   }

   public static CompoundTag getDataFixContextTag(final Registry<LevelStem> dimensions, final ResourceKey<Level> dimension) {
      ChunkGenerator generator = dimensions.getValueOrThrow(Registries.levelToLevelStem(dimension)).generator();
      return ChunkMap.getChunkDataFixContextTag(dimension, generator.getTypeNameForDataFixer());
   }

   public static boolean verifyChunkPosAndEraseCache(final ChunkPos pos, final CompoundTag upgradedTag) {
      verifyChunkPos(pos, upgradedTag);
      boolean changed = upgradedTag.contains("Heightmaps");
      upgradedTag.remove("Heightmaps");
      changed = changed || upgradedTag.contains("isLightOn");
      upgradedTag.remove("isLightOn");
      ListTag sections = upgradedTag.getListOrEmpty("sections");

      for (int i = 0; i < sections.size(); i++) {
         Optional<CompoundTag> maybeSection = sections.getCompound(i);
         if (!maybeSection.isEmpty()) {
            CompoundTag section = maybeSection.get();
            changed = changed || section.contains("BlockLight");
            section.remove("BlockLight");
            changed = changed || section.contains("SkyLight");
            section.remove("SkyLight");
         }
      }

      return changed;
   }

   public static boolean verifyChunkPos(final ChunkPos pos, final CompoundTag upgradedTag) {
      ChunkPos storedPos = new ChunkPos(upgradedTag.getIntOr("xPos", 0), upgradedTag.getIntOr("zPos", 0));
      if (!storedPos.equals(pos)) {
         LOGGER.warn("Chunk {} has invalid position {}", pos, storedPos);
      }

      return false;
   }

   public void cancel() {
      this.upgradeProgress.setCanceled();

      try {
         this.thread.join();
      } catch (InterruptedException var2) {
      }
   }

   private void work() {
      long conversionTime = Util.getMillis();
      int currentVersion = SharedConstants.getCurrentVersion().dataVersion().version();
      LOGGER.info("Upgrading entities");
      this.upgradeLevels(
         DataFixTypes.ENTITY_CHUNK,
         new RegionStorageUpgrader.Builder(this.dataFixer)
            .setTypeAndFolderName("entities")
            .setRecreateRegionFiles(this.recreateRegionFiles)
            .trackProgress(this.upgradeProgress)
      );
      LOGGER.info("Upgrading POIs");
      this.upgradeLevels(
         DataFixTypes.POI_CHUNK,
         new RegionStorageUpgrader.Builder(this.dataFixer)
            .setTypeAndFolderName("poi")
            .setDefaultVersion(1945)
            .setRecreateRegionFiles(this.recreateRegionFiles)
            .trackProgress(this.upgradeProgress)
      );
      LOGGER.info("Upgrading blocks");
      this.upgradeLevels(
         DataFixTypes.CHUNK,
         new RegionStorageUpgrader.Builder(this.dataFixer)
            .setType("chunk")
            .setFolderName("region")
            .setRecreateRegionFiles(this.recreateRegionFiles)
            .trackProgress(this.upgradeProgress),
         (levelSpecificBuilder, level) -> levelSpecificBuilder.setDataFixContextTag(getDataFixContextTag(this.dimensions, level))
            .addTagModifier(currentVersion, this.eraseCache ? WorldUpgrader::verifyChunkPosAndEraseCache : WorldUpgrader::verifyChunkPos)
      );
      this.overworldSavedDataStorage.saveAndJoin();
      conversionTime = Util.getMillis() - conversionTime;
      LOGGER.info("World optimization finished after {} seconds", conversionTime / 1000L);
      this.upgradeProgress.setFinished(true);
   }

   private void upgradeLevels(final DataFixTypes dataFixType, final RegionStorageUpgrader.Builder builder) {
      this.upgradeLevels(dataFixType, builder, (levelSpecificBuilder, level) -> levelSpecificBuilder);
   }

   private void upgradeLevels(
      final DataFixTypes dataFixType,
      final RegionStorageUpgrader.Builder builder,
      final BiFunction<RegionStorageUpgrader.Builder, ResourceKey<Level>, RegionStorageUpgrader.Builder> levelSpecificBuilder
   ) {
      List<RegionStorageUpgrader> upgraders = new ArrayList<>();
      this.upgradeProgress.reset(dataFixType);
      this.upgradeProgress.setType(UpgradeProgress.Type.REGIONS);
      builder.setDataFixType(dataFixType);
      int previousCopiesFileAmounts = 0;

      for (ResourceKey<Level> level : this.levels) {
         RegionStorageUpgrader upgrader = levelSpecificBuilder.apply(builder.copy(), level).build(previousCopiesFileAmounts);
         upgrader.init(level, this.levelStorage);
         previousCopiesFileAmounts += upgrader.fileAmount();
         upgraders.add(upgrader);
      }

      upgraders.forEach(RegionStorageUpgrader::upgrade);
   }

   public boolean isFinished() {
      return this.upgradeProgress.isFinished();
   }

   public Set<ResourceKey<Level>> levels() {
      return this.levels;
   }

   public float dimensionProgress(final ResourceKey<Level> dimension) {
      return this.upgradeProgress.getDimensionProgress(dimension);
   }

   public float getTotalProgress() {
      return this.upgradeProgress.getTotalProgress();
   }

   public int getTotalChunks() {
      return this.upgradeProgress.getTotalChunks();
   }

   public int getConverted() {
      return this.upgradeProgress.getConverted();
   }

   public int getSkipped() {
      return this.upgradeProgress.getSkipped();
   }

   public Component getStatus() {
      return this.statusTranslator.translate(this.upgradeProgress);
   }

   @Override
   public void close() {
      this.overworldSavedDataStorage.close();
   }
}
