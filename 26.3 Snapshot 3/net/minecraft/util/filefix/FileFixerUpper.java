package net.minecraft.util.filefix;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.DataFixerBuilder.Result;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BiFunction;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.FileUtil;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.filefix.virtualfilesystem.CopyOnWriteFileSystem;
import net.minecraft.util.filefix.virtualfilesystem.FileMove;
import net.minecraft.util.worldupdate.UpgradeProgress;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class FileFixerUpper {
   private static final int FILE_FIXER_INTRODUCTION_VERSION = 4772;
   private static final Gson GSON = new Gson().newBuilder().setPrettyPrinting().create();
   private static final String FILE_FIX_DIRECTORY_NAME = "filefix";
   private static final String NEW_WORLD_TEMP_NAME = "new_world";
   private static final String UPGRADE_IN_PROGRESS_NAME = "upgrade_in_progress.json";
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Result dataFixer;
   private final List<FileFix> fileFixes;
   private final int latestFileFixerVersion;

   public FileFixerUpper(final Result dataFixer, final List<FileFix> fileFixes, final int latestFileFixerVersion) {
      this.dataFixer = dataFixer;
      this.fileFixes = List.copyOf(fileFixes);
      this.latestFileFixerVersion = latestFileFixerVersion;
   }

   public static int worldVersionToFileFixerVersion(final int levelDataVersion) {
      return levelDataVersion < 4772 ? 0 : levelDataVersion;
   }

   public boolean requiresFileFixing(final int levelDataVersion) {
      return worldVersionToFileFixerVersion(levelDataVersion) < this.latestFileFixerVersion;
   }

   public Dynamic<?> fix(final LevelStorageSource.LevelStorageAccess worldAccess, final Dynamic<?> levelDataTag, final UpgradeProgress upgradeProgress) throws FileFixException {
      return this.fix(worldAccess, levelDataTag, upgradeProgress, SharedConstants.getCurrentVersion().dataVersion().version());
   }

   @VisibleForTesting
   public Dynamic<?> fix(
      final LevelStorageSource.LevelStorageAccess worldAccess, Dynamic<?> levelDataTag, final UpgradeProgress upgradeProgress, final int toVersion
   ) throws FileFixException {
      int loadedVersion = NbtUtils.getDataVersion(levelDataTag);
      if (this.requiresFileFixing(loadedVersion)) {
         LOGGER.info("Starting upgrade for world \"{}\"", worldAccess.getLevelId());
         upgradeProgress.notifications().worldUpgradeStarted();
         Path worldFolder = worldAccess.getLevelDirectory().path();
         Path fileFixDirectory = worldFolder.resolve("filefix");
         Path tempWorld = fileFixDirectory.resolve("new_world");

         List<FileMove> moves;
         try {
            moves = this.startOrContinueFileFixing(upgradeProgress, toVersion, worldFolder, tempWorld, fileFixDirectory, loadedVersion);
         } catch (IOException e) {
            upgradeProgress.notifications().worldUpgradeFailed("The world upgrade failed during file fixing");
            throw new AbortedFileFixException(e);
         }

         try {
            swapInFixedWorld(worldAccess, moves, fileFixDirectory, tempWorld, upgradeProgress);
         } catch (AbortedFileFixException e) {
            if (e.notRevertedMoves().isEmpty()) {
               cleanup(fileFixDirectory);
               upgradeProgress.notifications().worldUpgradeFailed("The world upgrade failed while swapping in the file-fixed world");
            } else {
               upgradeProgress.notifications()
                  .worldUpgradeFailed("The world upgrade failed while swapping in the file-fixed world and some moves were not reverted");
            }

            throw e;
         }

         try {
            levelDataTag = worldAccess.getUnfixedDataTag(false);
         } catch (IOException e) {
            upgradeProgress.notifications().worldUpgradeFailed("The world upgrade failed while reading level.dat");
            throw new UncheckedIOException(e);
         }

         loadedVersion = NbtUtils.getDataVersion(levelDataTag);
      }

      Dynamic<?> fixedLevelDataTag = DataFixTypes.LEVEL.updateToCurrentVersion(this.dataFixer.fixer(), levelDataTag, loadedVersion);
      return addVersionsToLevelData(fixedLevelDataTag, toVersion);
   }

   private List<FileMove> startOrContinueFileFixing(
      final UpgradeProgress upgradeProgress,
      final int toVersion,
      final Path worldFolder,
      final Path tempWorld,
      final Path fileFixDirectory,
      final int loadedVersion
   ) throws IOException {
      Path upgradeInProgressFile = fileFixDirectory.resolve("upgrade_in_progress.json");
      List<FileMove> moves;
      if (Files.exists(upgradeInProgressFile)) {
         LOGGER.warn("Found previously interrupted world upgrade, attempting to continue it");
         moves = readMoves(worldFolder, tempWorld, upgradeInProgressFile);
      } else {
         if (Files.exists(fileFixDirectory)) {
            deleteDirectory(fileFixDirectory);
         }

         try {
            Files.createDirectory(fileFixDirectory);
            moves = this.applyFileFixersOnCow(upgradeProgress, loadedVersion, toVersion, worldFolder, fileFixDirectory, tempWorld);
         } catch (Exception e) {
            cleanup(fileFixDirectory);
            throw e;
         }
      }

      return moves;
   }

   private static void deleteDirectory(final Path directory) throws IOException {
      Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
         public FileVisitResult visitFile(final Path realPath, final BasicFileAttributes attrs) {
            try {
               Files.deleteIfExists(realPath);
            } catch (IOException var4) {
            }

            return FileVisitResult.CONTINUE;
         }

         public FileVisitResult postVisitDirectory(final Path realPath, final @Nullable IOException e) {
            try {
               Files.deleteIfExists(realPath);
            } catch (IOException var4) {
            }

            return FileVisitResult.CONTINUE;
         }
      });
      if (Files.exists(directory)) {
         PathUtils.deleteDirectory(directory);
      }
   }

   private static void cleanup(final Path fileFixDirectory) {
      try {
         deleteDirectory(fileFixDirectory);
      } catch (Exception ex) {
         LOGGER.error("Failed to clean up", ex);
      }
   }

   private List<FileMove> applyFileFixersOnCow(
      final UpgradeProgress upgradeProgress,
      final int loadedVersion,
      final int toVersion,
      final Path worldFolder,
      final Path fileFixDirectory,
      final Path tempWorld
   ) throws IOException {
      try (CopyOnWriteFileSystem fs = CopyOnWriteFileSystem.create(
            worldFolder.getFileName().toString(), worldFolder, fileFixDirectory.resolve("cow"), fileFixDirectory::equals
         )) {
         this.applyFileFixers(upgradeProgress, loadedVersion, toVersion, fs.rootPath());
         CopyOnWriteFileSystem.Moves moves = fs.collectMoveOperations(tempWorld);
         Files.createDirectory(tempWorld);
         CopyOnWriteFileSystem.createDirectories(moves.directories());
         CopyOnWriteFileSystem.moveFiles(moves.copiedFiles());
         return moves.preexistingFiles();
      }
   }

   @VisibleForTesting
   public void applyFileFixers(final UpgradeProgress upgradeProgress, final int loadedVersion, final int toVersion, final Path basePath) throws IOException {
      List<FileFix> applicableFixers = this.getApplicableFixers(loadedVersion, toVersion);
      upgradeProgress.setType(UpgradeProgress.Type.FILES);
      this.countFiles(applicableFixers, upgradeProgress);
      upgradeProgress.setStatus(UpgradeProgress.Status.UPGRADING);
      upgradeProgress.setApplicableFixerAmount(applicableFixers.size());

      for (FileFix fileFix : applicableFixers) {
         upgradeProgress.incrementRunningFileFixer();
         fileFix.runFixOperations(basePath, upgradeProgress);
      }

      this.writeUpdatedLevelData(basePath, toVersion);
      Files.deleteIfExists(basePath.resolve("level.dat_old"));
      Files.deleteIfExists(basePath.resolve("session.lock"));
   }

   private List<FileFix> getApplicableFixers(final int fromVersion, final int toVersion) {
      int fileFixerFromVersion = worldVersionToFileFixerVersion(fromVersion);
      return this.fileFixes.stream().filter(fileFix -> fileFix.getVersion() > fileFixerFromVersion && fileFix.getVersion() <= toVersion).toList();
   }

   private void countFiles(final List<FileFix> applicableFixers, final UpgradeProgress upgradeProgress) {
      upgradeProgress.setStatus(UpgradeProgress.Status.COUNTING);
      int totalFiles = 0;

      for (FileFix fileFix : applicableFixers) {
         totalFiles += fileFix.countFileOperations();
      }

      upgradeProgress.addTotalFileFixOperations(totalFiles);
   }

   private void writeUpdatedLevelData(final Path worldFolder, final int toVersion) throws IOException {
      Path levelDatPath = worldFolder.resolve(LevelResource.LEVEL_DATA_FILE.id());
      CompoundTag unfixedLevelDat = NbtIo.readCompressed(levelDatPath, NbtAccounter.defaultQuota());
      CompoundTag unfixedDataTag = unfixedLevelDat.getCompoundOrEmpty("Data");
      int dataVersion = NbtUtils.getDataVersion(unfixedDataTag);
      Dynamic<?> fixed = DataFixTypes.LEVEL.update(this.dataFixer.fixer(), new Dynamic(NbtOps.INSTANCE, unfixedDataTag), dataVersion, toVersion);
      fixed = addVersionsToLevelData(fixed, toVersion);
      Dynamic<?> dynamic = fixed.emptyMap().set("Data", fixed);
      NbtIo.writeCompressed((CompoundTag)dynamic.convert(NbtOps.INSTANCE).getValue(), levelDatPath);
   }

   private static Dynamic<?> addVersionsToLevelData(Dynamic<?> fixed, final int toVersion) {
      fixed = NbtUtils.addDataVersion(fixed, toVersion);
      fixed = PrimaryLevelData.writeLastPlayed(fixed);
      return PrimaryLevelData.writeVersionTag(fixed);
   }

   @VisibleForTesting
   protected static void swapInFixedWorld(
      final LevelStorageSource.LevelStorageAccess worldAccess,
      final List<FileMove> moves,
      final Path fileFixDirectory,
      final Path tempWorld,
      final UpgradeProgress upgradeProgress
   ) throws FileFixException {
      Path worldFolder = worldAccess.getLevelDirectory().path();
      Path savesDirectory = worldFolder.getParent();
      String worldName = worldFolder.getFileName().toString();

      Path tempWorldTopLevel;
      Path oldWorldFolder;
      FileSystemCapabilities fileSystemCapabilities;
      try {
         fileSystemCapabilities = detectFileSystemCapabilities(fileFixDirectory);
         tempWorldTopLevel = savesDirectory.resolve(FileUtil.findAvailableName(savesDirectory, worldName + " upgraded", ""));
         oldWorldFolder = savesDirectory.resolve(FileUtil.findAvailableName(savesDirectory, worldName + " OUTDATED", ""));
      } catch (Exception e) {
         throw new AbortedFileFixException(e);
      }

      if (!fileSystemCapabilities.atomicMove()) {
         throw new AtomicMoveNotSupportedFileFixException(fileSystemCapabilities);
      }

      CopyOption[] moveOptions = fileSystemCapabilities.getMoveOptions();
      LOGGER.info("File system capabilities: {}", fileSystemCapabilities);
      Path movesFile = fileFixDirectory.resolve("upgrade_in_progress.json");

      try {
         if (fileSystemCapabilities.hardLinks()) {
            CopyOnWriteFileSystem.hardLinkFiles(moves);
         } else {
            writeMoves(moves, worldFolder, tempWorld, movesFile);
         }
      } catch (Exception e) {
         throw new AbortedFileFixException(e, List.of(), fileSystemCapabilities);
      }

      LOGGER.info("Applying file structure changes for world \"{}\"", worldAccess.getLevelId());
      if (fileSystemCapabilities.hardLinks()) {
         try {
            LOGGER.info("Moving new hardlinked world to top level");
            Files.move(tempWorld, tempWorldTopLevel, moveOptions);
         } catch (Exception e) {
            LOGGER.error("Encountered error trying to move world folder:", e);
            throw new AbortedFileFixException(e, List.of(), fileSystemCapabilities);
         }
      } else {
         try {
            LOGGER.info("Moving files into new file structure");
            CopyOnWriteFileSystem.moveFilesWithRetry(moves, moveOptions);
            LOGGER.info("Moving new world to top level");
            Files.move(tempWorld, tempWorldTopLevel, moveOptions);
         } catch (Exception e) {
            LOGGER.error("Encountered error while trying to create new world folder:", e);
            List<FileMove> failedMoves = CopyOnWriteFileSystem.tryRevertMoves(moves, moveOptions);
            if (failedMoves.isEmpty()) {
               try {
                  Files.deleteIfExists(movesFile);
               } catch (IOException e2) {
                  LOGGER.warn("Failed to delete {}", movesFile, e);
               }
            }

            throw new AbortedFileFixException(e, failedMoves, fileSystemCapabilities);
         }

         LOGGER.info("Complete move");

         try {
            Files.deleteIfExists(movesFile);
         } catch (IOException e) {
            LOGGER.warn("Failed to delete {}", movesFile, e);
         }
      }

      LOGGER.info("Start cleanup");

      try {
         Files.deleteIfExists(worldFolder.resolve("level.dat"));
         Files.deleteIfExists(worldFolder.resolve("level.dat_old"));
      } catch (Exception e) {
         LOGGER.warn("Failed to delete outdated level.dat files: ", e);
      }

      MutableBoolean succeeded = new MutableBoolean();

      try {
         worldAccess.releaseTemporarilyAndRun(() -> {
            LOGGER.info("Moving out old world folder");

            try {
               Files.move(worldFolder, oldWorldFolder, moveOptions);
            } catch (Exception e) {
               LOGGER.warn("Failed to move outdated world folder out of the way; will try to delete instead: ", ex);

               try {
                  deleteDirectory(worldFolder);
               } catch (Exception e2) {
                  LOGGER.warn("Failed to delete outdated world folder: ", ex);
                  throw new FailedCleanupFileFixException(ex, tempWorldTopLevel.getFileName().toString(), fileSystemCapabilities);
               }
            }

            LOGGER.info("Moving in new world folder");

            try {
               Files.move(tempWorldTopLevel, worldFolder, moveOptions);
            } catch (Exception e) {
               LOGGER.warn("Failed to move in new world folder: ", exx);
               throw new FailedCleanupFileFixException(exx, tempWorldTopLevel.getFileName().toString(), fileSystemCapabilities);
            }

            succeeded.setTrue();
         });
      } catch (IOException e) {
         Path newWorldFolder = succeeded.isTrue() ? worldFolder : tempWorldTopLevel;
         throw new FailedCleanupFileFixException(e, newWorldFolder.getFileName().toString(), fileSystemCapabilities);
      }

      LOGGER.info("Done applying file structure changes for world \"{}\". Cleaning up outdated data...", worldAccess.getLevelId());

      try {
         if (Files.exists(oldWorldFolder)) {
            deleteDirectory(oldWorldFolder);
         }
      } catch (Exception e) {
         LOGGER.warn("Failed to clean up old world folder", e);
      }

      LOGGER.info("Upgrade done for world \"{}\"", worldAccess.getLevelId());
      upgradeProgress.notifications().worldUpgradeFinished();
   }

   private static void writeMoves(final List<FileMove> moves, final Path fromDirectory, final Path toDirectory, final Path filePath) throws IOException {
      Codec<FileFixerUpper.UpgradeInProgress> codec = FileFixerUpper.UpgradeInProgress.codec(fromDirectory, toDirectory);
      JsonElement json = (JsonElement)codec.encodeStart(JsonOps.INSTANCE, new FileFixerUpper.UpgradeInProgress(moves)).getOrThrow();
      Files.writeString(filePath, GSON.toJson(json), StandardOpenOption.DSYNC, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
   }

   private static List<FileMove> readMoves(final Path fromDirectory, final Path toDirectory, final Path filePath) throws IOException {
      JsonObject json = (JsonObject)GSON.fromJson(Files.readString(filePath), JsonObject.class);
      Codec<FileFixerUpper.UpgradeInProgress> codec = FileFixerUpper.UpgradeInProgress.codec(fromDirectory, toDirectory);
      return ((FileFixerUpper.UpgradeInProgress)((Pair)codec.decode(JsonOps.INSTANCE, json).getOrThrow()).getFirst()).moves;
   }

   public static FileSystemCapabilities detectFileSystemCapabilities(final Path dir) throws IOException {
      return new FileSystemCapabilities(supportsAtomicMove(dir), supportsHardLinks(dir));
   }

   private static boolean supportsAtomicMove(final Path dir) throws IOException {
      Path sourceFile = dir.resolve(UUID.randomUUID().toString());
      Path targetFile = dir.resolve(UUID.randomUUID().toString());

      try {
         Files.createFile(sourceFile);

         try {
            Files.move(sourceFile, targetFile, StandardCopyOption.ATOMIC_MOVE);
            return true;
         } catch (AtomicMoveNotSupportedException var8) {
            return false;
         }
      } finally {
         Files.deleteIfExists(sourceFile);
         Files.deleteIfExists(targetFile);
      }
   }

   private static boolean supportsHardLinks(final Path dir) throws IOException {
      Path sourceFile = dir.resolve(UUID.randomUUID().toString());
      Path targetFile = dir.resolve(UUID.randomUUID().toString());

      try {
         Files.createFile(sourceFile);

         try {
            Files.createLink(targetFile, sourceFile);
            return true;
         } catch (Exception var8) {
            return false;
         }
      } finally {
         Files.deleteIfExists(sourceFile);
         Files.deleteIfExists(targetFile);
      }
   }

   public static class Builder {
      public final List<FileFix> fileFixes = new ArrayList<>();
      private final int currentVersion;
      private int latestFileFixerVersion;
      private final List<Schema> knownSchemas = new ArrayList<>();

      public Builder(final int currentVersion) {
         this.currentVersion = currentVersion;
      }

      public void addFixer(final FileFix fileFix) {
         if (!this.knownSchemas.contains(fileFix.getSchema())) {
            throw new IllegalArgumentException("Tried to add file fixer with unknown schema. Add it through FileFixerUpper#addSchema instead");
         }

         int fileFixVersion = fileFix.getVersion();
         if (fileFix.getVersion() > this.currentVersion) {
            throw new IllegalArgumentException(
               String.format(
                  Locale.ROOT, "Tried to add too recent file fix for version: %s. The data version of the game is: %s", fileFixVersion, this.currentVersion
               )
            );
         }

         if (!this.fileFixes.isEmpty()) {
            FileFix last = this.fileFixes.getLast();
            if (last.getVersion() > fileFixVersion) {
               throw new IllegalArgumentException(
                  String.format(
                     Locale.ROOT, "Tried to add too recent file fix for version: %s. The most recent file fix version is %s", fileFixVersion, last.getVersion()
                  )
               );
            }
         }

         this.fileFixes.add(fileFix);
      }

      public Schema addSchema(final DataFixerBuilder fixerUpper, final int version, final BiFunction<Integer, Schema, Schema> factory) {
         this.latestFileFixerVersion = Math.max(version, this.latestFileFixerVersion);
         Schema schema = fixerUpper.addSchema(version, factory);
         this.knownSchemas.add(schema);
         return schema;
      }

      public FileFixerUpper build(final Result dataFixer) {
         return new FileFixerUpper(dataFixer, this.fileFixes, this.latestFileFixerVersion);
      }
   }

   public record UpgradeInProgress(List<FileMove> moves) {
      public static Codec<FileFixerUpper.UpgradeInProgress> codec(final Path fromDirectory, final Path toDirectory) {
         return RecordCodecBuilder.create(
            i -> i.group(FileMove.moveCodec(fromDirectory, toDirectory).listOf().fieldOf("moves").forGetter(FileFixerUpper.UpgradeInProgress::moves))
               .apply(i, FileFixerUpper.UpgradeInProgress::new)
         );
      }
   }
}
