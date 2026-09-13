package net.minecraft.util.worldupdate;

import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RecreatingSimpleRegionStorage;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RegionStorageUpgrader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String NEW_DIRECTORY_PREFIX = "new_";
   private static final Pattern REGEX = Pattern.compile("^r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.mca$");
   private final DataFixer dataFixer;
   private final UpgradeProgress upgradeProgress;
   private final String type;
   private final String folderName;
   protected @Nullable CompletableFuture<Void> previousWriteFuture;
   protected final DataFixTypes dataFixType;
   protected final int defaultVersion;
   private final boolean recreateRegionFiles;
   private @Nullable ResourceKey<Level> dimensionKey;
   private @Nullable SimpleRegionStorage storage;
   private @Nullable List<FileToUpgrade> files;
   private final int startIndex;
   private final @Nullable CompoundTag dataFixContextTag;
   private final Int2ObjectMap<RegionStorageUpgrader.TagModifier> tagModifiers;

   protected RegionStorageUpgrader(
      final DataFixer dataFixer,
      final DataFixTypes dataFixType,
      final String type,
      final String folderName,
      final int defaultVersion,
      final boolean recreateRegionFiles,
      final UpgradeProgress upgradeProgress,
      final int startIndex,
      final @Nullable CompoundTag dataFixContextTag,
      final Int2ObjectMap<RegionStorageUpgrader.TagModifier> tagModifiers
   ) {
      this.dataFixer = dataFixer;
      this.dataFixType = dataFixType;
      this.type = type;
      this.folderName = folderName;
      this.recreateRegionFiles = recreateRegionFiles;
      this.defaultVersion = defaultVersion;
      this.upgradeProgress = upgradeProgress;
      this.startIndex = startIndex;
      this.dataFixContextTag = dataFixContextTag;
      this.tagModifiers = tagModifiers;
   }

   public void init(final ResourceKey<Level> dimensionKey, final LevelStorageSource.LevelStorageAccess levelStorage) {
      RegionStorageInfo info = new RegionStorageInfo(levelStorage.getLevelId(), dimensionKey, this.type);
      Path regionFolder = levelStorage.getDimensionPath(dimensionKey).resolve(this.folderName);
      this.dimensionKey = dimensionKey;
      this.storage = this.createStorage(info, regionFolder);
      this.files = this.getFilesToProcess(info, regionFolder);
   }

   public void upgrade() {
      if (this.dimensionKey == null || this.storage == null || this.files == null) {
         throw new IllegalStateException("RegionStorageUpgrader has not been initialized");
      }

      if (!this.files.isEmpty()) {
         float totalSize = this.upgradeProgress.getTotalFileFixStats().totalOperations();
         this.upgradeProgress.setStatus(UpgradeProgress.Status.UPGRADING);
         ListIterator<FileToUpgrade> iterator = this.files.listIterator();

         while (!this.upgradeProgress.isCanceled()) {
            boolean worked = false;
            float totalProgress = 0.0F;
            if (iterator.hasNext()) {
               FileToUpgrade fileToUpgrade = iterator.next();
               boolean converted = true;

               for (ChunkPos chunkPos : fileToUpgrade.chunksToUpgrade()) {
                  converted = converted && this.processOnePosition(this.storage, chunkPos);
                  worked = true;
               }

               if (this.recreateRegionFiles) {
                  if (converted) {
                     this.onFileFinished(fileToUpgrade.file());
                  } else {
                     LOGGER.error("Failed to convert region file {}", fileToUpgrade.file().getPath());
                  }
               }
            }

            int nextIndex = iterator.nextIndex();
            float currentDimensionProgress = nextIndex / totalSize;
            float currentTotalProgress = (this.startIndex + nextIndex) / totalSize;
            this.upgradeProgress.setDimensionProgress(this.dimensionKey, currentDimensionProgress);
            totalProgress += currentTotalProgress;
            this.upgradeProgress.setTotalProgress(totalProgress);
            if (!worked) {
               break;
            }
         }

         this.upgradeProgress.setStatus(UpgradeProgress.Status.FINISHED);

         try {
            this.storage.close();
         } catch (Exception e) {
            LOGGER.error("Error upgrading chunk", e);
         }
      }
   }

   protected final SimpleRegionStorage createStorage(final RegionStorageInfo info, final Path regionFolder) {
      return this.recreateRegionFiles
         ? new RecreatingSimpleRegionStorage(
            info.withTypeSuffix("source"),
            regionFolder,
            info.withTypeSuffix("target"),
            resolveRecreateDirectory(regionFolder),
            this.dataFixer,
            true,
            this.dataFixType
         )
         : new SimpleRegionStorage(info, regionFolder, this.dataFixer, true, this.dataFixType);
   }

   private List<FileToUpgrade> getFilesToProcess(final RegionStorageInfo info, final Path regionFolder) {
      List<FileToUpgrade> filesToUpgrade = getAllChunkPositions(info, regionFolder);
      this.upgradeProgress.addTotalFileFixOperations(filesToUpgrade.size());
      this.upgradeProgress.addTotalChunks(filesToUpgrade.stream().mapToInt(fileToUpgrade -> fileToUpgrade.chunksToUpgrade().size()).sum());
      return filesToUpgrade;
   }

   public int fileAmount() {
      return this.files == null ? 0 : this.files.size();
   }

   private static List<FileToUpgrade> getAllChunkPositions(final RegionStorageInfo info, final Path regionFolder) {
      File[] files = regionFolder.toFile().listFiles((dir, name) -> name.endsWith(".mca"));
      if (files == null) {
         return List.of();
      }

      List<FileToUpgrade> regionFileChunks = Lists.newArrayList();

      for (File regionFile : files) {
         Matcher regex = REGEX.matcher(regionFile.getName());
         if (regex.matches()) {
            int xOffset = Integer.parseInt(regex.group(1)) << 5;
            int zOffset = Integer.parseInt(regex.group(2)) << 5;
            List<ChunkPos> chunkPositions = Lists.newArrayList();

            try (RegionFile regionSource = new RegionFile(info, regionFile.toPath(), regionFolder, true)) {
               for (int x = 0; x < 32; x++) {
                  for (int z = 0; z < 32; z++) {
                     ChunkPos pos = new ChunkPos(x + xOffset, z + zOffset);
                     if (regionSource.doesChunkExist(pos)) {
                        chunkPositions.add(pos);
                     }
                  }
               }

               if (!chunkPositions.isEmpty()) {
                  regionFileChunks.add(new FileToUpgrade(regionSource, chunkPositions));
               }
            } catch (Throwable t) {
               LOGGER.error("Failed to read chunks from region file {}", regionFile.toPath(), t);
            }
         }
      }

      return regionFileChunks;
   }

   private boolean processOnePosition(final SimpleRegionStorage storage, final ChunkPos pos) {
      boolean converted = false;

      try {
         converted = this.tryProcessOnePosition(storage, pos);
      } catch (ReportedException | CompletionException e) {
         Throwable cause = e.getCause();
         if (!(cause instanceof IOException)) {
            throw e;
         }

         LOGGER.error("Error upgrading chunk {}", pos, cause);
      }

      if (converted) {
         this.upgradeProgress.incrementConverted();
      } else {
         this.upgradeProgress.incrementSkipped();
      }

      return converted;
   }

   protected boolean tryProcessOnePosition(final SimpleRegionStorage storage, final ChunkPos pos) {
      CompoundTag chunkTag = storage.read(pos).join().orElse(null);
      if (chunkTag != null) {
         int version = NbtUtils.getDataVersion(chunkTag);
         int latestVersion = SharedConstants.getCurrentVersion().dataVersion().version();
         boolean changed = false;
         ObjectIterator upgradedTag = this.tagModifiers.int2ObjectEntrySet().iterator();

         while (upgradedTag.hasNext()) {
            Entry<RegionStorageUpgrader.TagModifier> tagFixer = (Entry<RegionStorageUpgrader.TagModifier>)upgradedTag.next();
            int neededVersion = tagFixer.getIntKey();
            chunkTag = this.upgradeTag(storage, chunkTag, neededVersion);
            changed |= ((RegionStorageUpgrader.TagModifier)tagFixer.getValue()).modifyTagAfterFix(pos, chunkTag);
         }

         CompoundTag upgradedTagx = this.upgradeTag(storage, chunkTag, latestVersion);
         changed |= version < latestVersion;
         if (changed || this.recreateRegionFiles) {
            if (this.previousWriteFuture != null) {
               this.previousWriteFuture.join();
            }

            this.previousWriteFuture = storage.write(pos, upgradedTagx);
            return true;
         }
      }

      return false;
   }

   protected CompoundTag upgradeTag(final SimpleRegionStorage storage, final CompoundTag chunkTag, final int targetVersion) {
      return storage.upgradeChunkTag(chunkTag, this.defaultVersion, this.dataFixContextTag, targetVersion);
   }

   private void onFileFinished(final RegionFile regionFile) {
      if (this.recreateRegionFiles) {
         if (this.previousWriteFuture != null) {
            this.previousWriteFuture.join();
         }

         Path filePath = regionFile.getPath();
         Path directoryPath = filePath.getParent();
         Path newFilePath = resolveRecreateDirectory(directoryPath).resolve(filePath.getFileName().toString());

         try {
            if (newFilePath.toFile().exists()) {
               Files.delete(filePath);
               Files.move(newFilePath, filePath);
            } else {
               LOGGER.error("Failed to replace an old region file. New file {} does not exist.", newFilePath);
            }
         } catch (IOException e) {
            LOGGER.error("Failed to replace an old region file", e);
         }
      }
   }

   protected static Path resolveRecreateDirectory(final Path directoryPath) {
      return directoryPath.resolveSibling("new_" + directoryPath.getFileName().toString());
   }

   public static class Builder {
      private final DataFixer dataFixer;
      private @Nullable DataFixTypes dataFixType;
      private @Nullable String type;
      private @Nullable String folderName;
      private int defaultVersion = -1;
      private boolean recreateRegionFiles;
      private UpgradeProgress upgradeProgress = new UpgradeProgress.Noop();
      private @Nullable CompoundTag dataFixContextTag;
      private Int2ObjectAVLTreeMap<RegionStorageUpgrader.TagModifier> tagModifiers = new Int2ObjectAVLTreeMap();

      public Builder(final DataFixer dataFixer) {
         this.dataFixer = dataFixer;
      }

      public RegionStorageUpgrader.Builder setDataFixType(final DataFixTypes dataFixType) {
         this.dataFixType = dataFixType;
         return this;
      }

      public RegionStorageUpgrader.Builder setTypeAndFolderName(final String name) {
         return this.setType(name).setFolderName(name);
      }

      public RegionStorageUpgrader.Builder setType(final String type) {
         this.type = type;
         return this;
      }

      public RegionStorageUpgrader.Builder setFolderName(final String folderName) {
         this.folderName = folderName;
         return this;
      }

      public RegionStorageUpgrader.Builder setDefaultVersion(final int defaultVersion) {
         this.defaultVersion = defaultVersion;
         return this;
      }

      public RegionStorageUpgrader.Builder setRecreateRegionFiles(final boolean recreateRegionFiles) {
         this.recreateRegionFiles = recreateRegionFiles;
         return this;
      }

      public RegionStorageUpgrader.Builder trackProgress(final UpgradeProgress upgradeProgress) {
         this.upgradeProgress = upgradeProgress;
         return this;
      }

      public RegionStorageUpgrader.Builder setDataFixContextTag(final @Nullable CompoundTag dataFixContextTag) {
         this.dataFixContextTag = dataFixContextTag;
         return this;
      }

      public RegionStorageUpgrader.Builder addTagModifier(final int version, final RegionStorageUpgrader.TagModifier tagModifier) {
         if (this.tagModifiers.containsKey(version)) {
            throw new IllegalStateException("Can't add two fixers for the same data version");
         }

         this.tagModifiers.put(version, tagModifier);
         return this;
      }

      private RegionStorageUpgrader.Builder setTagModifiers(final Int2ObjectAVLTreeMap<RegionStorageUpgrader.TagModifier> tagModifiers) {
         this.tagModifiers = tagModifiers;
         return this;
      }

      public RegionStorageUpgrader.Builder copy() {
         return new RegionStorageUpgrader.Builder(this.dataFixer)
            .setDataFixType(this.dataFixType)
            .setType(this.type)
            .setFolderName(this.folderName)
            .setDefaultVersion(this.defaultVersion)
            .setRecreateRegionFiles(this.recreateRegionFiles)
            .trackProgress(this.upgradeProgress)
            .setDataFixContextTag(this.dataFixContextTag)
            .setTagModifiers(this.tagModifiers.clone());
      }

      public RegionStorageUpgrader build(final int previousCopiesFileAmounts) {
         return new RegionStorageUpgrader(
            this.dataFixer,
            Objects.requireNonNull(this.dataFixType),
            Objects.requireNonNull(this.type),
            Objects.requireNonNull(this.folderName),
            this.defaultVersion,
            this.recreateRegionFiles,
            this.upgradeProgress,
            previousCopiesFileAmounts,
            this.dataFixContextTag,
            this.tagModifiers
         );
      }
   }

   @FunctionalInterface
   public interface TagModifier {
      boolean modifyTagAfterFix(final ChunkPos pos, final CompoundTag upgradedTag);
   }
}
