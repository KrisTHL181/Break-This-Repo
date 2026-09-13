package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class OpenServerTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.configure.world.opening");
   private final RealmsServer serverData;
   private final Screen returnScreen;
   private final boolean join;
   private final Minecraft minecraft;

   public OpenServerTask(final RealmsServer realmsServer, final Screen returnScreen, final boolean join, final Minecraft minecraft) {
      this.serverData = realmsServer;
      this.returnScreen = returnScreen;
      this.join = join;
      this.minecraft = minecraft;
   }

   @Override
   public void run() {
      RealmsClient client = RealmsClient.getOrCreate();

      for (int i = 0; i < 25; i++) {
         if (this.aborted()) {
            return;
         }

         try {
            boolean openResult = client.open(this.serverData.id);
            if (openResult) {
               this.minecraft.execute(() -> {
                  if (this.returnScreen instanceof RealmsConfigureWorldScreen screen) {
                     screen.stateChanged();
                  }

                  this.serverData.state = RealmsServer.State.OPEN;
                  if (this.join) {
                     RealmsMainScreen.play(this.serverData, this.returnScreen);
                  } else {
                     this.minecraft.gui.setScreen(this.returnScreen);
                  }
               });
               break;
            }
         } catch (RetryCallException e) {
            if (this.aborted()) {
               return;
            }

            pause(e.delaySeconds);
         } catch (Exception e) {
            if (this.aborted()) {
               return;
            }

            LOGGER.error("Failed to open server", e);
            this.error(e);
         }
      }
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }
}
