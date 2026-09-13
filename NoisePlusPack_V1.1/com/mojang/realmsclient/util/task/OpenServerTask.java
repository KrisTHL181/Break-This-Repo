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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class OpenServerTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.configure.world.opening");
   private final RealmsServer serverData;
   private final Screen returnScreen;
   private final boolean join;
   private final Minecraft minecraft;

   public OpenServerTask(RealmsServer p_181344_, Screen p_181345_, boolean p_181347_, Minecraft p_181348_) {
      this.serverData = p_181344_;
      this.returnScreen = p_181345_;
      this.join = p_181347_;
      this.minecraft = p_181348_;
   }

   @Override
   public void run() {
      RealmsClient realmsclient = RealmsClient.getOrCreate();

      for (int i = 0; i < 25; i++) {
         if (this.aborted()) {
            return;
         }

         try {
            boolean flag = realmsclient.open(this.serverData.id);
            if (flag) {
               this.minecraft.execute(() -> {
                  if (this.returnScreen instanceof RealmsConfigureWorldScreen realmsconfigureworldscreen) {
                     realmsconfigureworldscreen.stateChanged();
                  }

                  this.serverData.state = RealmsServer.State.OPEN;
                  if (this.join) {
                     RealmsMainScreen.play(this.serverData, this.returnScreen);
                  } else {
                     this.minecraft.setScreen(this.returnScreen);
                  }
               });
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

            LOGGER.error("Failed to open server", exception);
            this.error(exception);
         }
      }
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }
}
