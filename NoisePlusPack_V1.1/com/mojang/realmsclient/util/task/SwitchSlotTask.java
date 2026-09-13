package com.mojang.realmsclient.util.task;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.exception.RetryCallException;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SwitchSlotTask extends LongRunningTask {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Component TITLE = Component.translatable("mco.minigame.world.slot.screen.title");
   private final long realmId;
   private final int slot;
   private final Runnable callback;

   public SwitchSlotTask(long p_90459_, int p_90460_, Runnable p_90461_) {
      this.realmId = p_90459_;
      this.slot = p_90460_;
      this.callback = p_90461_;
   }

   @Override
   public void run() {
      RealmsClient realmsclient = RealmsClient.getOrCreate();

      for (int i = 0; i < 25; i++) {
         try {
            if (this.aborted()) {
               return;
            }

            if (realmsclient.switchSlot(this.realmId, this.slot)) {
               this.callback.run();
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

            LOGGER.error("Couldn't switch world!");
            this.error(exception);
         }
      }
   }

   @Override
   public Component getTitle() {
      return TITLE;
   }
}
