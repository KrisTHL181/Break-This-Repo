package net.minecraft.world.level.storage;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public class PlayerDataStorage {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final File playerDir;
   protected final DataFixer fixerUpper;

   public PlayerDataStorage(final LevelStorageSource.LevelStorageAccess levelAccess, final DataFixer fixerUpper) {
      this.fixerUpper = fixerUpper;
      this.playerDir = levelAccess.getLevelPath(LevelResource.PLAYER_DATA_DIR).toFile();
      this.playerDir.mkdirs();
   }

   public void save(final Player player) {
      try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(player.problemPath(), LOGGER)) {
         TagValueOutput output = TagValueOutput.createWithContext(reporter, player.registryAccess());
         player.saveWithoutId(output);
         Path playerDirPath = this.playerDir.toPath();
         Path tmpFile = Files.createTempFile(playerDirPath, player.getStringUUID() + "-", ".dat");
         CompoundTag dataToStore = output.buildResult();
         NbtIo.writeCompressed(dataToStore, tmpFile);
         Path realFile = playerDirPath.resolve(player.getStringUUID() + ".dat");
         Path oldFile = playerDirPath.resolve(player.getStringUUID() + ".dat_old");
         Util.safeReplaceFile(realFile, tmpFile, oldFile);
      } catch (Exception ignored) {
         LOGGER.warn("Failed to save player data for {}", player.getPlainTextName());
      }
   }

   private void backup(final NameAndId nameAndId, final String suffix) {
      Path playerDirPath = this.playerDir.toPath();
      String idString = nameAndId.id().toString();
      Path realPath = playerDirPath.resolve(idString + suffix);
      Path backupPath = playerDirPath.resolve(idString + "_corrupted_" + ZonedDateTime.now().format(FileNameDateFormatter.FORMATTER) + suffix);
      if (Files.isRegularFile(realPath)) {
         try {
            Files.copy(realPath, backupPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
         } catch (Exception e) {
            LOGGER.warn("Failed to copy the player.dat file for {}", nameAndId.name(), e);
         }
      }
   }

   private Optional<CompoundTag> load(final NameAndId nameAndId, final String suffix) {
      File realFile = new File(this.playerDir, nameAndId.id() + suffix);
      if (realFile.exists() && realFile.isFile()) {
         try {
            return Optional.of(NbtIo.readCompressed(realFile.toPath(), NbtAccounter.unlimitedHeap()));
         } catch (Exception ignored) {
            LOGGER.warn("Failed to load player data for {}", nameAndId.name());
         }
      }

      return Optional.empty();
   }

   public Optional<CompoundTag> load(final NameAndId nameAndId) {
      Optional<CompoundTag> optTag = this.load(nameAndId, ".dat");
      if (optTag.isEmpty()) {
         this.backup(nameAndId, ".dat");
      }

      return optTag.or(() -> this.load(nameAndId, ".dat_old")).map(tag -> {
         int version = NbtUtils.getDataVersion(tag);
         return DataFixTypes.PLAYER.updateToCurrentVersion(this.fixerUpper, tag, version);
      });
   }
}
