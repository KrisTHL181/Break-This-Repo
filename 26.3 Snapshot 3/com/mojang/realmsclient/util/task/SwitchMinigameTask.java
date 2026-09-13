package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class SwitchMinigameTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.minigame.world.starting.screen.title");
   private final long realmId;
   private final WorldTemplate worldTemplate;
   private final RealmsConfigureWorldScreen nextScreen;

   public SwitchMinigameTask(final long realmId, final WorldTemplate worldTemplate, final RealmsConfigureWorldScreen nextScreen) {
      this.realmId = realmId;
      this.worldTemplate = worldTemplate;
      this.nextScreen = nextScreen;
   }

   @Override
   public void run() {
      RealmsClient client = RealmsClient.getOrCreate();

      for (int i = 0; i < 25; i++) {
         try {
            if (this.aborted()) {
               return;
            }

            if (client.putIntoMinigameMode(this.realmId, this.worldTemplate.id())) {
               setScreen(this.nextScreen);
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

            LOGGER.error("Couldn't start mini game!");
            this.error(e);
         }
      }
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }
}
