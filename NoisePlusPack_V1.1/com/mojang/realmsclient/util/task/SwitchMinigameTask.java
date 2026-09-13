package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SwitchMinigameTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.minigame.world.starting.screen.title");
   private final long realmId;
   private final WorldTemplate worldTemplate;
   private final RealmsConfigureWorldScreen nextScreen;

   public SwitchMinigameTask(long p_90451_, WorldTemplate p_90452_, RealmsConfigureWorldScreen p_409687_) {
      this.realmId = p_90451_;
      this.worldTemplate = p_90452_;
      this.nextScreen = p_409687_;
   }

   @Override
   public void run() {
      RealmsClient realmsclient = RealmsClient.getOrCreate();

      for (int i = 0; i < 25; i++) {
         try {
            if (this.aborted()) {
               return;
            }

            if (realmsclient.putIntoMinigameMode(this.realmId, this.worldTemplate.id())) {
               setScreen(this.nextScreen);
               break;
            }
         } catch (RetryCallException retrycallexception) {
            if (this.aborted()) {
               return;
            }

            pause(retrycallexception.delaySeconds);
         } catch (Exception exception) {
            if (this.aborted()) {
               return;
            }

            LOGGER.error("Couldn't start mini game!");
            this.error(exception);
         }
      }
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }
}
