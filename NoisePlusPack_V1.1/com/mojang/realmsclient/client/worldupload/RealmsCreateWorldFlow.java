package com.mojang.realmsclient.client.worldupload;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSetting;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.util.task.RealmCreationTask;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionException;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.RegistryLayer;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsCreateWorldFlow {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static void createWorld(
      Minecraft p_367436_, Screen p_363355_, Screen p_369120_, int p_369852_, RealmsServer p_360755_, @Nullable RealmCreationTask p_370180_
   ) {
      CreateWorldScreen.openFresh(
         p_367436_,
         () -> p_367436_.setScreen(p_363355_),
         (p_420595_, p_420596_, p_420597_, p_420598_) -> {
            Path path;
            try {
               path = createTemporaryWorldFolder(p_420596_, p_420597_, p_420598_);
            } catch (IOException ioexception) {
               LOGGER.warn("Failed to create temporary world folder.");
               return true;
            }

            RealmsWorldOptions realmsworldoptions = RealmsWorldOptions.createFromSettings(
               p_420597_.getLevelSettings(), SharedConstants.getCurrentVersion().name()
            );
            RealmsSlot realmsslot = new RealmsSlot(
               p_369852_, realmsworldoptions, List.of(RealmsSetting.hardcoreSetting(p_420597_.getLevelSettings().hardcore()))
            );
            RealmsWorldUpload realmsworldupload = new RealmsWorldUpload(
               path, realmsslot, p_367436_.getUser(), p_360755_.id, RealmsWorldUploadStatusTracker.noOp()
            );
            p_367436_.setScreenAndShow(
               new AlertScreen(
                  realmsworldupload::cancel, Component.translatable("mco.create.world.reset.title"), Component.empty(), CommonComponents.GUI_CANCEL, false
               )
            );
            if (p_370180_ != null) {
               p_370180_.run();
            }

            realmsworldupload.packAndUpload().handleAsync((p_366683_, p_363012_) -> {
               if (p_363012_ != null) {
                  if (p_363012_ instanceof CompletionException completionexception) {
                     p_363012_ = completionexception.getCause();
                  }

                  if (p_363012_ instanceof RealmsUploadCanceledException) {
                     p_367436_.setScreenAndShow(p_369120_);
                  } else if (p_363012_ instanceof RealmsUploadFailedException realmsuploadfailedexception) {
                     LOGGER.warn("Failed to create realms world {}", realmsuploadfailedexception.getStatusMessage());
                  } else {
                     LOGGER.warn("Failed to create realms world {}", p_363012_.getMessage());
                  }
               } else {
                  if (p_363355_ instanceof RealmsConfigureWorldScreen realmsconfigureworldscreen) {
                     realmsconfigureworldscreen.fetchServerData(p_360755_.id);
                  }

                  if (p_370180_ != null) {
                     RealmsMainScreen.play(p_360755_, p_363355_, true);
                  } else {
                     p_367436_.setScreenAndShow(p_363355_);
                  }

                  RealmsMainScreen.refreshServerList();
               }

               return null;
            }, p_367436_);
            return true;
         }
      );
   }

   private static Path createTemporaryWorldFolder(LayeredRegistryAccess<RegistryLayer> p_363722_, PrimaryLevelData p_362242_, @Nullable Path p_362088_) throws IOException {
      Path path = Files.createTempDirectory("minecraft_realms_world_upload");
      if (p_362088_ != null) {
         Files.move(p_362088_, path.resolve("datapacks"));
      }

      CompoundTag compoundtag = p_362242_.createTag(p_363722_.compositeAccess(), null);
      CompoundTag compoundtag1 = new CompoundTag();
      compoundtag1.put("Data", compoundtag);
      Path path1 = Files.createFile(path.resolve("level.dat"));
      NbtIo.writeCompressed(compoundtag1, path1);
      return path;
   }
}
