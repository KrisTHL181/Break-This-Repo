package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsJoinInformation;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.RealmsBrokenWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoConnectTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopups;
import com.mojang.realmsclient.gui.screens.RealmsTermsScreen;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GetServerDetailsTask extends LongRunningTask {
   private static final Component APPLYING_PACK_TEXT = Component.translatable("multiplayer.applyingPack");
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.connect.connecting");
   private final RealmsServer server;
   private final Screen lastScreen;

   public GetServerDetailsTask(Screen p_90333_, RealmsServer p_90334_) {
      this.lastScreen = p_90333_;
      this.server = p_90334_;
   }

   @Override
   public void run() {
      RealmsJoinInformation realmsjoininformation;
      try {
         realmsjoininformation = this.fetchServerAddress();
      } catch (CancellationException cancellationexception) {
         LOGGER.info("User aborted connecting to realms");
         return;
      } catch (RealmsServiceException realmsserviceexception) {
         switch (realmsserviceexception.realmsError.errorCode()) {
            case 6002:
               setScreen(new RealmsTermsScreen(this.lastScreen, this.server));
               return;
            case 6006:
               boolean flag1 = Minecraft.getInstance().isLocalPlayer(this.server.ownerUUID);
               setScreen(new RealmsBrokenWorldScreen(this.lastScreen, this.server.id, this.server.isMinigameActive()));
               return;
            default:
               this.error(realmsserviceexception);
               LOGGER.error("Couldn't connect to world", realmsserviceexception);
               return;
         }
      } catch (TimeoutException timeoutexception) {
         this.error(Component.translatable("mco.errorMessage.connectionFailure"));
         return;
      } catch (Exception exception) {
         LOGGER.error("Couldn't connect to world", exception);
         this.error(exception);
         return;
      }

      if (realmsjoininformation.address() == null) {
         this.error(Component.translatable("mco.errorMessage.connectionFailure"));
      } else {
         boolean flag = realmsjoininformation.resourcePackUrl() != null && realmsjoininformation.resourcePackHash() != null;
         Screen screen = flag
            ? this.resourcePackDownloadConfirmationScreen(realmsjoininformation, generatePackId(this.server), this::connectScreen)
            : this.connectScreen(realmsjoininformation);
         setScreen(screen);
      }
   }

   private static UUID generatePackId(RealmsServer p_311215_) {
      return p_311215_.minigameName != null
         ? UUID.nameUUIDFromBytes(("minigame:" + p_311215_.minigameName).getBytes(StandardCharsets.UTF_8))
         : UUID.nameUUIDFromBytes(("realms:" + Objects.requireNonNullElse(p_311215_.name, "") + ":" + p_311215_.activeSlot).getBytes(StandardCharsets.UTF_8));
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }

   private RealmsJoinInformation fetchServerAddress() throws RealmsServiceException, TimeoutException, CancellationException {
      RealmsClient realmsclient = RealmsClient.getOrCreate();

      for (int i = 0; i < 40; i++) {
         if (this.aborted()) {
            throw new CancellationException();
         }

         try {
            return realmsclient.join(this.server.id);
         } catch (RetryCallException retrycallexception) {
            pause(retrycallexception.delaySeconds);
         }
      }

      throw new TimeoutException();
   }

   public RealmsLongRunningMcoTaskScreen connectScreen(RealmsJoinInformation p_409236_) {
      return new RealmsLongRunningMcoConnectTaskScreen(this.lastScreen, p_409236_, new ConnectTask(this.lastScreen, this.server, p_409236_));
   }

   private PopupScreen resourcePackDownloadConfirmationScreen(
      RealmsJoinInformation p_409898_, UUID p_309510_, Function<RealmsJoinInformation, Screen> p_167641_
   ) {
      Component component = Component.translatable("mco.configure.world.resourcepack.question");
      return RealmsPopups.infoPopupScreen(this.lastScreen, component, p_404766_ -> {
         setScreen(new GenericMessageScreen(APPLYING_PACK_TEXT));
         this.scheduleResourcePackDownload(p_409898_, p_309510_).thenRun(() -> setScreen(p_167641_.apply(p_409898_))).exceptionally(p_287306_ -> {
            Minecraft.getInstance().getDownloadedPackSource().cleanupAfterDisconnect();
            LOGGER.error("Failed to download resource pack from {}", p_409898_, p_287306_);
            return null;
         });
      });
   }

   private CompletableFuture<?> scheduleResourcePackDownload(RealmsJoinInformation p_407280_, UUID p_312305_) {
      try {
         if (p_407280_.resourcePackUrl() == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("resourcePackUrl was null"));
         }

         if (p_407280_.resourcePackHash() == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("resourcePackHash was null"));
         }

         DownloadedPackSource downloadedpacksource = Minecraft.getInstance().getDownloadedPackSource();
         CompletableFuture<Void> completablefuture = downloadedpacksource.waitForPackFeedback(p_312305_);
         downloadedpacksource.allowServerPacks();
         downloadedpacksource.pushPack(p_312305_, new URL(p_407280_.resourcePackUrl()), p_407280_.resourcePackHash());
         return completablefuture;
      } catch (Exception exception) {
         return CompletableFuture.failedFuture(exception);
      }
   }
}
