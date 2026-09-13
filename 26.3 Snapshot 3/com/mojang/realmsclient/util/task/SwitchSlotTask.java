package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RetryCallException;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

public class SwitchSlotTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.minigame.world.slot.screen.title");
   private final long realmId;
   private final int slot;
   private final Runnable callback;

   public SwitchSlotTask(final long realmId, final int slot, final Runnable callback) {
      this.realmId = realmId;
      this.slot = slot;
      this.callback = callback;
   }

   @Override
   public void run() {
      RealmsClient client = RealmsClient.getOrCreate();

      for (int i = 0; i < 25; i++) {
         try {
            if (this.aborted()) {
               return;
            }

            if (client.switchSlot(this.realmId, this.slot)) {
               this.callback.run();
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

            LOGGER.error("Couldn't switch world!");
            this.error(e);
         }
      }
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }
}
