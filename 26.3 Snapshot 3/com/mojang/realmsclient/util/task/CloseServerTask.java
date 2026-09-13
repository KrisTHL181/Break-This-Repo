package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class CloseServerTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.configure.world.closing");
   private final RealmsServer serverData;
   private final RealmsConfigureWorldScreen configureScreen;

   public CloseServerTask(final RealmsServer realmsServer, final RealmsConfigureWorldScreen configureWorldScreen) {
      this.serverData = realmsServer;
      this.configureScreen = configureWorldScreen;
   }

   @Override
   public void run() {
      RealmsClient client = RealmsClient.getOrCreate();

      for (int i = 0; i < 25; i++) {
         if (this.aborted()) {
            return;
         }

         try {
            boolean closeResult = client.close(this.serverData.id);
            if (closeResult) {
               this.configureScreen.stateChanged();
               this.serverData.state = RealmsServer.State.CLOSED;
               setScreen(this.configureScreen);
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

            LOGGER.error("Failed to close server", e);
            this.error(e);
         }
      }
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }
}
