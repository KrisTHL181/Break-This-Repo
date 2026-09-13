package com.mojang.realmsclient.client.worldupload;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSetting;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.util.task.RealmCreationTask;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class RealmsCreateWorldFlow {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static void createWorld(
      final Minecraft minecraft,
      final Screen returnScreen,
      final Screen lastScreen,
      final int slot,
      final RealmsServer realmsServer,
      final @Nullable RealmCreationTask realmCreationTask
   ) {
      CreateWorldScreen.openFresh(
         minecraft,
         () -> minecraft.gui.setScreen(returnScreen),
         (createWorldScreen, finalLayers, worldDataAndGenSettings, gameRules, tempDataPackDir) -> {
            Path worldFolder;
            try {
               worldFolder = createTemporaryWorldFolder(finalLayers.compositeAccess(), worldDataAndGenSettings, gameRules, tempDataPackDir);
            } catch (IOException e) {
               LOGGER.warn("Failed to create temporary world folder", e);
               minecraft.gui.setScreen(new RealmsGenericErrorScreen(Component.translatable("mco.create.world.failed"), lastScreen));
               return true;
            }

            RealmsWorldOptions realmsWorldOptions = RealmsWorldOptions.createFromSettings(
               worldDataAndGenSettings.data().getLevelSettings(), SharedConstants.getCurrentVersion().name()
            );
            RealmsSlot realmsSlot = new RealmsSlot(
               slot, realmsWorldOptions, List.of(RealmsSetting.hardcoreSetting(worldDataAndGenSettings.data().isHardcore()))
            );
            RealmsWorldUpload realmsWorldUpload = new RealmsWorldUpload(
               worldFolder, realmsSlot, minecraft.getUser(), realmsServer.id, RealmsWorldUploadStatusTracker.noOp()
            );
            minecraft.setScreenAndShow(
               new AlertScreen(
                  realmsWorldUpload::cancel, Component.translatable("mco.create.world.reset.title"), Component.empty(), CommonComponents.GUI_CANCEL, false
               )
            );
            if (realmCreationTask != null) {
               realmCreationTask.run();
            }

            realmsWorldUpload.packAndUpload().handleAsync((result, exception) -> {
               if (exception != null) {
                  if (exception instanceof CompletionException e) {
                     exception = e.getCause();
                  }

                  if (exception instanceof RealmsUploadCanceledException) {
                     minecraft.setScreenAndShow(lastScreen);
                  } else {
                     if (exception instanceof RealmsUploadFailedException realmsUploadFailedException) {
                        LOGGER.warn("Failed to create realms world {}", realmsUploadFailedException.getStatusMessage());
                     } else {
                        LOGGER.warn("Failed to create realms world", exception);
                     }

                     minecraft.setScreenAndShow(new RealmsGenericErrorScreen(Component.translatable("mco.create.world.failed"), lastScreen));
                  }
               } else {
                  if (returnScreen instanceof RealmsConfigureWorldScreen configureWorldScreen) {
                     configureWorldScreen.fetchServerData(realmsServer.id);
                  }

                  if (realmCreationTask != null) {
                     RealmsMainScreen.play(realmsServer, returnScreen, true);
                  } else {
                     minecraft.setScreenAndShow(returnScreen);
                  }

                  RealmsMainScreen.refreshServerList();
               }

               return null;
            }, minecraft);
            return true;
         }
      );
   }

   private static Path createTemporaryWorldFolder(
      final RegistryAccess registryAccess,
      final LevelDataAndDimensions.WorldDataAndGenSettings worldDataAndGenSettings,
      final Optional<GameRules> gameRulesOpt,
      final @Nullable Path tempDataPackDir
   ) throws IOException {
      Path worldFolder = Files.createTempDirectory("minecraft_realms_world_upload");
      if (tempDataPackDir != null) {
         Files.move(tempDataPackDir, worldFolder.resolve("datapacks"));
      }

      WorldData worldData = worldDataAndGenSettings.data();
      CompoundTag dataTag = worldData.createTag(null);
      CompoundTag root = new CompoundTag();
      root.put("Data", dataTag);
      Path levelDat = Files.createFile(worldFolder.resolve("level.dat"));
      NbtIo.writeCompressed(root, levelDat);
      LevelStorageSource.writeWorldGenSettings(registryAccess, worldFolder, worldDataAndGenSettings.genSettings());
      if (gameRulesOpt.isPresent()) {
         LevelStorageSource.writeGameRules(worldData, worldFolder, gameRulesOpt.get());
      }

      return worldFolder;
   }
}
